package com.qabas.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سجل التدقيق (Audit Log): يوثّق كل إجراء حساس في لوحة المطور.
 * - سحابياً: Firestore collection `audit_log` (auto-id مستند).
 * - محلياً: كاش JSON في qabas_prefs (يعمل دون اتصال ويُقرأ في قسم جديد باللوحة).
 */
object AuditLogger {
    private const val TAG = "AuditLogger"
    private const val PREFS_KEY = "audit_log_entries"
    private const val MAX_LOCAL_ENTRIES = 100

    data class AuditEntry(
        val id: String,
        val action: String,
        val detail: String,
        val actor: String,
        val timeMs: Long
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(context: Context, action: String, detail: String) {
        val entry = AuditEntry(
            id = java.util.UUID.randomUUID().toString(),
            action = action,
            detail = detail,
            actor = AdminGuard.currentIdentity(context),
            timeMs = System.currentTimeMillis()
        )
        appendLocal(context, entry)
        try {
            if (CloudServices.isFirebaseInitialized) {
                scope.launch {
                    runCatching {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("audit_log")
                            .add(
                                mapOf(
                                    "action" to entry.action,
                                    "detail" to entry.detail,
                                    "actor" to entry.actor,
                                    "timestamp" to entry.timeMs,
                                    "app" to "qabas_dev_dashboard"
                                )
                            ).await()
                    }.onFailure { Log.w(TAG, "cloud audit failed: ${it.message}") }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "audit log failed: ${e.message}")
        }
        Log.d(TAG, "[audit] $action :: $detail :: by ${entry.actor}")
    }

    private fun appendLocal(context: Context, entry: AuditEntry) {
        runCatching {
            val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(PREFS_KEY, "[]") ?: "[]")
            arr.put(
                org.json.JSONObject()
                    .put("id", entry.id)
                    .put("action", entry.action)
                    .put("detail", entry.detail)
                    .put("actor", entry.actor)
                    .put("timeMs", entry.timeMs)
            )
            while (arr.length() > MAX_LOCAL_ENTRIES) arr.remove(0)
            prefs.edit().putString(PREFS_KEY, arr.toString()).apply()
        }
    }

    fun readLocal(context: Context): List<AuditEntry> {
        return runCatching {
            val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(PREFS_KEY, "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AuditEntry(
                    id = o.optString("id", ""),
                    action = o.optString("action", ""),
                    detail = o.optString("detail", ""),
                    actor = o.optString("actor", ""),
                    timeMs = o.optLong("timeMs", 0L)
                )
            }.sortedByDescending { it.timeMs }
        }.getOrDefault(emptyList())
    }

    fun formatTime(timeMs: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(timeMs))
}
