package com.example

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    onBack: () -> Unit,
    onAudioSelected: (String) -> Unit = {},
    onApplyToProject: (audioPath: String, usageType: String) -> Unit = { path, _ -> onAudioSelected(path) }
) {
    var selectedTab by remember { mutableStateOf("library") } // "library", "ai_voice", "saved"
    val context = LocalContext.current

    // Global Active Player State
    var activePlayingUrl by remember { mutableStateOf<String?>(null) }
    var activePlayingTitle by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(1L) }
    var playerVolume by remember { mutableFloatStateOf(0.85f) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    var showApplyDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(audioPath, title)

    val coroutineScope = rememberCoroutineScope()

    // Helper to stop/release playback
    fun stopAndReleasePlayer() {
        try {
            mediaPlayerInstance?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayerInstance = null
        isPlaying = false
        activePlayingUrl = null
    }

    // Playback controller
    fun playTrack(audioUrl: String, title: String) {
        if (activePlayingUrl == audioUrl && isPlaying) {
            mediaPlayerInstance?.pause()
            isPlaying = false
            return
        }
        if (activePlayingUrl == audioUrl && mediaPlayerInstance != null) {
            mediaPlayerInstance?.start()
            isPlaying = true
            return
        }

        stopAndReleasePlayer()
        activePlayingUrl = audioUrl
        activePlayingTitle = title

        try {
            val player = MediaPlayer()
            when {
                audioUrl.startsWith("assets/") -> {
                    val assetPath = audioUrl.removePrefix("assets/")
                    val afd = context.assets.openFd(assetPath)
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                audioUrl.startsWith("http://") || audioUrl.startsWith("https://") -> {
                    player.setDataSource(audioUrl)
                }
                audioUrl.startsWith("/") -> {
                    val file = File(audioUrl)
                    if (file.exists()) {
                        player.setDataSource(audioUrl)
                    } else {
                        val afd = context.assets.openFd("audio/recitation_yusuf.mp3")
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                }
                else -> {
                    try {
                        val afd = context.assets.openFd("audio/recitation_yusuf.mp3")
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } catch (_: Exception) {
                        player.setDataSource(audioUrl)
                    }
                }
            }

            player.setVolume(playerVolume, playerVolume)
            player.setOnPreparedListener { mp ->
                totalDurationMs = mp.duration.toLong().coerceAtLeast(1000L)
                mp.start()
                isPlaying = true
            }
            player.setOnCompletionListener {
                isPlaying = false
                currentPositionMs = 0L
            }
            player.setOnErrorListener { _, _, _ ->
                isPlaying = false
                Toast.makeText(context, "تعذر تشغيل هذا المقطع الصوتي", Toast.LENGTH_SHORT).show()
                true
            }
            player.prepareAsync()
            mediaPlayerInstance = player
        } catch (e: Exception) {
            isPlaying = false
            Toast.makeText(context, "خطأ في تشغيل الصوت: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Ticker for current progress
    LaunchedEffect(isPlaying, mediaPlayerInstance) {
        while (isActive && isPlaying && mediaPlayerInstance != null) {
            try {
                mediaPlayerInstance?.let { mp ->
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition.toLong()
                    }
                }
            } catch (_: Exception) {}
            delay(250)
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            stopAndReleasePlayer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioTabButton("المكتبة الصوتية", selectedTab == "library", Modifier.weight(1f)) {
                            selectedTab = "library"
                        }
                        AudioTabButton("استوديو التعليق AI", selectedTab == "ai_voice", Modifier.weight(1.1f)) {
                            selectedTab = "ai_voice"
                        }
                        AudioTabButton("المحفوظات", selectedTab == "saved", Modifier.weight(0.9f)) {
                            selectedTab = "saved"
                        }
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
        bottomBar = {
            // Global Floating Audio Controller Bar (shown when a track is active)
            AnimatedVisibility(
                visible = activePlayingUrl != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = Color(0xFF151B2B),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Title & Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activePlayingTitle.ifBlank { "معاينة صوتية حية" },
                                        color = TextPrimary,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}",
                                        color = TextSecondary,
                                        fontFamily = NotoSansFont,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Apply to project button
                                Button(
                                    onClick = {
                                        showApplyDialog = Pair(activePlayingUrl ?: "", activePlayingTitle)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("اعتماد للمشروع", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { stopAndReleasePlayer() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seek Bar
                        Slider(
                            value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                                mediaPlayerInstance?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = Color(0xFF2A344A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        )

                        // Volume Control & Play/Pause Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Volume slider row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (playerVolume == 0f) Icons.Default.VolumeOff else if (playerVolume < 0.5f) Icons.Default.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "مستوى الصوت",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Slider(
                                    value = playerVolume,
                                    onValueChange = { vol ->
                                        playerVolume = vol
                                        mediaPlayerInstance?.setVolume(vol, vol)
                                    },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GoldPrimary,
                                        activeTrackColor = GoldPrimary,
                                        inactiveTrackColor = Color(0xFF2A344A)
                                    ),
                                    modifier = Modifier.width(120.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${(playerVolume * 100).toInt()}%",
                                    color = TextSecondary,
                                    fontFamily = NotoSansFont,
                                    fontSize = 11.sp
                                )
                            }

                            // Play/Pause button
                            IconButton(
                                onClick = {
                                    activePlayingUrl?.let { playTrack(it, activePlayingTitle) }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = DeepSlate,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DeepSlate
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                "library" -> {
                    AudioLibrarySection(
                        activePlayingUrl = activePlayingUrl,
                        isPlaying = isPlaying,
                        onPlayTrack = { url, title -> playTrack(url, title) },
                        onApply = { url, title -> showApplyDialog = Pair(url, title) }
                    )
                }
                "ai_voice" -> {
                    AIVoiceoverSection(
                        onPlayGenerated = { path, title -> playTrack(path, title) },
                        onApply = { path, title -> showApplyDialog = Pair(path, title) }
                    )
                }
                "saved" -> {
                    SavedAudioTracksSection(
                        activePlayingUrl = activePlayingUrl,
                        isPlaying = isPlaying,
                        onPlayTrack = { url, title -> playTrack(url, title) },
                        onApply = { url, title -> showApplyDialog = Pair(url, title) }
                    )
                }
            }
        }
    }

    // Apply Audio Dialog
    if (showApplyDialog != null) {
        val (audioPath, trackTitle) = showApplyDialog!!
        AlertDialog(
            onDismissRequest = { showApplyDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اعتماد الصوت في المشروع", fontFamily = CairoFont, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "كيف ترغب في تطبيق \"$trackTitle\" على مشروعك الحالي؟",
                        fontFamily = NotoSansFont,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    // Option 1: As Voiceover
                    Button(
                        onClick = {
                            onApplyToProject(audioPath, "voiceover")
                            Toast.makeText(context, "تم تعيين الصوت كـ (تعليق صوتي رئيسي) للمشروع 🎙️", Toast.LENGTH_SHORT).show()
                            showApplyDialog = null
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعيين كـ تعليق صوتي رئيسي (Voiceover)", color = TextPrimary, fontFamily = CairoFont, fontSize = 13.sp)
                    }

                    // Option 2: As Background Music / Ambient
                    Button(
                        onClick = {
                            onApplyToProject(audioPath, "ambient")
                            Toast.makeText(context, "تم تعيين الصوت كـ (خلفية ومؤثر محيطي) للمشروع 🎵", Toast.LENGTH_SHORT).show()
                            showApplyDialog = null
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعيين كـ خلفية صوتية ومؤثر (Background)", color = TextPrimary, fontFamily = CairoFont, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showApplyDialog = null }) {
                    Text("إلغاء", color = TextSecondary, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AudioTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GoldPrimary else Color(0xFF151B2B))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) DeepSlate else TextSecondary,
            fontFamily = CairoFont,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AudioLibrarySection(
    activePlayingUrl: String?,
    isPlaying: Boolean,
    onPlayTrack: (url: String, title: String) -> Unit,
    onApply: (url: String, title: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = remember {
        listOf("الكل", "تلاوات قرآنية", "أصوات الطبيعة والهدوء", "مؤثرات حركية (SFX)", "آهات بشرية وقورة", "أناشيد (بدون موسيقى)")
    }

    val sampleTracks = remember {
        listOf(
            LiveAudioItem(
                id = "quran_yusuf",
                title = "سورة يوسف - تلاوة خاشعة مباركة",
                category = "تلاوات قرآنية",
                duration = "0:30",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "محلي فائق النقاء",
                author = "تلاوة وقورة"
            ),
            LiveAudioItem(
                id = "quran_rahman",
                title = "سورة الرحمن - فبأي آلاء ربكما تكذبان",
                category = "تلاوات قرآنية",
                duration = "3:45",
                audioUrl = "https://everyayah.com/data/Alafasy_128kbps/055001.mp3",
                isAsset = false,
                badge = "مرتل",
                author = "مشاري العفاسي"
            ),
            LiveAudioItem(
                id = "quran_qaf",
                title = "سورة ق - تلاوة تدبرية مؤثرة",
                category = "تلاوات قرآنية",
                duration = "4:20",
                audioUrl = "https://everyayah.com/data/Alafasy_128kbps/050001.mp3",
                isAsset = false,
                badge = "خاشع",
                author = "مشاري العفاسي"
            ),
            LiveAudioItem(
                id = "nature_wind",
                title = "هبوب الرياح والسكينة الجبلية",
                category = "أصوات الطبيعة والهدوء",
                duration = "2:15",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "طبيعة",
                author = "مؤثر بيئي"
            ),
            LiveAudioItem(
                id = "nature_rain",
                title = "صوت المطر الغزير والرعد البعيد",
                category = "أصوات الطبيعة والهدوء",
                duration = "3:10",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "مطر",
                author = "مؤثر هادئ"
            ),
            LiveAudioItem(
                id = "sfx_swish",
                title = "مؤثر انتقال سويش سينمائي (Cinematic Swish)",
                category = "مؤثرات حركية (SFX)",
                duration = "0:03",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "انتقال",
                author = "قبس SFX"
            ),
            LiveAudioItem(
                id = "sfx_horses",
                title = "صهيل الخيل وسيوف التاريخ الإسلامي",
                category = "مؤثرات حركية (SFX)",
                duration = "0:12",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "تاريخي",
                author = "قبس SFX"
            ),
            LiveAudioItem(
                id = "vocal_humming",
                title = "آهات بشرية وقورة للمونتاج والوثائقيات",
                category = "آهات بشرية وقورة",
                duration = "1:30",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "بدون إيقاع",
                author = "إنشاد بشري"
            ),
            LiveAudioItem(
                id = "nasheed_hope",
                title = "أنشودة تفاءل بالخير وأبشر بالأمل",
                category = "أناشيد (بدون موسيقى)",
                duration = "2:50",
                audioUrl = "assets/audio/recitation_yusuf.mp3",
                isAsset = true,
                badge = "حماسي",
                author = "كورال إسلامي"
            )
        )
    }

    val filteredTracks by remember(sampleTracks, selectedCategory, searchQuery) {
        derivedStateOf {
            sampleTracks.filter { track ->
                (selectedCategory == "الكل" || track.category == selectedCategory) &&
                (searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.category.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0F19))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(Translator.tr("ابحث في التلاوات، المؤثرات، الأناشيد..."), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedBorderColor = GoldPrimary,
                    unfocusedContainerColor = Color(0xFF151B2B),
                    focusedContainerColor = Color(0xFF151B2B),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // Categories Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat,
                            fontFamily = CairoFont,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF151B2B),
                        labelColor = TextSecondary,
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = DeepSlate
                    ),
                    border = BorderStroke(1.dp, if (selectedCategory == cat) GoldPrimary else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Track List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredTracks, key = { it.id }) { track ->
                val isTrackPlaying = isPlaying && activePlayingUrl == track.audioUrl
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                    border = BorderStroke(1.dp, if (isTrackPlaying) GoldPrimary else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause button
                        IconButton(
                            onClick = { onPlayTrack(track.audioUrl, track.title) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isTrackPlaying) GoldPrimary else Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = if (isTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTrackPlaying) "إيقاف" else "تشغيل",
                                tint = if (isTrackPlaying) DeepSlate else GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isTrackPlaying) GoldPrimary else TextPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(track.category, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp)
                                Text(" • ", color = Color.DarkGray, fontSize = 10.sp)
                                Text(track.duration, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(track.badge, color = GoldPrimary, fontSize = 9.sp, fontFamily = CairoFont)
                                }
                            }
                        }

                        // Apply to Project Button
                        Button(
                            onClick = { onApply(track.audioUrl, track.title) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اعتماد", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AIVoiceoverSection(
    onPlayGenerated: (path: String, title: String) -> Unit,
    onApply: (path: String, title: String) -> Unit
) {
    val context = LocalContext.current
    var scriptText by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableStateOf("AZURE") } // "AZURE" or "ELEVENLABS"
    var selectedVoiceName by remember { mutableStateOf("ar-SA-HamedNeural") }
    var selectedTone by remember { mutableStateOf("وثائقي") }
    var speakingRate by remember { mutableStateOf("+0%") } // "-10%", "+0%", "+15%", "+30%"
    var pitchSetting by remember { mutableStateOf("+0%") } // "-5%", "+0%", "+5%"

    var isGenerating by remember { mutableStateOf(false) }
    var generatedAudioPath by remember { mutableStateOf<String?>(null) }
    var generatedAudioTitle by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val azureVoices = remember {
        listOf(
            VoiceCharacter("ar-SA-HamedNeural", "حامد (سعودي)", "وقور ورخيم - تدبر وتلاوة", "خاشع"),
            VoiceCharacter("ar-EG-ShakirNeural", "شاكر (مصري)", "وثائقي ورصين - سيرة وقصص", "وثائقي"),
            VoiceCharacter("ar-SA-NaayfNeural", "نايف (سعودي)", "حماسي وسريع - خطاف وريلز", "حماسي"),
            VoiceCharacter("ar-SA-ZariyahNeural", "سلمى (سعودية)", "هادئة وإرشادية - تزكية وخواطر", "هادئ"),
            VoiceCharacter("ar-AE-HamdanNeural", "حمدان (إماراتي)", "فصيح ودافئ - حكمة وعبر", "رسمي"),
            VoiceCharacter("ar-QA-MoazNeural", "معاذ (قطري)", "روحاني خاشع - أدعية وأذكار", "دعاء")
        )
    }

    val elevenlabsVoices = remember {
        listOf(
            VoiceCharacter("21m00Tcm4TlvDq8ikWAM", "Rachel (ElevenLabs)", "هادئ متزن وواضح", "طبيعي"),
            VoiceCharacter("pNInz6obpgDQGcFmaJgB", "Adam (ElevenLabs)", "عميق وملحمي سينمائي", "ملحمي"),
            VoiceCharacter("ErXwobaYiN019PkySvjV", "Antoni (ElevenLabs)", "حيوي وشبابي معبّر", "حماسي"),
            VoiceCharacter("EXAVITQu4vr4xnSDxMaL", "Bella (ElevenLabs)", "سردي وناعم للمونتاج", "قصصي")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Quick Presets Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("قوالب نصوص سريعة جاهزة للإلقاء:", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SuggestionPresetChip("📜 قصة صحابي") {
                            scriptText = "في ظلال السيرة النبوية، سطّر أصحاب النبي صلى الله عليه وسلم ملاحم من الصبر واليقين أنارت دروب البشرية جمعاء."
                            selectedTone = "قصصي"
                            selectedVoiceName = "ar-EG-ShakirNeural"
                        }
                    }
                    item {
                        SuggestionPresetChip("🌿 تدبر خاشع") {
                            scriptText = "يا ابن آدم، لو تأملت في ملكوت السماوات والأرض لوجدت أن كل ذرة تسبح بحمد الله.. فما بالك بقلبك؟"
                            selectedTone = "روحاني"
                            selectedVoiceName = "ar-SA-HamedNeural"
                        }
                    }
                    item {
                        SuggestionPresetChip("⚡ خطاف ريلز") {
                            scriptText = "ثلاث دقائق قد تغيّر مسار يومك بالكامل! هل فكرت يوماً ماذا لو كانت هذه اللحظة هي بدايتك الحقيقية مع الله؟"
                            selectedTone = "حماسي"
                            selectedVoiceName = "ar-SA-NaayfNeural"
                        }
                    }
                    item {
                        SuggestionPresetChip("🕊️ وصية نبوية") {
                            scriptText = "احرص على ما ينفعك واستعن بالله ولا تعجز، فما قدّر الله لك نافذ، والخير في كل قضاء."
                            selectedTone = "رسمي"
                            selectedVoiceName = "ar-AE-HamdanNeural"
                        }
                    }
                }
            }
        }

        // Script Text Input
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("النص المراد تحويله لصوت:", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${scriptText.length} حرف", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = scriptText,
                onValueChange = { scriptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = {
                    Text(
                        "اكتب نص السكريبت هنا مشكولاً أو عادياً (مثال: في عمق التاريخ الإسلامي، برزت أسماء أنارت دروب البشرية...)",
                        color = TextSecondary,
                        fontFamily = NotoSansFont,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedBorderColor = GoldPrimary,
                    unfocusedContainerColor = Color(0xFF151B2B),
                    focusedContainerColor = Color(0xFF151B2B),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Engine Selector
        Column {
            Text("محرك التوليد الصوتي:", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineCard(
                    title = "Azure Neural Speech",
                    subtitle = "نطق عربي وتشكيل فائق الدقة",
                    icon = Icons.Default.Bolt,
                    isSelected = selectedEngine == "AZURE",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedEngine = "AZURE"
                    selectedVoiceName = "ar-SA-HamedNeural"
                }

                EngineCard(
                    title = "ElevenLabs Studio",
                    subtitle = "نبرات سينمائية فائقة النقاء",
                    icon = Icons.Default.Mic,
                    isSelected = selectedEngine == "ELEVENLABS",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedEngine = "ELEVENLABS"
                    selectedVoiceName = "21m00Tcm4TlvDq8ikWAM"
                }
            }
        }

        // Voice Character Selector
        Column {
            Text("المؤدي الصوتي والنبرة:", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val currentVoiceList = if (selectedEngine == "AZURE") azureVoices else elevenlabsVoices
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(currentVoiceList) { voice ->
                    val isSelected = selectedVoiceName == voice.id
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable {
                                selectedVoiceName = voice.id
                                selectedTone = voice.tag
                            },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color(0xFF151B2B)),
                        border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(voice.name, color = if (isSelected) GoldPrimary else TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(voice.desc, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Pacing & Rate Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("سرعة الإلقاء:", color = TextPrimary, fontFamily = CairoFont, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("-10%" to "هادئ 0.9x", "+0%" to "عادي 1.0x", "+15%" to "سريع 1.15x").forEach { (valRate, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (speakingRate == valRate) GoldPrimary else Color(0xFF151B2B))
                                .clickable { speakingRate = valRate }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (speakingRate == valRate) DeepSlate else TextSecondary, fontSize = 10.sp, fontFamily = CairoFont)
                        }
                    }
                }
            }
        }

        // Generate Button
        Button(
            onClick = {
                if (scriptText.isBlank()) {
                    Toast.makeText(context, "الرجاء كتابة نص السكريبت أولاً ✍️", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isGenerating = true
                coroutineScope.launch {
                    val resultPath = if (selectedEngine == "ELEVENLABS") {
                        AppServices.generateVoiceover(
                            text = scriptText,
                            tone = selectedTone,
                            voiceId = selectedVoiceName,
                            engine = selectedEngine,
                            rate = speakingRate,
                            pitch = pitchSetting
                        )
                    } else {
                        AppServices.generateVoiceover(
                            text = scriptText,
                            tone = selectedTone,
                            customVoiceName = selectedVoiceName,
                            engine = selectedEngine,
                            rate = speakingRate,
                            pitch = pitchSetting
                        )
                    }
                    isGenerating = false
                    if (resultPath != null) {
                        generatedAudioPath = resultPath
                        generatedAudioTitle = "تعليق صوتي: ${scriptText.take(25)}..."
                        Toast.makeText(context, "تم توليد الصوت وحفظه في الذاكرة بنجاح! 🎙️✨", Toast.LENGTH_SHORT).show()

                        // Auto-archive in Room database
                        try {
                            val trackEntity = AudioTrackEntity(
                                id = "AI_VOICE_${System.currentTimeMillis()}",
                                title = generatedAudioTitle,
                                artistOrVibe = "$selectedEngine | $selectedTone",
                                typeCategory = "AI_VOICEOVER",
                                audioUrlOrPath = resultPath,
                                duration = "0:30",
                                scriptText = scriptText,
                                createdAt = System.currentTimeMillis()
                            )
                            AppDatabase.getDatabase(context).audioTrackDao().insertTrack(trackEntity)
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(context, "تعذر التوليد. تأكد من مفاتيح API أو الاتصال.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("جاري توليد الصوت الحقيقي...", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = DeepSlate)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توليد التعليق الصوتي الذكي 🎙️", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Generated Audio Preview Card
        if (generatedAudioPath != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                border = BorderStroke(1.5.dp, GoldPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الصوت جاهز في ذاكرة المشروع", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            onPlayGenerated(generatedAudioPath!!, generatedAudioTitle)
                        }) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "معاينة", tint = GoldPrimary, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        generatedAudioTitle,
                        color = TextSecondary,
                        fontFamily = NotoSansFont,
                        fontSize = 12.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onApply(generatedAudioPath!!, generatedAudioTitle)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اعتماد للمشروع", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val file = File(generatedAudioPath!!)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الصوتي"))
                                    } else {
                                        Toast.makeText(context, "الملف غير متوفر في الذاكرة", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "المسار: $generatedAudioPath", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, Color(0xFF2A344A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", color = TextPrimary, fontFamily = CairoFont, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SavedAudioTracksSection(
    activePlayingUrl: String?,
    isPlaying: Boolean,
    onPlayTrack: (url: String, title: String) -> Unit,
    onApply: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioTrackDao = remember { AppDatabase.getDatabase(context).audioTrackDao() }
    val savedTracks by audioTrackDao.getAllAudioTracks().collectAsState(initial = emptyList())

    if (savedTracks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                Text("لا توجد تسجيلات أو تعليقات صوتية محفوظة بعد", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
                Text("قم بتوليد تعليق صوتي بالذكاء الاصطناعي وسيتم حفظه وأرشفته هنا تلقائياً.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(savedTracks, key = { it.id }) { item ->
                val isTrackPlaying = isPlaying && activePlayingUrl == item.audioUrlOrPath
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2B)),
                    border = BorderStroke(1.dp, if (isTrackPlaying) GoldPrimary else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onPlayTrack(item.audioUrlOrPath, item.title) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isTrackPlaying) GoldPrimary else Color(0xFF1E293B))
                            ) {
                                Icon(
                                    if (isTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isTrackPlaying) DeepSlate else GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    color = if (isTrackPlaying) GoldPrimary else TextPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${item.artistOrVibe} • ${formatTimestamp(item.createdAt)}",
                                    color = TextSecondary,
                                    fontFamily = NotoSansFont,
                                    fontSize = 11.sp
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        audioTrackDao.deleteTrackById(item.id)
                                        Toast.makeText(context, "تم حذف المقطع من الأرشيف", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (item.scriptText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "\"${item.scriptText}\"",
                                color = TextSecondary,
                                fontFamily = NotoSansFont,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onApply(item.audioUrlOrPath, item.title) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تطبيق على المشروع", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionPresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = GoldPrimary, fontFamily = CairoFont, fontSize = 11.sp)
    }
}

@Composable
fun EngineCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color(0xFF151B2B)),
        border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(icon, contentDescription = null, tint = if (isSelected) GoldPrimary else TextSecondary, modifier = Modifier.size(18.dp))
                if (isSelected) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = if (isSelected) GoldPrimary else TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(subtitle, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.sp, maxLines = 1)
        }
    }
}

data class LiveAudioItem(
    val id: String,
    val title: String,
    val category: String,
    val duration: String,
    val audioUrl: String,
    val isAsset: Boolean = false,
    val badge: String = "قبس",
    val author: String = ""
)

data class VoiceCharacter(
    val id: String,
    val name: String,
    val desc: String,
    val tag: String
)

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
