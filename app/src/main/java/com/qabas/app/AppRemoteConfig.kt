package com.qabas.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * إعدادات التحكم البعيد (Remote Config) — قلب «لوحة التحكم الكاملة».
 *
 * - المصدر الحاسم: مستند Firestore `app_config/config` إن وُجد Firebase.
 *   (قابل للتوسيع لاحقاً إلى جدول Supabase app_config بنفس المفاتيح.)
 * - كاش محلي: `qabas_prefs` (يعمل دون اتصال ولا يكسر البناء بلا سحابة).
 * - أي زر في شاشة المطور يكتب سحابياً ثم يحدّث الكاش حتى تتأثر كل الأجهزة.
 */
object AppRemoteConfig {
    private const val TAG = "AppRemoteConfig"

    const val COLLECTION = "app_config"
    const val DOC_ID = "config"

    const val KEY_MAINTENANCE = "sys_maintenance_mode"
    const val KEY_ACCEPT_REQUESTS = "sys_accept_requests"
    const val KEY_AUTO_AI_REPLY = "sys_auto_ai_reply"
    const val KEY_MAINTENANCE_MESSAGE = "sys_maintenance_message"
    const val KEY_MIN_VERSION = "sys_min_version_code"
    const val KEY_LAST_SYNC = "sys_config_last_sync"

    /** لقطة إعدادات موحّدة تُقرأ من السحابة أو الكاش أو الافتراضيات. */
    data class ConfigData(
        val maintenanceMode: Boolean = false,
        val acceptRequests: Boolean = true,
        val autoAiReply: Boolean = true,
        val maintenanceMessage: String = DEFAULT_MESSAGE,
        val minVersionCode: Int = 0,
        val lastSyncMs: Long = 0L
    ) {
        companion object {
            const val DEFAULT_MESSAGE = "الخدمة متوقفة مؤقتاً للتحديث والصيانة، يرجى المحاولة لاحقاً."
        }
    }

    /** كاش في الذاكرة للقراءة السريعة بعد أول تحديث سحابي. */
    @Volatile
    private var cached: ConfigData? = null

    fun readLocal(context: Context): ConfigData {
        val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
        return ConfigData(
            maintenanceMode = prefs.getBoolean(KEY_MAINTENANCE, false),
            acceptRequests = prefs.getBoolean(KEY_ACCEPT_REQUESTS, true),
            autoAiReply = prefs.getBoolean(KEY_AUTO_AI_REPLY, true),
            maintenanceMessage = prefs.getString(KEY_MAINTENANCE_MESSAGE, ConfigData.DEFAULT_MESSAGE)
                ?: ConfigData.DEFAULT_MESSAGE,
            minVersionCode = prefs.getInt(KEY_MIN_VERSION, 0),
            lastSyncMs = prefs.getLong(KEY_LAST_SYNC, 0L)
        )
    }

    fun current(context: Context): ConfigData = cached ?: readLocal(context)

    private fun writeLocal(context: Context, data: ConfigData) {
        val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_MAINTENANCE, data.maintenanceMode)
            .putBoolean(KEY_ACCEPT_REQUESTS, data.acceptRequests)
            .putBoolean(KEY_AUTO_AI_REPLY, data.autoAiReply)
            .putString(KEY_MAINTENANCE_MESSAGE, data.maintenanceMessage)
            .putInt(KEY_MIN_VERSION, data.minVersionCode)
            .putLong(KEY_LAST_SYNC, data.lastSyncMs)
            .apply()
        cached = data
    }

    private fun db() = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    /** جلب الإعدادات من السحابة وتحديث الكاش المحلي. يُرجع true عند النجاح. */
    suspend fun refreshFromCloud(context: Context): Boolean {
        if (!CloudServices.isFirebaseInitialized) return false
        return withContext(Dispatchers.IO) {
            try {
                val snap = db().collection(COLLECTION).document(DOC_ID).get().await()
                if (snap.exists()) {
                    val data = ConfigData(
                        maintenanceMode = snap.getBoolean(KEY_MAINTENANCE) ?: false,
                        acceptRequests = snap.getBoolean(KEY_ACCEPT_REQUESTS) ?: true,
                        autoAiReply = snap.getBoolean(KEY_AUTO_AI_REPLY) ?: true,
                        maintenanceMessage = snap.getString(KEY_MAINTENANCE_MESSAGE)
                            ?: ConfigData.DEFAULT_MESSAGE,
                        minVersionCode = (snap.getLong(KEY_MIN_VERSION) ?: 0L).toInt(),
                        lastSyncMs = snap.getLong(KEY_LAST_SYNC) ?: System.currentTimeMillis()
                    )
                    writeLocal(context, data)
                    Log.d(TAG, "Config loaded from cloud: $data")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "refreshFromCloud failed: ${e.message}")
                false
            }
        }
    }

    /** دفع الإعدادات إلى السحابة (Firestore) بعد تطبيقها محلياً. يُرجع true عند النجاح. */
    suspend fun pushToCloud(context: Context, data: ConfigData): Boolean {
        writeLocal(context, data)
        if (!CloudServices.isFirebaseInitialized) return false
        return withContext(Dispatchers.IO) {
            try {
                val doc = hashMapOf<String, Any>(
                    KEY_MAINTENANCE to data.maintenanceMode,
                    KEY_ACCEPT_REQUESTS to data.acceptRequests,
                    KEY_AUTO_AI_REPLY to data.autoAiReply,
                    KEY_MAINTENANCE_MESSAGE to data.maintenanceMessage,
                    KEY_MIN_VERSION to data.minVersionCode,
                    KEY_LAST_SYNC to System.currentTimeMillis(),
                    "updatedBy" to AdminGuard.currentIdentity(context)
                )
                db().collection(COLLECTION).document(DOC_ID)
                    .set(doc, com.google.firebase.firestore.SetOptions.merge()).await()
                Log.d(TAG, "Config pushed to cloud")
                true
            } catch (e: Exception) {
                Log.e(TAG, "pushToCloud failed: ${e.message}", e)
                false
            }
        }
    }
}
