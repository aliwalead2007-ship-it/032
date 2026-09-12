package com.qabas.app

import android.content.Context
import android.util.Log

/**
 * حارس الوصول للوحة المطور (AdminGuard):
 * 1) الوصول المحلي «الراثي»: العلم `is_developer` يُحترَم فقط إذا كان مكتوباً فعلياً
 *    في التفضيلات (يساوي true) — لا يُمنح افتراضياً لأي جهاز جديد (إغلاق الثغرة).
 * 2) الوصول الحاسم: بريد مستخدم Firebase Auth الحالي == بريد المالك.
 * 3) يوفر هوية الجالس للاستخدام في سجل التدقيق.
 */
object AdminGuard {
    private const val TAG = "AdminGuard"

    fun isFirebaseUserOwner(): Boolean {
        return try {
            if (!CloudServices.isFirebaseInitialized) false
            else com.google.firebase.auth.FirebaseAuth.getInstance()
                .currentUser?.email.equals(CloudServices.OWNER_EMAIL, ignoreCase = true)
        } catch (e: Exception) {
            Log.w(TAG, "isFirebaseUserOwner failed: ${e.message}")
            false
        }
    }

    /** العلم المحلي يُمنح فقط لمن كُتب له صراحةً — الجديد لا يحصل عليه. */
    private fun localDeveloperFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
        return prefs.contains("is_developer") && prefs.getBoolean("is_developer", false)
    }

    fun isDashboardAccessAllowed(context: Context): Boolean =
        isFirebaseUserOwner() || localDeveloperFlag(context)

    fun currentIdentity(context: Context): String {
        val fb = try {
            if (CloudServices.isFirebaseInitialized) {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            } else null
        } catch (e: Exception) { null }
        return fb ?: run {
            val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
            prefs.getString("dev_display_name", null) ?: "local_device"
        }
    }
}
