package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NotoSansFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeStudioScreen(
    onBack: () -> Unit,
    onStartSeries: () -> Unit = {},
    onStartEpisode: (title: String, scriptText: String, format: String) -> Unit = { _, _, _ -> }
) {
    var showThumbnailDialog by remember { mutableStateOf(false) }
    var showSeriesDialog by remember { mutableStateOf(false) }

    if (showThumbnailDialog) {
        AutoThumbnailGeneratorDialog(
            context = LocalContext.current,
            initialTitle = "استراتيجية نمو القنوات والدعوة بالذكاء الاصطناعي",
            onDismiss = { showThumbnailDialog = false }
        )
    }

    if (showSeriesDialog) {
        AutoSeriesGeneratorDialog(
            context = LocalContext.current,
            initialSeriesTitle = "سلسلة صحابة رسول الله ﷺ",
            onDismiss = { showSeriesDialog = false },
            onExportSeriesToProjects = { episodes ->
                showSeriesDialog = false
                onStartSeries()
            },
            onStartSingleEpisode = { ep, format ->
                showSeriesDialog = false
                val fullScript = "الحلقة ${ep.episodeNumber}: ${ep.title}\n\nالخطاف:\n${ep.hook}\n\nالمضمون:\n${ep.bodySummary}\n\nالختام:\n${ep.epilogCliffhanger}"
                onStartEpisode(ep.title, fullScript, format)
            }
        )
    }

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = { Text(Translator.tr("استوديو يوتيوب الذكي"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("العودة"), tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Stats
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBox(
                        title = Translator.tr("المشاهدات (الشهرية)"),
                        value = "124K",
                        subtitle = "+12% " + Translator.tr("عن الشهر الماضي"),
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = Translator.tr("المشتركين الجدد"),
                        value = "850",
                        subtitle = "+5% " + Translator.tr("عن الشهر الماضي"),
                        icon = Icons.Default.GroupAdd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Series Progress
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldPrimary.copy(alpha = 0.1f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { showSeriesDialog = true }
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(Translator.tr("مولد السلاسل والروابط التلقائية (Auto Series Engine) 🚀"), color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Translator.tr("إنشاء حزم فيديوهات متسلسلة تلقائياً مع ربط الحلقة القادمة بضغطة زر"), color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // Tools Section
            item {
                Text(Translator.tr("أدوات الذكاء الاصطناعي للفيديوهات الطويلة"), color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            item {
                val tools = listOf(
                    ToolItem(Translator.tr("صورة وعنوان"), Translator.tr("توليد صورة مصغرة جذابة وعناوين يوتيوب مقترحة للفت الانتباه."), Icons.Default.Image, Color(0xFFF44336), "thumbnail"),
                    ToolItem(Translator.tr("إزالة الصمت"), Translator.tr("إزالة تلقائية للوقفات الطويلة والمترددة لزيادة التفاعل."), Icons.Default.ContentCut, Color(0xFFE91E63)),
                    ToolItem(Translator.tr("تصفية الصوت"), Translator.tr("إزالة ضجيج الخلفية وتضخيم الصوت ليكون بجودة البودكاست."), Icons.Default.GraphicEq, Color(0xFF2196F3)),
                    ToolItem(Translator.tr("تقطيع للريلز"), Translator.tr("استخراج أهم 5 لقطات قصيرة ونشرها على يوتيوب شورتس."), Icons.Default.VideoLibrary, Color(0xFF9C27B0)),
                    ToolItem(Translator.tr("الفصول الزمنية"), Translator.tr("توليد فصول وعناوين لليوتيوب تلقائياً مع التوقيت."), Icons.Default.FormatListNumbered, Color(0xFFFF9800)),
                    ToolItem(Translator.tr("ترجمة احترافية"), Translator.tr("إضافة نص تفاعلي متحرك أسفل الفيديو بـ 30 لغة."), Icons.Default.Subtitles, Color(0xFF4CAF50))
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 800.dp),
                    userScrollEnabled = false
                ) {
                    items(tools) { tool ->
                        ToolCard(tool = tool, onClick = {
                            if (tool.id == "thumbnail") {
                                showThumbnailDialog = true
                            }
                        })
                    }
                }
            }

            // Recent Projects
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Translator.tr("المشاريع الطويلة الأخيرة"), color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            items(3) { index ->
                RecentLongProjectItem(index)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showSeriesDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translator.tr("بدء سلسلة محتوى جديدة بنفس النمط"), color = GoldPrimary, fontWeight = FontWeight.Bold, fontFamily = CairoFont)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatBox(title: String, value: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp)
                Icon(icon, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
        }
    }
}

data class ToolItem(val title: String, val desc: String, val icon: ImageVector, val color: Color, val id: String = "")

@Composable
fun ToolCard(tool: ToolItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tool.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(tool.title, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(tool.desc, color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
    }
}

@Composable
fun RecentLongProjectItem(index: Int) {
    val titles = listOf(Translator.tr("شرح السيرة النبوية - الحلقة 5"), Translator.tr("خطبة الجمعة: فضل الصدقة"), Translator.tr("دورة التجويد الميسر - الدرس الأول"))
    val durations = listOf("45:20", "28:15", "1:15:00")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(titles[index], color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(durations[index], color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
        }
        
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ThumbnailGeneratorDialog(onDismiss: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var generatedImage by remember { mutableStateOf<String?>(null) }
    var suggestedTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(Translator.tr("صانع الصور المصغرة والعناوين"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(Translator.tr("ارفع نسبة النقر إلى الظهور (CTR) من خلال عناوين وصور مصغرة احترافية."), color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(Translator.tr("موضوع الفيديو (مثال: أثر الصدقة)"), color = Color.Gray, fontFamily = CairoFont) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            delay(2000) // Mock API call
                            generatedImage = "https://picsum.photos/800/450?random=yt"
                            suggestedTitles = listOf(
                                "سر عظيم في الصدقة يغفل عنه الكثيرون!",
                                "كيف تغير الصدقة حياتك؟ (قصص واقعية)",
                                "الصدقة: استثمارك الحقيقي في الآخرة"
                            )
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isGenerating && prompt.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = DeepSlate)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translator.tr("توليد الصورة والعناوين"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (generatedImage != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(Translator.tr("الصورة المصغرة المقترحة:"), color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = generatedImage,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(Translator.tr("العناوين المقترحة:"), color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    suggestedTitles.forEach { title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Title, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(Translator.tr("إغلاق"), color = Color.Gray, fontFamily = CairoFont)
                }
            }
        }
    }
}
