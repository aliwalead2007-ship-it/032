package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * طبقة البيانات المركزية لربط 'العقل' (Qabas Brain) بقاعدة بيانات Firestore
 * مسؤولة عن استرجاع كائنات الأنماط (Style Objects) المحفوظة،
 * تحويلها لذاكرة مرجعية ديناميكية (Dynamic Few-Shot Reference Memory)،
 * وحقنها في ردود وقرارات الذكاء الاصطناعي (Gemini).
 */
class QabasBrainRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("qabas_brain_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "QabasBrainRepository"

        @Volatile
        private var INSTANCE: QabasBrainRepository? = null

        fun getInstance(context: Context): QabasBrainRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QabasBrainRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * استرجاع كائنات الأنماط المخزنة للمستخدم والمجتمع من Firestore
     * مع التدهور الأنيق للذاكرة التراكمية المحلية إذا لم تتوفر السحابة
     */
    suspend fun getReferenceStyleObjects(userId: String? = null): List<StyleObject> = withContext(Dispatchers.IO) {
        val cloudStyles = if (CloudServices.isFirebaseInitialized) {
            try {
                CloudServices.Database.getCloudStyleObjects(userId)
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching from Firestore, falling back to local: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        if (cloudStyles.isNotEmpty()) {
            return@withContext cloudStyles
        }

        // استخدام الأنماط المخزنة في ذاكرة StyleBrain المحلية كاحتياطي موثوق
        val localAbsorbed = StyleBrain.absorbedStyles.value
        return@withContext localAbsorbed.map { StyleObject.fromAbsorbedStyle(it) }
    }

    /**
     * تدفق مراقبة حي لكائنات الأنماط من Firestore
     */
    fun observeStyleObjects(): Flow<List<StyleObject>> {
        return CloudServices.Database.observeCloudStyleObjects().flowOn(Dispatchers.IO)
    }

    /**
     * حفظ كائن نمط فني في سحابة Firestore وتحديث الذاكرة التراكمية للعقل
     */
    suspend fun saveStyleObject(styleObject: StyleObject): Boolean = withContext(Dispatchers.IO) {
        var cloudSaved = false
        if (CloudServices.isFirebaseInitialized) {
            cloudSaved = CloudServices.Database.saveStyleObjectToCloud(styleObject)
        }
        
        // حفظ في الذاكرة التراكمية وتطوير العقل
        StyleBrain.absorbStyleObject(context, styleObject)
        return@withContext cloudSaved || StyleBrain.absorbedStyles.value.isNotEmpty()
    }

    /**
     * صياغة ذاكرة مرجعية ديناميكية (Dynamic Few-Shot Reference Memory Context)
     * تستخرج الأنماط الأعلى تقييماً والأقرب للمحتوى المطلوب لحقنها في الـ Prompt الموجه إلى Gemini
     */
    suspend fun buildFewShotStyleMemory(
        targetIdea: String,
        targetTone: String,
        limit: Int = 3
    ): String = withContext(Dispatchers.IO) {
        val allStyles = getReferenceStyleObjects()
        if (allStyles.isEmpty()) {
            return@withContext """
                [الذاكرة المرجعية الأساسية لهوية قبس]:
                - النمط الأساسي: أسلوب وثائقي سينمائي وقور
                - الألوان المسيطرة: خلفية داكنة فاخرة Deep Slate (#0B0F19)، لمسات ذهبية متوهجة Gold Primary (#E8C547)، نصوص ناصعة (#F8FAFC)
                - إيقاع الحركة: زووم بطيء متصاعد (Slow Cinematic Zoom-in 1.05x)، انتقالات تلاشي ناعمة (Smooth Dissolve)
                - التيبوغرافيا: خط كوفي عريض في المنطقة الآمنة، ظهور كلمة بكلمة مع تمييز الكلمات المفتاحية بالذهبي
                - النبرة الدعوية: روحانية ملهمة، وقورة، موثوقة ومحفزة للتأمل
            """.trimIndent()
        }

        // ترتيب الأنماط بناءً على التوافق الدلالي والتقييم الإجمالي
        val rankedStyles = allStyles.sortedWith(
            compareByDescending<StyleObject> { style ->
                var relevance = 0
                if (style.contentTone.tone.contains(targetTone, ignoreCase = true)) relevance += 20
                if (targetIdea.isNotBlank() && style.tags.any { tag -> targetIdea.contains(tag, ignoreCase = true) }) relevance += 30
                if (style.visualStyle.visualTraits.any { vt -> vt.contains("ذهب") || vt.contains("داكن") }) relevance += 15
                relevance + style.overallScore
            }
        ).take(limit)

        val memoryBuilder = StringBuilder()
        memoryBuilder.append("=== ذاكرة العقل الإخراجي لكائنات الأنماط المحفوظة في Firestore (Reference Style Memory) ===\n")
        memoryBuilder.append("استخدم هذه الأنماط المرجعية كقواعد صارمة لتوجيه القرارات الإخراجية وتشكيل السيناريو والفلاتر:\n\n")

        rankedStyles.forEachIndexed { index, style ->
            memoryBuilder.append("【النمط المرجعي ${index + 1}: ${style.name} (تقييم العقل: ${style.overallScore}/100)】\n")
            memoryBuilder.append("- النبرة والهدف: ${style.contentTone.tone} | الجمهور: ${style.contentTone.targetAudience}\n")
            memoryBuilder.append("- الهوية البصرية: ألوان [${style.visualStyle.dominantColors}]، لون رئيسي: ${style.visualStyle.primaryColorHex}، خلفية: ${style.visualStyle.backgroundColorHex}\n")
            memoryBuilder.append("- السمات البصرية: ${style.visualStyle.visualTraits.joinToString("، ")}\n")
            memoryBuilder.append("- إيقاع المونتاج: ${style.motionRhythm.overallRhythm} (سرعة: ${style.motionRhythm.transitionSpeed}، انتقال: ${style.motionRhythm.transitionType})\n")
            memoryBuilder.append("- التيبوغرافيا والكابشن: ${style.contentTone.typographyStyle}، تحريك: ${style.contentTone.captionAnimation}\n")
            if (style.analysisSummary.isNotBlank()) {
                memoryBuilder.append("- التوجيه الإخراجي: ${style.analysisSummary}\n")
            }
            memoryBuilder.append("\n")
        }

        return@withContext memoryBuilder.toString().trim()
    }

    /**
     * تحديث أوزان النمط في Firestore بناءً على تغذية المستخدم الراجعة (Reinforcement)
     */
    suspend fun recordStyleFeedback(styleNameOrId: String, isPositive: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            val allStyles = getReferenceStyleObjects()
            val target = allStyles.find { it.id == styleNameOrId || it.name.equals(styleNameOrId, ignoreCase = true) }
            if (target != null) {
                val updatedScore = if (isPositive) {
                    (target.overallScore + 2).coerceAtMost(99)
                } else {
                    (target.overallScore - 3).coerceAtLeast(70)
                }
                val updatedStyle = target.copy(overallScore = updatedScore)
                saveStyleObject(updatedStyle)
                StyleBrain.recordUserFeedbackOnStyle(context, target.name, isPositive, updatedStyle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording style feedback in repository: ${e.message}")
        }
    }

    /**
     * حفظ عنصر جديد في سجل قرارات العقل
     */
    suspend fun saveDecisionHistoryItem(item: BrainDecisionHistoryItem): Unit = withContext(Dispatchers.IO) {
        try {
            val currentList = getDecisionHistory().toMutableList()
            // إزالة التكرار إن وجد
            currentList.removeAll { it.id == item.id }
            currentList.add(0, item) // إضافة في المقدمة

            val jsonArr = org.json.JSONArray()
            // الاحتفاظ بأحدث 50 قراراً فقط لضمان كفاءة التخزين والذاكرة
            currentList.take(50).forEach {
                jsonArr.put(it.toJsonObject())
            }

            prefs.edit().putString("qabas_brain_decision_history_json", jsonArr.toString()).apply()
            Log.d(TAG, "Saved decision history item: ${item.decision.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving decision history: ${e.message}", e)
        }
    }

    /**
     * جلب كافة السجلات المحفوظة لقرارات العقل
     */
    suspend fun getDecisionHistory(): List<BrainDecisionHistoryItem> = withContext(Dispatchers.IO) {
        try {
            val savedJson = prefs.getString("qabas_brain_decision_history_json", null)
            if (!savedJson.isNullOrBlank()) {
                val jsonArr = org.json.JSONArray(savedJson)
                val items = mutableListOf<BrainDecisionHistoryItem>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    items.add(BrainDecisionHistoryItem.fromJsonObject(obj))
                }
                if (items.isNotEmpty()) {
                    return@withContext items
                }
            }

            // إذا كان السجل فارغاً لأول مرة، تهيئة سجلات توجيهية أولية غنية بالأمثلة
            val defaultHistory = createInitialSampleDecisionHistory()
            val jsonArr = org.json.JSONArray()
            defaultHistory.forEach { jsonArr.put(it.toJsonObject()) }
            prefs.edit().putString("qabas_brain_decision_history_json", jsonArr.toString()).apply()
            return@withContext defaultHistory
        } catch (e: Exception) {
            Log.e(TAG, "Error loading decision history: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * حذف عنصر محدد من السجل
     */
    suspend fun deleteDecisionHistoryItem(id: String): Unit = withContext(Dispatchers.IO) {
        try {
            val currentList = getDecisionHistory().filter { it.id != id }
            val jsonArr = org.json.JSONArray()
            currentList.forEach { jsonArr.put(it.toJsonObject()) }
            prefs.edit().putString("qabas_brain_decision_history_json", jsonArr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting decision history item: ${e.message}")
        }
    }

    /**
     * مسح كافة عناصر السجل
     */
    suspend fun clearAllDecisionHistory(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove("qabas_brain_decision_history_json").apply()
    }

    /**
     * تحديث تقييم المستخدم للقرار وتعزيز كائن النمط المرتبط
     */
    suspend fun updateDecisionFeedback(id: String, isPositive: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            val currentList = getDecisionHistory().toMutableList()
            val index = currentList.indexOfFirst { it.id == id }
            if (index != -1) {
                val oldItem = currentList[index]
                val updatedItem = oldItem.copy(userFeedback = isPositive)
                currentList[index] = updatedItem

                val jsonArr = org.json.JSONArray()
                currentList.forEach { jsonArr.put(it.toJsonObject()) }
                prefs.edit().putString("qabas_brain_decision_history_json", jsonArr.toString()).apply()

                // تعزيز كائن النمط المرتبط
                val styleToReinforce = oldItem.influencingStyleObject?.name ?: oldItem.decision.selectedStyleName
                if (styleToReinforce.isNotBlank()) {
                    recordStyleFeedback(styleToReinforce, isPositive)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating decision feedback: ${e.message}")
        }
    }

    private fun createInitialSampleDecisionHistory(): List<BrainDecisionHistoryItem> {
        val now = System.currentTimeMillis()
        val coreStyle = StyleObject(
            id = "style_qabas_core",
            name = "هوية قبس النورانية",
            visualStyle = VisualStylePattern(
                dominantColors = "Deep Slate #0B0F19 مع ذهب قبس #E8C547",
                primaryColorHex = "#E8C547",
                backgroundColorHex = "#0B0F19",
                visualTraits = listOf("خلفية داكنة فاخرة", "نصوص ذهبية متوهجة", "تباين عالي")
            ),
            motionRhythm = MotionRhythmPattern(
                transitionSpeed = "متوسطة متوازنة",
                transitionType = "Dissolve",
                movementPatterns = "Slow Cinematic Zoom-in",
                overallRhythm = "تصاعدي وقور ومؤثر"
            ),
            contentTone = ContentTonePattern(
                tone = "وقور وملهم",
                targetAudience = "الجمهور العام",
                typographyStyle = "خط كوفي عريض في المنطقة الآمنة"
            ),
            overallScore = 96
        )

        val cinematicStyle = StyleObject(
            id = "style_cinematic_nature",
            name = "الوثائقي الدعوي الهادئ",
            visualStyle = VisualStylePattern(
                dominantColors = "درجات الزمردي #1A4D2E مع إضاءة ناعمة #F5D76E",
                primaryColorHex = "#F5D76E",
                backgroundColorHex = "#1A4D2E",
                visualTraits = listOf("طبيعة وتأمل", "إضاءة ناعمة", "ألوان ترابية وزمردية")
            ),
            motionRhythm = MotionRhythmPattern(
                transitionSpeed = "بطيئة متدرجة",
                transitionType = "Crossfade",
                movementPatterns = "Pan Up",
                overallRhythm = "هادئ وتأملي"
            ),
            contentTone = ContentTonePattern(
                tone = "هادئ وتأملي",
                targetAudience = "الشباب والمهتمين بالتزكية",
                typographyStyle = "خط رقعة سينمائي"
            ),
            overallScore = 93
        )

        return listOf(
            BrainDecisionHistoryItem(
                id = "history_sample_1",
                timestamp = now - (1000 * 60 * 15), // منذ 15 دقيقة
                ideaInput = "فضل صيام يوم عرفة وتكفير ذنوب سنتين بأسلوب مؤثر وسينمائي",
                tone = "وقور وملهم",
                targetAudience = "الجمهور العام من الشباب والمهتمين بالفضائل",
                decision = BrainDecision(
                    title = "يوم عرفة: فرصة المغفرة العظمى",
                    hook = "ما من يوم أكثر من أن يعتق الله فيه عبداً من النار من يوم عرفة.. هل أنت مستعد؟",
                    scriptScenes = listOf(
                        Scene("المقدمة المشوقة", "زووم بطيء على سماء الغروب مع نبرة وقورة مهيبة", 5, "Slow ZoomIn", "متوسط", "Dissolve"),
                        Scene("صلب الرسالة", "عرض حديث النبي ﷺ عن صيام يوم عرفة وتكفير سنة ماضية وقادمة", 10, "Gold Text Focus", "تصاعدي", "Fade"),
                        Scene("الخاتمة والدعوة", "دعوة لاغتنام الساعات الفاضلة بالدعاء والاستغفار", 5, "Cinematic Dimming", "هادئ", "Cut")
                    ),
                    voiceOverTone = "وقور وملهم",
                    visualDirectives = "خلفية داكنة DeepSlate #0B0F19 مع إضاءة ذهبية مركزة على النصوص العربية الكوفية",
                    primaryColorHex = "#E8C547",
                    backgroundColorHex = "#0B0F19",
                    ffmpegFilterSnippet = "scale=1080:1920,zoompan=z='min(zoom+0.0015,1.08)':d=125,eq=contrast=1.15:brightness=0.02",
                    audioSfxSuggestions = listOf("صوت هواء دافئ ناعم", "أثر خفيف لصدى الكلمات"),
                    selectedStyleName = coreStyle.name,
                    compatibilityScore = 96,
                    aiRationale = "تم اختيار هوية قبس النورانية لتحقيق أقصى درجات الوقار والهيبة لتعظيم فضل يوم عرفة مع تباين ألوان هادئ يجذب انتباه المشاهد فوراً.",
                    targetAudience = "الجمهور العام",
                    callToAction = "شارك المقطع لتنال أجر الدال على الخير"
                ),
                influencingStyleObject = coreStyle,
                userFeedback = true
            ),
            BrainDecisionHistoryItem(
                id = "history_sample_2",
                timestamp = now - (1000 * 60 * 60 * 3), // منذ 3 ساعات
                ideaInput = "أثر الكلمة الطيبة في بناء النفوس وجبر الخواطر",
                tone = "هادئ وتأملي",
                targetAudience = "صناع المحتوى والدعاة",
                decision = BrainDecision(
                    title = "الكلمة الطيبة: صدقة تحيي القلوب",
                    hook = "رب كلمة لم تلق لها بالاً أنقذت قلباً من الانكسار.. كيف نغرس الطمأنينة؟",
                    scriptScenes = listOf(
                        Scene("افتتاحية التأمل", "مشهد انسيابي مع ظهور عبارة 'ألم تر كيف ضرب الله مثلاً كلمة طيبة'", 6, "Soft Crossfade", "هادئ", "Dissolve"),
                        Scene("المعنى النفسي والشرعي", "تأثير الكلام الطيب كشجرة طيبة أصلها ثابت وفرعها في السماء", 8, "Slow Pan Up", "انسيابي", "Dissolve"),
                        Scene("الختام العملي", "اجعل لسانك بلسماً لمن حولك في كل لقاء", 5, "Subtle Gold Vignette", "خاشع", "Fade")
                    ),
                    voiceOverTone = "هادئ وتأملي",
                    visualDirectives = "درجات الزمردي الإسلامي #1A4D2E مع إضاءة ناعمة وحركات انتقال بطيئة Dissolve",
                    primaryColorHex = "#F5D76E",
                    backgroundColorHex = "#1A4D2E",
                    ffmpegFilterSnippet = "scale=1080:1920,fade=t=in:st=0:d=1,eq=saturation=1.1:contrast=1.05",
                    audioSfxSuggestions = listOf("رنين مائي هادئ", "صوت نسيم خفيف"),
                    selectedStyleName = cinematicStyle.name,
                    compatibilityScore = 93,
                    aiRationale = "الاعتماد على نمط وثائقي دعوي هادئ لتعزيز الطمأنينة والسكينة النفسية المتوافقة مع مفهوم جبر الخواطر.",
                    targetAudience = "الشباب والمهتمين بالتزكية",
                    callToAction = "اكتب كلمة طيبة لشخص عزيز الآن"
                ),
                influencingStyleObject = cinematicStyle,
                userFeedback = null
            )
        )
    }
}
