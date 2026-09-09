package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
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

/**
 * مدير الهدايا والأكواد.
 *
 * مصدر الحقيقة الوحيد للأكواد الصالحة هو Firestore (مجموعة promo_codes).
 * الأكواد المخصصة التي يضيفها المطوّر من لوحة الإدارة تُكتب في Firestore أولاً،
 * ثم تُخزن نسخة محلية مختصرة في SharedPreferences لتجنّب رحلات شبكة إضافية
 * عند الاسترداد المتكرر. كما يُحمَّل الكاش السحابي عند الإقلاع.
 */
class GiftManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("gift_manager_prefs", Context.MODE_PRIVATE)
    private val accountService = AppServices.getAccountService(context)

    // كاش الأكواد الصالحة المحمَّل من Firestore مرة واحدة. يُحدَّث بعد كل كتابة جديدة.
    @Volatile
    private var validPromoCodes: Map<String, Long> = emptyMap()
    @Volatile
    private var validGiftCards: Map<String, Int> = emptyMap()

    init {
        // تحميل أولي متزامن عند الإقلاع لتفادي سباق بين init والقراءة.
        runCatching {
            runBlocking {
                loadValidCodesFromCloud()
            }
        }.onFailure {
            Log.e(TAG, "Failed initial cloud load: ${it.message}", it)
        }
    }

    /**
     * يجلب الأكواد الصالحة من Firestore ويحدّث الكاش المحلي.
     * يستدعى عند الإقلاع (داخل init) وبعد كل عملية إضافة.
     */
    suspend fun loadValidCodesFromCloud() {
        val (promos, gifts) = CloudServices.Database.fetchValidCodesCache()
        validPromoCodes = promos
        validGiftCards = gifts
        Log.d(TAG, "Loaded ${promos.size} promo codes and ${gifts.size} gift cards from Firestore.")
    }

    private companion object {
        private const val TAG = "GiftManager"
    }

    /**
     * إضافة كود ترقية جديد.
     * - يكتب في Firestore أولاً مع await (لا fire-and-forget).
     * - يُحدّث الكاش المحلي عند النجاح.
     * - يحتفظ بنسخة محلية مختصرة في SharedPreferences كاحتياط.
     * - يُرجع true عند النجاح، false عند فشل الكتابة السحابية.
     */
    suspend fun addCustomPromoCode(code: String, durationDays: Int): Boolean {
        val cleanCode = code.trim().uppercase(Locale.US)
        val saved = CloudServices.Database.savePromoCodeToCloud(cleanCode, "PROMO", durationDays.toLong())
        if (!saved) return false
        // تحديث الكاش المحلي فوراً لإمكانية الاسترداد دون انتظار التحميل الكامل
        validPromoCodes = validPromoCodes + (cleanCode to durationDays.toLong() * 24L * 60L * 60L * 1000L)
        // نسخة احتياطية محلية
        val customPromos = prefs.getStringSet("custom_promos", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        customPromos.add("$cleanCode:$durationDays")
        prefs.edit().putStringSet("custom_promos", customPromos).apply()
        Log.d(TAG, "Promo code ($cleanCode) saved to Firestore successfully.")
        return true
    }

    /**
     * إضافة بطاقة هدايا جديدة.
     * - يكتب في Firestore أولاً مع await (لا fire-and-forget).
     * - يُحدّث الكاش المحلي عند النجاح.
     * - يُرجع true عند النجاح، false عند فشل الكتابة السحابية.
     */
    suspend fun addCustomGiftCard(code: String, points: Int): Boolean {
        val cleanCode = code.trim().uppercase(Locale.US)
        val saved = CloudServices.Database.savePromoCodeToCloud(cleanCode, "GIFT", points.toLong())
        if (!saved) return false
        // تحديث الكاش المحلي فوراً
        validGiftCards = validGiftCards + (cleanCode to points)
        // نسخة احتياطية محلية
        val customGifts = prefs.getStringSet("custom_gifts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        customGifts.add("$cleanCode:$points")
        prefs.edit().putStringSet("custom_gifts", customGifts).apply()
        Log.d(TAG, "Gift card ($cleanCode) saved to Firestore successfully.")
        return true
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

    /**
     * استرداد كود.
     * المصدر الوحيد للأكواد الصالحة هو Firestore (الكاش المحلي) مدمجاً مع الأكواد
     * المخصصة المحفوظة محلياً كاحتياط في حال عدم توفر اتصال.
     */
    fun redeemCode(code: String): RedemptionResult {
        val cleanCode = code.trim().uppercase(Locale.US)

        // التحقق مما إذا كان الكود مستخدماً من قبل
        val usedCodes = prefs.getStringSet("used_codes", mutableSetOf()) ?: mutableSetOf()
        if (usedCodes.contains(cleanCode)) {
            return RedemptionResult.ALREADY_USED
        }

        // 1. التحقق من أكواد الترقية (Promo Codes) — الكاش السحابي + المحلي الاحتياطي
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

        // 2. التحقق من بطاقات الهدايا (Gift Cards) — الكاش السحابي + المحلي الاحتياطي
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
