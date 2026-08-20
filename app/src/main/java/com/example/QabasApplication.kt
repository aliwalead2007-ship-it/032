package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

class QabasApplication : Application() {

    var firebaseAnalytics: FirebaseAnalytics? = null
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase & Analytics with respect to user settings
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(this)
                
                // Respect user analytics preference from SharedPreferences
                val prefs = getSharedPreferences("qabas_prefs", MODE_PRIVATE)
                val isAnalyticsEnabled = prefs.getBoolean("analytics_enabled", true)
                firebaseAnalytics?.setAnalyticsCollectionEnabled(isAnalyticsEnabled)
                Log.d("QabasApplication", "Firebase Analytics initialized with enabled state: $isAnalyticsEnabled")
            }
        } catch (t: Throwable) {
            Log.w("QabasApplication", "Firebase Analytics initialization deferred: ${t.message}")
        }

        // Initialize core application services
        AppServices.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            VideoCacheService.release()
        } catch (e: Exception) {
            Log.w("QabasApplication", "Error releasing VideoCacheService on terminate: ${e.message}")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            VideoCacheService.release()
        } catch (e: Exception) {
            Log.w("QabasApplication", "Error releasing VideoCacheService on low memory: ${e.message}")
        }
    }
}
