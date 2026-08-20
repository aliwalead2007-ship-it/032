package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

data class PortfolioSampleItem(
    val id: String,
    val title: String,
    val category: String,
    val duration: String,
    val styleBadge: String,
    val thumbnailUrl: String,
    val videoPreviewUrl: String,
    val description: String,
    val clientType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevPortfolioShowcaseScreen(
    context: Context,
    onBack: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("الجميع") }
    var selectedSample by remember { mutableStateOf<PortfolioSampleItem?>(null) }
    var showPitchDialog by remember { mutableStateOf(false) }

    val sampleList = remember {
        listOf(
            PortfolioSampleItem(
                id = "sample_1",
                title = "قصص الأنبياء - بأسلوب وثائقي سينمائي",
                category = "قصص وتاريخ",
                duration = "00:45",
                styleBadge = "4K Cinematic Teal & Gold",
                thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&q=80",
                videoPreviewUrl = "https://assets.mixkit.co/videos/preview/mixkit-mosque-illuminated-at-night-42284-large.mp4",
                description = "مقطع ريلز وثائقي قصير يروي سيرة إيمانية بأسلوب مشوّق مع خطوط ذهبية متحركة وتعليق صوتي جهوري.",
                clientType = "قنوات دعوية ومؤثرين"
            ),
            PortfolioSampleItem(
                id = "sample_2",
                title = "تدبر آية - سكينة وإشراق زمردي",
                category = "تدبر وتلاوات",
                duration = "00:30",
                styleBadge = "Emerald Quran & Soft Fade",
                thumbnailUrl = "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=600&q=80",
                videoPreviewUrl = "https://assets.mixkit.co/videos/preview/mixkit-hands-holding-an-open-quran-42286-large.mp4",
                description = "عرض آية قرآنية مع تشكيل كامل وتأثير ضوئي ناعم ومؤثرات صوتية هادئة بدون ألحان.",
                clientType = "مراكز تحفيظ وجمعيات"
            ),
            PortfolioSampleItem(
                id = "sample_3",
                title = "شبهات وردود - قالب قفز خاطف (Viral)",
                category = "ردود وتوعية",
                duration = "00:50",
                styleBadge = "High Contrast Subtitles",
                thumbnailUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&q=80",
                videoPreviewUrl = "https://assets.mixkit.co/videos/preview/mixkit-starry-sky-time-lapse-4008-large.mp4",
                description = "مقطع تفاعلي سريع يعالج شبهة مع إبراز الكلمات المفتاحية باللون الأصفر والرمادي الداكن.",
                clientType = "منصات ومواقع إخبارية"
            ),
            PortfolioSampleItem(
                id = "sample_4",
                title = "روائع العمارة الإسلامية - الأندلس",
                category = "حضارة وتراث",
                duration = "01:00",
                styleBadge = "Parchment & Sepia Paper Wipe",
                thumbnailUrl = "https://images.unsplash.com/photo-1548013146-72479768bada?w=600&q=80",
                videoPreviewUrl = "https://assets.mixkit.co/videos/preview/mixkit-intricate-details-of-a-mosque-interior-42287-large.mp4",
                description = "جولة وثائقية تراثية بين المعالم الأندلسية مع إطار ورقي تعتيقي وتأثيرات انتقالية ناعمة.",
                clientType = "مؤسسات ثقافية وسياحة إسلامية"
            )
        )
    }

    val categories = listOf("الجميع", "قصص وتاريخ", "تدبر وتلاوات", "ردود وتوعية", "حضارة وتراث")

    val filteredList = remember(selectedCategory) {
        if (selectedCategory == "الجميع") sampleList else sampleList.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("بروفايل ونماذج أعمال المطور ✦", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("عينة أعمال احترافية لإقناع العملاء والجمعيات بضغطات زر", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Pitch Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(GoldPrimary.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = GoldPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("معرض أعمال المطور والوكالة", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("جاهز للمشاركة المباشرة مع المؤسسات والعملاء المرتقبين", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "يستعرض هذا الملف قوة مخرجات الذكاء الاصطناعي والمونتاج الآلي عبر قبس. أرسل رابط هذا المعرض أو العرض التقديمي للتوقيع مع الجمعيات والقنوات فوراً.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = CairoFont,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showPitchDialog = true },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة العرض للعملاء ✦", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareText = """
                                        ✦ معرض أعمال الوكالة الإعلامية لإنتاج المحتوى الإسلامي ✦
                                        نُقدم لجهتكم الموقرة خدمات إنتاج المقاطع القصيرة (Reels / Shorts) بأحدث تقنيات الذكاء الاصطناعي والمونتاج السينمائي:
                                        
                                        1. كتابة وتوثيق النصوص الشرعية.
                                        2. تعليق صوتي سينمائي بدقة عالية.
                                        3. مونتاج تلقائي مع B-Roll إسلامي فاخر.
                                        4. قدرة إنتاج: 30 مقطع شهرياً بشعار وهوية جهتك.
                                        
                                        للاطلاع على العينات وللتواصل المباشر معنا عبر تطبيق قبس المعتمد.
                                    """.trimIndent()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "إرسال ملف نماذج الأعمال"))
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                border = BorderStroke(1.dp, GoldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير سريح", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Categories LazyRow
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontFamily = NotoSansFont, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = DeepSlate,
                                containerColor = CardSurface,
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF1E293B),
                                selectedBorderColor = GoldPrimary
                            )
                        )
                    }
                }
            }

            // Samples List
            items(filteredList) { sample ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSample = sample }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(sample.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = sample.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )

                            // Top Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = GoldPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        sample.styleBadge,
                                        color = DeepSlate,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(sample.duration, color = Color.White, fontSize = 12.sp, fontFamily = NotoSansFont)
                                    }
                                }
                            }

                            // Center Play Icon Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                                    .background(GoldPrimary.copy(alpha = 0.85f), CircleShape)
                                    .clickable { selectedSample = sample },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(28.dp))
                            }
                        }

                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(sample.title, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sample.description, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, lineHeight = 18.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الجهة المستهدفة: ${sample.clientType}", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                TextButton(
                                    onClick = { selectedSample = sample },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("معاينة النموذج ✦", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Preview Dialog
    selectedSample?.let { sample ->
        AlertDialog(
            onDismissRequest = { selectedSample = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sample.title, color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = sample.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("معاينة المونتاج السينمائي الحي", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("الوصف الإخراجي:", color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(sample.description, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, lineHeight = 18.sp)

                    Surface(
                        color = DeepSlate,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("النمط البصري: ${sample.styleBadge}", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            Text("المدة: ${sample.duration}", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "شاهد عينة محتوى إسلامي احترافي بأسلوب (${sample.title}):\n${sample.videoPreviewUrl}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "إرسال العينة"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("إرسال العينة للعميل ✦", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSample = null }) {
                    Text("إغلاق", color = TextSecondary, fontFamily = CairoFont)
                }
            },
            containerColor = CardSurface
        )
    }

    // Pitch Dialog
    if (showPitchDialog) {
        val defaultPitch = """
            السلام عليكم ورحمة الله وبركاته،
            إلى القائمين على ${"مؤسستكم الموقرة ✦"}
            
            يسرنا تقديم عرض خدمات إنتاج المحتوى الرقمي والإعلامي الدعوي باستخدام تقنيات الذكاء الاصطناعي المتقدمة والمونتاج الآلي عبر تطبيق "قبس".
            
            خدماتنا تشمل:
            1. إنتاج 30 فيديو Reels / Shorts شهرياً بأسلوب وثائقي وسينمائي.
            2. كتابة وتوثيق النصوص والأدلة الشرعية بذكاء اصطناعي متخصص.
            3. تعليق صوتي سينمائي بلغة عربية فصحى ومؤثرات صوتية هادئة.
            4. طباعة شعار وهوية جهتك الموقرة على جميع المقاطع.
            
            يمكنكم معاينة نماذج أعمالنا عبر هذا الرابط، ويسعدنا التواصل لتحديد الخطة المناسبة.
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showPitchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("رسالة الخطاب الترويجي للجمعيات 📄", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("صيغة خطاب رسمي جاهز للإرسال عبر واتساب أو البريد:", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    Surface(
                        color = DeepSlate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Text(
                            defaultPitch,
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontSize = 11.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Pitch", defaultPitch)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ نص الخطاب للعملاء بنجاح 📋", Toast.LENGTH_SHORT).show()
                        showPitchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("نسخ الخطاب 📋", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPitchDialog = false }) {
                    Text("إلغاء", color = TextSecondary, fontFamily = CairoFont)
                }
            },
            containerColor = CardSurface
        )
    }
}
