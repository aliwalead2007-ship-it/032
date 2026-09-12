package com.qabas.app

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@Immutable
data class ReelTemplate(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val durationText: String,
    val styleBadge: String,
    val sampleScript: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    onBack: () -> Unit,
    onStartCreating: () -> Unit = {},
    onStartCreatingWithScript: (String) -> Unit = {},
    onOpenTeleprompter: (String) -> Unit = {},
    onStartSmartDirector: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("studio") } // "studio", "templates", "feed"

    var currentStudioStep by remember { mutableIntStateOf(0) } // 0: فكرة, 1: سكريبت, 2: B-Roll, 3: كابشنز ومونتاج, 4: معاينة وتصدير
    val studioSteps = remember {
        listOf(
            "1. الفكرة والخطاف 💡",
            "2. السيناريو 📜",
            "3. نمط B-Roll 🕌",
            "4. الكابشنز 🎨",
            "5. المعاينة والإنتاج 🎬"
        )
    }

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabHeaderButton("استوديو الريلز", selectedTab == "studio") { selectedTab = "studio" }
                        Spacer(modifier = Modifier.width(12.dp))
                        TabHeaderButton("القوالب الفيروسية", selectedTab == "templates") { selectedTab = "templates" }
                        Spacer(modifier = Modifier.width(12.dp))
                        TabHeaderButton("مجتمع الإلهام", selectedTab == "feed") { selectedTab = "feed" }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("العودة"), tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19))
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(280)))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(200)))
            },
            label = "ReelsTabTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { targetTab ->
            when (targetTab) {
                "studio" -> ReelsStudioSection(
                    currentStep = currentStudioStep,
                    onStepChanged = { currentStudioStep = it },
                    steps = studioSteps,
                    onStartCreatingWithScript = onStartCreatingWithScript,
                    onOpenTeleprompter = onOpenTeleprompter,
                    onStartSmartDirector = onStartSmartDirector
                )
                "templates" -> ReelsTemplatesSection { script ->
                    onStartCreatingWithScript(script)
                }
                "feed" -> ReelsFeedSection()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsStudioSection(
    currentStep: Int,
    onStepChanged: (Int) -> Unit,
    steps: List<String>,
    onStartCreatingWithScript: (String) -> Unit,
    onOpenTeleprompter: (String) -> Unit = {},
    onStartSmartDirector: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var scriptText by remember { mutableStateOf("") }
    var topicForHook by remember { mutableStateOf("") }
    var isGeneratingFullReel by remember { mutableStateOf(false) }
    var fullReelProgressStep by remember { mutableStateOf("") }
    var fullReelProgressPercent by remember { mutableFloatStateOf(0f) }
    var generatedHooks by remember { mutableStateOf<List<String>>(emptyList()) }

    // Captions & Customizer State
    var showCaptionsSheet by remember { mutableStateOf(false) }
    var selectedCaptionStyle by remember { mutableStateOf("أسلوب 3nvus نيون 🌟") }
    var isJumpCutEnabled by remember { mutableStateOf(true) }
    var selectedBRollTheme by remember { mutableStateOf("مآذن سينمائية 🕌") }
    
    // Hashtags generator state
    var generatedHashtags by remember { mutableStateOf("") }
    var chosenStyleMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingHashtags by remember { mutableStateOf(false) }

    val quickTopics = remember {
        listOf(
            "سر الخشوع في الصلاة 🕌",
            "تدبر آية عظيمة 📖",
            "فضل الاستغفار والتسبيح 💫",
            "قصة مؤثرة عن الصبر 🌟"
        )
    }

    fun launchSequentialReelGeneration(customTopic: String? = null) {
        val activeTopic = (customTopic ?: topicForHook).ifBlank { "محتوى إسلامي وتدبر هادف" }
        if (customTopic != null) {
            topicForHook = customTopic
        }
        isGeneratingFullReel = true
        fullReelProgressPercent = 0.15f
        fullReelProgressStep = "1/4 ✦ صياغة الخطاف الخاطف (Viral Hook) والعنوان المشوق..."
        

        scope.launch {
            try {
                // Style Brain Selection
                val bestStyle = StyleBrain.chooseBestStyleForIdea(activeTopic, 30, "تيك توك ريلز, حماسي هادف", "الشباب")
                val stylePrompt = if (bestStyle != null) {
                    chosenStyleMessage = "🧠 تم اختيار الأسلوب الأمثل بناءً على خبرة العقل: ${bestStyle.name}"
                    val traits = (bestStyle.visualTraits + bestStyle.motionTraits + bestStyle.textTraits).joinToString("، ")
                    "| استخدم ميزات الأسلوب المهضوم: $traits"
                } else {
                    chosenStyleMessage = "🧠 العقل ما زال في طور البناء، سيتم استخدام الأسلوب القوي الافتراضي"
                    "| استخدم أسلوباً حماسياً قوياً، مونتاج سريع (Jump Cuts)، ألوان داكنة بهوية AI بنفسجية وسيان، ونصوص عريضة ديناميكية."
                }
                
                // 1. Generate Viral Hook & Title
                val hooksRaw = AppServices.generateViralHooks(activeTopic + " " + stylePrompt)

                val hooksList = hooksRaw.split("\n")
                    .filter { it.isNotBlank() && (it.contains("•") || it.contains("-") || it.first().isDigit()) }
                    .map { it.replace(Regex("^[0-9.•-]+\\s*"), "") }
                val bestHook = hooksList.firstOrNull() ?: "«هل تعلم السر العظيم وراء هذه الآية التي تقرأها يومياً؟»"
                generatedHooks = hooksList

                val titlesRaw = AppServices.generateTitles(activeTopic)
                val catchyTitle = titlesRaw.split("\n")
                    .firstOrNull { it.isNotBlank() }
                    ?.replace(Regex("^[0-9.•-]+\\s*"), "")
                    ?: "ريلز قبس: $activeTopic"

                // 2. Generate Quick 9:16 Script with timing
                fullReelProgressPercent = 0.50f
                fullReelProgressStep = "2/4 ✦ كتابة السيناريو الرأسي (9:16) وتنسيق الدعوة للتفاعل..."
                val rawScript = AppServices.generateQuickScript(activeTopic + " " + stylePrompt, 30)

                // 3. Smart B-Roll & Visual Style detection
                fullReelProgressPercent = 0.75f
                fullReelProgressStep = "3/4 ✦ تحليل المشاهد وتحديد نمط B-Roll والكابشنز..."
                val topicLower = activeTopic.lowercase()
                selectedBRollTheme = when {
                    topicLower.contains("صلاة") || topicLower.contains("مسجد") || topicLower.contains("أذان") -> "مآذن سينمائية 🕌"
                    topicLower.contains("تدبر") || topicLower.contains("قرآن") || topicLower.contains("آية") -> "طبيعة خاشعة 🍃"
                    topicLower.contains("صبر") || topicLower.contains("استغفار") || topicLower.contains("شروق") || topicLower.contains("أمل") -> "شروق الشمس 🌅"
                    else -> "نقوش إسلامية ✨"
                }

                // 4. Generate Viral Hashtags & Final Assembly
                fullReelProgressPercent = 0.95f
                fullReelProgressStep = "4/4 ✦ توليد الهاشتاغات الفيروسية وتجهيز استوديو الإنتاج..."
                val tags = AppServices.generateHashtags(activeTopic)
                generatedHashtags = tags

                // Assemble cohesive full structured script
                val compiledScript = buildString {
                    appendLine("🎬 العنوان: $catchyTitle")
                    appendLine("🔥 الخطاف: $bestHook")
                    appendLine()
                    appendLine(rawScript.ifBlank { "في كل لحظة من حياتنا، هناك إشارة وهداية من الله تحتاج فقط لقلب حاضر وعين متأملة. لا تدع يومك يمر دون أن تجدد نيتك وتسأل الله التوفيق والبركة." })
                    appendLine()
                    appendLine("📌 دعوة للتفاعل: تابع @Qabas_Studio لأثر لا ينقطع ✨")
                }
                scriptText = compiledScript

                // Persist auto-draft to local Room database in background
                try {
                    val scriptEntity = ReelScriptEntity(
                        id = "REEL_${System.currentTimeMillis()}",
                        topic = activeTopic,
                        hookText = bestHook,
                        bodyText = compiledScript,
                        ctaText = "تابع @Qabas_Studio لأثر لا ينقطع ✨",
                        duration = "0:30",
                        vibe = selectedBRollTheme,
                        captionStyle = selectedCaptionStyle,
                        createdAt = System.currentTimeMillis()
                    )
                    AppDatabase.getDatabase(context).reelScriptDao().insertScript(scriptEntity)
                } catch (dbEx: Throwable) {
                    android.util.Log.w("ReelsScreen", "Auto-draft save: ${dbEx.message}")
                }

                fullReelProgressPercent = 1.0f
                Toast.makeText(context, "اكتمل التوليد المتسلسل بنجاح! 🚀 جاهز للإنتاج الفوري", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                scriptText = "🎬 ريلز: $activeTopic\n🔥 الخطاف: «سر عظيم يغير يومك بالكامل في 30 ثانية!»\n\nتأمل في نعم الله عليك وتذكر أن مع العسر يسراً، وكل خطوة تخطوها بصدق تفتح لك أبواب الخير والسكينة.\n\n📌 دعوة: شارك الأثر وتابع @Qabas_Studio ✨"
                generatedHashtags = "#قبس #ريلز_إسلامي #تدبر #خشوع #أثر_جميل"
                fullReelProgressPercent = 1.0f
                Toast.makeText(context, "تم تجهيز مسودة الريلز بنجاح! 🎬", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingFullReel = false
                fullReelProgressStep = ""
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Banner
        item {
            ReelHeroHeaderCard(
                title = "استوديو الريلز الرأسي (9:16)",
                subtitle = "صناعة سريعة فائقة الجودة لإنستغرام، تيك توك، وYouTube Shorts."
            )
        }

        // Golden Step Indicator Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "مسار الإنتاج: ${steps[currentStep]}",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${currentStep + 1}/${steps.size}",
                            color = Color.White,
                            fontFamily = RobotoMonoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Step Dots and Lines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { index, _ ->
                            val isActive = index <= currentStep
                            val isCurrent = index == currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .padding(horizontal = 2.dp)
                                    .background(
                                        if (isActive) GoldPrimary else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Step Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(steps.size) { index ->
                            val isSelected = currentStep == index
                            Surface(
                                onClick = { onStepChanged(index) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF0B0F19),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldPrimary else Color(0xFF151B2B)
                                )
                            ) {
                                Text(
                                    steps[index],
                                    color = if (isSelected) GoldPrimary else Color.Gray,
                                    fontFamily = CairoFont,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1-Click Fast AI Reel Generator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الإنشاء الفوري الذكي (1-Click Reel)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Surface(
                            color = GoldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "توليد متسلسل وتلقائي ⚡",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "اختر موضوعاً سريعاً أو اكتب فكرتك لتوليد (الخطاف + السيناريو + B-Roll + الوسوم) فورياً وبأعلى معايير الإخراج:",
                        color = TextSecondary,
                        fontFamily = NotoSansFont,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick topic chips with instant 1-tap generation
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickTopics) { topicChip ->
                            val cleanTopic = topicChip.replace(Regex("[^\u0600-\u06FF\\s]"), "").trim()
                            val isSelected = topicForHook == cleanTopic
                            Surface(
                                onClick = {
                                    launchSequentialReelGeneration(cleanTopic)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color(0xFF0B0F19),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        topicChip,
                                        color = if (isSelected) GoldPrimary else Color.LightGray,
                                        fontFamily = CairoFont,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "توليد فوري",
                                        tint = if (isSelected) GoldPrimary else Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = topicForHook,
                        onValueChange = { topicForHook = it },
                        placeholder = { Text("اكتب فكرة المقطع... (مثال: أثر صلاة الفجر في البركة)", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            launchSequentialReelGeneration()
                        }),
                        trailingIcon = {
                            if (topicForHook.isNotBlank() && !isGeneratingFullReel) {
                                IconButton(onClick = { launchSequentialReelGeneration() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "توليد", tint = GoldPrimary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0B0F19),
                            unfocusedContainerColor = Color(0xFF0B0F19)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sequential Full AI Generator Action Button
                    Button(
                        onClick = {
                            launchSequentialReelGeneration()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isGeneratingFullReel
                    ) {
                        if (isGeneratingFullReel) {
                            QabasGlowingSpinner(size = 20.dp, strokeWidth = 2.dp, color = DeepSlate)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري هندسة الريلز الاحترافي...", color = DeepSlate, fontFamily = NotoSansFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد ريلز كامل بالذكاء الاصطناعي بنقرة واحدة 🚀", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (isGeneratingFullReel) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        fullReelProgressStep,
                                        color = GoldPrimary,
                                        fontFamily = CairoFont,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${(fullReelProgressPercent * 100).toInt()}%",
                                        color = Color.White,
                                        fontFamily = RobotoMonoFont,
                                        fontSize = 11.sp
                                    )
                                }
                                QabasProgressBar(
                                    progress = fullReelProgressPercent,
                                    label = "",
                                    height = 6.dp
                                )
                            }
                        }
                    }

                    if (chosenStyleMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = chosenStyleMessage!!,
                                color = GoldPrimary,
                                fontFamily = NotoSansFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (generatedHooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("خطافات بديلة مقترحة (اضغط للتبديل):", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            generatedHooks.take(2).forEach { hook ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (scriptText.contains("🔥 الخطاف:")) {
                                                scriptText = scriptText.replace(Regex("🔥 الخطاف:.*"), "🔥 الخطاف: $hook")
                                            } else {
                                                scriptText = "🔥 الخطاف: $hook\n\n$scriptText"
                                            }
                                            Toast.makeText(context, "تم استبدال الخطاف! ✍️", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(hook, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Script Input Box
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نص السيناريو والتعليق الصوتي 📜", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${scriptText.length} حرف", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    placeholder = { Text("اكتب أو الصق نص السيناريو هنا (أو استخدم مولد الخطاف والقوالب الفيروسية باللائحة)...", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (scriptText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val textToUse = scriptText
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val scriptEntity = ReelScriptEntity(
                                            id = "REEL_${System.currentTimeMillis()}",
                                            topic = topicForHook.ifBlank { "محتوى إسلامي 9:16" },
                                            hookText = generatedHooks.firstOrNull() ?: "",
                                            bodyText = textToUse,
                                            ctaText = "تابع @Qabas_Studio للمزيد",
                                            duration = "0:30",
                                            vibe = selectedBRollTheme,
                                            captionStyle = selectedCaptionStyle,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        AppDatabase.getDatabase(context).reelScriptDao().insertScript(scriptEntity)
                                    } catch (_: Throwable) {}
                                }
                                val fullInstructions = "$textToUse\n\nتوجيهات إضافية للمونتاج: استخدم خلفيات B-Roll بنمط $selectedBRollTheme، ونمط الكابشنز: $selectedCaptionStyle، وقص الصمت: ${if (isJumpCutEnabled) "مفعل" else "معطل"}."
                                onStartCreatingWithScript(fullInstructions)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إنتاج آلي 🎬", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val textToUse = scriptText
                                onOpenTeleprompter(textToUse)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بالمُلقن 🎙️", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val fullContent = buildString {
                                    append(scriptText)
                                    if (generatedHashtags.isNotBlank()) {
                                        append("\n\n")
                                        append(generatedHashtags)
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(fullContent))
                                Toast.makeText(context, "تم نسخ السيناريو والوسوم إلى الحافظة! 📋", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("نسخ 📋", fontFamily = NotoSansFont, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 9:16 Smartphone Preview Simulator Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("معاينة منطقة الأمان الشاشية (9:16 Safe Zones)", color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF151B2B), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("TikTok / Reels", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated 9:16 Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF151B2B), Color(0xFF0B0F19))))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Theme Icon Mock
                        Icon(
                            Icons.Default.Mosque,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.size(120.dp)
                        )

                        // Center Captions Overlay Simulation
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .background(
                                    if (selectedCaptionStyle.contains("نيون")) Color.Black.copy(alpha = 0.7f) else Color(0xFFE8C547),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = scriptText.ifBlank { "«معاينة الكابشنز والنصوص الفيروسية المتحركة هنا...»" },
                                color = if (selectedCaptionStyle.contains("نيون")) GoldPrimary else DeepSlate,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right Actions Column Simulation (Likes, Comments, Share)
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Icon(Icons.Default.Comment, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }

                        // Bottom Title Bar Simulation
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("@Qabas_Studio • قبس", color = Color.White.copy(alpha = 0.8f), fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Quick Editing Tools Row
        item {
            Column {
                Text("أدوات التحرير والمونتاج الرأسي ⚙️", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Captions Tool Card
                    ReelToolInteractiveCard(
                        title = "نصوص Captions",
                        subtitle = selectedCaptionStyle,
                        icon = Icons.Default.Subtitles,
                        accentColor = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = { showCaptionsSheet = true }
                    )

                    // Jump Cut Silence Removal Card
                    ReelToolInteractiveCard(
                        title = "قص الصمت",
                        subtitle = if (isJumpCutEnabled) "مُفعل (-30dB) ⚡" else "معطل ⚪",
                        icon = Icons.Default.ContentCut,
                        accentColor = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isJumpCutEnabled = !isJumpCutEnabled
                            Toast.makeText(context, if (isJumpCutEnabled) "تم تفعيل قص الصمت التلقائي ✂️" else "تم إيقاف قص الصمت", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // B-Roll Theme Card
                    ReelToolInteractiveCard(
                        title = "خلفية B-Roll",
                        subtitle = selectedBRollTheme,
                        icon = Icons.Default.VideoLibrary,
                        accentColor = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val themes = listOf("مآذن سينمائية 🕌", "شروق الشمس 🌅", "نقوش إسلامية ✨", "طبيعة خاشعة 🍃")
                            val nextIdx = (themes.indexOf(selectedBRollTheme) + 1) % themes.size
                            selectedBRollTheme = themes[nextIdx]
                            Toast.makeText(context, "تم تغيير الخلفية إلى: $selectedBRollTheme", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // AI Hashtag Generator Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مولد الوسوم والعناوين الفيروسية (AI Hashtags)", color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                isGeneratingHashtags = true
                                scope.launch {
                                    try {
                                        generatedHashtags = AppServices.generateHashtags(scriptText.ifBlank { "محتوى إسلامي وتدبر" })
                                    } catch (e: Exception) {
                                        generatedHashtags = "#قبس #ريلز_إسلامي #تدبر #حديث_شريف #استرجاع_الخشوع #دعوة_إلكترونية"
                                    } finally {
                                        isGeneratingHashtags = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151B2B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            enabled = !isGeneratingHashtags
                        ) {
                            if (isGeneratingHashtags) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("توليد الوسوم ✨", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                        }
                    }

                    if (generatedHashtags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(generatedHashtags, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2)
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(generatedHashtags))
                                    Toast.makeText(context, "تم نسخ الوسوم! 📋", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step Navigation Buttons (السابق والتالي) & Primary Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { onStepChanged(currentStep - 1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("السابق", color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                        }
                    }

                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = {
                                // Auto-save on next
                                val textToUse = scriptText
                                if (textToUse.isNotBlank()) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val scriptEntity = ReelScriptEntity(
                                                id = "REEL_${System.currentTimeMillis()}",
                                                topic = topicForHook.ifBlank { "محتوى إسلامي 9:16" },
                                                hookText = generatedHooks.firstOrNull() ?: "",
                                                bodyText = textToUse,
                                                ctaText = "تابع @Qabas_Studio للمزيد",
                                                duration = "0:30",
                                                vibe = selectedBRollTheme,
                                                captionStyle = selectedCaptionStyle,
                                                createdAt = System.currentTimeMillis()
                                            )
                                            AppDatabase.getDatabase(context).reelScriptDao().insertScript(scriptEntity)
                                        } catch (_: Throwable) {}
                                    }
                                }
                                onStepChanged(currentStep + 1)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("الخطوة التالية ➔", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        val textToUse = scriptText.ifBlank { "سيناريو فيديو قصير بأسلوب " + selectedCaptionStyle }
                        
                        // Persist Reel script to Room
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val scriptEntity = ReelScriptEntity(
                                    id = "REEL_${System.currentTimeMillis()}",
                                    topic = topicForHook.ifBlank { "محتوى إسلامي 9:16" },
                                    hookText = generatedHooks.firstOrNull() ?: "",
                                    bodyText = textToUse,
                                    ctaText = "تابع @Qabas_Studio للمزيد",
                                    duration = "0:30",
                                    vibe = selectedBRollTheme,
                                    captionStyle = selectedCaptionStyle,
                                    createdAt = System.currentTimeMillis()
                                )
                                AppDatabase.getDatabase(context).reelScriptDao().insertScript(scriptEntity)
                            } catch (e: Throwable) {
                                android.util.Log.w("ReelsScreen", "Failed to save reel script to Room: ${e.message}")
                            }
                        }

                        val fullInstructions = "$textToUse\n\nتوجيهات إضافية للمونتاج: استخدم خلفيات B-Roll بنمط $selectedBRollTheme، ونمط الكابشنز: $selectedCaptionStyle، وقص الصمت: ${if (isJumpCutEnabled) "مفعل" else "معطل"}."
                        onStartCreatingWithScript(fullInstructions)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = DeepSlate)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء المونتاج والإنتاج المباشر (9:16) 🎬", color = DeepSlate, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onStartSmartDirector,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح المخرج الذكي والتنفيذ الآلي ⚡", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Captions Customizer Bottom Sheet Modal
    if (showCaptionsSheet) {
        AlertDialog(
            onDismissRequest = { showCaptionsSheet = false },
            title = {
                Text("تخصيص أنماط الكابشنز المتحركة (Captions) 🎨", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر خط ونمط الكابشنز المستهدف للفيديو:", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)

                    val captionOptions = listOf(
                        "أسلوب 3nvus نيون 🌟",
                        "كلمة بكلمة صفراء 🟡",
                        "كابشنز شادوو داكن ⬛",
                        "نمط التفسير القرآني 📖"
                    )

                    captionOptions.forEach { style ->
                        val isSelected = selectedCaptionStyle == style
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) GoldPrimary.copy(alpha = 0.2f) else CardSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .clickable { selectedCaptionStyle = style }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { selectedCaptionStyle = style }, colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(style, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCaptionsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ النمط", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepSlate
        )
    }
}

@Composable
fun ReelsTemplatesSection(onSelectTemplateScript: (String) -> Unit) {
    val context = LocalContext.current

    val templates = remember {
        listOf(
            ReelTemplate(
                id = "t1",
                title = "تأمل سينمائي (9:16)",
                category = "تدبر قرآن",
                description = "قالب بأسلوب إخراجي سينمائي داكن مع صوت عميق ونصوص نيون صفراء.",
                durationText = "15-30 ثانية",
                styleBadge = "3nvus Neon",
                sampleScript = """
                    🔥 الخطاف: «آية واحدة لو تدبرتها اليوم لتبدل خوفك إلى طمأنينة!»
                    
                    💡 النص: قال تعالى: {أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ}. الطمأنينة ليست غياب المشاكل، بل حضور الله في قلبك مع كل نفس.
                    
                    📖 المصدر: سورة الرعد - آية 28
                """.trimIndent()
            ),
            ReelTemplate(
                id = "t2",
                title = "قصة صحابي بأسلوب غامض",
                category = "سير الأبطال",
                description = "بداية مشوقة تسأل عن صحابي جليل دون ذكر اسمه حتى نهاية الفيديو.",
                durationText = "45 ثانية",
                styleBadge = "وثائقي غامض",
                sampleScript = """
                    🔥 الخطاف: «رجل واحد كان إسلامه نقطة تحول هزت عرش الرحمن!»
                    
                    💡 القصة: أسلم وعمره 31 عاماً، ومات وعمره 37 عاماً! 6 سنوات في الإسلام كانت كافية لكي يخرج في جنازته 70 ألف ملك. هو سعد بن معاذ رضي الله عنه.
                    
                    📖 المصدر: صحيح البخاري ومسلم
                """.trimIndent()
            ),
            ReelTemplate(
                id = "t3",
                title = "تصحيح مفهوم شائع (Myth Buster)",
                category = "تعديل سلوك",
                description = "كشف خطأ شائع يقع فيه الكثيرون وتصحيحه فوراً بالدليل الشرعي.",
                durationText = "30 ثانية",
                styleBadge = "سريع ومباشر",
                sampleScript = """
                    🔥 الخطاف: «خطأ شائع يقع فيه 90% من الناس بعد الصلاة يضيع الأجر!»
                    
                    💡 التصحيح: الانصراف السريع دون التسبيح والذكر. التسبيح 33 مرة بعد الفريضة سبب لمغفرة الذنوب ولو كانت مثل زبد البحر.
                    
                    📖 المصدر: صحيح مسلم
                """.trimIndent()
            ),
            ReelTemplate(
                id = "t4",
                title = "مدافعة عاجلة لتريند شبهة",
                category = "مرصد الثغور",
                description = "رد قاطع ومختصر وموثق للرد على الموجات الشبهاتية المنتشرة.",
                durationText = "30 ثانية",
                styleBadge = "حماسي قاطع",
                sampleScript = """
                    🔥 الخطاف: «رد حاسم على الشبهة المنتشرة حول بر الوالدين والأسرة!»
                    
                    💡 الرد الشرعي: الإسلام جعل رضا الله مقروناً برضا الوالدين، ولا تعارض بين طموحك الشخصي وبرك بوالديك بل هو سر التوفيق والأثر.
                    
                    📖 المصدر: سورة الإسراء - آية 23
                """.trimIndent()
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("قوالب وسكريبتات الريلز الفيروسية ⚡", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("قوالب إخراجية جاهزة ومثبتة النجاح. اختر القالب ليتم تحميل هيكله فوراً بالاستوديو.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
        }

        items(
            items = templates,
            key = { template -> template.title }
        ) { template ->
            ReelTemplateCard(
                template = template,
                onSelect = onSelectTemplateScript
            )
        }
    }
}

@Composable
fun ReelsFeedSection() {
    var feedItems by remember { mutableStateOf<List<ProjectService.Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val fetched = CloudServices.Database.getFeedProjects()
        if (fetched.isNotEmpty()) {
            feedItems = fetched
        } else {
            feedItems = listOf(
                ProjectService.Project(title = "أهمية الوقت في الإسلام", idea = "مقطع سريع يوضح قيمة الوقت لشباب الأمة", status = "مكتمل", lastScreen = "25K مشاهدة"),
                ProjectService.Project(title = "تدبر آية: سورة الكهف", idea = "تأملات سينمائية خاشعة في سورة الكهف", status = "مكتمل", lastScreen = "12K مشاهدة"),
                ProjectService.Project(title = "كيف تخشع في صلاتك؟", idea = "خطوات عملية للوصول إلى الخشوع التام", status = "مكتمل", lastScreen = "50K مشاهدة")
            )
        }
        isLoading = false
    }

    if (isLoading) {
        QabasReelFeedSkeleton()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    Translator.tr("مجتمع الإلهام والريلز 🌐"),
                    color = Color.White,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    Translator.tr("استعرض أعمال زملائك صناع المحتوى واستلهم أفكار المونتاج والأنماط البصرية."),
                    color = Color.Gray,
                    fontFamily = NotoSansFont,
                    fontSize = 12.sp
                )
            }

            items(
                items = feedItems,
                key = { project -> project.id.ifEmpty { project.title } }
            ) { project ->
                ReelFeedCard(project = project)
            }
        }
    }
}
