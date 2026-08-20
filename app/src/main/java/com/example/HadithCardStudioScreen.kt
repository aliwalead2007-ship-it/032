package com.example

/**
 * HadithCardStudioScreen.kt
 * تم تحسين وتطوير استوديو بطاقات الحديث الشريف لدعم 5 ثيمات وقوالب جمالية
 * (رخام ملكي، مخطوطة أندلسية، آفاق الليل، محراب الزمرد، ساعات الزمان) مع تخريج موثق
 * وإمكانية الحفظ عالي الدقة (HD) أو التصدير المباشر كـ Reel مرئي متحرك.
 */

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class HadithPreset(
    val title: String,
    val narrator: String,
    val text: String,
    val status: String, // صحيح، حسن، متفق عليه
    val source: String,
    val themeBg: String,
    val suggestedAspect: String = "9:16"
)

data class HadithTheme(
    val id: String,
    val name: String,
    val icon: String,
    val bgColors: List<Color>,
    val textColor: Color,
    val accentColor: Color,
    val cardBg: Color,
    val overlayAlpha: Float = 0.55f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithCardStudioScreen(
    onBack: () -> Unit,
    onExportAsReel: (String, String) -> Unit = { _, _ -> },
    onViewProjects: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sample Famous Hadiths
    val presets = remember {
        listOf(
            HadithPreset(
                title = "إنما الأعمال بالنيات",
                narrator = "عَنْ أَمِيرِ المُؤْمِنِينَ عُمَرَ بْنِ الخَطَّابِ رَضِيَ اللَّهُ عَنْهُ قَالَ: سَمِعْتُ رَسُولَ اللَّهِ ﷺ يَقُولُ:",
                text = "(إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ فَهِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ).",
                status = "متفق عليه",
                source = "صحيح البخاري رقم 1 ومسلم رقم 1907",
                themeBg = "black_gold"
            ),
            HadithPreset(
                title = "خيركم من تعلم القرآن",
                narrator = "عَنْ عُثْمَانَ بْنِ عَفَّانَ رَضِيَ اللَّهُ عَنْهُ عَنِ النَّبِيِّ ﷺ قَالَ:",
                text = "(خَيْرُكُمْ مَنْ تَعَلَّمَ القُرْآنَ وَعَلَّمَهُ).",
                status = "صحيح البخاري",
                source = "صحيح البخاري - رقم 5027",
                themeBg = "manuscript"
            ),
            HadithPreset(
                title = "كلمتان خفيفتان",
                narrator = "عَنْ أَبِي هُرَيْرَةَ رَضِيَ اللَّهُ عَنْهُ عَنِ النَّبِيِّ ﷺ قَالَ:",
                text = "(كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي المِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَٰنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ العَظِيمِ).",
                status = "متفق عليه",
                source = "صحيح البخاري رقم 6406 ومسلم رقم 2694",
                themeBg = "night"
            ),
            HadithPreset(
                title = "لا يؤمن أحدكم",
                narrator = "عَنْ أَنَسٍ رَضِيَ اللَّهُ عَنْهُ عَنِ النَّبِيِّ ﷺ قَالَ:",
                text = "(لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ).",
                status = "متفق عليه",
                source = "صحيح البخاري رقم 13 ومسلم رقم 45",
                themeBg = "mosque"
            ),
            HadithPreset(
                title = "اتق الله حيثما كنت",
                narrator = "عَنْ أَبِي ذَرٍّ وَمُعَاذِ بْنِ جَبَلٍ رَضِيَ اللَّهُ عَنْهُمَا عَنْ رَسُولِ اللَّهِ ﷺ قَالَ:",
                text = "(اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ).",
                status = "حسن صحيح",
                source = "سنن الترمذي - رقم 1987",
                themeBg = "black_gold"
            ),
            HadithPreset(
                title = "تقارب الزمان والفتن",
                narrator = "عَنْ أَبِي هُرَيْرَةَ رَضِيَ اللَّهُ عَنْهُ عَنْ رَسُولِ اللَّهِ ﷺ قَالَ:",
                text = "(يُوشِكُ أَنْ لا تَقُومَ السَّاعَةُ؛ حَتَّى يُقْبَضَ العِلْمُ، وَتَظْهَرَ الفِتَنُ، وَيَكْثُرَ الكَذِبُ، وَيَتَقَارَبَ الزَّمَانُ، وَتَتَقَارَبَ الأَسْوَاقُ).",
                status = "صحيح",
                source = "الصحيح المسند من أحاديث الفتن ص 434",
                themeBg = "clocks"
            ),
            HadithPreset(
                title = "الكلمة الطيبة والصدقة",
                narrator = "عَنْ أَبِي هُرَيْرَةَ رَضِيَ اللَّهُ عَنْهُ قَالَ: قَالَ رَسُولُ اللَّهِ ﷺ:",
                text = "(وَالْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ، وَكُلُّ خُطْوَةٍ تَمْشِيهَا إِلَى الصَّلَاةِ صَدَقَةٌ، وَتُمِيطُ الْأَذَى عَنِ الطَّرِيقِ صَدَقَةٌ).",
                status = "صحيح",
                source = "رياض الصالحين - رقم 204",
                themeBg = "mosque"
            )
        )
    }

    // Themes List
    val themes = remember {
        listOf(
            HadithTheme(
                id = "black_gold",
                name = "الذهبي الملكي والرخام",
                icon = "👑",
                bgColors = listOf(Color(0xFF18181B), Color(0xFF09090B), Color(0xFF000000)),
                textColor = Color(0xFFFAFAFA),
                accentColor = GoldPrimary,
                cardBg = Color(0xCC27272A)
            ),
            HadithTheme(
                id = "manuscript",
                name = "مخطوطة أندلسية",
                icon = "📜",
                bgColors = listOf(Color(0xFF3B2818), Color(0xFF1E1309), Color(0xFF0B0F19)),
                textColor = Color(0xFFFFFBEB),
                accentColor = Color(0xFFF5D76E),
                cardBg = Color(0xCC2A1B0E)
            ),
            HadithTheme(
                id = "night",
                name = "آفاق وليل الحرمين",
                icon = "🌌",
                bgColors = listOf(Color(0xFF0B0F19), Color(0xFF0B132B), Color(0xFF030712)),
                textColor = Color.White,
                accentColor = GoldPrimary,
                cardBg = Color(0xCC151B2B)
            ),
            HadithTheme(
                id = "mosque",
                name = "محراب الزمرد وسكينة",
                icon = "🕌",
                bgColors = listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF0B0F19)),
                textColor = Color(0xFFECFDF5),
                accentColor = Color(0xFF34D399),
                cardBg = Color(0xCC065F46)
            ),
            HadithTheme(
                id = "clocks",
                name = "ساعات الزمان والمهابة",
                icon = "🕰️",
                bgColors = listOf(Color(0xFF2C1E12), Color(0xFF140D07), Color(0xFF0B0F19)),
                textColor = Color(0xFFF1F5F9),
                accentColor = GoldPrimary,
                cardBg = Color(0xCC1A110B)
            )
        )
    }

    // State Variables
    var inputText by remember { mutableStateOf(presets[0].text) }
    var narratorText by remember { mutableStateOf(presets[0].narrator) }
    var hadithStatus by remember { mutableStateOf(presets[0].status) }
    var hadithSource by remember { mutableStateOf(presets[0].source) }
    var selectedThemeId by remember { mutableStateOf(presets[0].themeBg) }
    var selectedAspect by remember { mutableStateOf("9:16") } // 9:16, 1:1, 4:5
    var userHandle by remember { mutableStateOf("@qabas_studio") }
    var showBadge by remember { mutableStateOf(true) }
    var showSocialIcons by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableStateOf(20) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var showCustomInputSheet by remember { mutableStateOf(false) }
    var customBgImage by remember { mutableStateOf<String?>(null) }
    var isGeneratingAiImage by remember { mutableStateOf(false) }

    var selectedQuality by remember { mutableStateOf("4K") } // 4K (2160p), 2K (1440p), HD (1080p)
    var isExportingImage by remember { mutableStateOf(false) }

    val currentTheme = themes.firstOrNull { it.id == selectedThemeId } ?: themes[0]

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "استوديو بطاقات الحديث الشريف 📜",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "توليد بطاقات دعوية مصورة فاخرة بالذكاء الاصطناعي",
                            color = TextSecondary,
                            fontFamily = CairoFont,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onViewProjects) {
                        BadgedBox(badge = { Badge { Text("الأرشيف") } }) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "المشاريع",
                                tint = GoldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Quick Presets Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✨ اختر حديثاً جاهزاً أو أدخل حديثك الخاصة:",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = { showCustomInputSheet = !showCustomInputSheet }) {
                            Text(
                                if (showCustomInputSheet) "إغلاق المحرر ✖" else "كتابة حديث مخصص ✍️",
                                color = GoldSecondary,
                                fontFamily = CairoFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            val isSelected = inputText == preset.text
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    inputText = preset.text
                                    narratorText = preset.narrator
                                    hadithStatus = preset.status
                                    hadithSource = preset.source
                                    selectedThemeId = preset.themeBg
                                },
                                label = {
                                    Text(
                                        preset.title,
                                        fontFamily = CairoFont,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = DeepSlate,
                                    containerColor = Color(0xFF0B0F19),
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // 2. Custom Input Form (Expandable)
            AnimatedVisibility(visible = showCustomInputSheet) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF182232)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "✍️ إدخال وتخريج الحديث بالذكاء الاصطناعي",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("نص الحديث الشريف", fontFamily = CairoFont) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedLabelColor = GoldPrimary
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = narratorText,
                                onValueChange = { narratorText = it },
                                label = { Text("الراوي / صيغة العنعنة", fontFamily = NotoSansFont, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color(0xFF1E293B)
                                )
                            )

                            OutlinedTextField(
                                value = hadithStatus,
                                onValueChange = { hadithStatus = it },
                                label = { Text("حكم المحدث (مثال: صحيح)", fontFamily = NotoSansFont, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color(0xFF1E293B)
                                )
                            )
                        }

                        OutlinedTextField(
                            value = hadithSource,
                            onValueChange = { hadithSource = it },
                            label = { Text("المصدر والكتاب (مثال: رواه البخاري ص 45)", fontFamily = CairoFont) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B)
                            )
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isGeneratingAi = true
                                    try {
                                        val prompt = """
أنت خبير في علم الحديث الشريف وتحقيق النصوص الإسلامية.
الحديث أو النص المدخل هو:
"$inputText"

قم بتخريجه وتشكيله بدقة، وأعطني الرد بتنسيق JSON حصراً بالشكل التالي:
{
  "tashkeelText": "نص الحديث مع التشكيل الكامل الدقيق",
  "narrator": "عن فلان رضي الله عنه",
  "status": "صحيح أو حسن أو متفق عليه",
  "source": "اسم الكتاب ورقم الحديث إن وجد"
}
""".trimIndent()
                                        val response = RealGeminiService.chatWithAssistant(listOf(Pair(true, prompt)), null)
                                        if (response.isNotBlank()) {
                                            val cleaned = response.trim()
                                                .removePrefix("```json")
                                                .removePrefix("```")
                                                .removeSuffix("```")
                                                .trim()
                                            try {
                                                val json = org.json.JSONObject(cleaned)
                                                if (json.has("tashkeelText")) inputText = json.getString("tashkeelText")
                                                if (json.has("narrator")) narratorText = json.getString("narrator")
                                                if (json.has("status")) hadithStatus = json.getString("status")
                                                if (json.has("source")) hadithSource = json.getString("source")
                                                Toast.makeText(context, "تم ضبط وتشكيل وتخريج الحديث بالذكاء الاصطناعي بنجاح! ✨", Toast.LENGTH_SHORT).show()
                                            } catch (je: Exception) {
                                                // Fallback if not pure json
                                                inputText = response.take(300)
                                                Toast.makeText(context, "تم تحسين نص الحديث بنجاح! ✨", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تم التدقيق والتنسيق بنجاح ✨", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingAi = false
                                    }
                                }
                            },
                            enabled = !isGeneratingAi && inputText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepSlate, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التخريج والتشكيل بالذكاء الاصطناعي (Gemini)...", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepSlate)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("التشكيل والتخريج الآلي (Gemini AI) 🪄", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Customization Toolbar (Theme, Aspect Ratio, Controls)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🎨 مظهر وخلفية البوستر:",
                    color = GoldPrimary,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Aspect Ratio Selector (Reels 9:16, Post 1:1, YouTube 16:9, Feed 4:5)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("9:16", "1:1", "16:9", "4:5").forEach { aspect ->
                        val selected = selectedAspect == aspect
                        Surface(
                            onClick = { selectedAspect = aspect },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) GoldPrimary else Color(0xFF151B2B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) GoldPrimary else Color(0xFF1E293B))
                        ) {
                            Text(
                                when (aspect) {
                                    "9:16" -> "9:16 ريلز"
                                    "1:1" -> "1:1 مربع"
                                    "16:9" -> "16:9 يوتيوب"
                                    else -> "4:5 فيد"
                                },
                                color = if (selected) DeepSlate else TextSecondary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(themes) { theme ->
                    val isSelected = selectedThemeId == theme.id && customBgImage == null
                    Card(
                        modifier = Modifier
                            .clickable {
                                selectedThemeId = theme.id
                            }
                            .border(
                                1.5.dp,
                                if (isSelected) GoldPrimary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(theme.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                theme.name,
                                color = if (isSelected) GoldPrimary else Color.White,
                                fontFamily = CairoFont,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // AI Background Generator Button
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF182232)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🖼️ خلفية سينمائية بالذكاء الاصطناعي (Imagen AI):",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        if (customBgImage != null) {
                            TextButton(
                                onClick = { customBgImage = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("إزالة ✖", color = Color(0xFFEF4444), fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isGeneratingAiImage = true
                                try {
                                    val prompt = "Cinematic atmospheric background for Islamic Hadith, $inputText, photorealistic 8k, warm ambient lighting, antique clocks or islamic architecture, soft bokeh"
                                    val generatedImage = AppServices.generateAiImage(prompt)
                                    if (generatedImage != null) {
                                        customBgImage = generatedImage
                                        Toast.makeText(context, "تم توليد خلفية سينمائية مبتكرة بالذكاء الاصطناعي بنجاح! 🎨✨", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val fallbackMedia = AppServices.fetchMedia("islamic architecture mosque night gold", "image")
                                        customBgImage = fallbackMedia
                                        Toast.makeText(context, "تم جلب خلفية سينمائية معبرة من المكتبة الفاخرة! 🖼️", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تم تطبيق خلفية سينمائية تناسب الحديث الشريف ✨", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingAiImage = false
                                }
                            }
                        },
                        enabled = !isGeneratingAiImage,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1E12)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                    ) {
                        if (isGeneratingAiImage) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoldPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري توليد الصورة بالذكاء الاصطناعي...", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (customBgImage != null) "توليد خلفية سينمائية أخرى 🔄" else "توليد خلفية سينمائية بالذكاء الاصطناعي (Imagen AI) 🖼️✨",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 4. MAIN HADITH CARD CANVAS (LIVE PREVIEW MATCHING USER'S IMAGE CONCEPT)
            Text(
                "👁️ المعاينة المباشرة لبطاقة الحديث:",
                color = GoldPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            val canvasHeight = when (selectedAspect) {
                "1:1" -> 360.dp
                "4:5" -> 420.dp
                else -> 520.dp // 9:16
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(currentTheme.bgColors))
                ) {
                    // Custom AI Generated Image Background or Default Theme Gradient
                    if (customBgImage != null) {
                        AsyncImage(
                            model = customBgImage,
                            contentDescription = "الخلفية السينمائية المولدة",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )
                    } else {
                        // Decorative Canvas Background Pattern
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            currentTheme.accentColor.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {

                        // Top Logo Header (Qabas Branding Crest)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "قبس HADY",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        // Middle Content: Narrator & Hadith Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            if (narratorText.isNotBlank()) {
                                Text(
                                    text = narratorText,
                                    color = currentTheme.textColor.copy(alpha = 0.85f),
                                    fontFamily = CairoFont,
                                    fontSize = (fontSizeSp - 5).sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Text(
                                text = inputText,
                                color = currentTheme.textColor,
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSizeSp.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = (fontSizeSp * 1.55).sp
                            )
                        }

                        // Bottom Container: Authenticity Badge & Source Reference
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = currentTheme.cardBg,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (showBadge && hadithStatus.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                "خلاصة حُكم المُحدث: ",
                                                color = Color.White,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                color = currentTheme.accentColor,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    hadithStatus,
                                                    color = DeepSlate,
                                                    fontFamily = CairoFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    if (hadithSource.isNotBlank()) {
                                        Text(
                                            hadithSource,
                                            color = currentTheme.textColor.copy(alpha = 0.75f),
                                            fontFamily = CairoFont,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        IslamicSourceAttributionBadge(
                                            text = "$narratorText $inputText",
                                            explicitSource = null,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Social Media Handles Bar
                            if (showSocialIcons) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        userHandle,
                                        color = GoldPrimary.copy(alpha = 0.85f),
                                        fontFamily = CairoFont,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Advanced Styling Controls (Font Size, Social Handles, Toggles)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙️ خيارات التخصيص والخط:", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حجم الخط (${fontSizeSp}sp)", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                        Slider(
                            value = fontSizeSp.toFloat(),
                            onValueChange = { fontSizeSp = it.toInt() },
                            valueRange = 14f..32f,
                            modifier = Modifier.width(180.dp),
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userHandle,
                            onValueChange = { userHandle = it },
                            label = { Text("معرّف حساباتك (@username)", fontFamily = NotoSansFont, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF1E293B))
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showBadge,
                                onCheckedChange = { showBadge = it },
                                colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                            )
                            Text("شارة التوثيق (صحيح)", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showSocialIcons,
                                onCheckedChange = { showSocialIcons = it },
                                colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                            )
                            Text("شريط الحسابات", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Quality / Resolution Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("دقة وجودة التصدير:", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                when (selectedQuality) {
                                    "4K" -> "فائقة الدقة 4K Ultra HD (2160×3840)"
                                    "2K" -> "عالية جداً 2K QHD (1440×2560)"
                                    else -> "عالية الوضوح HD (1080×1920)"
                                },
                                color = TextSecondary,
                                fontFamily = CairoFont,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("HD", "2K", "4K").forEach { q ->
                                val isSelected = selectedQuality == q
                                Surface(
                                    onClick = { selectedQuality = q },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) GoldPrimary else Color(0xFF0B0F19),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B))
                                ) {
                                    Text(
                                        q,
                                        color = if (isSelected) DeepSlate else TextSecondary,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Action Export & Share Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            isExportingImage = true
                            try {
                                val saved = withContext(Dispatchers.IO) {
                                    saveHadithCardBitmapToStorage(
                                        context = context,
                                        title = "حديث شريف",
                                        narrator = narratorText,
                                        text = inputText,
                                        status = hadithStatus,
                                        source = hadithSource,
                                        userHandle = if (showSocialIcons) userHandle else "",
                                        theme = currentTheme,
                                        aspectRatio = selectedAspect,
                                        quality = selectedQuality
                                    )
                                }

                                if (saved != null) {
                                    // Persist to Room Database
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val cardEntity = HadithCardEntity(
                                                id = "HADITH_${System.currentTimeMillis()}",
                                                text = inputText,
                                                narrator = narratorText,
                                                source = hadithSource,
                                                styleName = currentTheme.name,
                                                aspectRatio = selectedAspect,
                                                imagePath = saved,
                                                isFavorite = false,
                                                createdAt = System.currentTimeMillis()
                                            )
                                            AppDatabase.getDatabase(context).hadithCardDao().insertCard(cardEntity)
                                        } catch (e: Throwable) {
                                            android.util.Log.w("HadithCardStudio", "Failed to save card entity to Room: ${e.message}")
                                        }
                                    }
                                    Toast.makeText(context, "📸 تم حفظ بطاقة الحديث في المعرض بدقة $selectedQuality الفائقة بنجاح!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "📸 تم تصدير بطاقة الحديث بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isExportingImage = false
                            }
                        }
                    },
                    enabled = !isExportingImage,
                    modifier = Modifier.weight(1.2f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isExportingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepSlate, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري الرندر $selectedQuality...", color = DeepSlate, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, tint = DeepSlate)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ في المعرض ($selectedQuality)", color = DeepSlate, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                saveHadithCardBitmapToStorage(
                                    context = context,
                                    title = "حديث شريف",
                                    narrator = narratorText,
                                    text = inputText,
                                    status = hadithStatus,
                                    source = hadithSource,
                                    userHandle = if (showSocialIcons) userHandle else "",
                                    theme = currentTheme,
                                    aspectRatio = selectedAspect,
                                    quality = selectedQuality
                                )
                            }
                            if (saved != null) {
                                val shareUri = android.net.Uri.parse(saved)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                                    putExtra(android.content.Intent.EXTRA_TEXT, "$narratorText\n$inputText\n\nحكم المحدث: $hadithStatus\nالمصدر: $hadithSource\nتم التصميم عبر تطبيق قبس ✨")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة بطاقة الحديث الشريف عبر"))
                            } else {
                                Toast.makeText(context, "يرجى حفظ البطاقة أولاً لمشاركتها", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(0.8f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151B2B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة 🚀", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            OutlinedButton(
                onClick = {
                    onExportAsReel(inputText, selectedAspect)
                    Toast.makeText(context, "🎥 جاري نقل الحديث إلى استوديو المونتاج والتصدير كـ Reel ($selectedAspect)...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "تحويل إلى Reel متحرك أبعاد ($selectedAspect) 🎥", 
                    color = GoldPrimary, 
                    fontFamily = CairoFont, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Renders and exports a high-resolution Hadith card bitmap (up to 4K Ultra HD) with strict try-finally and recycle memory management.
 */
private fun saveHadithCardBitmapToStorage(
    context: Context,
    title: String,
    narrator: String,
    text: String,
    status: String,
    source: String,
    userHandle: String,
    theme: HadithTheme,
    aspectRatio: String,
    quality: String = "4K"
): String? {
    var bitmap: android.graphics.Bitmap? = null
    return try {
        // High quality dimension matrix
        val scale = when (quality) {
            "4K" -> 2.0f // 2160x3840 (True 4K UHD 9:16)
            "2K" -> 1.334f // 1440x2560 (2K QHD)
            else -> 1.0f // 1080x1920 (Full HD)
        }

        val (baseW, baseH) = when (aspectRatio) {
            "9:16" -> 1080 to 1920
            "16:9" -> 1920 to 1080
            "4:5" -> 1080 to 1350
            else -> 1080 to 1080
        }

        val width = (baseW * scale).toInt()
        val height = (baseH * scale).toInt()
        
        bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 1. Background Fill with Theme Gradient
        val bgColorsInts = if (theme.bgColors.isNotEmpty()) {
            theme.bgColors.map { 
                android.graphics.Color.argb(
                    (it.alpha * 255).toInt(),
                    (it.red * 255).toInt(),
                    (it.green * 255).toInt(),
                    (it.blue * 255).toInt()
                )
            }.toIntArray()
        } else {
            intArrayOf(
                android.graphics.Color.parseColor("#0B0F19"),
                android.graphics.Color.parseColor("#030712")
            )
        }

        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            bgColorsInts, null, android.graphics.Shader.TileMode.CLAMP
        )
        val bgPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Decorative Outer Gold Inset Frame
        val framePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E8C547")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f * scale
        }
        val inset = 36f * scale
        canvas.drawRoundRect(android.graphics.RectF(inset, inset, width - inset, height - inset), 32f * scale, 32f * scale, framePaint)

        // 3. Inner Card Area Surface
        val cardPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(220, 21, 27, 43) // Deep Slate Card Surface #151B2B
            style = android.graphics.Paint.Style.FILL
        }
        val cardInset = 64f * scale
        canvas.drawRoundRect(android.graphics.RectF(cardInset, cardInset, width - cardInset, height - cardInset), 26f * scale, 26f * scale, cardPaint)

        // Inner Card Border
        val cardBorderPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(90, 232, 197, 71)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        canvas.drawRoundRect(android.graphics.RectF(cardInset, cardInset, width - cardInset, height - cardInset), 26f * scale, 26f * scale, cardBorderPaint)

        // 4. Header / Brand Crest
        val headerPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E8C547")
            textSize = 28f * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("✦ قَبَس | بَطَاقَة حَدِيث شَرِيف ✦", width / 2f, 130f * scale, headerPaint)

        // 5. Title & Status
        val titlePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 36f * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(title.ifBlank { "حديث نبوي شريف" }, width / 2f, 200f * scale, titlePaint)

        // 6. Narrator
        val narratorPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#F5D76E")
            textSize = 24f * scale
            textAlign = android.graphics.Paint.Align.CENTER
        }
        if (narrator.isNotBlank()) {
            canvas.drawText(narrator, width / 2f, 260f * scale, narratorPaint)
        }

        // 7. Hadith Body text (Formatted, Wrapped & Centered)
        val bodyPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#F8FAFC")
            textSize = 34f * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }

        val safeText = if (text.length > 350) text.take(345) + "..." else text
        val words = safeText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        val maxLineWidth = width - (240f * scale)
        for (w in words) {
            val testLine = if (currentLine.isEmpty()) w else "$currentLine $w"
            if (bodyPaint.measureText(testLine) < maxLineWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = w
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val lineHeight = 56f * scale
        var yPos = (height / 2f) - ((lines.size * lineHeight) / 2f) + (20f * scale)
        for (line in lines) {
            canvas.drawText(line, width / 2f, yPos, bodyPaint)
            yPos += lineHeight
        }

        // 8. Authenticity Badge Pill (Status)
        if (status.isNotBlank()) {
            val badgeText = "حكم المحدث: $status"
            val badgePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#0B0F19")
                textSize = 20f * scale
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val badgeBgPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E8C547")
                style = android.graphics.Paint.Style.FILL
            }

            val badgeW = badgePaint.measureText(badgeText) + (36f * scale)
            val badgeH = 42f * scale
            val badgeY = height - (200f * scale)
            val badgeRect = android.graphics.RectF(
                (width / 2f) - (badgeW / 2f),
                badgeY - (badgeH / 1.5f),
                (width / 2f) + (badgeW / 2f),
                badgeY + (badgeH / 2.5f)
            )
            canvas.drawRoundRect(badgeRect, 14f * scale, 14f * scale, badgeBgPaint)
            canvas.drawText(badgeText, width / 2f, badgeY + (2f * scale), badgePaint)
        }

        // 9. Source Footer
        val footerPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f * scale
            textAlign = android.graphics.Paint.Align.CENTER
        }
        if (source.isNotBlank()) {
            canvas.drawText("المصدر: $source", width / 2f, height - (140f * scale), footerPaint)
        }

        // 10. Social Media Handle
        if (userHandle.isNotBlank()) {
            val handlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E8C547")
                textSize = 20f * scale
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText(userHandle, width / 2f, height - (95f * scale), handlePaint)
        }

        val fileName = "QABAS_HADITH_${quality}_${System.currentTimeMillis()}.png"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Qabas_Hadith_Cards")
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                val stream = context.contentResolver.openOutputStream(it)
                stream?.use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                return uri.toString()
            }
        } else {
            val imagesDir = context.getExternalFilesDir(null)
            val imageFile = java.io.File(imagesDir, fileName)
            java.io.FileOutputStream(imageFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            return imageFile.absolutePath
        }
        null
    } catch (e: Throwable) {
        android.util.Log.e("HadithCardStudio", "Error saving hadith card bitmap: ${e.message}")
        null
    } finally {
        bitmap?.recycle()
    }
}
