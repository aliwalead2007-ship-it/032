package com.qabas.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object ProductionPipelineTracker {

    private const val PREFS_NAME = "qabas_pipeline_events"
    private const val KEY_EVENTS = "pipeline_events"
    private const val MAX_EVENTS = 200

    enum class Stage(val label: String) {
        IDEA_INPUT("إدخال الفكرة"),
        CONTENT_GUARD("فحص المحتوى"),
        STYLE_SELECTION("اختيار الأسلوب"),
        SCRIPT_GENERATION("توليد السكربت"),
        SCENE_PROCESSING("معالجة المشاهد"),
        BROLL_FETCH("جلب B-Roll"),
        TTS_GENERATION("توليد الصوت"),
        FFmpeg_MERGE("دمج FFmpeg"),
        TEXT_OVERLAY("طبقة النص"),
        COLOR_GRADING("التلوين السينمائي"),
        VIDEO_ENGINE("محرك الفيديو"),
        EXPORT("التصدير النهائي"),
        STYLE_FEEDBACK("تغذية الأسلوب")
    }

    enum class Result { SUCCESS, FAILURE, FALLBACK, SKIPPED, TIMEOUT }

    data class PipelineEvent(
        val timestamp: Long,
        val stage: Stage,
        val result: Result,
        val message: String,
        val detail: String = "",
        val durationMs: Long = 0
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("ts", timestamp)
            put("stage", stage.name)
            put("result", result.name)
            put("msg", message)
            put("detail", detail)
            put("dur", durationMs)
        }

        companion object {
            fun fromJson(j: JSONObject) = PipelineEvent(
                timestamp = j.optLong("ts", 0),
                stage = try { Stage.valueOf(j.optString("stage", "")) } catch (_: Exception) { Stage.VIDEO_ENGINE },
                result = try { Result.valueOf(j.optString("result", "")) } catch (_: Exception) { Result.FAILURE },
                message = j.optString("msg", ""),
                detail = j.optString("detail", ""),
                durationMs = j.optLong("dur", 0)
            )
        }
    }

    fun record(
        context: Context,
        stage: Stage,
        result: Result,
        message: String,
        detail: String = "",
        durationMs: Long = 0
    ) {
        val event = PipelineEvent(System.currentTimeMillis(), stage, result, message, detail, durationMs)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString(KEY_EVENTS, "[]") ?: "[]")
        } catch (_: Exception) { JSONArray() }

        arr.put(event.toJson())

        while (arr.length() > MAX_EVENTS) {
            arr.remove(0)
        }

        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
    }

    fun getEvents(context: Context): List<PipelineEvent> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString(KEY_EVENTS, "[]") ?: "[]")
        } catch (_: Exception) { JSONArray() }

        return (0 until arr.length()).mapNotNull { i ->
            try { PipelineEvent.fromJson(arr.getJSONObject(i)) } catch (_: Exception) { null }
        }.sortedByDescending { it.timestamp }
    }

    fun getStageStats(context: Context): Map<Stage, Map<Result, Int>> {
        val events = getEvents(context)
        val stats = mutableMapOf<Stage, MutableMap<Result, Int>>()
        for (e in events) {
            stats.getOrPut(e.stage) { mutableMapOf() }[e.result] =
                (stats[e.stage]?.get(e.result) ?: 0) + 1
        }
        return stats
    }

    fun clearEvents(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_EVENTS).apply()
    }

    fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    fun resultEmoji(r: Result): String = when (r) {
        Result.SUCCESS -> "✅"
        Result.FAILURE -> "❌"
        Result.FALLBACK -> "⚠️"
        Result.SKIPPED -> "⏭️"
        Result.TIMEOUT -> "⏱️"
    }

    fun resultColor(r: Result): Long = when (r) {
        Result.SUCCESS -> 0xFF10B981
        Result.FAILURE -> 0xFFEF4444
        Result.FALLBACK -> 0xFFF59E0B
        Result.SKIPPED -> 0xFF6B7280
        Result.TIMEOUT -> 0xFF8B5CF6
    }
}
