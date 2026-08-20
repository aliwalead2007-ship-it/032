package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ClonedStylePreset(
    val id: String,
    val title: String,
    val sourcePlatform: String, // "YouTube Shorts", "TikTok", "Instagram Reels", "Custom"
    val description: String,
    val analysis: VideoStyleAnalysis,
    val isPreset: Boolean = false
)

object StyleVaultManager {
    private const val PREFS_NAME = "qabas_cloned_styles_vault"

    val defaultPresets = listOf(
        ClonedStylePreset(
            id = "preset_epic_doc",
            title = "أسلوب الوثائقيات الملحمية 🏛️",
            sourcePlatform = "YouTube Shorts",
            description = "ألوان داكنة دافئة مع تباين ذهبي سينمائي، صوت وثائقي عميق، وزووم ناعم وتأثيرات أثرية.",
            analysis = VideoStyleAnalysis(
                dominantColors = "أسود سينمائي عميق + ذهبي دافئ (Contrast 1:10)",
                transitionSpeed = "بطيئة متوازنة (3-4 ثوان لكل مشهد)",
                movementPatterns = "Slow Cinematic Zoom-In & Pan",
                overallRhythm = "ملحمي، خشوع، تصاعدي",
                audioStyle = "صوت وثائقي عميق + مؤكدات صدى ناعمة بدون إيقاع",
                typographyStyle = "خط كوفي عريض متوهج مع ظهور كلمة بكلمة",
                contentTone = "مؤثر ومهيب",
                targetAudience = "المهتمون بالتاريخ والتدبر",
                keywords = listOf("epic", "history", "cinematic", "gold")
            ),
            isPreset = true
        ),
        ClonedStylePreset(
            id = "preset_viral_3nvus",
            title = "أسلوب الفيديوهات السريعة (3nvus Style) ⚡",
            sourcePlatform = "TikTok",
            description = "قصات خاطفة كل 1.5 ثانية، كابشن أصفر نيون في المنتصف، خطاف صادم في أول ثانية مع مؤثرات Swoosh.",
            analysis = VideoStyleAnalysis(
                dominantColors = "أسود داكن + أصفر نيون شاشات",
                transitionSpeed = "خاطفة جداً (1.2 - 1.8 ثانية لكل مشهد)",
                movementPatterns = "Glitch / Whip Zoom / Speed Ramp",
                overallRhythm = "حماسي مشدود لخطف الانتباه",
                audioStyle = "صوت جهوري حماسي + مؤثرات حركة خاطفة Swoosh",
                typographyStyle = "خط أصفر عريض متمركز بمنتصف الشاشة animated",
                contentTone = "حماسي ومباشر",
                targetAudience = "جيل الشباب والجمهور السريع",
                keywords = listOf("fast", "viral", "neon", "tiktok")
            ),
            isPreset = true
        ),
        ClonedStylePreset(
            id = "preset_sunset_quran",
            title = "أسلوب تلاوات الغروب والسكينة 🌅",
            sourcePlatform = "Instagram Reels",
            description = "خلفيات طبيعة و غروب دافئة، خط قرآن تجويدي فاخر باللون الأبيض، ونبرة تلاوة خاشعة مرققة.",
            analysis = VideoStyleAnalysis(
                dominantColors = "ألوان غروب برتقالية دافئة وأسود خفيف",
                transitionSpeed = "سلسة جداً وناعمة (Slow Fade Out)",
                movementPatterns = "Horizontal Smooth Pan (طبيعة)",
                overallRhythm = "سكينة وراحة نفسية",
                audioStyle = "تلاوة خاشعة ناعمة + صوت عصافير/مطر محيطي",
                typographyStyle = "خط عثماني/أميري فاخر باللون الأبيض الناصع",
                contentTone = "روحاني هادئ",
                targetAudience = "مُحبو التدبر والتلاوات الخاشعة",
                keywords = listOf("nature", "quran", "peaceful", "sunset")
            ),
            isPreset = true
        )
    )

