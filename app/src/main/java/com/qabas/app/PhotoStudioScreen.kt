package com.qabas.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.graphics.Brush

data class PhotoTemplate(
    val title: String,
    val description: String,
    val suggestedText: String,
    val suggestedAudio: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoStudioScreen(
    onBack: () -> Unit,
    onNavigateToAdvancedEdit: (String) -> Unit = {},
    onSaveProject: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentStep by remember { mutableStateOf(1) } // 1 = Select, 2 = Edit, 3 = Export
    
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var textOverlay by remember { mutableStateOf("") }
    var selectedAudioOption by remember { mutableStateOf("تلاوة هادئة") }
    var selectedTemplate by remember { mutableStateOf<PhotoTemplate?>(null) }

    // Adjustment states
    var brightness by remember { mutableFloatStateOf(0f) } // -50 to 50
    var contrast by remember { mutableFloatStateOf(1f) }    // 0.5 to 1.5
    var saturation by remember { mutableFloatStateOf(1f) }  // 0 to 2
    var selectedCropRatio by remember { mutableStateOf("9:16") } // 9:16, 1:1, 16:9, 4:5
    var textStyleTheme by remember { mutableStateOf("GOLD_DEEP") }
    
    var processing by remember { mutableStateOf(false) }
    var generatedVideoPath by remember { mutableStateOf<String?>(null) }
    var exportedPhotoPath by remember { mutableStateOf<String?>(null) }
    var exportType by remember { mutableStateOf("PHOTO") } // "PHOTO" or "VIDEO"
    var progressMessage by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = (selectedImageUris + uris).distinct()
            if (currentStep == 1 && selectedImageUris.isNotEmpty()) {
                currentStep = 2
            }
        }
    }

    val audioOptions = listOf("بدون صوت", "تلاوة هادئة", "مؤثرات طبيعية", "أنشودة تأملية")
    
    val photoTemplates = remember { listOf(
        PhotoTemplate("قرآن", "آية هادئة", "وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ", "تلاوة هادئة", Icons.Default.Star, GoldPrimary),
        PhotoTemplate("أذكار", "راحة وطمأنينة", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "مؤثرات طبيعية", Icons.Default.Favorite, Color(0xFF6366F1)),
        PhotoTemplate("حكم", "مواعظ مؤثرة", "مَن عَرَفَ نَفْسَهُ اشْتَغَلَ بِإِصْلَاحِهَا", "بدون صوت", Icons.Default.FormatQuote, Color(0xFF10B981)),
        PhotoTemplate("قصة", "سرد متسلسل", "فِي كُلِّ تَأْخِيرَةٍ خِيرَةٌ مِنَ اللَّهِ", "تلاوة هادئة", Icons.Default.List, Color(0xFF0EA5E9)),
        PhotoTemplate("حر", "إبداع خاص", "", "بدون صوت", Icons.Default.Edit, Color(0xFF8B5CF6))
    ) }

    if (selectedTemplate == null) {
        selectedTemplate = photoTemplates.first()
        textOverlay = selectedTemplate!!.suggestedText
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استوديو الصور والمصمم الدعوي", fontFamily = CairoFont, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && !processing && generatedVideoPath == null && exportedPhotoPath == null) {
                            currentStep--
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = currentStep, label = "Step Transition") { step ->
                when (step) {
                    1 -> Step1ImageSelection(
                        selectedImageUris = selectedImageUris,
                        onSelectMore = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onRemoveImage = { uri ->
                            selectedImageUris = selectedImageUris.filter { it != uri }
                        },
                        onNext = { currentStep = 2 }
                    )
                    2 -> Step2Editor(
                        selectedImageUris = selectedImageUris,
                        textOverlay = textOverlay,
                        onTextChange = { textOverlay = it },
                        selectedTemplate = selectedTemplate!!,
                        onTemplateChange = { 
                            selectedTemplate = it
                            textOverlay = it.suggestedText
                            selectedAudioOption = it.suggestedAudio
                        },
                        templates = photoTemplates,
                        selectedAudioOption = selectedAudioOption,
                        onAudioChange = { selectedAudioOption = it },
                        audioOptions = audioOptions,
                        brightness = brightness,
                        onBrightnessChange = { brightness = it },
                        contrast = contrast,
                        onContrastChange = { contrast = it },
                        saturation = saturation,
                        onSaturationChange = { saturation = it },
                        selectedCropRatio = selectedCropRatio,
                        onCropRatioChange = { selectedCropRatio = it },
                        textStyleTheme = textStyleTheme,
                        onTextStyleThemeChange = { textStyleTheme = it },
                        onAutoEnhance = {
                            coroutineScope.launch {
                                textOverlay = "جاري استنباط عبارة دعوية..."
                                try {
                                    val prompt = "اكتب عبارة دعوية أو آية أو حكمة إيمانية قصيرة جداً ومؤثرة بالتشكيل تصلح كتصميم أو منشور إسلامي فاخر. العبارة فقط."
                                    val response = AppServices.chatWithAssistant(listOf(Pair(true, prompt)), "أنت مساعد متخصص في كتابة نصوص دعوية ملهمة ومختصرة.")
                                    textOverlay = response.trim('"', ' ', '\n')
                                    
                                    val bestStyle = StyleBrain.chooseBestStyleForIdea(textOverlay, 15, "هادئ", "عام")
                                    val styleName = bestStyle?.name ?: ""
                                    selectedTemplate = photoTemplates.find { it.title.contains(styleName) || styleName.contains(it.title) } ?: photoTemplates.shuffled().first()
                                } catch (e: Exception) {
                                    textOverlay = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ"
                                }
                            }
                        },
                        onExportImage = {
                            if (selectedImageUris.isEmpty()) {
                                Toast.makeText(context, "الرجاء اختيار صورة للتحرير", Toast.LENGTH_SHORT).show()
                                return@Step2Editor
                            }
                            exportType = "PHOTO"
                            currentStep = 3
                            processing = true
                            progressMessage = "جاري تطبيق المعالجة وتصدير الصورة..."

                            coroutineScope.launch {
                                val params = ImageAdjustmentParams(
                                    brightness = brightness,
                                    contrast = contrast,
                                    saturation = saturation,
                                    cropRatio = selectedCropRatio,
                                    textOverlay = textOverlay,
                                    textStyleTheme = textStyleTheme,
                                    showBrandWatermark = true
                                )
                                val outFile = ImageEditorEngine.processAndExportImage(
                                    context = context,
                                    inputUri = selectedImageUris.first(),
                                    params = params
                                )
                                processing = false
                                if (outFile != null && outFile.exists()) {
                                    exportedPhotoPath = outFile.absolutePath
                                    Toast.makeText(context, "تم حفظ وتصدير الصورة بدقة عالية بنجاح ✦", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "حدث خطأ أثناء تصدير الصورة", Toast.LENGTH_LONG).show()
                                    currentStep = 2
                                }
                            }
                        },
                        onExportVideo = {
                            if (selectedImageUris.size < 2) {
                                Toast.makeText(context, "لتصدير فيديو، الرجاء اختيار صورتين على الأقل", Toast.LENGTH_SHORT).show()
                                return@Step2Editor
                            }
                            exportType = "VIDEO"
                            currentStep = 3
                            processing = true
                            progressMessage = "جاري تجهيز الصور..."
                            
                            coroutineScope.launch {
                                val outPath = File(context.cacheDir, "photo_studio_out_${System.currentTimeMillis()}.mp4").absolutePath
                                
                                var audioPathToUse: String? = null
                                if (selectedAudioOption == "مؤثرات طبيعية" || selectedAudioOption == "أنشودة تأملية") {
                                    try {
                                        val assetFile = File(context.cacheDir, "nature_bg.mp3")
                                        if (!assetFile.exists()) {
                                            context.assets.open("audio/activity.mp3").use { input ->
                                                assetFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        }
                                        audioPathToUse = assetFile.absolutePath
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else if (selectedAudioOption == "تلاوة هادئة") {
                                    progressMessage = "جاري تحميل التلاوة..."
                                    try {
                                        val recitationFile = File(context.cacheDir, "recitation_alafasy_255.mp3")
                                        if (!recitationFile.exists()) {
                                            withContext(Dispatchers.IO) {
                                                java.net.URL("https://cdn.islamic.network/quran/audio/128/ar.alafasy/255.mp3")
                                                    .openStream().use { input ->
                                                        recitationFile.outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                            }
                                        }
                                        audioPathToUse = recitationFile.absolutePath
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                progressMessage = "جاري تجميع الفيديو وإضافة المؤثرات..."
                                val success = PhotoStudioEngine.generateVideoFromImages(
                                    context,
                                    selectedImageUris,
                                    textOverlay.takeIf { it.isNotBlank() },
                                    audioPathToUse,
                                    outPath
                                )
                                
                                processing = false
                                if (success) {
                                    generatedVideoPath = outPath
                                } else {
                                    Toast.makeText(context, "حدث خطأ أثناء الإنتاج.", Toast.LENGTH_LONG).show()
                                    currentStep = 2
                                }
                            }
                        }
                    )
                    3 -> Step3Export(
                        processing = processing,
                        progressMessage = progressMessage,
                        videoPath = generatedVideoPath,
                        photoPath = exportedPhotoPath,
                        exportType = exportType,
                        onShare = {
                            val targetPath = if (exportType == "PHOTO") exportedPhotoPath else generatedVideoPath
                            if (targetPath != null) {
                                val file = File(targetPath)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (exportType == "PHOTO") "image/jpeg" else "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الإبداع الدعوي"))
                                }
                            }
                        },
                        onSaveProject = { path ->
                            Toast.makeText(context, "تم حفظ المشروع في مسوداتك بنجاح", Toast.LENGTH_SHORT).show()
                            onSaveProject(path)
                        },
                        onNavigateToAdvancedEdit = { path ->
                            onNavigateToAdvancedEdit(path)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Step1ImageSelection(
    selectedImageUris: List<Uri>,
    onSelectMore: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("الخطوة 1: اختيار الصور", color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("اختر صورة واحدة للتصميم أو أكثر لإنتاج مقطع مرئي", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 13.sp)
        
        Spacer(modifier = Modifier.height(12.dp))
        // Hint
        Row(
            modifier = Modifier.background(Color(0xFF042F2E), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("استوديو عالي الدقة يدعم النصوص العربية والهوية البصرية", color = Color(0xFF10B981), fontFamily = CairoFont, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        if (selectedImageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardSurface)
                    .border(2.dp, GoldPrimary, RoundedCornerShape(24.dp))
                    .clickable { onSelectMore() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload", tint = GoldPrimary, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("انقر لرفع الصور", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("معرض الصور / الكاميرا", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 13.sp)
                }
            }
        } else {
            // Grid/List of selected images
            Column(modifier = Modifier.weight(1f)) {
                Text("الصور المختارة (${selectedImageUris.size}):", color = TextPrimary, fontFamily = CairoFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, GoldPrimary.copy(alpha=0.4f), RoundedCornerShape(16.dp))) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove button
                            IconButton(
                                onClick = { onRemoveImage(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardSurface)
                                .border(1.dp, GoldPrimary.copy(alpha=0.5f), RoundedCornerShape(16.dp))
                                .clickable { onSelectMore() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = "Add More", tint = GoldPrimary, modifier = Modifier.size(32.dp))
                                Text("إضافة", color = GoldPrimary, fontFamily = CairoFont, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Button(
            onClick = onNext,
            enabled = selectedImageUris.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = CardSurface)
        ) {
            Text("متابعة للمحرر", color = if(selectedImageUris.isNotEmpty()) DeepSlate else TextSecondary, fontFamily = CairoFont, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if(selectedImageUris.isNotEmpty()) DeepSlate else TextSecondary)
        }
    }
}

@Composable
fun Step2Editor(
    selectedImageUris: List<Uri>,
    textOverlay: String,
    onTextChange: (String) -> Unit,
    selectedTemplate: PhotoTemplate,
    onTemplateChange: (PhotoTemplate) -> Unit,
    templates: List<PhotoTemplate>,
    selectedAudioOption: String,
    onAudioChange: (String) -> Unit,
    audioOptions: List<String>,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    selectedCropRatio: String,
    onCropRatioChange: (String) -> Unit,
    textStyleTheme: String,
    onTextStyleThemeChange: (String) -> Unit,
    onAutoEnhance: () -> Unit,
    onExportImage: () -> Unit,
    onExportVideo: () -> Unit
) {
    var activeTab by remember { mutableStateOf("القص والضبط") }
    val tabs = listOf("القص والضبط", "النص والتصميم", "القالب", "الصوت")

    val cropRatios = listOf("9:16", "1:1", "16:9", "4:5")

    val previewAspectRatio = when (selectedCropRatio) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        "16:9" -> 16f / 9f
        "4:5" -> 4f / 5f
        else -> 9f / 16f
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Preview Area (Top Half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .background(Color(0xFF030712)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUris.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.95f)
                        .aspectRatio(previewAspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                ) {
                    // Custom Color Matrix for real-time live preview
                    val previewColorMatrix = remember(brightness, contrast, saturation) {
                        val cm = ColorMatrix()
                        // Brightness
                        val bScale = 1f
                        val bOffset = brightness / 255f
                        // Set saturation
                        cm.setToSaturation(saturation)
                        cm
                    }

                    AsyncImage(
                        model = selectedImageUris.first(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(previewColorMatrix),
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Dark Gradient overlay for text readability
                    Box(modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 250f
                        )
                    ))
                    
                    // Frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    )

                    // Overlay Text
                    if (textOverlay.isNotBlank()) {
                        val textColor = when (textStyleTheme) {
                            "GOLD_DEEP" -> GoldPrimary
                            "ROYAL_WHITE" -> Color.White
                            "NEON_EMERALD" -> Color(0xFF10B981)
                            else -> selectedTemplate.color
                        }
                        Text(
                            text = textOverlay,
                            color = textColor,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 20.dp)
                        )
                    }

                    // Watermark
                    Text(
                        "✦ قَبَس | QABAS STUDIO ✦",
                        color = GoldPrimary.copy(alpha = 0.8f),
                        fontFamily = CairoFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                    )
                }
            }
            
            // Auto Enhance FAB Overlay
            FloatingActionButton(
                onClick = onAutoEnhance,
                containerColor = GoldPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "تحسين تلقائي", tint = DeepSlate, modifier = Modifier.size(24.dp))
            }
        }
        
        // Editing Area (Bottom Half)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .background(DeepSlate)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(activeTab),
                containerColor = DeepSlate,
                contentColor = GoldPrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (tabs.indexOf(activeTab) >= 0) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(activeTab)]),
                            color = GoldPrimary
                        )
                    }
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = GoldPrimary,
                        unselectedContentColor = TextSecondary
                    )
                }
            }
            
            // Content
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 10.dp)) {
                when (activeTab) {
                    "القص والضبط" -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("نسبة أبعاد الصورة (القص):", color = TextPrimary, fontFamily = CairoFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cropRatios.forEach { ratio ->
                                    val isSelected = selectedCropRatio == ratio
                                    Button(
                                        onClick = { onCropRatioChange(ratio) },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) GoldPrimary else CardSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            ratio,
                                            color = if (isSelected) DeepSlate else TextPrimary,
                                            fontFamily = NotoSansFont,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E293B))

                            // Brightness Slider
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الإضاءة", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                                Text("${brightness.toInt()}", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                            Slider(
                                value = brightness,
                                onValueChange = onBrightnessChange,
                                valueRange = -50f..50f,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                            )

                            // Contrast Slider
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("التباين", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                                Text(String.format("%.1fx", contrast), color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                            Slider(
                                value = contrast,
                                onValueChange = onContrastChange,
                                valueRange = 0.5f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                            )

                            // Saturation Slider
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("التشبع اللوني", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                                Text(String.format("%.1fx", saturation), color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                            Slider(
                                value = saturation,
                                onValueChange = onSaturationChange,
                                valueRange = 0f..2f,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                            )
                        }
                    }
                    "النص والتصميم" -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = textOverlay,
                                onValueChange = onTextChange,
                                label = { Text("نص التصميم العربي", fontFamily = CairoFont) },
                                placeholder = { Text("اكتب الآية أو الحكمة الدعوية...", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                minLines = 2,
                                maxLines = 4
                            )

                            Text("لون ونمط الخط:", color = TextPrimary, fontFamily = CairoFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf(
                                    Triple("GOLD_DEEP", "ذهبي ملكي", GoldPrimary),
                                    Triple("ROYAL_WHITE", "أبيض ناصع", Color.White),
                                    Triple("NEON_EMERALD", "زمردي إسلامي", Color(0xFF10B981))
                                )
                                themes.forEach { (id, label, color) ->
                                    val isSelected = textStyleTheme == id
                                    OutlinedButton(
                                        onClick = { onTextStyleThemeChange(id) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        border = BorderStroke(1.5.dp, if (isSelected) color else Color(0xFF1E293B)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) color.copy(alpha = 0.15f) else CardSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(label, color = color, fontFamily = CairoFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    "القالب" -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(templates) { tmpl ->
                                val isSelected = tmpl == selectedTemplate
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) tmpl.color.copy(0.2f) else CardSurface)
                                        .border(2.dp, if (isSelected) tmpl.color else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { onTemplateChange(tmpl) }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(tmpl.icon, contentDescription = null, tint = tmpl.color)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(tmpl.title, color = TextPrimary, fontFamily = CairoFont, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                    "الصوت" -> {
                        Column {
                            audioOptions.forEach { audio ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { onAudioChange(audio) }.padding(vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedAudioOption == audio,
                                        onClick = { onAudioChange(audio) },
                                        colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(audio, color = TextPrimary, fontFamily = NotoSansFont, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            // Dual Action Export Buttons (Export Photo vs Export Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Export Image
                Button(
                    onClick = onExportImage,
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير صورة HD", color = DeepSlate, fontFamily = CairoFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Export Video (if multiple images)
                OutlinedButton(
                    onClick = onExportVideo,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير فيديو", color = GoldPrimary, fontFamily = CairoFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Step3Export(
    processing: Boolean,
    progressMessage: String,
    videoPath: String?,
    photoPath: String?,
    exportType: String,
    onShare: () -> Unit,
    onSaveProject: (String) -> Unit,
    onNavigateToAdvancedEdit: (String) -> Unit
) {
    val resultPath = if (exportType == "PHOTO") photoPath else videoPath

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (processing) {
            CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("جاري الإنتاج والمعالجة...", color = TextPrimary, fontFamily = CairoFont, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(progressMessage, color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("يتم تجهيز المخرج وفق أعلى معايير الجودة", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
        } else if (resultPath != null) {
            Icon(Icons.Default.CheckCircle, contentDescription = "نجاح", tint = Color(0xFF10B981), modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                if (exportType == "PHOTO") "تم تصدير وحفظ الصورة بنجاح! 🖼️" else "تم تجهيز الفيديو بنجاح! 🎬",
                color = TextPrimary,
                fontFamily = CairoFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (exportType == "PHOTO" && photoPath != null) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = "Exported Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = DeepSlate)
                Spacer(modifier = Modifier.width(10.dp))
                Text("مشاركة الصورة والتصميم", color = DeepSlate, fontFamily = CairoFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { onSaveProject(resultPath) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = BorderStroke(1.dp, TextSecondary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("حفظ في مشاريعي والعودة", fontFamily = CairoFont, fontSize = 15.sp)
            }

            if (exportType == "VIDEO") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onNavigateToAdvancedEdit(resultPath) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    border = BorderStroke(1.dp, GoldPrimary)
                ) {
                    Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("نقل إلى المونتاج المتقدم", fontFamily = CairoFont, fontSize = 15.sp, color = GoldPrimary)
                }
            }
        }
    }
}
