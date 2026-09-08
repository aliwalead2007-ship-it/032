package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QabasCrashGuard {
    private const val TAG = "QabasCrashGuard"
    private const val MAX_LOG_FILES = 20
    private const val RECOVERY_WINDOW_MS = 15_000L
    private const val PREF_LAST_TS = "crash_guard_last_ts"
    private const val PREF_COUNT = "crash_guard_count"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val appContext = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            var relaunched = false
            try {
                val message = record(appContext, thread, throwable)
                if (thread.name == "main") {
                    val prefs = appContext.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                    val now = System.currentTimeMillis()
                    val lastTs = prefs.getLong(PREF_LAST_TS, 0L)
                    prefs.edit()
                        .putLong(PREF_LAST_TS, now)
                        .putLong(PREF_COUNT, prefs.getLong(PREF_COUNT, 0L) + 1)
                        .apply()
                    if (now - lastTs > RECOVERY_WINDOW_MS) {
                        val intent = appContext.packageManager
                            .getLaunchIntentForPackage(appContext.packageName)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        if (intent != null) {
                            appContext.startActivity(intent)
                            relaunched = true
                        }
                    }
                    try {
                        SystemLogsManager.addLog(
                            "خطأ",
                            "انهيار تم التعافي منه: $message",
                            Color(0xFFF44336)
                        )
                    } catch (ignored: Throwable) {
                    }
                }
            } catch (ignored: Throwable) {
            }
            if (!relaunched) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun record(context: Context, thread: Thread, throwable: Throwable): String {
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(now)
        val display = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now)
        val stack = StringWriter().also { sw ->
            PrintWriter(sw).use { pw ->
                pw.println("════════════════════════════════════════════")
                pw.println("Qabas Crash Log — $display")
                pw.println("Thread: ${thread.name}")
                throwable.printStackTrace(pw)
            }
        }
        try {
            val dir = File(context.filesDir, "crash_logs").apply { mkdirs() }
            File(dir, "crash_$stamp.txt").writeText(stack.toString())
            val existing = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (existing.size > MAX_LOG_FILES) {
                existing.drop(MAX_LOG_FILES).forEach { it.delete() }
            }
        } catch (ignored: Throwable) {
        }
        val root = throwable.message ?: throwable.javaClass.simpleName
        return root.take(120)
    }
}