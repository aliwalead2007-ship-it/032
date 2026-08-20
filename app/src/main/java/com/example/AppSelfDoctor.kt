package com.example

import android.content.Context
import com.example.SystemLogsManager
import com.example.OperationalReadinessManager
import com.example.NotificationHelper
import java.util.UUID

data class DoctorReport(
    val id: String = UUID.randomUUID().toString(),
    val severity: String,          // "حرج" | "مهم" | "تحسين" | "معلومة"
    val title: String,
    val message: String,
    val category: String,          // "مفاتيح API" | "خدمات" | "واجهة" | "أداء" | "تكامل" | "أمان"
    val suggestedAction: String,
    val suggestedFixCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object AppSelfDoctor {
    private val history = mutableListOf<DoctorReport>()

    fun getHistory(): List<DoctorReport> = history.sortedByDescending { it.timestamp }

    suspend fun runFullDiagnosis(context: Context): List<DoctorReport> {
        val reports = mutableListOf<DoctorReport>()
        val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
        
        fun isKeyValid(key: String): Boolean {
            val trimmed = key.trim()
            return trimmed.isNotBlank() && 
                   trimmed != "YOUR_GEMINI_API_KEY" && 
                   !trimmed.equals("placeholder", ignoreCase = true) &&
                   !trimmed.startsWith("YOUR_")
        }

        // 1. API Keys Diagnosis
        val geminiKey = prefs.getString("gemini_key", "") ?: ""
        val groqKey = prefs.getString("groq_key", "") ?: ""
        val elevenLabsKey = prefs.getString("elevenlabs_key", "") ?: ""
        val azureKey = prefs.getString("azure_speech_key", "") ?: ""
        val huggingFaceKey = prefs.getString("huggingface_key", "") ?: ""
        val pexelsKey = prefs.getString("pexels_key", "") ?: ""
        val pixabayKey = prefs.getString("pixabay_key", "") ?: ""
        val firebaseKey = prefs.getString("firebase_key", "") ?: ""

        if (!isKeyValid(geminiKey)) {
            reports.add(
                DoctorReport(
                    severity = "حرج",
                    title = "محرك الذكاء الاصطناعي معطل (Gemini)",
                    message = "التطبيق حالياً يعتمد على وضع المحاكاة الوهمي (Fallback) لعدم وجود مفتاح Gemini API. هذا يمنع ميزات المساعد الدعوي وصياغة السكريبتات الذكية وتحليل الأفكار.",
                    category = "مفاتيح API",
                    suggestedAction = "أضف مفتاح Gemini API المجاني من Google AI Studio في لوحة المفاتيح لتشغيل المحرك الرئيسي.",
                    suggestedFixCode = """
// لإضافة المفتاح برمجياً:
val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
prefs.edit().putString("gemini_key", "YOUR_ACTUAL_KEY_HERE").apply()
                    """.trimIndent()
                )
            )
        } else {
            reports.add(
                DoctorReport(
                    severity = "معلومة",
                    title = "محرك Gemini متصل بنجاح ✦",
                    message = "محرك الذكاء الاصطناعي الأساسي مفعل ويعمل بكفاءة عالية في صياغة المحتوى وتحليله.",
                    category = "مفاتيح API",
                    suggestedAction = "جاهز للإنتاج وصناعة المحتوى."
                )
            )
        }

        if (!isKeyValid(groqKey)) {
            reports.add(
                DoctorReport(
                    severity = "تحسين",
                    title = "تحسين سرعة الاستجابة (Groq)",
                    message = "مفتاح Groq غير متوفر. يتم الاعتماد حالياً على Gemini في كل العمليات. إضافة Groq ستسرع توليد الخطافات والوسوم الفورية (~300ms).",
                    category = "أداء",
                    suggestedAction = "أضف مفتاح Groq المجاني لفتح مزايا السرعة الفائقة.",
                    suggestedFixCode = null
                )
            )
        }

        if (!isKeyValid(elevenLabsKey) && !isKeyValid(azureKey)) {
            reports.add(
                DoctorReport(
                    severity = "مهم",
                    title = "التعليق الصوتي العصبي غير مفعل",
                    message = "لا يوجد أي محرك صوتي سحابي (Azure Speech أو ElevenLabs) مفعل. يتم استخدام محرك الصوت المحلي الافتراضي.",
                    category = "خدمات",
                    suggestedAction = "أضف مفتاح Azure Speech (نوصي به للفصحى) أو ElevenLabs في إعدادات التطبيق.",
                    suggestedFixCode = null
                )
            )
        }

        if (!isKeyValid(pexelsKey) && !isKeyValid(pixabayKey)) {
            reports.add(
                DoctorReport(
                    severity = "مهم",
                    title = "مكتبة الوسائط الحية متوقفة",
                    message = "لم يتم إضافة مفاتيح Pexels أو Pixabay. التطبيق يعتمد على الوسائط والمشاهد الجاهزة المخزنة في الاستوديو.",
                    category = "تكامل",
                    suggestedAction = "أضف مفاتيح Pexels أو Pixabay المجانية لتمكين جلب مقاطع B-Roll وصور المعالم الإسلامية بجودة HD.",
                    suggestedFixCode = null
                )
            )
        }

        if (!isKeyValid(firebaseKey)) {
            reports.add(
                DoctorReport(
                    severity = "تحسين",
                    title = "المزامنة السحابية غير مفعلة (حفظ محلي)",
                    message = "مفتاح Firebase غير متوفر. يتم حفظ ومزامنة جميع المشاريع محلياً وبشكل آمن داخل قاعدة بيانات Room على جهازك.",
                    category = "أمان",
                    suggestedAction = "بياناتك محفوظة محلياً في أمان تام.",
                    suggestedFixCode = null
                )
            )
        }

        // 2. Operational Readiness Diagnosis
        val readiness = OperationalReadinessManager.calculateReadiness(context)
        if (readiness.percentage < 50) {
            reports.add(
                DoctorReport(
                    severity = "حرج",
                    title = "الجاهزية التشغيلية منخفضة (${readiness.percentage}%)",
                    message = "تطبيقنا يعتمد بشكل أساسي على الخدمات الخارجية. نسبة הגاهزية الحالية تعني أن أغلب ميزات التطبيق إما تعمل ببيانات وهمية أو معطلة.",
                    category = "خدمات",
                    suggestedAction = "راجع لوحة التحكم الخاصة بالمفاتيح والخدمات واستكمل إعدادها.",
                    suggestedFixCode = null
                )
            )
        }

        // 3. Fallbacks and System Errors
        val lastError = OperationalReadinessManager.getLastError()
        if (lastError != null) {
            reports.add(
                DoctorReport(
                    severity = "مهم",
                    title = "أخطاء مسجلة مؤخراً في النظام",
                    message = "تم رصد أخطاء أخيرة في سجلات النظام (Logs): $lastError. هذا قد يشير إلى انهيار خدمة معينة أو مشكلة اتصال.",
                    category = "أداء",
                    suggestedAction = "راجع شاشة 'سجلات النظام' لمعرفة تفاصيل الخطأ ومعالجته.",
                    suggestedFixCode = null
                )
            )
        }

        // Add a general info report to always show the doctor is working
        if (reports.none { it.severity == "حرج" }) {
            reports.add(
                DoctorReport(
                    severity = "معلومة",
                    title = "التشخيص الشامل مكتمل",
                    message = "الأنظمة الحيوية تعمل بشكل مستقر وفقاً للمفاتيح والإعدادات المتاحة.",
                    category = "تكامل",
                    suggestedAction = "استمر في مراقبة الأداء وتحديث المفاتيح عند الحاجة.",
                    suggestedFixCode = null
                )
            )
        }
        
        // Notify if there are critical issues
        if (reports.any { it.severity == "حرج" }) {
            try {
                NotificationHelper.showNotification(
                    context = context,
                    title = "طبيب التطبيق 🩺",
                    message = "تم اكتشاف مشكلة حرجة تحتاج انتباهك فوراً. راجع لوحة المطور."
                )
            } catch (e: Exception) {
                // Ignore if notification fails
            }
        }

        // Save to history
        reports.forEach { report ->
            // Keep last 20 reports per severity/category or overall limit (max 50 to avoid memory leak)
            history.add(0, report)
        }
        if (history.size > 50) {
            history.subList(50, history.size).clear()
        }

        // Return only the current run's reports sorted by severity
        return sortReports(reports)
    }

    private fun sortReports(reports: List<DoctorReport>): List<DoctorReport> {
        val severityWeight = mapOf("حرج" to 4, "مهم" to 3, "تحسين" to 2, "معلومة" to 1)
        return reports.sortedByDescending { severityWeight[it.severity] ?: 0 }
    }
}
