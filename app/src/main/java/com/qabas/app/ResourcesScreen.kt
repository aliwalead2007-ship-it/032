package com.qabas.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qabas.app.ui.input.MediaPickerHelpers
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    mediaResources: List<MediaResource>,
    onAddResource: (MediaResource) -> Unit,
    onUpdateResource: (MediaResource) -> Unit,
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
    onAmbientChange: (String) -> Unit,
    /** وصف الفكرة الحالي — يُستخدم لاقتراح B-Roll مناسب + تلميح تقليد الستايل */
    ideaText: String = "",
    videoStyleAnalysis: VideoStyleAnalysis? = null
) {
    val context = LocalContext.current
    val analyticsContext = LocalContext.current
    LaunchedEffect(Unit) { AppServices.getAnalyticsService(analyticsContext).logScreenView("Resources") }

    val scope = rememberCoroutineScope()
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showBRollSheet by remember { mutableStateOf(false) }
    var brollCategory by remember { mutableStateOf("الجميع") }
    var brollQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val goldGradient = Brush.horizontalGradient(colors = listOf(GoldSecondary, GoldPrimary))

    // —— Real pickers ——
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val meta = MediaPickerHelpers.extractVideoMeta(context, uri)
        onAddResource(
            MediaResource(
                id = UUID.randomUUID().toString(),
                type = "video",
                name = meta.fileName,
                icon = MediaResource.iconForType("video"),
                uri = uri.toString(),
                durationLabel = meta.durationFormatted,
                sizeLabel = meta.sizeFormatted
            )
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val name = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                } ?: "image_${System.currentTimeMillis()}.jpg"
            } catch (_: Exception) { "image_${System.currentTimeMillis()}.jpg" }
            onAddResource(
                MediaResource(
                    id = UUID.randomUUID().toString(),
                    type = "image",
                    name = name,
                    icon = MediaResource.iconForType("image"),
                    uri = uri.toString()
                )
            )
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val meta = MediaPickerHelpers.extractAudioMeta(context, uri)
        onAddResource(
            MediaResource(
                id = UUID.randomUUID().toString(),
                type = "audio",
                name = meta.fileName,
                icon = MediaResource.iconForType("audio"),
                uri = uri.toString(),
                durationLabel = meta.durationFormatted,
                sizeLabel = meta.sizeFormatted
            )
        )
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: "document.pdf"
        } catch (_: Exception) { "document.pdf" }
        onAddResource(
            MediaResource(
                id = UUID.randomUUID().toString(),
                type = "document",
                name = name,
                icon = MediaResource.iconForType("document"),
                uri = uri.toString()
            )
        )
    }

    // B-Roll suggested by idea text
    val suggestedBRoll = remember(ideaText) {
        if (ideaText.isBlank()) emptyList()
        else listOf(BRollEngine.matchBRoll(ideaText))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Translator.tr("جمع الموارد والإعدادات"),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("العودة"), tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // —— Style clone hint (point 5) ——
            if (videoStyleAnalysis != null && videoStyleAnalysis.detectedStyle.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Translator.tr("ستايل مستنسخ نشط"),
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = videoStyleAnalysis.detectedStyle.take(120),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            Translator.tr("الموارد التي ترفعها ستُستخدم مع هذا الستايل عند التوليد."),
                            color = Color(0xFF34D399),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // —— Section 1: My Media (real pickers) ——
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    Translator.tr("وسائطي"),
                    color = GoldPrimary,
                    fontSize = 18.sp,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Default.Movie, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translator.tr("رفع فيديو"), color = TextPrimary, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Default.Image, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translator.tr("رفع صور"), color = TextPrimary, fontSize = 13.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { documentPicker.launch("application/pdf") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Default.Description, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translator.tr("رفع مستند"), color = TextPrimary, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { audioPicker.launch("audio/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Default.Audiotrack, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translator.tr("إضافة صوت"), color = TextPrimary, fontSize = 13.sp)
                    }
                }

                // Resource list with previews
                if (mediaResources.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mediaResources.forEach { resource ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardSurface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (resource.uri != null && (resource.type == "image" || resource.type == "broll")) {
                                        AsyncImage(
                                            model = resource.uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Icon(resource.icon, null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(
                                            resource.name,
                                            color = TextPrimary,
                                            fontFamily = NotoSansFont,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 13.sp
                                        )
                                        val meta = listOfNotNull(
                                            resource.type,
                                            resource.durationLabel,
                                            resource.sizeLabel,
                                            if (resource.isBRoll) "B-Roll" else null
                                        ).joinToString(" • ")
                                        Text(meta, color = TextSecondary, fontSize = 11.sp)
                                        
                                        var expanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.padding(top = 4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF1E293B))
                                                    .clickable { expanded = true }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = resource.category?.let { "🏷️ $it" } ?: "🏷️ إضافة تصنيف",
                                                    color = if (resource.category != null) GoldPrimary else TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontFamily = NotoSansFont
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                containerColor = Color(0xFF151B2B)
                                            ) {
                                                listOf("درامي", "خاشع", "حماسي", "هادئ", "طبيعة", "أشخاص", "عام").forEach { tag ->
                                                    DropdownMenuItem(
                                                        text = { Text(tag, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp) },
                                                        onClick = {
                                                            expanded = false
                                                            onUpdateResource(resource.copy(category = tag))
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = { onRemoveResource(resource.id) }) {
                                    Icon(Icons.Default.Delete, Translator.tr("حذف"), tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface.copy(alpha = 0.5f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Translator.tr("لم يتم إرفاق وسائط مخصصة (سيقوم الذكاء الاصطناعي بتوليد كل المشاهد تلقائياً ✨)"),
                            color = TextSecondary,
                            fontFamily = NotoSansFont,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // —— Section: Smart B-Roll (point 3) ——
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Translator.tr("مكتبة B-Roll الذكية"),
                        color = GoldPrimary,
                        fontSize = 18.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showBRollSheet = true }) {
                        Text(Translator.tr("تصفح الكل"), color = GoldPrimary, fontSize = 12.sp)
                    }
                }

                if (suggestedBRoll.isNotEmpty()) {
                    Text(
                        Translator.tr("مقترح حسب فكرتك"),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(suggestedBRoll, key = { it.id }) { item ->
                            BRollChipCard(
                                item = item,
                                alreadyAdded = mediaResources.any { it.uri == item.videoUrl },
                                onAdd = {
                                    onAddResource(
                                        MediaResource(
                                            id = "broll_${item.id}_${System.currentTimeMillis()}",
                                            type = "broll",
                                            name = item.title,
                                            icon = MediaResource.iconForType("broll"),
                                            uri = item.videoUrl,
                                            isBRoll = true,
                                            category = item.category
                                        )
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(Translator.tr("تمت إضافة مقطع B-Roll"))
                                    }
                                }
                            )
                        }
                    }
                }

                // Quick category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BRollEngine.categories.take(5).forEach { cat ->
                        ChoiceChip(
                            text = cat,
                            isSelected = false,
                            onClick = {
                                brollCategory = cat
                                showBRollSheet = true
                            }
                        )
                    }
                }
            }

            // —— Section 2: Production Settings ——
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161E2E))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    Translator.tr("إعدادات الإنتاج"),
                    color = GoldPrimary,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("الأبعاد"), color = TextSecondary, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1:1", "16:9", "9:16").forEach { ratio ->
                            RatioButton(
                                text = ratio,
                                isSelected = selectedRatio == ratio,
                                gradient = goldGradient,
                                modifier = Modifier.weight(1f)
                            ) { onRatioChange(ratio) }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("المدة"), color = TextSecondary, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Translator.tr("15 ثانية"), Translator.tr("30 ثانية"), Translator.tr("60 ثانية")).forEach { duration ->
                            ChoiceChip(
                                text = duration,
                                isSelected = videoDuration == duration,
                                onClick = { onDurationChange(duration) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("أسلوب المونتاج"), color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("3nvus", Translator.tr("سينمائي"), Translator.tr("وثائقي")).forEach { style ->
                            ChoiceChip(text = style, isSelected = editingStyle == style, onClick = { onStyleChange(style) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("التمثيل الصوتي"), color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Translator.tr("عميق"), Translator.tr("حساس"), Translator.tr("رئاسي")).forEach { tone ->
                            ChoiceChip(text = tone, isSelected = voiceOver == tone, onClick = { onVoiceChange(tone) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("المؤثرات الصوتية"), color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Translator.tr("طبيعة"),
                            Translator.tr("مدينة"),
                            Translator.tr("هادئ"),
                            Translator.tr("نشاط"),
                            Translator.tr("درامي"),
                            Translator.tr("بدون")
                        ).forEach { vibe ->
                            ChoiceChip(text = vibe, isSelected = musicVibe == vibe, onClick = { onMusicChange(vibe) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Translator.tr("قالب المونتاج"), color = TextSecondary, fontSize = 13.sp)
                        TextButton(onClick = { showCustomizeDialog = true }) {
                            Text(Translator.tr("تخصيص القالب"), color = GoldPrimary, fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Translator.tr("تلقائي"),
                            Translator.tr("داكن سينمائي"),
                            Translator.tr("سريع Reels"),
                            Translator.tr("وثائقي"),
                            Translator.tr("تحفيزي"),
                            Translator.tr("فكاهي سريع")
                        ).forEach { t ->
                            ChoiceChip(text = t, isSelected = selectedTemplate == t, onClick = { onTemplateChange(t) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("جودة التوليد"), color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Translator.tr("قياسية (1080p HD)"), Translator.tr("احترافية (4K Cinematic)")).forEach { q ->
                            ChoiceChip(text = q, isSelected = videoQuality == q, onClick = { onQualityChange(q) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Translator.tr("المؤثرات الصوتية المحيطة"), color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Translator.tr("طبيعة"),
                            Translator.tr("مدينة"),
                            Translator.tr("هادئ"),
                            Translator.tr("نشاط"),
                            Translator.tr("تلقائي"),
                            Translator.tr("بدون")
                        ).forEach { ambient ->
                            ChoiceChip(text = ambient, isSelected = ambientSound == ambient, onClick = { onAmbientChange(ambient) })
                        }
                    }
                }
            }

            // —— Section 3: AI Options (pipeline wiring) ——
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    Translator.tr("خيارات الذكاء"),
                    color = GoldPrimary,
                    fontSize = 18.sp,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                if (mediaResources.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(Translator.tr("الرجاء رفع موارد أولاً"))
                                    }
                                } else {
                                    onNext()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                Translator.tr("استخدم مواردي فقط"),
                                color = TextPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            Translator.tr("يستخدم فقط الموارد التي رفعتها"),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = onNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(goldGradient, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Translator.tr("دع الذكاء يكمل الناقص"),
                                    color = DeepSlate,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Text(
                            Translator.tr("يبحث ويولد المكملات من B-Roll + Pexels"),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Summary chip
                if (mediaResources.isNotEmpty()) {
                    Text(
                        text = Translator.tr("جاهز للإنتاج") + " • ${mediaResources.size} " + Translator.tr("مورد"),
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Customize template dialog
        if (showCustomizeDialog) {
            var tempSpeed by remember { mutableStateOf(customTemplate?.parameters?.get("speed")?.toFloatOrNull() ?: 0.6f) }
            var tempColors by remember { mutableStateOf(customTemplate?.parameters?.get("colors") ?: Translator.tr("ذهبي")) }
            var tempTransitions by remember { mutableStateOf(customTemplate?.parameters?.get("transitions") ?: "Fade") }
            var tempTextAnim by remember { mutableStateOf(customTemplate?.parameters?.get("textAnimation") ?: Translator.tr("الكتابة")) }

            AlertDialog(
                onDismissRequest = { showCustomizeDialog = false },
                title = { Text(Translator.tr("تخصيص القالب"), color = GoldPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(Translator.tr("سرعة الانتقالات (بطيء إلى سريع)"), color = TextPrimary)
                            Slider(
                                value = tempSpeed,
                                onValueChange = { tempSpeed = it },
                                valueRange = 0.3f..1.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column {
                            Text(Translator.tr("الألوان السائدة"), color = TextPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(Translator.tr("ذهبي"), Translator.tr("داكن"), Translator.tr("مشرق"), Translator.tr("عشوائي")).forEach { c ->
                                    ChoiceChip(text = c, isSelected = tempColors == c, onClick = { tempColors = c })
                                }
                            }
                        }
                        Column {
                            Text(Translator.tr("نوع الانتقالات"), color = TextPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Fade", "Slide", "Zoom", "Glitch", Translator.tr("عشوائي")).forEach { t ->
                                    ChoiceChip(text = t, isSelected = tempTransitions == t, onClick = { tempTransitions = t })
                                }
                            }
                        }
                        Column {
                            Text(Translator.tr("حركة النصوص"), color = TextPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Translator.tr("كتابة"),
                                    Translator.tr("قفز"),
                                    Translator.tr("اهتزاز"),
                                    Translator.tr("تلاشي"),
                                    Translator.tr("عشوائي")
                                ).forEach { a ->
                                    ChoiceChip(text = a, isSelected = tempTextAnim == a, onClick = { tempTextAnim = a })
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newTemplate = Template(
                            id = "custom_${System.currentTimeMillis()}",
                            name = Translator.tr("مخصص"),
                            parameters = mapOf(
                                "speed" to tempSpeed.toString(),
                                "colors" to tempColors,
                                "transitions" to tempTransitions,
                                "textAnimation" to tempTextAnim
                            )
                        )
                        onCustomTemplateChange(newTemplate)
                        showCustomizeDialog = false
                    }) {
                        Text(Translator.tr("حفظ التخصيص"), color = GoldPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomizeDialog = false }) {
                        Text(Translator.tr("إلغاء"), color = TextSecondary)
                    }
                },
                containerColor = DeepSlate
            )
        }

        // B-Roll bottom sheet browser
        if (showBRollSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBRollSheet = false },
                containerColor = DeepSlate,
                dragHandle = { BottomSheetDefaults.DragHandle(color = GoldPrimary) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        Translator.tr("مكتبة B-Roll"),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    OutlinedTextField(
                        value = brollQuery,
                        onValueChange = { brollQuery = it },
                        placeholder = { Text(Translator.tr("ابحث..."), color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BRollEngine.categories.forEach { cat ->
                            ChoiceChip(
                                text = cat,
                                isSelected = brollCategory == cat,
                                onClick = { brollCategory = cat }
                            )
                        }
                    }

                    val filtered = remember(brollCategory, brollQuery) {
                        val base = BRollEngine.getBRollForCategory(brollCategory)
                        if (brollQuery.isBlank()) base else BRollEngine.searchBRoll(brollQuery)
                    }

                    filtered.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .clickable {
                                    onAddResource(
                                        MediaResource(
                                            id = "broll_${item.id}_${System.currentTimeMillis()}",
                                            type = "broll",
                                            name = item.title,
                                            icon = MediaResource.iconForType("broll"),
                                            uri = item.videoUrl,
                                            isBRoll = true,
                                            category = item.category
                                        )
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(Translator.tr("تمت الإضافة: ") + item.title)
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                                Text(item.category, color = TextSecondary, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.AddCircle, null, tint = GoldPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BRollChipCard(
    item: BRollItem,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(
                1.dp,
                if (alreadyAdded) Color(0xFF34D399).copy(alpha = 0.5f) else Color(0xFF1E293B),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !alreadyAdded, onClick = onAdd)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            item.title,
            color = TextPrimary,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = CairoFont
        )
        Text(
            if (alreadyAdded) Translator.tr("مضاف ✓") else Translator.tr("إضافة"),
            color = if (alreadyAdded) Color(0xFF34D399) else GoldPrimary,
            fontSize = 10.sp
        )
    }
}
