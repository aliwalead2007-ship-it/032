package com.qabas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsScreen(
    mediaResources: List<MediaResource>,
    onAddResource: (MediaResource) -> Unit,
    onRemoveResource: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    selectedRatio: String,
    onRatioChange: (String) -> Unit,
    videoDuration: String,
    onDurationChange: (String) -> Unit,
    editingStyle: String,
    onStyleChange: (String) -> Unit,
    voiceOver: String,
    onVoiceChange: (String) -> Unit,
    musicVibe: String,
    onMusicChange: (String) -> Unit,
    selectedTemplate: String,
    onTemplateChange: (String) -> Unit,
    customTemplate: Template?,
    onCustomTemplateChange: (Template?) -> Unit,
    videoQuality: String,
    onQualityChange: (String) -> Unit,
    ambientSound: String,
    onAmbientChange: (String) -> Unit
) {
    val analyticsContext = LocalContext.current
    LaunchedEffect(Unit) { AppServices.getAnalyticsService(analyticsContext).logScreenView("StudioSettings") }

    val goldGradient = Brush.horizontalGradient(colors = listOf(GoldSecondary, GoldPrimary))
    
    Scaffold(
        containerColor = DeepSlate, // Darker studio background
        topBar = {
            TopAppBar(
                title = { Text(Translator.tr("إعدادات الاستوديو المتقدمة"), color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("العودة"), tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. My Media Assets
            StudioPanel(title = Translator.tr("أصول الوسائط (Media Assets)"), icon = Icons.Default.FolderSpecial) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaButton(text = Translator.tr("فيديو"), icon = Icons.Default.Movie, onClick = { onAddResource(MediaResource(java.util.UUID.randomUUID().toString(), Translator.tr("فيديو"), "video_${mediaResources.size}.mp4", Icons.Default.Movie)) }, modifier = Modifier.weight(1f))
                    MediaButton(text = Translator.tr("صورة"), icon = Icons.Default.Image, onClick = { onAddResource(MediaResource(java.util.UUID.randomUUID().toString(), Translator.tr("صورة"), "image_${mediaResources.size}.jpg", Icons.Default.Image)) }, modifier = Modifier.weight(1f))
                    MediaButton(text = Translator.tr("صوت"), icon = Icons.Default.Audiotrack, onClick = { onAddResource(MediaResource(java.util.UUID.randomUUID().toString(), Translator.tr("صوت"), "audio_${mediaResources.size}.mp3", Icons.Default.Audiotrack)) }, modifier = Modifier.weight(1f))
                }
                
                if (mediaResources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mediaResources.forEach { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF151B2B), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(res.icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(res.name, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                                }
                                IconButton(onClick = { onRemoveResource(res.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = Translator.tr("حذف"), tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Format & Layout
            StudioPanel(title = Translator.tr("تنسيق العرض (Layout & Format)"), icon = Icons.Default.AspectRatio) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioChip(text = Translator.tr("9:16 (ريلز/تيك توك)"), isSelected = selectedRatio == "9:16", onClick = { onRatioChange("9:16") })
                    StudioChip(text = Translator.tr("16:9 (يوتيوب)"), isSelected = selectedRatio == "16:9", onClick = { onRatioChange("16:9") })
                    StudioChip(text = Translator.tr("1:1 (انستجرام)"), isSelected = selectedRatio == "1:1", onClick = { onRatioChange("1:1") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translator.tr("مدة الفيديو التقريبية:"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val durations = listOf(Translator.tr("15 ثانية"), Translator.tr("30 ثانية"), Translator.tr("60 ثانية"), Translator.tr("3 دقائق (يوتيوب)"))
                    items(durations.size) { i ->
                        StudioChip(text = durations[i], isSelected = videoDuration == durations[i], onClick = { onDurationChange(durations[i]) })
                    }
                }
            }

            // 3. Visual Style & Template
            StudioPanel(title = Translator.tr("الهوية البصرية (Visual Identity)"), icon = Icons.Default.Palette) {
                Text(Translator.tr("الأسلوب الفني:"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf(Translator.tr("روحاني داكن مع إضاءة ذهبية"), Translator.tr("وثائقي سينمائي"), Translator.tr("مينيمال (بسيط ونظيف)"), Translator.tr("إسلامي تراثي (زخارف)"))
                    items(styles.size) { i ->
                        StudioChip(text = styles[i], isSelected = editingStyle == styles[i], onClick = { onStyleChange(styles[i]) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translator.tr("العلامة المائية وهوية الصانع:"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                var includeSocialMedia by remember { mutableStateOf(true) }
                Row(
                    modifier = Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Translator.tr("إضافة حسابات السوشيال ميديا"), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(Translator.tr("سيتم وضعها بأسلوب أنيق بنهاية الفيديو"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    }
                    Switch(
                        checked = includeSocialMedia,
                        onCheckedChange = { includeSocialMedia = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepSlate, checkedTrackColor = GoldPrimary, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF151B2B))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translator.tr("قالب المونتاج (Template):"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val templates = listOf(Translator.tr("تلقائي"), Translator.tr("سريع (Fast Cut)"), Translator.tr("بطيء ومؤثر"), Translator.tr("زوم وديناميكي"))
                    items(templates.size) { i ->
                        StudioChip(text = templates[i], isSelected = selectedTemplate == templates[i], onClick = { onTemplateChange(templates[i]) })
                    }
                }
            }

            // 4. Audio Engineering
            StudioPanel(title = Translator.tr("هندسة الصوت (Audio Engineering)"), icon = Icons.Default.GraphicEq) {
                Text(Translator.tr("صوت المعلق (Voice Over):"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val voices = listOf(Translator.tr("بدون"), Translator.tr("عميق ووقور"), Translator.tr("شاب وحماسي"), Translator.tr("ترتيل وتجويد"), Translator.tr("هادئ ومريح"))
                    items(voices.size) { i ->
                        StudioChip(text = voices[i], isSelected = voiceOver == voices[i], onClick = { onVoiceChange(voices[i]) })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translator.tr("الخلفية الصوتية (BGM):"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val vibes = listOf(Translator.tr("بدون"), Translator.tr("أناشيد بدون موسيقى"), Translator.tr("ملحمية إسلامية"), Translator.tr("تأملية هادئة"), Translator.tr("إيقاع حماسي (Daff)"))
                    items(vibes.size) { i ->
                        StudioChip(text = vibes[i], isSelected = musicVibe == vibes[i], onClick = { onMusicChange(vibes[i]) })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(Translator.tr("المؤثرات المحيطية (Ambient FX):"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ambients = listOf(Translator.tr("تلقائي"), Translator.tr("رياح وصحراء"), Translator.tr("مطر ورعد"), Translator.tr("أصوات مسجد / جموع"), Translator.tr("طيور وطبيعة"))
                    items(ambients.size) { i ->
                        StudioChip(text = ambients[i], isSelected = ambientSound == ambients[i], onClick = { onAmbientChange(ambients[i]) })
                    }
                }
            }

            // 5. Output Quality
            StudioPanel(title = Translator.tr("جودة الريندر (Render Quality)"), icon = Icons.Default.HighQuality) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioChip(text = Translator.tr("قياسية (1080p HD)"), isSelected = videoQuality == Translator.tr("قياسية (1080p HD)") || videoQuality.contains("قياسية"), onClick = { onQualityChange(Translator.tr("قياسية (1080p HD)")) })
                    StudioChip(text = Translator.tr("عالية (4K Cinematic)"), isSelected = videoQuality == Translator.tr("عالية (4K Cinematic)") || videoQuality.contains("عالية") || videoQuality.contains("احترافية"), onClick = { onQualityChange(Translator.tr("عالية (4K Cinematic)")) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Produce Button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = GoldPrimary),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(goldGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Translator.tr("بدء المعالجة السينمائية"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StudioPanel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun MediaButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151B2B)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StudioChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GoldPrimary else Color(0xFF151B2B))
            .border(1.dp, if (isSelected) GoldPrimary else Color(0xFF1E293B), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) DeepSlate else Color.LightGray, fontFamily = NotoSansFont, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}
