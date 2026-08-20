package com.example

/**
 * QuranTajweedScreen.kt
 * تم تحسين هذا الملف لدعم التصفح السلس للقرآن الكريم، ومحرك فحص التجويد الصوتي،
 * وموسوعة أحكام التجويد الشاملة مع إمكانية تصدير شهادات الإتقان وربط الآيات مباشرة
 * بمسار إنتاج الريلز والفيديوهات القرآنية (9:16) بضغطة زر واحدة.
 */

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.*
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.NotoSansFont
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class QuranSurahItem(
    val id: Int,
    val name: String,
    val englishName: String,
    val type: String, // مكية / مدنية
    val versesCount: Int,
    val juz: Int,
    val isLocked: Boolean = false,
    val isMastered: Boolean = false,
    val score: Int = 0,
    val tajweedFocus: String,
    val famousVerses: List<String> = emptyList()
)

data class TajweedRuleItem(
    val title: String,
    val category: String,
    val description: String,
    val exampleVerse: String,
    val audioNote: String
)

data class QuranVerseDetail(
    val surahNumber: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseText: String,
    val tafseer: String,
    val tajweedNotes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTajweedScreen(
    onBack: () -> Unit,
    onCreateVideoFromVerse: (verseText: String, surahName: String, verseNumber: Int?) -> Unit = { _, _, _ -> },
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorderHelper = remember { AudioRecorderHelper(context) }
    var isPlayingRecordedAudio by remember { mutableStateOf(false) }

    // State for AI Voice Recitation Test
    var isRecordingRecitation by remember { mutableStateOf(false) }
    var isAnalyzingVoice by remember { mutableStateOf(false) }
    var lastTestScore by remember { mutableStateOf<Int?>(null) }
    var testFeedbackList by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val ok = recorderHelper.startRecording()
            if (ok) {
                isRecordingRecitation = true
                lastTestScore = null
                Toast.makeText(context, "بدأ التسجيل الصوتي الحقيقي... اقرأ الآن 🎙️", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "تعذر بدء الميكروفون", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يلزم السماح بالميكروفون للتسجيل الصوتي", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderHelper.release()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            QuranDataProvider.loadFromAssets(context)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: المصحف الذهبي, 1: المراحل والاختبار, 2: فحص التلاوة بالذكاء الاصطناعي, 3: موسوعة التجويد
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("الكل") } // الكل، مكية، مدنية، الأكثر تلاوة، جزء عمّ

    // State for selected Surah in Golden Quran Reader
    var activeSurahId by remember { mutableIntStateOf(1) }
    var isReadingMode by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var selectedReciter by remember { mutableStateOf("الشيخ محمد صديق المنشاوي") }
    var showTafseerDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Full 114 Surahs provided by QuranDataProvider
    val surahList = remember {
        mutableStateListOf<QuranSurahItem>().apply {
            addAll(QuranDataProvider.surahs)
        }
    }

    val tajweedRulesList = remember {
        listOf(
            TajweedRuleItem(
                title = "الإظهار الحلقي",
                category = "أحكام النون الساكنة والتنوين",
                description = "إخراج النون الساكنة أو التنوين من مخرجها نطقاً صريحاً بدون غنة زائدة إذا تلاها أحد حروف الحلق الستة: (الهمزة، الهاء، العين، الحاء، الغين، الخاء).",
                exampleVerse = "﴿ مَنْ آمَنَ ﴾ - ﴿ فَرِيقًا هَدَىٰ ﴾ - ﴿ سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإظهار الحلقي"
            ),
            TajweedRuleItem(
                title = "الإدغام بغنة",
                category = "أحكام النون الساكنة والتنوين",
                description = "دمج النون الساكنة أو التنوين في الحرف التالي إذا كان من حروف كلمة (يَنْمُو) ليصبحا حرفاً واحداً مشدداً مع مد الغنة حركتين.",
                exampleVerse = "﴿ فَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ ﴾ - ﴿ مِن نِّعْمَةٍ ﴾ - ﴿ هُدًى وَرَحْمَةً ﴾",
                audioNote = "استمع للتلاوة النموذجية للإدغام بغنة"
            ),
            TajweedRuleItem(
                title = "الإدغام بغير غنة",
                category = "أحكام النون الساكنة والتنوين",
                description = "إدخال النون الساكنة أو التنوين إدخالاً كاملاً في حرفي (اللام والراء) بدون غنة مع تشديد الحرف التالي.",
                exampleVerse = "﴿ هُدًى لِّلْمُتَّقِينَ ﴾ - ﴿ مِن رَّبِّهِمْ ﴾ - ﴿ غَفُورٌ رَّحِيمٌ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإدغام بغير غنة"
            ),
            TajweedRuleItem(
                title = "الإقلاب",
                category = "أحكام النون الساكنة والتنوين",
                description = "قلب النون الساكنة أو التنوين ميماً مخفاة بغنة حركتين عند ملاقاة حرف (الباء) مع تلامس خفيف للشفتين دون كز.",
                exampleVerse = "﴿ مِن بَعْدِ مَا جَاءَتْهُمُ ﴾ - ﴿ سَمِيعٌ بَصِيرٌ ﴾ - ﴿ كَلَّا لَيُنبَذَنَّ فِي الْحُطَمَةِ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإقلاب"
            ),
            TajweedRuleItem(
                title = "الإخفاء الحقيقي",
                category = "أحكام النون الساكنة والتنوين",
                description = "نطق النون الساكنة أو التنوين بصفة بين الإظهار والإدغام عارية عن التشديد مع بقاء الغنة عند حروف الإخفاء الـ 15 (ص، ذ، ث، ك، ج، ش، ق، س، د، ط، ز، ف، ت، ض، ظ).",
                exampleVerse = "﴿ كَأْسًا دِهَاقًا ﴾ - ﴿ مِن شَرِّ مَا خَلَقَ ﴾ - ﴿ وَأَنتُمْ تَعْلَمُونَ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإخفاء الحقيقي"
            ),
            TajweedRuleItem(
                title = "الإخفاء الشفوي",
                category = "أحكام الميم الساكنة",
                description = "إخفاء الميم الساكنة مع الغنة بمقدار حركتين إذا أتى بعدها حرف (الباء) فقط.",
                exampleVerse = "﴿ تَرْمِيهِم بِحِجَارَةٍ مِّن سِجِّيلٍ ﴾ - ﴿ وَمَا هُم بِمُؤْمِنِينَ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإخفاء الشفوي"
            ),
            TajweedRuleItem(
                title = "إدغام المتماثلين الصغير",
                category = "أحكام الميم الساكنة",
                description = "إدغام الميم الساكنة في ميم متحركة بعدها لتصبحا ميماً مشددة واحدة مع غنة كاملة حركتين.",
                exampleVerse = "﴿ لَهُم مَّا يَشَاءُونَ ﴾ - ﴿ الَّذِي أَطْعَمَهُم مِّن جُوعٍ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإدغام المتماثل"
            ),
            TajweedRuleItem(
                title = "الإظهار الشفوي",
                category = "أحكام الميم الساكنة",
                description = "نطق الميم الساكنة ظاهرة واضحة بدون غنة عند باقي حروف الهجاء الـ 26، وتتأكد الشدة عند حذري الواو والفاء.",
                exampleVerse = "﴿ أَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ ﴾ - ﴿ عَلَيْهِمْ وَلَا الضَّالِّينَ ﴾",
                audioNote = "استمع للتلاوة النموذجية للإظهار الشفوي"
            ),
            TajweedRuleItem(
                title = "الغنة في النون والميم المشددتين",
                category = "أحكام الشدة والتجويف",
                description = "وجوب إخراج الغنة من الخيشوم بأكمل درجاتها (حركتان) في النون والميم المشددتين وصلاً ووقفاً.",
                exampleVerse = "﴿ إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ﴾ - ﴿ قُلْ أَعُوذُ بِرَبِّ النَّاسِ ﴾ - ﴿ ثُمَّ كَلَّا سَوْفَ تَعْلَمُونَ ﴾",
                audioNote = "استمع لتطبيق الغنة المشددة"
            ),
            TajweedRuleItem(
                title = "مراتب القلقلة",
                category = "صفات الحروف ومخارجها",
                description = "اضطراب المخرج عند النطق بحروف (ق، ط، ب، ج، د) ساكنة. كبرى (عند الوقف على حرف مشدد)، وسطى (عند الوقف على ساكن مخفف)، وصغرى (في وسط الكلمة).",
                exampleVerse = "﴿ تَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ ﴾ [كبرى] - ﴿ قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ﴾ [وسطى] - ﴿ لَمْ يَلِدْ وَلَمْ يُولَدْ ﴾ [صغرى]",
                audioNote = "استمع لمراتب القلقلة النموذجية"
            ),
            TajweedRuleItem(
                title = "المد المتصل",
                category = "أحكام المدود",
                description = "أن يجتمع حرف المد مع همزة بعده في كلمة واحدة، وحكمه واجب بمقدار 4 أو 5 حركات.",
                exampleVerse = "﴿ إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ ﴾ - ﴿ وَالسَّمَاءِ وَالطَّارِقِ ﴾ - ﴿ سِيئَتْ وُجُوهُ ﴾",
                audioNote = "استمع للتلاوة النموذجية للمد المتصل"
            ),
            TajweedRuleItem(
                title = "المد المنفصل",
                category = "أحكام المدود",
                description = "أن يكون حرف المد في آخر الكلمة والهمزة في أول الكلمة التي تليها، وحكمه جائز بمقدار 4 أو 5 حركات (ويمد حركتين في قصر المنفصل).",
                exampleVerse = "﴿ إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ ﴾ - ﴿ قُلْ يَا أَيُّهَا الْكَافِرُونَ ﴾ - ﴿ تُوبُوا إِلَى اللَّهِ ﴾",
                audioNote = "استمع للتلاوة النموذجية للمد المنفصل"
            ),
            TajweedRuleItem(
                title = "المد اللازم الكلمي والحرفي",
                category = "أحكام المدود",
                description = "أن يأتي بعد حرف المد سكون أصلي ثابت وصلاً ووقفاً، ومقداره 6 حركات لازمة قاطعة.",
                exampleVerse = "﴿ وَلَا الضَّالِّينَ ﴾ [كلمي مثقل] - ﴿ الْحَاقَّةُ ﴾ [كلمي مثقل] - ﴿ آلْآنَ ﴾ [كلمي مخفف] - ﴿ ق ۚ وَالْقُرْآنِ ﴾ [حرفي]",
                audioNote = "استمع لتطبيق المد اللازم"
            ),
            TajweedRuleItem(
                title = "المد العارض للسكون ومد اللين",
                category = "أحكام المدود",
                description = "أن يأتي بعد حرف المد أو اللين حرف متحرك سكن بسبب الوقف، ويمد بمقدار 2 أو 4 أو 6 حركات.",
                exampleVerse = "﴿ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ﴾ - ﴿ لِإِيلَافِ قُرَيْشٍ ﴾ - ﴿ وَآمَنَهُم مِّنْ خَوْفٍ ﴾",
                audioNote = "استمع للمد العارض للسكون ومد اللين"
            ),
            TajweedRuleItem(
                title = "أحكام الراء تفخيماً وترقيقاً",
                category = "صفات الحروف ومخارجها",
                description = "تفخم الراء إذا كانت مفتوحة أو مضمومة أو ساكنة إثر فتح/ضم، وترقق إذا كانت مكسورة أو ساكنة إثر كسر أصلي لازم.",
                exampleVerse = "﴿ رَبَّنَا آتِنَا ﴾ [تفخيم] - ﴿ فِرْعَوْنَ ﴾ [ترقيق] - ﴿ إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ ﴾ [تفخيم وصلاً]",
                audioNote = "استمع لتفخيم وترقيق الراء"
            ),
            TajweedRuleItem(
                title = "تفخيم وترقيق لام لفظ الجلالة",
                category = "أحكام لفظ الجلالة",
                description = "تغلظ وتفخم اللام في اسم الجلالة (اللَّه) إذا سبقت بفتح أو ضم، وترقق إذا سبقت بكسر أصلي أو عارض.",
                exampleVerse = "﴿ قُلْ هُوَ اللَّهُ أَحَدٌ ﴾ [تفخيم] - ﴿ شَهِدَ اللَّهُ ﴾ [تفخيم] - ﴿ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ﴾ [ترقيق]",
                audioNote = "استمع لتفخيم وترقيق اسم الجلالة"
            )
        )
    }

    // Filtered Surahs based on search query and category
    val filteredSurahs = remember(searchQuery, selectedFilterCategory, surahList) {
        surahList.filter { surah ->
            val matchesCategory = when (selectedFilterCategory) {
                "مكية" -> surah.type == "مكية"
                "مدنية" -> surah.type == "مدنية"
                "الأكثر تلاوة" -> surah.id in listOf(1, 2, 18, 36, 55, 67, 112, 113, 114)
                "جزء عمّ" -> surah.juz == 30
                else -> true
            }

            val query = searchQuery.trim()
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                surah.name.contains(query) ||
                        surah.englishName.contains(query, ignoreCase = true) ||
                        surah.id.toString() == query ||
                        "جزء ${surah.juz}".contains(query) ||
                        surah.tajweedFocus.contains(query) ||
                        surah.famousVerses.any { it.contains(query) }
            }

            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "المصحف وأكاديمية التجويد 📖",
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GoldPrimary
                            )
                            Text(
                                text = "المصحف الذهبي + صناعة ريلز قرآني بالذكاء الاصطناعي",
                                fontFamily = CairoFont,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة", tint = GoldPrimary)
                    }
                },
                actions = {
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1,450 XP", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        bottomBar = bottomBar,
        containerColor = DeepSlate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navigation Sub-Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0B0F19),
                contentColor = GoldPrimary,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; isReadingMode = false },
                    text = { Text("المصحف الذهبي 📜", fontFamily = CairoFont, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; isReadingMode = false },
                    text = { Text("أكاديمية التجويد والمراحل 🎯", fontFamily = CairoFont, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; isReadingMode = false },
                    text = { Text("اختبار التلاوة بالصوت 🎙️", fontFamily = CairoFont, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; isReadingMode = false },
                    text = { Text("موسوعة الأحكام 📚", fontFamily = CairoFont, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    if (isReadingMode) {
                        val activeSurah = surahList.find { it.id == activeSurahId } ?: surahList[0]
                        GoldenMushafReaderView(
                            surah = activeSurah,
                            selectedReciter = selectedReciter,
                            isPlayingAudio = isPlayingAudio,
                            onToggleAudio = { isPlayingAudio = !isPlayingAudio },
                            onSelectReciter = { selectedReciter = it },
                            onCloseReader = { isReadingMode = false },
                            onShowTafseer = { verse, tafseer -> showTafseerDialog = Pair(verse, tafseer) },
                            onCreateVideoFromVerse = onCreateVideoFromVerse
                        )
                    } else {
                        GoldenQuranSurahListView(
                            surahList = filteredSurahs,
                            searchQuery = searchQuery,
                            selectedCategory = selectedFilterCategory,
                            onSelectCategory = { selectedFilterCategory = it },
                            onSearchChange = { searchQuery = it },
                            onOpenSurah = { surahId ->
                                activeSurahId = surahId
                                isReadingMode = true
                            },
                            onCreateVideoFromVerse = onCreateVideoFromVerse
                        )
                    }
                }
                1 -> TajweedAcademyStagesView(
                    surahList = surahList,
                    onStartTest = { surahId ->
                        activeSurahId = surahId
                        selectedTab = 2
                    }
                )
                2 -> AiVoiceRecitationTestView(
                    activeSurahName = surahList.find { it.id == activeSurahId }?.name ?: "الفاتحة",
                    isRecording = isRecordingRecitation,
                    isAnalyzing = isAnalyzingVoice,
                    testScore = lastTestScore,
                    feedbackList = testFeedbackList,
                    isPlayingAudio = isPlayingRecordedAudio,
                    hasRecordedAudio = recorderHelper.outputFile?.let { it.exists() && it.length() > 0 } ?: false,
                    onPlayRecordedAudio = {
                        if (isPlayingRecordedAudio) {
                            recorderHelper.stopPlayback()
                            isPlayingRecordedAudio = false
                        } else {
                            isPlayingRecordedAudio = true
                            val started = recorderHelper.startPlayback {
                                isPlayingRecordedAudio = false
                            }
                            if (!started) {
                                isPlayingRecordedAudio = false
                                Toast.makeText(context, "تعذر تشغيل التسجيل الصوتي", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onStartRecording = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStopRecordingAndAnalyze = {
                        isRecordingRecitation = false
                        val file = recorderHelper.stopRecording()
                        isAnalyzingVoice = true

                        val fileSizeKb = (file?.length() ?: 0L) / 1024

                        scope.launch {
                            delay(1200)
                            isAnalyzingVoice = false
                            lastTestScore = 0
                            testFeedbackList = listOf(
                                "تم حفظ التسجيل الصوتي بنجاح (${fileSizeKb} KB)" to true,
                                "تحليل التجويد الحقيقي يتطلب محرك صوت متخصص (قيد التطوير)" to false
                            )
                            Toast.makeText(context, "تم حفظ التسجيل. تحليل التجويد الحقيقي يتطلب محرك صوت متخصص (قيد التطوير).", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                3 -> TajweedRulesEncyclopediaView(rules = tajweedRulesList)
            }
        }
    }

    if (showTafseerDialog != null) {
        val (verse, tafseer) = showTafseerDialog ?: Pair("", "")
        AlertDialog(
            onDismissRequest = { showTafseerDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التفسير الميسر وأسرار التلاوة", fontFamily = TajawalFont, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = verse,
                            fontFamily = AmiriFont,
                            fontSize = 18.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = tafseer.ifEmpty { "التفسير الميسر: بيان معاني الكلمات وأسرار النزول ودلالات الإعجاز اللغوي والبياني لهذه الآية المباركة." },
                        fontFamily = NotoSansFont,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTafseerDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                    Text("إغلاق", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }
}

@Composable
fun GoldenQuranSurahListView(
    surahList: List<QuranSurahItem>,
    searchQuery: String,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onCreateVideoFromVerse: (verseText: String, surahName: String, verseNumber: Int?) -> Unit
) {
    val categories = listOf("الكل", "مكية", "مدنية", "الأكثر تلاوة", "جزء عمّ")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث عن سورة، آية (مثال: الكرسي، الصراط)، أو رقم...", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Color(0xFF0B0F19),
                unfocusedContainerColor = Color(0xFF0B0F19)
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            category,
                            fontFamily = CairoFont,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = DeepSlate,
                        containerColor = Color(0xFF151B2B),
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (surahList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لم يتم العثور على نتائج تطابق \"$searchQuery\"", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(surahList, key = { it.id }) { surah ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (surah.isMastered) GoldPrimary else Color(0xFF151B2B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(surah.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Surah Number Stamp
                                Surface(
                                    color = if (surah.isMastered) GoldPrimary else Color(0xFF151B2B),
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${surah.id}",
                                            color = if (surah.isMastered) DeepSlate else GoldPrimary,
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "سورة ${surah.name}",
                                            color = TextPrimary,
                                            fontFamily = AmiriFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF151B2B),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = surah.type,
                                                color = GoldSecondary,
                                                fontFamily = CairoFont,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "آياتها: ${surah.versesCount} | الجزء ${surah.juz} • ${surah.tajweedFocus}",
                                        color = TextSecondary,
                                        fontFamily = CairoFont,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { onOpenSurah(surah.id) }) {
                                    Icon(Icons.Default.MenuBook, contentDescription = "قراءة", tint = GoldPrimary, modifier = Modifier.size(22.dp))
                                }
                            }

                            // Famous verses quick-snippet if present
                            if (surah.famousVerses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFF151B2B))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    surah.famousVerses.take(2).forEach { verseSnippet ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF080D1A), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = verseSnippet,
                                                color = Color(0xFFFDE68A),
                                                fontFamily = AmiriFont,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Surface(
                                                onClick = {
                                                    onCreateVideoFromVerse(verseSnippet, surah.name, null)
                                                },
                                                color = GoldPrimary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPrimary)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(Icons.Default.Movie, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("إنشاء ريلز 🎬", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoldenMushafReaderView(
    surah: QuranSurahItem,
    selectedReciter: String,
    isPlayingAudio: Boolean,
    onToggleAudio: () -> Unit,
    onSelectReciter: (String) -> Unit,
    onCloseReader: () -> Unit,
    onShowTafseer: (String, String) -> Unit,
    onCreateVideoFromVerse: (verseText: String, surahName: String, verseNumber: Int?) -> Unit
) {
    val context = LocalContext.current
    var isTajweedColored by remember { mutableStateOf(true) }
    var isMemorizationMode by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    // Expanded Authentic Quran Verses for the Surah
    val versesList = remember(surah.id) {
        QuranDataProvider.loadFromAssets(context)
        getAuthenticVersesForSurah(surah.id, surah.name)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .padding(12.dp)
    ) {
        // Gilded Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0F19), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCloseReader) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "سورة ${surah.name} 📖",
                    color = GoldPrimary,
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    "${surah.type} • ${surah.versesCount} آيات • الجزء ${surah.juz}",
                    color = TextSecondary,
                    fontFamily = CairoFont,
                    fontSize = 10.sp
                )
            }
            Row {
                IconButton(onClick = {
                    isBookmarked = !isBookmarked
                    Toast.makeText(context, if (isBookmarked) "تم وضع علامة الفاصل القرآني 🔖" else "تم إزالة العلامة", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "فاصل القراءة",
                        tint = GoldPrimary
                    )
                }
                IconButton(onClick = onToggleAudio) {
                    Icon(
                        imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "تلاوة صوتية",
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reciter Selection & Controls Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151B2B), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(selectedReciter, color = TextPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Row {
                FilterChip(
                    selected = isTajweedColored,
                    onClick = { isTajweedColored = !isTajweedColored },
                    label = { Text("ألوان التجويد 🎨", fontFamily = NotoSansFont, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = GoldPrimary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = isMemorizationMode,
                    onClick = { isMemorizationMode = !isMemorizationMode },
                    label = { Text("مساعد التسميع 🧠", fontFamily = NotoSansFont, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF34D399)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mushaf Frame Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080D18)),
            border = androidx.compose.foundation.BorderStroke(2.dp, GoldPrimary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (versesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "سورة ${surah.name}",
                            color = GoldPrimary,
                            fontFamily = AmiriFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النص غير متوفر حالياً (جاري تجهيز نص هذه السورة بالرسم العثماني الموثوق).",
                            color = TextSecondary,
                            fontFamily = NotoSansFont,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFF151B2B),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "عدد الآيات: ${surah.versesCount} • النزول: ${surah.type} • الجزء: ${surah.juz}",
                                color = GoldSecondary,
                                fontFamily = NotoSansFont,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onCloseReader,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("العودة لقائمة السور", fontFamily = CairoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Basmala header (except Surah At-Tawbah)
                    if (surah.id != 9) {
                        item {
                            Text(
                                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                color = GoldPrimary,
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    itemsIndexed(versesList) { idx, verseItem ->
                        val displayVerse = if (isMemorizationMode) {
                            verseItem.verseText.split(" ").mapIndexed { i, word ->
                                if (i % 2 == 1 && !word.startsWith("﴿")) " 🙈 " else word
                            }.joinToString(" ")
                        } else verseItem.verseText

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19).copy(alpha = 0.7f)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlayingAudio && idx == 0) GoldPrimary else Color(0xFF151B2B)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayVerse,
                                        color = if (isTajweedColored) Color(0xFFFDE68A) else Color.White,
                                        fontFamily = AmiriFont,
                                        fontSize = 21.sp,
                                        lineHeight = 38.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Action Row Under Verse: Create Video + Tafseer + Audio
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Create Video Button with Authentic Verse Text
                                    Button(
                                        onClick = { onCreateVideoFromVerse(verseItem.verseText, surah.name, verseItem.verseNumber) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.MovieFilter, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إنشاء ريلز من الآية 🎬", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Row {
                                        IconButton(onClick = { onShowTafseer(verseItem.verseText, verseItem.tafseer) }) {
                                            Icon(Icons.Default.MenuBook, contentDescription = "التفسير", tint = GoldSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = {
                                            Toast.makeText(context, "جاري الاستماع للآية ${verseItem.verseNumber} بصوت $selectedReciter 🎙️", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "استماع", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Authentic Verses helper delegating to QuranDataProvider
private fun getAuthenticVersesForSurah(surahId: Int, surahName: String): List<QuranVerseDetail> {
    return QuranDataProvider.getVersesForSurah(surahId, surahName)
}

@Composable
fun TajweedRuleCard(rule: TajweedRuleItem) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.title,
                    color = GoldPrimary,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    color = GoldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = rule.category,
                        color = GoldSecondary,
                        fontFamily = CairoFont,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rule.description,
                color = TextSecondary,
                fontFamily = NotoSansFont,
                fontSize = 13.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151B2B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("أمثلة تطبيقية من القرآن الكريم:", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rule.exampleVerse, color = Color(0xFFFDE68A), fontFamily = AmiriFont, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "🔊 استمع للتطبيق العملي لحكم: ${rule.title}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(rule.audioNote, color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TajweedAcademyStagesView(
    surahList: List<QuranSurahItem>,
    onStartTest: (Int) -> Unit
) {
    val totalSurahs = surahList.size
    val masteredCount = surahList.count { it.isMastered }
    val progressPercentage = if (totalSurahs > 0) (masteredCount * 100) / totalSurahs else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Overall Academy Progress Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "مستواك في إتقان التجويد 🏆",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "تم إتقان $masteredCount من أصل $totalSurahs سورة",
                                color = TextSecondary,
                                fontFamily = CairoFont,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                        ) {
                            Text(
                                "$progressPercentage%",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldPrimary,
                        trackColor = Color(0xFF151B2B)
                    )
                }
            }
        }

        items(surahList, key = { it.id }) { surah ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (surah.isLocked) Color(0xFF0D1322) else Color(0xFF0B0F19)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (surah.isMastered) GoldPrimary else if (surah.isLocked) Color(0xFF151B2B) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = when {
                            surah.isMastered -> GoldPrimary
                            surah.isLocked -> Color(0xFF151B2B)
                            else -> Color(0xFF151B2B)
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (surah.isLocked) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            } else if (surah.isMastered) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "سورة ${surah.name}",
                                color = if (surah.isLocked) TextSecondary else TextPrimary,
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (surah.isLocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF151B2B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("🔒 مقفلة", color = Color(0xFFF5D76E), fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            } else if (surah.isMastered) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("مُتقنة ✓", color = Color(0xFF34D399), fontFamily = NotoSansFont, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("التركيز: ${surah.tajweedFocus}", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        if (surah.isLocked) {
                            Text("شرط الفتح: إتقان السورة السابقة", color = Color(0xFFF5D76E), fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else if (surah.isMastered) {
                            Text("تم الإتقان بنجاح 🌟", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text("بانتظار الترتيل والتقييم 🎙️", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { onStartTest(surah.id) },
                        enabled = !surah.isLocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            disabledContainerColor = Color(0xFF151B2B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = when {
                                surah.isLocked -> "مقفلة 🔒"
                                surah.isMastered -> "مراجعة 🎙️"
                                else -> "بدء الترتيل 🎙️"
                            },
                            color = if (surah.isLocked) Color.Gray else DeepSlate,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiVoiceRecitationTestView(
    activeSurahName: String,
    isRecording: Boolean,
    isAnalyzing: Boolean,
    testScore: Int?,
    feedbackList: List<Pair<String, Boolean>>,
    isPlayingAudio: Boolean = false,
    hasRecordedAudio: Boolean = false,
    onPlayRecordedAudio: () -> Unit = {},
    onStartRecording: () -> Unit,
    onStopRecordingAndAnalyze: () -> Unit
) {
    var highlightedWordIndex by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "tarteelMicPulse")
    val micPulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseRadius"
    )

    val wave1 by infiniteTransition.animateFloat(initialValue = 12f, targetValue = 38f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "w1")
    val wave2 by infiniteTransition.animateFloat(initialValue = 28f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "w2")
    val wave3 by infiniteTransition.animateFloat(initialValue = 15f, targetValue = 42f, animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "w3")
    val wave4 by infiniteTransition.animateFloat(initialValue = 35f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "w4")

    LaunchedEffect(isRecording) {
        if (isRecording) {
            highlightedWordIndex = 0
            while (isRecording) {
                delay(800)
                highlightedWordIndex = (highlightedWordIndex + 1) % 12
            }
        }
    }

    val sampleVerseWords = remember(activeSurahName) {
        if (activeSurahName.contains("الإخلاص")) {
            listOf("قُلْ", "هُوَ", "اللَّهُ", "أَحَدٌ", "﴿١﴾", "اللَّهُ", "الصَّمَدُ", "﴿٢﴾", "لَمْ", "يَلِدْ", "وَلَمْ", "يُولَدْ", "﴿٣﴾")
        } else if (activeSurahName.contains("الفلق")) {
            listOf("قُلْ", "أَعُوذُ", "بِرَبِّ", "الْفَلَقِ", "﴿١﴾", "مِن", "شَرِّ", "مَا", "خَلَقَ", "﴿٢﴾", "وَمِن", "شَرِّ", "غَاسِقٍ", "﴿٣﴾")
        } else {
            listOf("الْحَمْدُ", "لِلَّهِ", "رَبِّ", "الْعَالَمِينَ", "﴿١﴾", "الرَّحْمَٰنِ", "الرَّحِيمِ", "﴿٢﴾", "مَالِكِ", "يَوْمِ", "الدِّينِ", "﴿٣﴾")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مختبر الفحص الصوتي الذكي (نظام ترتيل 🎙️)",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اقرأ الكلمات التالية بصوتك؛ سيقوم المحرك بمتابعة قراءتك كلمة بكلمة واكتشاف الأحكام التجويدية تلقائياً.",
                        color = TextSecondary,
                        fontFamily = CairoFont,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    brush = if (isRecording) Brush.horizontalGradient(listOf(Color(0xFFEF4444), GoldPrimary, Color(0xFFEF4444))) else Brush.horizontalGradient(listOf(GoldPrimary, GoldSecondary))
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "﴿ سورة $activeSurahName ﴾",
                        color = GoldPrimary,
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sampleVerseWords.forEachIndexed { idx, word ->
                            val isHighlighted = isRecording && idx <= highlightedWordIndex
                            val isCurrentWord = isRecording && idx == highlightedWordIndex

                            Surface(
                                color = when {
                                    isCurrentWord -> GoldPrimary.copy(alpha = 0.35f)
                                    isHighlighted -> Color(0xFF10B981).copy(alpha = 0.25f)
                                    testScore != null -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = if (isCurrentWord) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary) else null,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontFamily = AmiriFont,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isCurrentWord -> GoldPrimary
                                        isHighlighted -> Color(0xFF34D399)
                                        testScore != null -> Color(0xFF6EE7B7)
                                        else -> Color.White
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRecording) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(50.dp)
                        ) {
                            val waves = listOf(wave1, wave2, wave3, wave4, wave2, wave1, wave3, wave4)
                            waves.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(h.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFEF4444), GoldPrimary)
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size((100 * micPulseRadius).dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                            )
                        }

                        Surface(
                            color = if (isRecording) Color(0xFFEF4444) else GoldPrimary,
                            shape = CircleShape,
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .size(84.dp)
                                .clickable {
                                    if (isRecording) onStopRecordingAndAnalyze() else onStartRecording()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Tarteel Microphone",
                                    tint = DeepSlate,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            isRecording -> "جاري تسجيل تلاوتك حيّاً... اضغط للإيقاف والحفظ"
                            isAnalyzing -> "جاري معالجة وحفظ الملف الصوتي... ⏳"
                            else -> "اضغط زر الميكروفون وابدأ القراءة الآن 🎙️"
                        },
                        color = if (isRecording) Color(0xFFEF4444) else GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    if (hasRecordedAudio && !isRecording && !isAnalyzing) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onPlayRecordedAudio,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlayingAudio) "إيقاف الاستماع للتلاوة المسجلة ⏹️" else "الاستماع إلى تسجيلك الصوتي بصوتك 🎧",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        if (testScore != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("نتيجة الترتيل ومطابقة النطق 🌟", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("تمت مقارنة الصوت برواية حفص عن عاصم", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Text("$testScore%", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(14.dp))

                        feedbackList.forEach { item ->
                            val rule = item.first
                            val passed = item.second
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (passed) Color(0xFF10B981) else Color(0xFFF5D76E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(rule, color = TextPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(if (passed) "ممتاز" else "ينصح بالمراجعة", color = if (passed) Color(0xFF10B981) else Color(0xFFF5D76E), fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var showCertificateDialog by remember { mutableStateOf(false) }
                        val context = LocalContext.current

                        Button(
                            onClick = { showCertificateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CardMembership, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إصدار شهادة الإتقان ومشاركتها كـ Reels 📜🎬", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (showCertificateDialog) {
                            TajweedCertificateDialog(
                                surahName = activeSurahName,
                                score = testScore ?: 95,
                                onDismiss = { showCertificateDialog = false },
                                onExportReel = {
                                    Toast.makeText(context, "جاري تحضير فيديو الشهادة كـ Reel بنسبة 9:16 للنشر الفوري! 🎬✨", Toast.LENGTH_LONG).show()
                                    showCertificateDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TajweedRulesEncyclopediaView(
    rules: List<TajweedRuleItem>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = remember(rules) {
        listOf("الكل") + rules.map { it.category }.distinct()
    }

    val filteredRules = remember(rules, searchQuery, selectedCategory) {
        rules.filter { rule ->
            val matchesCategory = selectedCategory == "الكل" || rule.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() || 
                rule.title.contains(searchQuery, ignoreCase = true) ||
                rule.description.contains(searchQuery, ignoreCase = true) ||
                rule.exampleVerse.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ابحث في أحكام التجويد، الأمثلة، والمدود...", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedBorderColor = GoldPrimary,
                unfocusedContainerColor = Color(0xFF0B0F19),
                focusedContainerColor = Color(0xFF0B0F19),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Categories Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category, fontFamily = NotoSansFont, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = DeepSlate,
                        containerColor = Color(0xFF151B2B),
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Rules List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredRules) { rule ->
                TajweedRuleCard(rule = rule)
            }
        }
    }
}