    fun getVaultStyles(context: Context): List<ClonedStylePreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("custom_styles", null) ?: return defaultPresets
        try {
            val list = mutableListOf<ClonedStylePreset>()
            list.addAll(defaultPresets)
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val analysisObj = obj.getJSONObject("analysis")
                val titleStr = obj.optString("title", "أسلوب مخصص")
                val analysis = VideoStyleAnalysis(
                    detectedStyle = analysisObj.optString("detectedStyle", titleStr),
                    dominantColors = analysisObj.optString("dominantColors"),
                    transitionSpeed = analysisObj.optString("transitionSpeed"),
                    movementPatterns = analysisObj.optString("movementPatterns"),
                    overallRhythm = analysisObj.optString("overallRhythm"),
                    audioStyle = analysisObj.optString("audioStyle"),
                    typographyStyle = analysisObj.optString("typographyStyle"),
                    contentTone = analysisObj.optString("contentTone"),
                    targetAudience = analysisObj.optString("targetAudience")
                )
                list.add(
                    ClonedStylePreset(
                        id = obj.getString("id"),
                        title = titleStr,
                        sourcePlatform = obj.optString("sourcePlatform", "UserStyle"),
                        description = obj.optString("description", "أسلوب خاص بالمستخدم"),
                        analysis = analysis,
                        isPreset = false
                    )
                )
            }
            return list
        } catch (e: Exception) {
            return defaultPresets
        }
    }

    fun saveCustomStyle(context: Context, style: ClonedStylePreset) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCustoms = getVaultStyles(context).filter { !it.isPreset && it.id != style.id }.toMutableList()
        currentCustoms.add(0, style) // top

        val jsonArray = JSONArray()
        for (item in currentCustoms) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("sourcePlatform", item.sourcePlatform)
                put("description", item.description)
                put("analysis", JSONObject().apply {
                    put("detectedStyle", item.analysis.detectedStyle.ifEmpty { item.title })
                    put("dominantColors", item.analysis.dominantColors)
                    put("transitionSpeed", item.analysis.transitionSpeed)
                    put("movementPatterns", item.analysis.movementPatterns)
                    put("overallRhythm", item.analysis.overallRhythm)
                    put("audioStyle", item.analysis.audioStyle)
                    put("typographyStyle", item.analysis.typographyStyle)
                    put("contentTone", item.analysis.contentTone)
                    put("targetAudience", item.analysis.targetAudience)
                })
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_styles", jsonArray.toString()).apply()
    }

    fun deleteCustomStyle(context: Context, styleId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCustoms = getVaultStyles(context).filter { !it.isPreset && it.id != styleId }

        val jsonArray = JSONArray()
        for (item in currentCustoms) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("sourcePlatform", item.sourcePlatform)
                put("description", item.description)
                put("analysis", JSONObject().apply {
                    put("detectedStyle", item.analysis.detectedStyle.ifEmpty { item.title })
                    put("dominantColors", item.analysis.dominantColors)
                    put("transitionSpeed", item.analysis.transitionSpeed)
                    put("movementPatterns", item.analysis.movementPatterns)
                    put("overallRhythm", item.analysis.overallRhythm)
                    put("audioStyle", item.analysis.audioStyle)
                    put("typographyStyle", item.analysis.typographyStyle)
                    put("contentTone", item.analysis.contentTone)
                    put("targetAudience", item.analysis.targetAudience)
                })
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_styles", jsonArray.toString()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStyleClonerScreen(
    onBack: () -> Unit,
    onGenerateProjectWithStyle: (topic: String, style: VideoStyleAnalysis) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputUrlOrDesc by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var activeAnalysisResult by remember { mutableStateOf<VideoStyleAnalysis?>(null) }
    var newTopicText by remember { mutableStateOf("") }
    
    var vaultList by remember { mutableStateOf(StyleVaultManager.getVaultStyles(context)) }
    var selectedPreset by remember { mutableStateOf<ClonedStylePreset?>(vaultList.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Translator.tr("مستنسخ الفيديوهات والأساليب الناجحة"),
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Translator.tr("رجوع"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner Explainer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldPrimary, Color(0xFF1E293B))))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                Translator.tr("خلطة الفيديوهات الناجحة 🪄"),
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            Translator.tr("بدلاً من البدء من الصفر! ضَع رابط أي فيديو يعجبك (Reels/TikTok/Shorts) وسيقوم محرك الذكاء الاصطناعي بتفكيك أسلوبه، خطافه، نبرته، وإيقاعه، وحفظ الخلطة لتطبيقها فوراً على موضوعاتك الجديدة!"),
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = CairoFont,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 2. Input Reference Link / Description Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            Translator.tr("1. أدخل رابط الفيديو المراد تقليده أو وصف أسلوبه:"),
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = inputUrlOrDesc,
                            onValueChange = { inputUrlOrDesc = it },
                            placeholder = {
                                Text(
                                    Translator.tr("مثال: https://youtube.com/shorts/... أو (أسلوب وثائقي داكن مع خط أصفر وصوت خاشع)"),
                                    color = Color.Gray,
                                    fontFamily = CairoFont,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (inputUrlOrDesc.isBlank()) {
                                    Toast.makeText(context, Translator.tr("الرجاء إدخال رابط أو وصف للفيديو أولاً!"), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isAnalyzing = true
                                coroutineScope.launch {
                                    try {
                                        val result = RealGeminiService.analyzeVideoStyle(inputUrlOrDesc)
                                        activeAnalysisResult = result
                                        
                                        val styleTitle = Translator.tr("أسلوب مُستنسخ: ") + inputUrlOrDesc.take(18) + "..."
                                        // 1. Absorb into StyleBrain core engine & create structured StyleObject
                                        val absorbed = StyleBrain.absorbStyleFromAnalysis(
                                            context = context,
                                            name = styleTitle,
                                            source = inputUrlOrDesc,
                                            analysisResult = result
                                        )

                                        // Store as StyleObject in Firestore
                                        val styleObj = StyleObject.fromAbsorbedStyle(absorbed, CloudServices.Auth.getCurrentUserId() ?: "creator_local")
                                        StyleBrain.storeStyleObjectInFirestore(styleObj)
                                        
                                        // 2. Save to vault presets
                                        val customPreset = ClonedStylePreset(
                                            id = "custom_${System.currentTimeMillis()}",
                                            title = styleTitle,
                                            sourcePlatform = if (inputUrlOrDesc.contains("youtube")) "YouTube Shorts" else if (inputUrlOrDesc.contains("tiktok")) "TikTok" else "Reels",
                                            description = result.dominantColors + " | " + result.transitionSpeed,
                                            analysis = result
                                        )
                                        StyleVaultManager.saveCustomStyle(context, customPreset)
                                        vaultList = StyleVaultManager.getVaultStyles(context)
                                        selectedPreset = customPreset
                                        
                                        Toast.makeText(context, Translator.tr("تم تحليل وتفكيك أسلوب الفيديو وهضمه في عقل الأساليب بنجاح! 🧠✨"), Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, Translator.tr("حدث خطأ أثناء التحليل، تم استخدام الأسلوب السينمائي المحسن."), Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isAnalyzing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAnalyzing
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translator.tr("جاري تحليل وتفكيك الفيديو بـ Gemini..."), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = DeepSlate)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translator.tr("تفكيك واستنساخ أسلوب الفيديو 🪄"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Active Style Blueprint Breakdown Display
            item {
                activeAnalysisResult?.let { analysis ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldPrimary, GoldPrimary)))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                Translator.tr("📊 المخطط المستخرج من الفيديو (Style Blueprint):"),
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            StyleFeatureChip(label = "الألوان والتباين", value = analysis.dominantColors, icon = Icons.Default.Palette)
                            StyleFeatureChip(label = "سرعة المونتاج", value = analysis.transitionSpeed, icon = Icons.Default.Speed)
                            StyleFeatureChip(label = "حركة الكاميرا", value = analysis.movementPatterns, icon = Icons.Default.Videocam)
                            StyleFeatureChip(label = "الهوية الصوتية", value = analysis.audioStyle, icon = Icons.Default.GraphicEq)
                            StyleFeatureChip(label = "الكابشن والخط", value = analysis.typographyStyle, icon = Icons.Default.TextFields)
                            StyleFeatureChip(label = "النبرة والعاطفة", value = analysis.contentTone, icon = Icons.Default.Mood)
                        }
                    }
                }
            }

            // 4. Style Vault Header & Preset Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Translator.tr("2. مكتبة الأساليب المُستنسخة والمحفوظة:"),
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${vaultList.size} " + Translator.tr("أساليب"),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(vaultList) { preset ->
                val isSelected = selectedPreset?.id == preset.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPreset = preset
                            activeAnalysisResult = preset.analysis
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF151B2B) else CardSurface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldPrimary, GoldPrimary))) else null
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPreset = preset
                                        activeAnalysisResult = preset.analysis
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                )
                                Text(
                                    preset.title,
                                    color = if (isSelected) GoldPrimary else Color.White,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    preset.sourcePlatform,
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            preset.description,
                            color = Color.Gray,
                            fontFamily = CairoFont,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
            }

            // 5. Apply Cloned Style to New Topic Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldPrimary, Color(0xFF1E293B))))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            Translator.tr("3. أنشئ فيديو جديد بالأسلوب المحدد الآن 🔥"),
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            Translator.tr("اكتب موضوع الفيديو الجديد، وسيتم توليد السيناريو، البصريات، التلاوة/الصوت، والتراكيب بنفس الأسلوب المستنسخ:"),
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = newTopicText,
                            onValueChange = { newTopicText = it },
                            placeholder = { Text(Translator.tr("مثال: قصة النبي يونس في بطن الحوت / تدبر سورة الملك"), color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (newTopicText.isBlank()) {
                                    Toast.makeText(context, Translator.tr("الرجاء إدخال موضوع الفيديو الجديد!"), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val styleToApply = activeAnalysisResult ?: selectedPreset?.analysis ?: StyleVaultManager.defaultPresets.first().analysis
                                Toast.makeText(context, Translator.tr("جاري بدء توليد الفيديو بـ الخلطة المستنسخة! 🚀"), Toast.LENGTH_SHORT).show()
                                onGenerateProjectWithStyle(newTopicText, styleToApply)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = DeepSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Translator.tr("توليد الفيديو الكامل بهذا الأسلوب الان 🔥"),
                                color = DeepSlate,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleFeatureChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ", color = Color.LightGray, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
    }
}
