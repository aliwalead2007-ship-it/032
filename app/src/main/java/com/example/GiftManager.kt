package com.example

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RedemptionResult {
    SUCCESS_PROMO,
    SUCCESS_GIFT_CARD,
    INVALID_CODE,
    ALREADY_USED,
    EXPIRED
}

class GiftManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("gift_manager_prefs", Context.MODE_PRIVATE)
    private val accountService = AppServices.getAccountService(context)

    // قاعدة بيانات وهمية للأكواد (في الواقع يجب التحقق منها عبر Firebase)
    private val validPromoCodes = mapOf(
        "QABAS-PRO-30" to 30L * 24 * 60 * 60 * 1000, // 30 يوماً
        "QABAS-PRO-7" to 7L * 24 * 60 * 60 * 1000,   // 7 أيام
        "WELCOME-24" to 24L * 60 * 60 * 1000         // يوم واحد
    )

    private val validGiftCards = mapOf(
        "GIFT-1000" to 1000, // 1000 نقطة ذهبية
        "GIFT-500" to 500,
        "CREATOR-10K" to 10000
    )

    
    fun addCustomPromoCode(code: String, durationDays: Int) {
        val cleanCode = code.trim().uppercase(Locale.US)
        val customPromos = prefs.getStringSet("custom_promos", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        customPromos.add("$cleanCode:$durationDays")
        prefs.edit().putStringSet("custom_promos", customPromos).apply()
        CloudServices.Database.savePromoCodeToCloud(cleanCode, "PROMO", durationDays.toLong())
    }

    fun addCustomGiftCard(code: String, points: Int) {
        val cleanCode = code.trim().uppercase(Locale.US)
        val customGifts = prefs.getStringSet("custom_gifts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        customGifts.add("$cleanCode:$points")
        prefs.edit().putStringSet("custom_gifts", customGifts).apply()
        CloudServices.Database.savePromoCodeToCloud(cleanCode, "GIFT", points.toLong())
    }

    fun getCustomPromoCodes(): Map<String, Long> {
        val customPromos = prefs.getStringSet("custom_promos", mutableSetOf()) ?: mutableSetOf()
        val map = mutableMapOf<String, Long>()
        for (item in customPromos) {
            val parts = item.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1].toLongOrNull()?.times(24 * 60 * 60 * 1000L) ?: 0L
            }
        }
        return map
    }

    fun getCustomGiftCards(): Map<String, Int> {
        val customGifts = prefs.getStringSet("custom_gifts", mutableSetOf()) ?: mutableSetOf()
        val map = mutableMapOf<String, Int>()
        for (item in customGifts) {
            val parts = item.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1].toIntOrNull() ?: 0
            }
        }
        return map
    }

    val activePromoExpiryTime: Long
        get() = prefs.getLong("promo_expiry_time", 0L)

    fun hasActivePromoUpgrade(): Boolean {
        val currentTime = System.currentTimeMillis()
        val expiryTime = activePromoExpiryTime
        
        if (expiryTime > currentTime) {
            return true
        } else if (expiryTime in 1..<currentTime) {
            // انتهت صلاحية العرض، قم بتصفيره
            prefs.edit().putLong("promo_expiry_time", 0L).apply()
        }
        return false
    }

    fun getPromoExpiryDateString(): String? {
        if (!hasActivePromoUpgrade()) return null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date(activePromoExpiryTime))
    }

    fun redeemCode(code: String): RedemptionResult {
        val cleanCode = code.trim().uppercase(Locale.US)
        
        // التحقق مما إذا كان الكود مستخدماً من قبل
        val usedCodes = prefs.getStringSet("used_codes", mutableSetOf()) ?: mutableSetOf()
        if (usedCodes.contains(cleanCode)) {
            return RedemptionResult.ALREADY_USED
        }

        // 1. التحقق من أكواد الترقية (Promo Codes)
        val allPromoCodes = validPromoCodes + getCustomPromoCodes()
        val durationMs = allPromoCodes[cleanCode]
        if (durationMs != null) {
            // إذا كان لديه عرض فعال، قم بتمديده، وإلا ابدأ من اليوم
            val currentExpiry = if (hasActivePromoUpgrade()) activePromoExpiryTime else System.currentTimeMillis()
            val newExpiry = currentExpiry + durationMs
            
            // حفظ التحديث
            val newUsedCodes = mutableSetOf<String>().apply { 
                addAll(usedCodes)
                add(cleanCode) 
            }
            
            prefs.edit()
                .putLong("promo_expiry_time", newExpiry)
                .putStringSet("used_codes", newUsedCodes)
                .apply()
                
            return RedemptionResult.SUCCESS_PROMO
        }

        // 2. التحقق من بطاقات الهدايا (Gift Cards)
        val allGiftCards = validGiftCards + getCustomGiftCards()
        val points = allGiftCards[cleanCode]
        if (points != null) {
            accountService.walletBalance += points
            
            val newUsedCodes = mutableSetOf<String>().apply { 
                addAll(usedCodes)
                add(cleanCode) 
            }
            
            prefs.edit()
                .putStringSet("used_codes", newUsedCodes)
                .apply()
                
            return RedemptionResult.SUCCESS_GIFT_CARD
        }

        return RedemptionResult.INVALID_CODE
    }
}
