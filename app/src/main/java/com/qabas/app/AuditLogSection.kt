package com.qabas.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*

/**
 * سجل التدقيق: يستعرض أثر الإجراءات الحساسة (المحفوظ محلياً + المدفوع سحابياً).
 */
@Composable
fun AuditLogSection(context: Context) {
    var entries by remember { mutableStateOf(AuditLogger.readLocal(context)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("سجل التدقيق (Audit Log)", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("كل إجراء حساس موثّق محلياً + سحابياً", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { entries = AuditLogger.readLocal(context) },
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
            ) {
                Text("تحديث", color = GoldPrimary, fontFamily = CairoFont, fontSize = 12.sp)
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "لا يوجد أثر تدقيق بعد — نفّذ إجراءً حساساً (رتبة/إيقاف/كود/بث) ليُسجَّل.",
                    color = TextSecondary, fontFamily = NotoSansFont, fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { e ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(e.action, color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(AuditLogger.formatTime(e.timeMs), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.sp)
                            }
                            Text(e.detail, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                            Text("بواسطة: ${e.actor}", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
