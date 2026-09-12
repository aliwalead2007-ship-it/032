package com.qabas.app

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.launch

data class CreatorStats(
    val name: String,
    val rank: Int,
    val points: Int,
    val views: String,
    val badge: String,
    val isCurrentUser: Boolean = false,
    val avatarColor: Color = GoldPrimary
)

data class FeedItem(
    val id: String,
    val title: String,
    val creatorName: String,
    val scriptPreview: String,
    val category: String,
    val likesCount: Int,
    val viewsCount: String,
    val ratio: String = "9:16",
    val duration: String = "30s",
    val isLikedByMe: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onUseInspirationScript: (String, String) -> Unit = { _, _ -> },
    onOpenRewards: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val goldGradient = Brush.horizontalGradient(colors = listOf(GoldSecondary, GoldPrimary))
    val silverGradient = Brush.horizontalGradient(colors = listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0)))
    val bronzeGradient = Brush.horizontalGradient(colors = listOf(Color(0xFFB45309), Color(0xFFF5D76E)))
    val purpleGradient = Brush.linearGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))

    var selectedTab by remember { mutableIntStateOf(0) } // 0: لوحة الشرف, 1: مجتمع الإلهام, 2: تحديات الأسبوع
    var timeFilter by remember { mutableStateOf("الأسبوع") } // الأسبوع, الشهر, الكل
    
    var topCreators by remember { mutableStateOf<List<CreatorStats>>(emptyList()) }
    var feedProjects by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var currentMission by remember { mutableStateOf<PointsManager.WeeklyMission?>(null) }
    var isMissionCompleted by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // تحميل البيانات
    val loadData: () -> Unit = {
        coroutineScope.launch {
            isLoading = true
            
            // 1. جلب المتصدرين
            val fetchedCreators = CloudServices.Database.getLeaderboard()
            if (fetchedCreators.isNotEmpty()) {
                topCreators = fetchedCreators
            } else {
                topCreators = listOf(
                    CreatorStats("أحمد الداعية", 1, 15400, "120K", "سفير النور", avatarColor = Color(0xFFE8C547)),
                    CreatorStats("فريق غراس الدعوي", 2, 12200, "95K", "مبدع ذهبي", avatarColor = Color(0xFF38BDF8)),
                    CreatorStats("عمر المحفز", 3, 10800, "88K", "مبدع فضي", avatarColor = Color(0xFFA855F7)),
                    CreatorStats("استوديو اليقين", 4, 8500, "45K", "صاعد", avatarColor = Color(0xFF10B981)),
                    CreatorStats("قناة الهدى المباركة", 5, 7200, "30K", "صاعد", avatarColor = Color(0xFFF97316)),
                    CreatorStats("أنت (صانع الأثر)", 6, 6450, "22K", "صاعد", isCurrentUser = true, avatarColor = GoldPrimary)
                )
            }

            // 2. جلب مشاريع المجتمع / الإلهام
            val cloudProjects = CloudServices.Database.getFeedProjects()
            if (cloudProjects.isNotEmpty()) {
                feedProjects = cloudProjects.mapIndexed { idx, p ->
                    FeedItem(
                        id = p.id.ifEmpty { "proj_$idx" },
                        title = p.title.ifEmpty { "روائع التدبر والدعوة" },
                        creatorName = "صانع أثر",
                        scriptPreview = p.script.ifEmpty { p.idea },
                        category = "قصص الأنبياء",
                        likesCount = 120 + idx * 37,
                        viewsCount = "${(idx + 1) * 4}.${(idx * 3) % 9}K",
                        ratio = "9:16",
                        duration = "45 ثانية"
                    )
                }
            } else {
                feedProjects = listOf(
                    FeedItem(
                        id = "feed_1",
                        title = "سر عجيب في استجابة دعاء يونس عليه السلام",
                        creatorName = "أحمد الداعية",
                        scriptPreview = "لا إله إلا أنت سبحانك إني كنت من الظالمين.. ثلاث كلمات هزت حيتان البحر وسماوات العرش، فما هو سرها العظيم؟",
                        category = "قصص الأنبياء",
                        likesCount = 482,
                        viewsCount = "18.4K",
                        duration = "30 ثانية"
                    ),
                    FeedItem(
                        id = "feed_2",
                        title = "كيف تتخلص من المقارنات القاتلة في السوشيال ميديا؟",
                        creatorName = "فريق غراس",
                        scriptPreview = "كلما فتحت هاتفك شعرت بنقص في حياتك؟ تذكر قول رسول الله صلى الله عليه وسلم: انظروا إلى من هو أسفل منكم ولا تنظروا إلى من هو فوقكم..",
                        category = "تزكية ونفس",
                        likesCount = 345,
                        viewsCount = "12.1K",
                        duration = "45 ثانية"
                    ),
                    FeedItem(
                        id = "feed_3",
                        title = "أثر الصدقة الخفية في شفاء الأمراض وتفريج الكروب",
                        creatorName = "عمر المحفز",
                        scriptPreview = "داووا مرضاكم بالصدقة.. قصة واقعية هزت المشاعر لرجل شفي مرضه بعد أن فرج كربة أسرة فقيرة في جنح الليل.",
                        category = "رقائق وعبر",
                        likesCount = 290,
                        viewsCount = "9.8K",
                        duration = "35 ثانية"
                    ),
                    FeedItem(
                        id = "feed_4",
                        title = "لماذا خلق الله الظلمات قبل النور؟ تدبر قرآني عميق",
                        creatorName = "استوديو اليقين",
                        scriptPreview = "﴿الْحَمْدُ لِلَّهِ الَّذِي خَلَقَ السَّمَاوَاتِ وَالأَرْضَ وَجَعَلَ الظُّلُمَاتِ وَالنُّورَ﴾.. حكمة بديعة تكشف لك سر الابتلاء والأمل في حياتك.",
                        category = "تدبر آية",
                        likesCount = 215,
                        viewsCount = "7.5K",
                        duration = "40 ثانية"
                    )
                )
            }

            // 3. جلب مهمة الأسبوع
            val mission = PointsManager.getCurrentWeeklyMission()
            currentMission = mission
            isMissionCompleted = PointsManager.isWeeklyMissionCompleted(context, mission.id)

            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DeepSlate)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Translator.tr("مجتمع قبس ولوحة الشرف"),
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
                        }
                    },
                    actions = {
                        // زر المكافآت السريع
                        IconButton(onClick = onOpenRewards) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "المكافآت", tint = GoldPrimary)
                        }
                        IconButton(onClick = {
                            isRefreshing = true
                            loadData()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
                )

                // التبويبات الثلاثة المنسقة
                val tabTitles = listOf(
                    "🏆 " + Translator.tr("لوحة الشرف"),
                    "✨ " + Translator.tr("مجتمع الإلهام"),
                    "🎯 " + Translator.tr("مهمة الأسبوع")
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeepSlate,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontFamily = CairoFont,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) GoldPrimary else TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        )
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    QabasLoadingState(
                        message = Translator.tr("جاري تحديث لوحة الشرف ومجتمع المبدعين..."),
                        subMessage = Translator.tr("يتم مزامنة نقاط الأثر والتفاعل مع السحابة")
                    )
                }
            } else {
                Crossfade(targetState = selectedTab, label = "TabTransition") { tabIndex ->
                    when (tabIndex) {
                        0 -> LeaderboardTab(
                            topCreators = topCreators,
                            timeFilter = timeFilter,
                            onTimeFilterChanged = { timeFilter = it },
                            goldGradient = goldGradient,
                            silverGradient = silverGradient,
                            bronzeGradient = bronzeGradient
                        )
                        1 -> InspirationFeedTab(
                            feedProjects = feedProjects,
                            onUseScript = onUseInspirationScript
                        )
                        2 -> WeeklyMissionTab(
                            mission = currentMission,
                            isCompleted = isMissionCompleted,
                            onStartMission = { prompt, topic ->
                                onUseInspirationScript(prompt, topic)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🏆 تبويب لوحة الشرف
 */
@Composable
private fun LeaderboardTab(
    topCreators: List<CreatorStats>,
    timeFilter: String,
    onTimeFilterChanged: (String) -> Unit,
    goldGradient: Brush,
    silverGradient: Brush,
    bronzeGradient: Brush
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        // بطاقة الدوري الأسبوعي والجوائز
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        Translator.tr("دوري قبس الأسبوعي"),
                                        color = TextPrimary,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        Translator.tr("أعلى 7 مبدعين ينالون ترقية Pro مجانية"),
                                        color = GoldSecondary,
                                        fontFamily = NotoSansFont,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    Translator.tr("متبقي 4 أيام"),
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // أزرار فلتر الوقت
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("الأسبوع", "الشهر", "الكل").forEach { filter ->
                                val isSelected = timeFilter == filter
                                Surface(
                                    onClick = { onTimeFilterChanged(filter) },
                                    color = if (isSelected) GoldPrimary else CardSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GoldPrimary else Color(0xFF334155)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        filter,
                                        color = if (isSelected) DeepSlate else TextSecondary,
                                        fontFamily = CairoFont,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // منصة التتويج الثلاثية (Top 3 Podium) إذا توفرت
        if (topCreators.size >= 3) {
            item {
                PodiumSection(
                    first = topCreators[0],
                    second = topCreators[1],
                    third = topCreators[2]
                )
            }
        }

        item {
            Text(
                Translator.tr("قائمة المتصدرين وسفراء النور"),
                color = TextPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // قائمة المتصدرين
        itemsIndexed(topCreators) { index, creator ->
            val rankGradient = when (creator.rank) {
                1 -> goldGradient
                2 -> silverGradient
                3 -> bronzeGradient
                else -> null
            }
            val badgeColor = when (creator.rank) {
                1 -> GoldPrimary
                2 -> Color(0xFFE2E8F0)
                3 -> Color(0xFFF5D76E)
                else -> TextSecondary
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (creator.isCurrentUser) Color(0xFF1E293B) else CardSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (creator.isCurrentUser) GoldPrimary else if (creator.rank <= 3) GoldPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (rankGradient != null) Modifier.background(rankGradient, alpha = 0.08f) else Modifier
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الترتيب
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (rankGradient != null) rankGradient else Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF1E293B))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${creator.rank}",
                            color = if (creator.rank <= 3) DeepSlate else Color.White,
                            fontFamily = RobotoMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // الصورة الرمزية والاسم
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(creator.avatarColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, creator.avatarColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            creator.name.take(1),
                            color = creator.avatarColor,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // الاسم والوسام
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                creator.name,
                                color = if (creator.isCurrentUser) GoldPrimary else Color.White,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (creator.isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        Translator.tr("أنت"),
                                        color = GoldPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                creator.badge,
                                color = badgeColor,
                                fontFamily = NotoSansFont,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // الإحصائيات (النقاط والمشاهدات)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${creator.points} ${Translator.tr("نقطة")}",
                            color = GoldPrimary,
                            fontFamily = RobotoMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                creator.views,
                                color = TextSecondary,
                                fontFamily = RobotoMonoFont,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🥇 منصة التتويج البصرية للمراكز الثلاثة الأولى
 */
@Composable
private fun PodiumSection(
    first: CreatorStats,
    second: CreatorStats,
    third: CreatorStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                Translator.tr("🌟 فرسان الصدارة لهذا الأسبوع"),
                color = GoldPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // المركز الثاني (فضي)
                PodiumColumn(
                    creator = second,
                    rankNumber = 2,
                    height = 95.dp,
                    color = Color(0xFFCBD5E1),
                    gradient = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155)))
                )

                // المركز الأول (ذهبي - الأطول)
                PodiumColumn(
                    creator = first,
                    rankNumber = 1,
                    height = 125.dp,
                    color = GoldPrimary,
                    gradient = Brush.verticalGradient(listOf(GoldPrimary, Color(0xFFB45309)))
                )

                // المركز الثالث (برونزي)
                PodiumColumn(
                    creator = third,
                    rankNumber = 3,
                    height = 75.dp,
                    color = Color(0xFFF59E0B),
                    gradient = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F)))
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    creator: CreatorStats,
    rankNumber: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    gradient: Brush
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        // تاج للمركز الأول
        if (rankNumber == 1) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // الصورة الرمزية
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                creator.name.take(1),
                color = color,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            creator.name.split(" ").firstOrNull() ?: creator.name,
            color = Color.White,
            fontFamily = CairoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${creator.points}ن",
            color = GoldSecondary,
            fontFamily = RobotoMonoFont,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // عامود المنصة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(gradient, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rankNumber",
                color = Color.White,
                fontFamily = RobotoMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * ✨ تبويب مجتمع الإلهام (Inspiration Community Feed)
 */
@Composable
private fun InspirationFeedTab(
    feedProjects: List<FeedItem>,
    onUseScript: (String, String) -> Unit
) {
    val context = LocalContext.current
    var likedSet by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
    ) {
        item {
            // شريط إرشادي ملهم
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Translator.tr("استلهم وأنتج مقطعك فوراً 💡"),
                            color = TextPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            Translator.tr("تصفح سيناريوهات المبدعين الحاصلة على أعلى مشاهدات، واقتبس الفكرة بضغطة زر."),
                            color = TextSecondary,
                            fontFamily = NotoSansFont,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // بطاقات الفيديوهات الملهمة
        items(feedProjects) { item ->
            val isLiked = item.id in likedSet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // العنوان والناشر
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    item.creatorName.take(1),
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    item.creatorName,
                                    color = TextPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    item.category,
                                    color = GoldSecondary,
                                    fontFamily = NotoSansFont,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                item.duration,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = RobotoMonoFont,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        item.title,
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // معاينة السكريبت
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            item.scriptPreview,
                            color = Color(0xFFCBD5E1),
                            fontFamily = NotoSansFont,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // الإحصائيات وأزرار الإجراءات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // زر الإعجاب التفاعلي
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    likedSet = if (isLiked) likedSet - item.id else likedSet + item.id
                                }
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isLiked) Color(0xFFEF4444) else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${item.likesCount + if (isLiked) 1 else 0}",
                                    color = if (isLiked) Color(0xFFEF4444) else TextSecondary,
                                    fontFamily = RobotoMonoFont,
                                    fontSize = 12.sp
                                )
                            }

                            // المشاهدات
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    item.viewsCount,
                                    color = TextSecondary,
                                    fontFamily = RobotoMonoFont,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // زر الإنتاج بهذا السيناريو مباشرة
                        Button(
                            onClick = {
                                onUseScript(item.scriptPreview, item.title)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                Translator.tr("استخدام الفكرة 🎬"),
                                color = DeepSlate,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🎯 تبويب مهمة الأسبوع وتحديات النقاط
 */
@Composable
private fun WeeklyMissionTab(
    mission: PointsManager.WeeklyMission?,
    isCompleted: Boolean,
    onStartMission: (String, String) -> Unit
) {
    if (mission == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Translator.tr("لا يوجد تحدي نشط حالياً"), color = TextSecondary, fontFamily = CairoFont)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF064E3B), Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "مهمة الأسبوع ${mission.weekNumber}",
                                        color = Color(0xFF10B981),
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Surface(
                                color = GoldPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "+${mission.points} " + Translator.tr("نقطة أثر"),
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            mission.topic,
                            color = TextPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            mission.description,
                            color = Color(0xFFE2E8F0),
                            fontFamily = NotoSansFont,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // نص البرومبت المقترح
                        Text(
                            Translator.tr("الموجه الذكي (AI Prompt) المخصص للتحدي:"),
                            color = GoldSecondary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                mission.prompt,
                                color = Color(0xFF94A3B8),
                                fontFamily = NotoSansFont,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isCompleted) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Translator.tr("تم إنجاز هذا التحدي وحصد النقاط بنجاح! 🎉"),
                                        color = Color(0xFF10B981),
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    onStartMission(mission.prompt, mission.topic)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Translator.tr("خوض التحدي وإنتاج الفيديو الآن 🚀"),
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

        // قائمة مهام النقاط اليومية
        item {
            Text(
                Translator.tr("طرق أخرى لكسب نقاط الأثر اليومية"),
                color = TextPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(PointAction.values().toList()) { action ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (action) {
                                    PointAction.EXPORT_VIDEO -> Icons.Default.Movie
                                    PointAction.USE_AI_HOOK -> Icons.Default.Lightbulb
                                    PointAction.SEARCH_DALEEL -> Icons.Default.Search
                                    PointAction.SHARE_APP -> Icons.Default.Share
                                    PointAction.DAILY_LOGIN -> Icons.Default.Today
                                    PointAction.WEEKLY_CHALLENGE -> Icons.Default.EmojiEvents
                                },
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            action.description,
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "+${action.points}",
                            color = GoldPrimary,
                            fontFamily = RobotoMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
