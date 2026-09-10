package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * نظام المكافآت والدوري الأسبوعي
 */
object LeagueService {
    private const val TAG = "LeagueService"

    // فحص وتحديث حالة دوري المتصدرين
    suspend fun checkAndProcessWeeklyLeague(context: Context) {
        if (!CloudServices.isFirebaseInitialized) return

        try {
            val db = FirebaseFirestore.getInstance()
            val leagueDocRef = db.collection("system").document("league_info")
            
            // 1. جلب معلومات الدوري الحالي
            val leagueDoc = leagueDocRef.get().await()
            val now = Date()
            
            var leagueEndDate = leagueDoc.getDate("end_date")
            
            // إذا لم يكن هناك دوري مسبق (أول مرة يعمل فيها النظام)، نقوم بإنشائه
            if (leagueEndDate == null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 7) // ينتهي بعد 7 أيام
                leagueEndDate = calendar.time
                
                leagueDocRef.set(mapOf("end_date" to leagueEndDate)).await()
                return
            }

            // 2. التحقق مما إذا كان الدوري قد انتهى
            if (now.after(leagueEndDate)) {
                Log.d(TAG, "League ended! Processing winners...")
                
                // بدء معاملة (Transaction) لضمان أن مستخدم واحد فقط يقوم بالتحديث
                db.runTransaction { transaction ->
                    val currentLeagueDoc = transaction.get(leagueDocRef)
                    val currentEndDate = currentLeagueDoc.getDate("end_date") ?: return@runTransaction
                    
                    // تأكيد مزدوج داخل المعاملة
                    if (now.after(currentEndDate)) {
                        // أ. جلب أفضل 7 مستخدمين
                        // ملاحظة: داخل Transaction يفضل عدم استخدام الاستعلامات المعقدة
                        // سنقوم بتسجيل موعد الانتهاء الجديد أولاً
                        
                        val newCalendar = Calendar.getInstance()
                        newCalendar.add(Calendar.DAY_OF_YEAR, 7)
                        val newEndDate = newCalendar.time
                        
                        transaction.update(leagueDocRef, "end_date", newEndDate)
                    }
                }.await()

                // ب. تحديد الفائزين ومنحهم المكافأة
                // هذه الخطوة تتم خارج Transaction لأنها تتطلب استعلام
                val freshLeagueDoc = leagueDocRef.get().await()
                val freshEndDate = freshLeagueDoc.getDate("end_date")
                
                // إذا تم التحديث للتو، نقوم بتوزيع الجوائز
                // سنقارن هل التاريخ الجديد أكبر من الحالي بيوم على الأقل للتأكد أنه تم تجديده
                if (freshEndDate != null && freshEndDate.time > now.time + 24 * 60 * 60 * 1000) {
                    processWinners(db)
                }
            }
            
            // 3. التحقق من حالة المستخدم الحالي وتحديث صلاحياته محلياً
            checkCurrentUserReward(context, db)
            
        } catch (e: Exception) {
            if (e.message?.contains("offline", ignoreCase = true) == true || e.message?.contains("GMS", ignoreCase = true) == true) {
                Log.d(TAG, "League processing postponed (offline / network unavailable): ${e.message}")
            } else {
                Log.w(TAG, "Note on processing weekly league: ${e.message}")
            }
        }
    }

    private suspend fun processWinners(db: FirebaseFirestore) {
        try {
            // جلب أفضل المتصدرين (نجلب أكثر من 7 لفرز المؤهلين منهم محلياً وتجنب فهارس Firestore المعقدة)
            val topUsersSnapshot = db.collection("leaderboard")
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(25)
                .get().await()

            // تصفية المستخدمين المؤهلين فقط (الذين ليس لديهم مكافأة فعالة)
            val eligibleUsers = topUsersSnapshot.documents.filter { doc ->
                doc.getBoolean("is_eligible") ?: true
            }.take(7)

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 7) // مدة المكافأة: أسبوع
            val rewardExpiryDate = calendar.time

            // منح المكافأة للفائزين (تحديث حساباتهم) وإخراجهم من منافسة الأسبوع القادم
            for (doc in eligibleUsers) {
                val userId = doc.id
                
                // 1. ترقية الحساب وتحديث سجل الفوز
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "has_reward" to true,
                            "reward_expiry" to rewardExpiryDate,
                            "last_won_at" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "total_wins" to com.google.firebase.firestore.FieldValue.increment(1)
                        )
                    ).await()
                    
                // 2. تصفير النقاط وإيقاف الأهلية (لكي لا يفوزوا مرة أخرى مباشرة)
                db.collection("leaderboard").document(userId)
                    .update(
                        mapOf(
                            "points" to 0,
                            "is_eligible" to false,
                            "last_won_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            
            // تصفير نقاط باقي المستخدمين لبدء دوري جديد (للمتفاعلين مؤخراً)
            for (doc in topUsersSnapshot.documents) {
                if (doc.id !in eligibleUsers.map { it.id }) {
                    db.collection("leaderboard").document(doc.id).update("points", 0).await()
                }
            }
            
            Log.d(TAG, "Successfully processed top ${eligibleUsers.size} eligible winners!")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing winners: ${e.message}")
        }
    }

    private suspend fun checkCurrentUserReward(context: Context, db: FirebaseFirestore) {
        val userId = CloudServices.Auth.getCurrentUserId() ?: return
        try {
            val userDoc = db.collection("users").document(userId).get().await()
            val hasReward = userDoc.getBoolean("has_reward") ?: false
            val rewardExpiry = userDoc.getDate("reward_expiry")
            val now = Date()

            val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            if (hasReward && rewardExpiry != null) {
                if (now.before(rewardExpiry)) {
                    // المكافأة لا تزال سارية
                    Log.d(TAG, "User has active reward until $rewardExpiry")
                    editor.putBoolean("is_relative", true) // منح صلاحيات حساب الأقارب (مجاني كامل)
                    editor.putBoolean("has_league_reward", true)
                } else {
                    // انتهت المكافأة
                    Log.d(TAG, "User reward expired.")
                    editor.putBoolean("is_relative", false)
                    editor.putBoolean("has_league_reward", false)
                    
                    // تحديث السحابة بإزالة المكافأة
                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "has_reward" to false,
                                "reward_expiry" to null
                            )
                        ).await()
                        
                    // إعادة المستخدم للمنافسة في الدوري القادم
                    db.collection("leaderboard").document(userId)
                        .update(
                            mapOf(
                                "points" to 0,
                                "is_eligible" to true
                            )
                        ).await()
                }
            } else {
                // التأكد من إزالة الصلاحية إن لم يكن لديه مكافأة (إلا إذا كان قريباً فعلاً)
                val isActuallyRelative = prefs.getBoolean("is_relative", false)
                
                if (!isActuallyRelative) {
                    editor.putBoolean("is_relative", false)
                }
                editor.putBoolean("has_league_reward", false)
            }
            editor.apply()
        } catch (e: Exception) {
            if (e.message?.contains("offline", ignoreCase = true) == true) {
                Log.d(TAG, "Offline mode: using cached local user state.")
            } else {
                Log.w(TAG, "Note on checking user reward status: ${e.message}")
            }
        }
    }
}
