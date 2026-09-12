package com.qabas.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

interface AnalyticsService {
    fun logScreenView(screenName: String)
    fun logEvent(eventName: String, parameters: Map<String, Any>?)
    fun logProjectCreated(projectId: String, template: String, duration: String)
    fun logVideoGenerated(projectId: String, cost: Double, libraryCount: Int, generatedCount: Int)
    fun logPaymentCompleted(projectId: String, amount: Double)
    fun logError(errorMessage: String, stackTrace: String)
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}

class RealAnalyticsService(context: Context) : AnalyticsService {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)

    private val firebaseAnalytics: FirebaseAnalytics? = try {
        FirebaseAnalytics.getInstance(appContext).also {
            val isEnabled = prefs.getBoolean("analytics_enabled", true)
            it.setAnalyticsCollectionEnabled(isEnabled)
        }
    } catch (e: Exception) {
        Log.w("Analytics", "FirebaseAnalytics initialization fallback: ${e.message}")
        null
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean("analytics_enabled", enabled).apply()
            firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            Log.w("Analytics", "Failed to set analytics enabled state: ${e.message}")
        }
        Log.d("Analytics", "Analytics collection enabled: $enabled")
    }

    override fun logScreenView(screenName: String) {
        val isEnabled = prefs.getBoolean("analytics_enabled", true)
        if (!isEnabled) {
            Log.d("Analytics", "Screen View skipped (Analytics disabled by user): $screenName")
            return
        }
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (e: Exception) {
            Log.w("Analytics", "Failed to log screen view to Firebase: ${e.message}")
        }
        Log.d("Analytics", "Screen View: $screenName")
    }

    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        val isEnabled = prefs.getBoolean("analytics_enabled", true)
        if (!isEnabled) {
            Log.d("Analytics", "Event skipped (Analytics disabled by user): $eventName")
            return
        }
        try {
            val bundle = Bundle().apply {
                parameters?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Float -> putFloat(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
        } catch (e: Exception) {
            Log.w("Analytics", "Failed to log event to Firebase: ${e.message}")
        }
        Log.d("Analytics", "Event: $eventName, Parameters: $parameters")
    }

    override fun logProjectCreated(projectId: String, template: String, duration: String) {
        val params = mapOf(
            "project_id" to projectId,
            "template_id" to template,
            "target_duration" to duration
        )
        logEvent("project_created", params)
    }

    override fun logVideoGenerated(projectId: String, cost: Double, libraryCount: Int, generatedCount: Int) {
        val params = mapOf(
            "project_id" to projectId,
            "cost" to cost,
            "library_scenes" to libraryCount,
            "generated_scenes" to generatedCount
        )
        logEvent("video_generated", params)
    }

    override fun logPaymentCompleted(projectId: String, amount: Double) {
        val params = mapOf(
            "project_id" to projectId,
            "amount" to amount,
            "currency" to "USD"
        )
        logEvent(FirebaseAnalytics.Event.PURCHASE, params)
    }

    override fun logError(errorMessage: String, stackTrace: String) {
        val params = mapOf(
            "error_message" to errorMessage,
            "stack_trace" to stackTrace.take(100)
        )
        logEvent("app_error", params)
    }
}

