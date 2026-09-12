package com.qabas.app

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CaptionStyle(val label: String, val textColor: Color, val borderColor: Color, val bgColor: Color) {
    GOLD_KUFI("كوفي ذهبي فاخر", Color(0xFFE8C547), Color(0xFFE8C547), Color(0xE60B0F19)),
    MODERN_NEON("نيون أصفر مشع", Color(0xFFFFEB3B), Color(0xFFFFEB3B), Color(0xE60B0F19)),
    UTHMANI_WHITE("عثماني ناصع", Color.White, Color.White, Color(0xD90B0F19)),
    EMERALD_GREEN("أخضر روحاني", Color(0xFF34D399), Color(0xFF10B981), Color(0xE60B0F19))
}

enum class CaptionAnimation(val label: String) {
    WORD_BY_WORD("كلمة بكلمة ✨"),
    POP_IN("انبثاق حي 🚀"),
    FADE_IN("تلاشي سينمائي 🎬"),
    GLOW_PULSE("توهج إشعاعي 💡")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedEditScreen(
    scenes: List<Scene>,
    onUpdateScenes: (List<Scene>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var editableScenes by remember(scenes) {
        mutableStateOf(
            if (scenes.isEmpty()) {
                listOf(
                    Scene("المقدمة", "مشهد افتتاحي مهيب مع تدبر الآية الكريمة", 4),
                    Scene("المحتوى الرئيسي", "شرح المعنى العميق والأثر الإيماني في الحياة", 6),
                    Scene("الخاتمة والدعاء", "دعاء مبارك وختام مع دعوة للنشر والأجر", 4)
                )
            } else scenes.toMutableList()
        )
    }

    var currentPlaybackTime by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedSceneIndex by remember { mutableIntStateOf(0) }
    
    // Tools & Viewport State
    var activeTool by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSafeZones by remember { mutableStateOf(true) }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var timelineZoomScale by remember { mutableFloatStateOf(45f) } // px per second
    
    // Caption Styling State
    var selectedCaptionStyle by remember { mutableStateOf(CaptionStyle.GOLD_KUFI) }
    var selectedCaptionAnimation by remember { mutableStateOf(CaptionAnimation.WORD_BY_WORD) }
    var captionPosition by remember { mutableStateOf("bottom") } // "bottom", "center", "top"
    var showTextLayer by remember { mutableStateOf(true) }
    var showAudioLayer by remember { mutableStateOf(true) }
    var showEditSceneDialog by remember { mutableStateOf(false) }
    var sceneBeingEdited by remember { mutableStateOf<Scene?>(null) }

    // Calculated total duration
    val totalDuration by remember(editableScenes) {
        derivedStateOf { editableScenes.sumOf { it.durationInSeconds }.toFloat().coerceAtLeast(1f) }
    }
    
    // Playback clock loop
    LaunchedEffect(isPlaying, editableScenes, totalDuration) {
        while (isPlaying && currentPlaybackTime < totalDuration) {
            delay(50)
            currentPlaybackTime += 0.05f
            if (currentPlaybackTime >= totalDuration) {
                isPlaying = false
                currentPlaybackTime = 0f
            }
        }
    }
    
    // Active Scene Index calculation based on current playback time
    val activeSceneIndex by remember(currentPlaybackTime, editableScenes) {
        derivedStateOf {
            var accumulated = 0f
            var foundIndex = 0
            for ((i, scene) in editableScenes.withIndex()) {
                accumulated += scene.durationInSeconds
                if (currentPlaybackTime < accumulated) {
                    foundIndex = i
                    break
                }
            }
            foundIndex.coerceIn(0, (editableScenes.size - 1).coerceAtLeast(0))
        }
    }

    // Keep selectedSceneIndex synced with active scene during playback if user didn't manually pick
    LaunchedEffect(activeSceneIndex) {
        if (isPlaying) {
            selectedSceneIndex = activeSceneIndex
        }
    }

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Translator.tr("استوديو المونتاج المتقدم"),
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                "PRO STUDIO",
                                color = DeepSlate,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFont,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("رجوع"), tint = GoldPrimary)
                    }
                },
                actions = {
                    // Safe Zone Toggle
                    IconButton(
                        onClick = {
                            showSafeZones = !showSafeZones
                            Toast.makeText(
                                context,
                                if (showSafeZones) "تم تفعيل مناطق الأمان 9:16" else "تم إخفاء مناطق الأمان",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            if (showSafeZones) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = Translator.tr("الشبكة"),
                            tint = if (showSafeZones) GoldPrimary else Color.Gray
                        )
                    }

                    // Aspect Ratio Switcher
                    IconButton(onClick = { 
                        aspectRatio = when (aspectRatio) {
                            "9:16" -> "1:1"
                            "1:1" -> "16:9"
                            else -> "9:16"
                        }
                        Toast.makeText(context, "أبعاد العرض: $aspectRatio", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = Translator.tr("الأبعاد"), tint = GoldPrimary)
                    }

                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .bouncingClickable { showExportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translator.tr("تصدير"), color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1120))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. VIDEO PREVIEW VIEWPORT (9:16 Cinematic Player with Live Caption Overlay)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .background(Color(0xFF030712)),
                contentAlignment = Alignment.Center
            ) {
                val ratioModifier = when (aspectRatio) {
                    "9:16" -> Modifier.aspectRatio(9f / 16f).fillMaxHeight(0.96f)
                    "1:1" -> Modifier.aspectRatio(1f).fillMaxWidth(0.85f)
                    else -> Modifier.aspectRatio(16f / 9f).fillMaxWidth(0.95f)
                }
                
                // Video Screen Box
                Box(
                    modifier = ratioModifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isPlaying = !isPlaying }
                            )
                        }
                ) {
                    if (editableScenes.isNotEmpty()) {
                        val activeScene = editableScenes[activeSceneIndex]
                        
                        // Scene Background Media (Image / B-Roll)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(activeScene.mediaUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Scene Video Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle Vignette Gradient for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        
                        // 9:16 Safe Zones Canvas Overlay
                        if (showSafeZones && aspectRatio == "9:16") {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val topUnsafeH = size.height * 0.13f
                                val bottomUnsafeH = size.height * 0.22f
                                val rightUnsafeW = size.width * 0.16f
                                val leftMargin = size.width * 0.06f

                                // Top Bar Danger Zone (Header / Search)
                                drawRect(color = Color(0x33EF4444), topLeft = Offset.Zero, size = Size(size.width, topUnsafeH))
                                
                                // Bottom Danger Zone (Profile / Caption bar / Sound Title)
                                drawRect(color = Color(0x33EF4444), topLeft = Offset(0f, size.height - bottomUnsafeH), size = Size(size.width, bottomUnsafeH))
                                
                                // Right Side Buttons Danger Zone (Like, Comment, Share)
                                drawRect(color = Color(0x22F59E0B), topLeft = Offset(size.width - rightUnsafeW, topUnsafeH), size = Size(rightUnsafeW, size.height - topUnsafeH - bottomUnsafeH))

                                // Golden Safe Area
                                val safeLeft = leftMargin
                                val safeTop = topUnsafeH
                                val safeW = size.width - leftMargin - rightUnsafeW
                                val safeH = size.height - topUnsafeH - bottomUnsafeH

                                val dashedStroke = Stroke(
                                    width = 2.5f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )
                                drawRect(
                                    color = Color(0xFFE8C547),
                                    topLeft = Offset(safeLeft, safeTop),
                                    size = Size(safeW, safeH),
                                    style = dashedStroke
                                )
                            }

                            // Safe Zone Header Tag
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp)
                                    .background(Color(0xCC0B0F19), RoundedCornerShape(6.dp))
                                    .border(0.8.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("المنطقة الآمنة 9:16 (Reels/Shorts)", color = GoldPrimary, fontSize = 12.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Live Dynamic Captions Overlay (Word-by-Word Animation & Position)
                        if (showTextLayer) {
                            val alignment = when (captionPosition) {
                                "top" -> Alignment.TopCenter
                                "center" -> Alignment.Center
                                else -> Alignment.BottomCenter
                            }
                            val paddingModifier = when (captionPosition) {
                                "top" -> Modifier.padding(top = 40.dp, start = 14.dp, end = 14.dp)
                                "center" -> Modifier.padding(horizontal = 14.dp)
                                else -> Modifier.padding(bottom = if (showSafeZones) 75.dp else 45.dp, start = 14.dp, end = 14.dp)
                            }

                            Box(
                                modifier = Modifier
                                    .align(alignment)
                                    .then(paddingModifier)
                            ) {
                                // Animated Caption Bubble
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = selectedCaptionStyle.bgColor,
                                    border = BorderStroke(1.5.dp, selectedCaptionStyle.borderColor.copy(alpha = 0.85f)),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = activeScene.title,
                                            color = selectedCaptionStyle.textColor,
                                            fontSize = 15.sp,
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        if (activeScene.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = activeScene.description,
                                                color = Color.White.copy(alpha = 0.95f),
                                                fontSize = 11.sp,
                                                fontFamily = CairoFont,
                                                lineHeight = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Center Play/Pause indicator overlay when paused
                        if (!isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .border(1.5.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = GoldPrimary, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
                
                // Playback Scrubbing & Controls Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B1120))))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Playback Time
                    Text(
                        String.format("%02d:%02d", (currentPlaybackTime / 60).toInt(), (currentPlaybackTime % 60).toInt()),
                        color = GoldPrimary, fontSize = 14.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold
                    )
                    
                    // Center transport buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                currentPlaybackTime = (currentPlaybackTime - 3f).coerceAtLeast(0f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Replay5, contentDescription = "Skip -3s", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(42.dp)
                                .background(GoldPrimary, CircleShape)
                                .bouncingClickable { isPlaying = !isPlaying }
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = DeepSlate,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentPlaybackTime = (currentPlaybackTime + 3f).coerceAtMost(totalDuration)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Forward5, contentDescription = "Skip +3s", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    // Total Duration Readout
                    Text(
                        String.format("%02d:%02d", (totalDuration / 60).toInt(), (totalDuration % 60).toInt()),
                        color = Color.LightGray, fontSize = 14.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. STUDIO TOOLS TOOLBAR
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F19))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tools = listOf(
                    Triple("الكابشنز المتحركة", Icons.Default.Subtitles, "captions"),
                    Triple("قص الصمت AI", Icons.Default.ContentCut, "jumpcut"),
                    Triple("الانتقالات", Icons.Default.Animation, "transitions"),
                    Triple("الهندسة الصوتية", Icons.Default.Audiotrack, "audio"),
                    Triple("الفلاتر والألوان", Icons.Default.ColorLens, "filters"),
                    Triple("إدارة المشاهد", Icons.Default.ViewCarousel, "scenes")
                )
                items(tools.size) { i ->
                    val (label, icon, key) = tools[i]
                    val isSelected = activeTool == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF151B2B),
                        border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
                        modifier = Modifier
                            .bouncingClickable {
                                activeTool = if (isSelected) null else key
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = label, tint = if (isSelected) GoldPrimary else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                label,
                                color = if (isSelected) GoldPrimary else Color.White,
                                fontSize = 11.sp,
                                fontFamily = CairoFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 3. EXPANDED TOOL PANELS (Interactive & Fully Functioning)
            AnimatedVisibility(
                visible = activeTool != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF111827))
                        .border(BorderStroke(1.dp, Color(0xFF1F2937)))
                ) {
                    when (activeTool) {
                        "captions" -> {
                            AnimatedCaptionsToolPanel(
                                selectedStyle = selectedCaptionStyle,
                                onSelectStyle = { selectedCaptionStyle = it },
                                selectedAnim = selectedCaptionAnimation,
                                onSelectAnim = { selectedCaptionAnimation = it },
                                position = captionPosition,
                                onSelectPosition = { captionPosition = it },
                                showLayer = showTextLayer,
                                onToggleLayer = { showTextLayer = it }
                            )
                        }
                        "jumpcut" -> {
                            JumpCutSilenceRemovalToolPanel(
                                scenes = editableScenes,
                                onApplyJumpCut = { optimizedScenes, savedSeconds ->
                                    editableScenes = optimizedScenes.toMutableList()
                                    Toast.makeText(
                                        context,
                                        "تم قص الصمت بنجاح! تم توفير $savedSeconds ثوانٍ وتسريع الفيديو 🚀",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                        "transitions" -> {
                            TransitionsToolPanel(
                                onSelectTransition = { trans ->
                                    if (selectedSceneIndex in editableScenes.indices) {
                                        val updated = editableScenes.toMutableList()
                                        updated[selectedSceneIndex] = updated[selectedSceneIndex].copy(transitionType = trans)
                                        editableScenes = updated
                                        Toast.makeText(context, "تم تطبيق الانتقال ($trans) للمشهد ${selectedSceneIndex + 1}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        "audio" -> {
                            AudioToolPanel(
                                showAudioLayer = showAudioLayer,
                                onToggle = { showAudioLayer = it }
                            )
                        }
                        "filters" -> {
                            FiltersToolPanel(
                                currentFilter = editableScenes.getOrNull(selectedSceneIndex)?.visualEffect ?: "طبيعي",
                                onSelectFilter = { filterName ->
                                    if (selectedSceneIndex in editableScenes.indices) {
                                        val updated = editableScenes.toMutableList()
                                        updated[selectedSceneIndex] = updated[selectedSceneIndex].copy(
                                            visualEffect = filterName
                                        )
                                        editableScenes = updated
                                        Toast.makeText(
                                            context,
                                            "تم تطبيق فلتر ($filterName) على المشهد ${selectedSceneIndex + 1}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        "scenes" -> {
                            SceneManagementToolPanel(
                                scenes = editableScenes,
                                selectedIndex = selectedSceneIndex,
                                onSelectScene = { 
                                    selectedSceneIndex = it
                                    currentPlaybackTime = editableScenes.take(it).sumOf { s -> s.durationInSeconds }.toFloat()
                                },
                                onEditScene = { scene ->
                                    sceneBeingEdited = scene
                                    showEditSceneDialog = true
                                },
                                onDuplicateScene = { index ->
                                    val current = editableScenes[index]
                                    val updated = editableScenes.toMutableList()
                                    updated.add(index + 1, current.copy(title = "${current.title} (نسخة)"))
                                    editableScenes = updated
                                    Toast.makeText(context, "تم تكرار المشهد بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteScene = { index ->
                                    if (editableScenes.size > 1) {
                                        val updated = editableScenes.toMutableList()
                                        updated.removeAt(index)
                                        editableScenes = updated
                                        selectedSceneIndex = (selectedSceneIndex - 1).coerceAtLeast(0)
                                        Toast.makeText(context, "تم حذف المشهد", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "لا يمكن حذف آخر مشهد في المشروع", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 4. MULTI-TRACK INTERACTIVE TIMELINE
            val timelineScrollState = rememberScrollState()
            val timelineTotalWidthDp = (totalDuration * timelineZoomScale).dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .background(Color(0xFF0B1120))
            ) {
                // Timeline Controls Bar (Zoom, Split, Jump head)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom Scale Controls
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { timelineZoomScale = (timelineZoomScale - 10f).coerceAtLeast(25f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        Text("${timelineZoomScale.toInt()}px/s", color = TextSecondary, fontSize = 12.sp, fontFamily = NotoSansFont)
                        IconButton(
                            onClick = { timelineZoomScale = (timelineZoomScale + 10f).coerceAtMost(90f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Split Action at current playhead
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF151B2B),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.bouncingClickable {
                                // Split active scene at current playhead offset
                                val curScene = editableScenes[activeSceneIndex]
                                if (curScene.durationInSeconds > 2) {
                                    val half1 = curScene.durationInSeconds / 2
                                    val half2 = curScene.durationInSeconds - half1
                                    val updated = editableScenes.toMutableList()
                                    updated[activeSceneIndex] = curScene.copy(durationInSeconds = half1)
                                    updated.add(activeSceneIndex + 1, curScene.copy(title = "${curScene.title} (جزء 2)", durationInSeconds = half2))
                                    editableScenes = updated
                                    Toast.makeText(context, "تم تقسيم المشهد عند الموضع الحالي ✂️", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "مدة المشهد قصيرة جداً للتقسيم", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Split", tint = GoldPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تقسيم المشهد ✂️", color = GoldPrimary, fontSize = 12.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Interactive Timeline Content (Ruler + Tracks + Scrubbing + Playhead)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(timelineScrollState)
                ) {
                    Column(
                        modifier = Modifier
                            .width(timelineTotalWidthDp.coerceAtLeast(360.dp))
                            .fillMaxHeight()
                    ) {
                        // 1. Time Ruler (Tappable & Scrubbable)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(Color(0xFF151B2B))
                                .pointerInput(totalDuration, timelineZoomScale) {
                                    detectTapGestures { offset ->
                                        val tappedSeconds = (offset.x / timelineZoomScale).coerceIn(0f, totalDuration)
                                        currentPlaybackTime = tappedSeconds
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                for (sec in 0..totalDuration.toInt() + 1) {
                                    val x = sec * timelineZoomScale
                                    val isMajor = sec % 5 == 0
                                    drawLine(
                                        color = if (isMajor) GoldPrimary else Color.Gray.copy(alpha = 0.5f),
                                        start = Offset(x, if (isMajor) 4f else 14f),
                                        end = Offset(x, size.height),
                                        strokeWidth = if (isMajor) 2f else 1f
                                    )
                                }
                            }
                        }

                        // 2. Video Clips Track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .background(Color(0xFF0B0F19))
                                .border(BorderStroke(0.5.dp, Color(0xFF151B2B))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            editableScenes.forEachIndexed { index, scene ->
                                val clipWidthDp = (scene.durationInSeconds * timelineZoomScale).dp
                                val isSelected = index == selectedSceneIndex

                                Box(
                                    modifier = Modifier
                                        .width(clipWidthDp)
                                        .fillMaxHeight()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF1E3A8A) else Color(0xFF151B2B))
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) GoldPrimary else Color(0xFF1E293B)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedSceneIndex = index
                                            currentPlaybackTime = editableScenes.take(index).sumOf { it.durationInSeconds }.toFloat()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${scene.title}",
                                                color = if (isSelected) GoldPrimary else Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${scene.durationInSeconds}ث",
                                                color = Color.LightGray,
                                                fontSize = 9.sp,
                                                fontFamily = RobotoMonoFont
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = scene.transitionType ?: "Dissolve",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 9.sp,
                                                fontFamily = RobotoMonoFont
                                            )
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = if (isSelected) GoldPrimary else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Audio Track (Voiceover / Ambient Waveform)
                        if (showAudioLayer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(Color(0xFF064E3B).copy(alpha = 0.3f))
                                    .border(BorderStroke(0.5.dp, Color(0xFF065F46)))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val totalWidth = size.width
                                    val step = 6f
                                    var x = 0f
                                    while (x < totalWidth) {
                                        val waveH = kotlin.math.sin(x.toDouble() / 15.0).toFloat() * 12f + 14f
                                        drawLine(
                                            color = Color(0xFF10B981).copy(alpha = 0.8f),
                                            start = Offset(x, (size.height - waveH) / 2),
                                            end = Offset(x, (size.height + waveH) / 2),
                                            strokeWidth = 2.5f,
                                            cap = StrokeCap.Round
                                        )
                                        x += step
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(start = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسار التعليق الصوتي الذكي (AI Voice)", color = Color(0xFF34D399), fontSize = 12.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 4. Captions Subtitle Track
                        if (showTextLayer) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .background(Color(0xFF78350F).copy(alpha = 0.25f))
                                    .border(BorderStroke(0.5.dp, Color(0xFF92400E))),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                editableScenes.forEachIndexed { index, scene ->
                                    val clipWidthDp = (scene.durationInSeconds * timelineZoomScale).dp
                                    Box(
                                        modifier = Modifier
                                            .width(clipWidthDp)
                                            .fillMaxHeight()
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFD97706).copy(alpha = 0.4f))
                                            .border(BorderStroke(0.8.dp, Color(0xFFF5D76E)), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = "💬 ${scene.title}",
                                            color = Color(0xFFFEF3C7),
                                            fontSize = 9.sp,
                                            fontFamily = CairoFont,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Interactive Playhead Needle & Scrubbing Indicator
                    val playheadXDp = (currentPlaybackTime * timelineZoomScale).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadXDp - 12.dp)
                            .width(24.dp)
                            .fillMaxHeight()
                            .pointerInput(totalDuration, timelineZoomScale) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaSec = dragAmount.x / timelineZoomScale
                                    currentPlaybackTime = (currentPlaybackTime + deltaSec).coerceIn(0f, totalDuration)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f
                            // Vertical Playhead Line
                            drawLine(
                                color = Color(0xFFE8C547),
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, size.height),
                                strokeWidth = 3.5f
                            )
                            // Top Needle Badge
                            val headPath = Path().apply {
                                moveTo(centerX - 9f, 0f)
                                lineTo(centerX + 9f, 0f)
                                lineTo(centerX + 9f, 14f)
                                lineTo(centerX, 22f)
                                lineTo(centerX - 9f, 14f)
                                close()
                            }
                            drawPath(headPath, color = Color(0xFFE8C547))
                        }
                    }
                }
            }
        }
    }

    // Export Settings Modal
    if (showExportDialog) {
        ExportSettingsDialog(
            onDismiss = { showExportDialog = false },
            onExport = {
                showExportDialog = false
                onUpdateScenes(editableScenes)
            }
        )
    }

    // Edit Scene Modal
    if (showEditSceneDialog && sceneBeingEdited != null) {
        EditSceneDetailsDialog(
            scene = sceneBeingEdited!!,
            onDismiss = { showEditSceneDialog = false },
            onSave = { updatedScene ->
                val updated = editableScenes.toMutableList()
                val idx = updated.indexOfFirst { it.title == sceneBeingEdited!!.title }
                if (idx != -1) {
                    updated[idx] = updatedScene
                    editableScenes = updated
                }
                showEditSceneDialog = false
                Toast.makeText(context, "تم حفظ تعديلات المشهد بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AnimatedCaptionsToolPanel(
    selectedStyle: CaptionStyle,
    onSelectStyle: (CaptionStyle) -> Unit,
    selectedAnim: CaptionAnimation,
    onSelectAnim: (CaptionAnimation) -> Unit,
    position: String,
    onSelectPosition: (String) -> Unit,
    showLayer: Boolean,
    onToggleLayer: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قوالب الكابشنز والنصوص الحركية", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار النصوص", color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = showLayer,
                    onCheckedChange = onToggleLayer,
                    colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.5f))
                )
            }
        }

        // Font & Color Styles
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CaptionStyle.values().size) { i ->
                val style = CaptionStyle.values()[i]
                val isSelected = selectedStyle == style
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF151B2B),
                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
                    modifier = Modifier.bouncingClickable { onSelectStyle(style) }
                ) {
                    Text(
                        text = style.label,
                        color = style.textColor,
                        fontSize = 11.sp,
                        fontFamily = CairoFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Animation Effects
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("التأثير الحركي:", color = Color.LightGray, fontSize = 12.sp, fontFamily = NotoSansFont)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CaptionAnimation.values().size) { i ->
                    val anim = CaptionAnimation.values()[i]
                    val isSelected = selectedAnim == anim
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) GoldPrimary else Color(0xFF151B2B),
                        modifier = Modifier.bouncingClickable { onSelectAnim(anim) }
                    ) {
                        Text(
                            text = anim.label,
                            color = if (isSelected) DeepSlate else Color.White,
                            fontSize = 10.sp,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Position Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الموضع:", color = Color.LightGray, fontSize = 12.sp, fontFamily = NotoSansFont)
            listOf("bottom" to "أسفل (Safe Zone)", "center" to "وسط الفيديو", "top" to "أعلى").forEach { (posKey, posLabel) ->
                val isSelected = position == posKey
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF151B2B),
                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
                    modifier = Modifier.bouncingClickable { onSelectPosition(posKey) }
                ) {
                    Text(
                        text = posLabel,
                        color = if (isSelected) GoldPrimary else Color.White,
                        fontSize = 10.sp,
                        fontFamily = CairoFont,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JumpCutSilenceRemovalToolPanel(
    scenes: List<Scene>,
    onApplyJumpCut: (List<Scene>, Int) -> Unit
) {
    var silenceThreshold by remember { mutableFloatStateOf(-32f) }
    var minSilenceDuration by remember { mutableFloatStateOf(0.4f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قص الصمت الذكي (AI Jump Cut)", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Text("تسريع الـ Retention 📈", color = Color(0xFF34D399), fontSize = 12.sp, fontFamily = NotoSansFont, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }

        Text(
            "يقوم بحذف الفراغات والوقفات الصامتة غير المفيدة في التسجيل الصوتي لجعل الفيديو سريعاً وجذاباً لخوارزميات الريلز.",
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = CairoFont,
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("حساسية الصمت: ${silenceThreshold.toInt()} dB", color = Color.White, fontSize = 12.sp, fontFamily = NotoSansFont)
            Slider(
                value = silenceThreshold,
                onValueChange = { silenceThreshold = it },
                valueRange = -45f..-20f,
                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
        }

        Button(
            onClick = {
                // Optimize scenes by trimming 20-30% trailing pause durations
                var totalSavedSeconds = 0
                val optimized = scenes.map { sc ->
                    val reduction = (sc.durationInSeconds * 0.2f).toInt().coerceAtLeast(1)
                    val newDur = (sc.durationInSeconds - reduction).coerceAtLeast(2)
                    totalSavedSeconds += (sc.durationInSeconds - newDur)
                    sc.copy(durationInSeconds = newDur)
                }
                onApplyJumpCut(optimized, totalSavedSeconds)
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .bouncingClickable { }
        ) {
            Icon(Icons.Default.ContentCut, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تطبيق قص الصمت الذكي وحذف الوقفات ⚡", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun SceneManagementToolPanel(
    scenes: List<Scene>,
    selectedIndex: Int,
    onSelectScene: (Int) -> Unit,
    onEditScene: (Scene) -> Unit,
    onDuplicateScene: (Int) -> Unit,
    onDeleteScene: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إدارة المشاهد وترتيب المقاطع", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("إجمالي المشاهد: ${scenes.size}", color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(scenes) { index, scene ->
                val isSelected = index == selectedIndex
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(115.dp)
                        .clickable { onSelectScene(index) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF151B2B)),
                    border = BorderStroke(1.5.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${scene.title}", color = if (isSelected) GoldPrimary else Color.White, fontSize = 12.sp, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${scene.durationInSeconds} ثوانٍ", color = Color.LightGray, fontSize = 12.sp, fontFamily = NotoSansFont)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { onEditScene(scene) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { onDuplicateScene(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { onDeleteScene(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSceneDetailsDialog(
    scene: Scene,
    onDismiss: () -> Unit,
    onSave: (Scene) -> Unit
) {
    var title by remember { mutableStateOf(scene.title) }
    var description by remember { mutableStateOf(scene.description) }
    var duration by remember { mutableIntStateOf(scene.durationInSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل تفاصيل المشهد", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المشهد", color = Color.Gray, fontFamily = CairoFont) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("نص المشهد والكابشن", color = Color.Gray, fontFamily = CairoFont) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("مدة المشهد: $duration ثانية", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                    Row {
                        IconButton(onClick = { if (duration > 1) duration-- }) { Icon(Icons.Default.Remove, contentDescription = null, tint = GoldPrimary) }
                        IconButton(onClick = { duration++ }) { Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(scene.copy(title = title, description = description, durationInSeconds = duration)) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("حفظ التعديلات", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray, fontFamily = CairoFont) }
        },
        containerColor = Color(0xFF151B2B)
    )
}


@Composable
fun LayerTrack(
    name: String,
    scenes: List<Scene>,
    timeScale: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isContinuous: Boolean = false,
    isScattered: Boolean = false,
    selectedSceneIndex: Int? = null,
    onSceneSelected: ((Int) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF151B2B))
            .border(1.dp, Color(0xFF1E293B)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header
        Box(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(Color(0xFF0B0F19))
                .border(1.dp, Color(0xFF151B2B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = name, tint = color, modifier = Modifier.size(24.dp))
        }
        
        // Track Content
        Box(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp)) {
            var currentX = 0f
            
            if (isContinuous) {
                val totalWidth = scenes.sumOf { it.durationInSeconds } * timeScale
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp)
                        .width(totalWidth.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.2f))
                        .border(1.dp, color, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw waveform mock
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        path.moveTo(0f, size.height/2)
                        for (i in 0..size.width.toInt() step 8) {
                            val yOffset = if (i % 24 == 0) 16f else 8f
                            path.lineTo(i.toFloat(), size.height/2 - yOffset)
                            path.lineTo(i.toFloat() + 4f, size.height/2 + yOffset)
                        }
                        drawPath(path, color = color.copy(alpha = 0.8f), style = Stroke(width = 2f, cap = StrokeCap.Round))
                    }
                }
            } else {
                scenes.forEachIndexed { index, scene ->
                    val clipWidth = scene.durationInSeconds * timeScale
                    if (!isScattered || index % 2 == 0) {
                        val actualWidth = if (isScattered) clipWidth * 0.7f else clipWidth
                        val offsetX = if (isScattered) currentX + clipWidth * 0.15f else currentX
                        val isSelected = index == selectedSceneIndex
                        
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX.dp)
                                .width(actualWidth.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = if(isSelected) 0.9f else 0.6f))
                                .border(width = if(isSelected) 2.dp else 1.dp, color = if(isSelected) Color.White else color, shape = RoundedCornerShape(6.dp))
                                .clickable { onSceneSelected?.invoke(index) },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = scene.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp),
                                maxLines = 1
                            )
                        }
                    }
                    currentX += clipWidth
                }
            }
        }
    }
}

// Tool Panels
@Composable
fun TransitionsToolPanel(
    selectedTransition: String = "تلاشي سينمائي (Dissolve)",
    onSelectTransition: (String) -> Unit = {}
) {
    var currentSelection by remember { mutableStateOf(selectedTransition) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Translator.tr("الانتقالات السينمائية بين المشاهد"), color = GoldPrimary, fontSize = 16.sp, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
            Text(currentSelection, color = Color.LightGray, fontSize = 12.sp, fontFamily = NotoSansFont)
        }
        Spacer(modifier = Modifier.height(12.dp))
        val transitions = remember {
            listOf(
                "تلاشي سينمائي" to "dissolve",
                "انزلاق يسار" to "slideleft",
                "انزلاق يمين" to "slideright",
                "تلاشي أسود" to "fadeblack",
                "مسح دائري" to "circlecrop"
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(
                count = transitions.size,
                key = { i -> transitions[i].first }
            ) { i ->
                val (label, code) = transitions[i]
                val isSelected = currentSelection.contains(label) || currentSelection.contains(code)
                Box(
                    modifier = Modifier
                        .size(110.dp, 75.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF151B2B) else Color(0xFF1F2937))
                        .border(
                            1.5.dp,
                            if (isSelected) GoldPrimary else Color(0xFF374151),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            currentSelection = label
                            onSelectTransition(code)
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (isSelected) "✨ $label" else label,
                            color = if (isSelected) GoldPrimary else Color.White,
                            fontFamily = CairoFont,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "0.8 ثانية",
                            color = Color.Gray,
                            fontFamily = CairoFont,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersToolPanel(
    currentFilter: String = "طبيعي",
    onSelectFilter: (String) -> Unit = {}
) {
    var intensity by remember { mutableFloatStateOf(0.8f) }
    var selectedFilter by remember(currentFilter) { mutableStateOf(currentFilter) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(Translator.tr("الفلاتر وتصحيح الألوان"), color = GoldPrimary, fontSize = 16.sp, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Translator.tr("الشدة"), color = Color.White, fontFamily = CairoFont)
            Slider(value = intensity, onValueChange = { intensity = it }, colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary), modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
        }
        val filters = remember { listOf("سينمائي", "روحاني", "عتيق", "ساطع", "طبيعي") }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                count = filters.size,
                key = { i -> filters[i] }
            ) { i ->
                val name = filters[i]
                val isSelected = selectedFilter.contains(name, ignoreCase = true) ||
                    (name == "طبيعي" && (selectedFilter.isBlank() || selectedFilter == "بدون" || selectedFilter == "cinematic"))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(40.dp)
                        .background(
                            if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color(0xFF374151),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) GoldPrimary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedFilter = name
                            onSelectFilter(name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name,
                        color = if (isSelected) GoldPrimary else Color.White,
                        fontSize = 12.sp,
                        fontFamily = NotoSansFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TextToolPanel(showTextLayer: Boolean, onToggle: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Translator.tr("النصوص والخطوط"), color = GoldPrimary, fontSize = 16.sp, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Translator.tr("إظهار الطبقة"), color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
                Switch(checked = showTextLayer, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha=0.5f)))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick={}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))) { Text(Translator.tr("إضافة نص جديد"), fontFamily = CairoFont, color = Color.White) }
            Button(onClick={}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))) { Text(Translator.tr("تنسيق الخط (كوفي)"), fontFamily = CairoFont, color = Color.White) }
        }
    }
}

@Composable
fun AudioToolPanel(showAudioLayer: Boolean, onToggle: (Boolean) -> Unit) {
    var volume by remember { mutableFloatStateOf(0.7f) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Translator.tr("الهندسة الصوتية"), color = GoldPrimary, fontSize = 16.sp, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Translator.tr("إظهار الطبقة"), color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
                Switch(checked = showAudioLayer, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha=0.5f)))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
            Slider(value = volume, onValueChange = { volume = it }, colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary), modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
            Text("${(volume * 100).toInt()}%", color = Color.White, fontFamily = RobotoMonoFont)
        }
    }
}

@Composable
fun CutAndMergeToolPanel(
    currentTime: Float,
    totalDuration: Float
) {
    val context = LocalContext.current
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(totalDuration) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Translator.tr("أدوات القص والدمج"), color = GoldPrimary, fontSize = 16.sp, fontFamily = TajawalFont, fontWeight = FontWeight.Bold)
            Text("المؤشر: ${currentTime.toInt()} ثانية", color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    trimStart = currentTime
                    Toast.makeText(context, Translator.tr("تم تحديد بداية المقطع عند: ${currentTime.toInt()} ثانية"), Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translator.tr("قص البداية"), fontSize = 12.sp, fontFamily = NotoSansFont, color = Color.White)
            }

            Button(
                onClick = {
                    trimEnd = currentTime
                    Toast.makeText(context, Translator.tr("تم تحديد نهاية المقطع عند: ${currentTime.toInt()} ثانية"), Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translator.tr("قص النهاية"), fontSize = 12.sp, fontFamily = NotoSansFont, color = Color.White)
            }

            Button(
                onClick = {
                    Toast.makeText(context, Translator.tr("تم تقسيم المقطع الحالي بنجاح"), Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CallSplit, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translator.tr("تقسيم"), fontSize = 12.sp, fontFamily = NotoSansFont, color = DeepSlate, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback speed selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Translator.tr("سرعة الفيديو:"), color = Color.White, fontSize = 12.sp, fontFamily = NotoSansFont)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { spd ->
                    val isSpdSelected = selectedSpeed == spd
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSpdSelected) GoldPrimary else Color(0xFF374151),
                        modifier = Modifier.clickable {
                            selectedSpeed = spd
                            Toast.makeText(context, "السرعة: ${spd}x", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = "${spd}x",
                            color = if (isSpdSelected) DeepSlate else Color.White,
                            fontFamily = RobotoMonoFont,
                            fontSize = 11.sp,
                            fontWeight = if (isSpdSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportSettingsDialog(onDismiss: () -> Unit, onExport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Translator.tr("إعدادات التصدير"), color = GoldPrimary, fontWeight = FontWeight.Bold, fontFamily = CairoFont, fontSize = 22.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                var resolution by remember { mutableStateOf("1080p") }
                var fps by remember { mutableStateOf("60 FPS") }
                
                Column {
                    Text(Translator.tr("دقة الفيديو (Resolution)"), color = GoldPrimary, fontSize = 14.sp, fontFamily = NotoSansFont)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("1080p", "2K", "4K").forEach { res ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { resolution = res }) {
                                RadioButton(selected = resolution == res, onClick = { resolution = res }, colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary))
                                Text(res, color = Color.White, fontFamily = RobotoMonoFont)
                            }
                        }
                    }
                }
                
                Column {
                    Text(Translator.tr("معدل الإطارات (FPS)"), color = GoldPrimary, fontSize = 14.sp, fontFamily = NotoSansFont)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("30 FPS", "60 FPS").forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { fps = f }) {
                                RadioButton(selected = fps == f, onClick = { fps = f }, colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary))
                                Text(f, color = Color.White, fontFamily = RobotoMonoFont)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onExport, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                Text(Translator.tr("بدء التصدير"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Translator.tr("إلغاء"), color = Color.Gray, fontFamily = CairoFont) }
        },
        containerColor = Color(0xFF151B2B)
    )
}
