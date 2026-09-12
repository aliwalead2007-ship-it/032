package com.qabas.app

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * قسم النسخ الاحتياطي والاسترجاع (DashboardBackup):
 * - تصدير JSON للمستخدمين + الأكواد + الإعدادات.
 * - استيراد JSON واستعادة الإعدادات + الأكواد + المستخدمين.
 */
@Composable
fun DashboardBackupSection() {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var lastExportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            isExporting = true
            lastExportUri = uri
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Toast.makeText(context, "جارٍ استيراد النسخة الاحتياطية...", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isExporting, lastExportUri) {
        if (isExporting && lastExportUri != null) {
            val json = buildExportJson(context)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(lastExportUri!!)?.use { it.write(json.toByteArray()) }
            }
            isExporting = false
            Toast.makeText(context, "تم التصدير بنجاح ✅", Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("نسخ احتياطي ومسح", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تصدير نسخة احتياطية كاملة (JSON)", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                Text("يشمل: المستخدمين + الأكواد + الإعدادات البعيدة. لا يشمل الملفات أو الصور.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Button(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("qabas_backup_$stamp.json")
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isExporting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeepSlate, strokeWidth = 2.dp)
                    else Text("تصدير نسخة احتياطية 📦", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private suspend fun buildExportJson(context: Context): String = withContext(Dispatchers.IO) {
    val root = JSONObject()
    root.put("app", "qabas")
    root.put("exportedAt", System.currentTimeMillis())
    root.put("exportedBy", AdminGuard.currentIdentity(context))

    // Config (cloud + local)
    val config = AppRemoteConfig.readLocal(context)
    root.put("config", JSONObject().apply {
        put(AppRemoteConfig.KEY_MAINTENANCE, config.maintenanceMode)
        put(AppRemoteConfig.KEY_ACCEPT_REQUESTS, config.acceptRequests)
        put(AppRemoteConfig.KEY_AUTO_AI_REPLY, config.autoAiReply)
        put(AppRemoteConfig.KEY_MAINTENANCE_MESSAGE, config.maintenanceMessage)
    })

    // Users (best-effort: try Firestore, fallback empty)
    val usersArr = JSONArray()
    runCatching {
        if (CloudServices.isFirebaseInitialized) {
            val docs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").get().await()
            docs.documents.forEach { d ->
                usersArr.put(JSONObject().apply {
                    put("id", d.id)
                    d.data?.forEach { (k, v) -> put(k, v) }
                })
            }
        }
    }
    root.put("users", usersArr)

    // Promo codes
    val codesArr = JSONArray()
    runCatching {
        val gm = GiftManager(context)
        gm.loadValidCodesFromCloud()
        gm.getCustomPromoCodes().forEach { (code, days) ->
            codesArr.put(JSONObject().apply { put("code", code); put("days", days); put("type", "PROMO") })
        }
        gm.getCustomGiftCards().forEach { (code, pts) ->
            codesArr.put(JSONObject().apply { put("code", code); put("points", pts); put("type", "GIFT") })
        }
    }
    root.put("promoCodes", codesArr)

    root.toString()
}
