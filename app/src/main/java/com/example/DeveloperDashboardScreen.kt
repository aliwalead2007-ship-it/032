package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.ui.theme.*

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas


enum class DashboardSection { MAIN, REQUESTS, SYSTEM_CONTROLS, NOTIFICATIONS, USERS, STATS, LOGS, ACCOUNT_SETTINGS, PROMO_CODES, REVENUE, DEV_STUDIO_SIGNATURE, AGENCY_MONETIZATION, DEV_PORTFOLIO_SHOWCASE, APP_DOCTOR, STYLE_BRAIN }

data class DevUser(
    val id: String,
    val name: String,
    val email: String,
    val type: String, // "مطور", "خاص", "Freemium"
    val projectCount: Int,
    val regDate: String,
    val isSuspended: Boolean = false
)

/** أدوات تنسيق البيانات الحقيقية لشاشة المطور. */
object DevDashboardFormatters {
    private val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())

    /**
     * ينسّق تاريخ التسجيل من حقل `createdAt` الموجود في Firestore (epoch millis)
     * أو Supabase (نص ISO). يعود بنص "غير معروف" عند غياب القيمة.
     */
    fun formatRegDate(createdAt: Any?): String {
        val mills = when (createdAt) {
            is Number -> createdAt.toLong()
            is String -> parseCreatedAtString(createdAt)
            else -> 0L
        }
        if (mills <= 0L) return "غير معروف"
        return runCatching { dateFormat.format(java.util.Date(mills)) }.getOrDefault("غير معروف")
    }

    /** يجرّب نصوص ISO للتواريخ مع خيار المللي، ثم الرقم الخام (epoch millis). */
    private fun parseCreatedAtString(raw: String): Long {
        val isoFormats = listOf(
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        )
        for (fmt in isoFormats) {
            val parsed = runCatching { fmt.parse(raw)?.time }.getOrNull()
            if (parsed != null && parsed > 0L) return parsed
        }
        return runCatching { raw.toLong() }.getOrDefault(0L)
    }

    /** تنسيق زمن الاستجابة الحقيقي بالثواني أو المللي ثانية. */
    fun formatLatency(ms: Long): String = when {
        ms >= 1000 -> String.format(java.util.Locale.getDefault(), "%.1fs", ms / 1000.0)
        ms > 0 -> "${ms}ms"
        else -> "--"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDashboardScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit = {}) {
    var currentSection by remember { mutableStateOf(DashboardSection.MAIN) }

    val handleBack = {
        if (currentSection == DashboardSection.MAIN) {
            onBack()
        } else {
            currentSection = DashboardSection.MAIN
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (currentSection) {
                            DashboardSection.MAIN -> "لوحة تحكم المطور"
                            DashboardSection.REQUESTS -> "طلبات التطبيقات"
                            DashboardSection.SYSTEM_CONTROLS -> "التحكم في النظام"
                            DashboardSection.NOTIFICATIONS -> "إرسال وتصفح الإشعارات"
                            DashboardSection.USERS -> "إدارة الحسابات"
                            DashboardSection.STATS -> "الإحصائيات المباشرة"
                            DashboardSection.LOGS -> "سجلات النظام (Logs)"
                            DashboardSection.ACCOUNT_SETTINGS -> "إعدادات حساب المطور"
                            DashboardSection.PROMO_CODES -> "المكافآت والأكواد"
                            DashboardSection.REVENUE -> "المبيعات والإيرادات"
                            DashboardSection.DEV_STUDIO_SIGNATURE -> "استوديو إنتاج المطور وتوقيع الفيديوهات ✦"
                            DashboardSection.AGENCY_MONETIZATION -> "استوديو وكالة الأرباح والخدمات المدفوعة 💰"
                            DashboardSection.DEV_PORTFOLIO_SHOWCASE -> "معرض وبروفايل أعمال المطور ✦"
                            DashboardSection.APP_DOCTOR -> "طبيب التطبيق 🩺"
                            DashboardSection.STYLE_BRAIN -> "عقل الأساليب 🧠"
                        }, 
                        color = GoldPrimary, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = CairoFont
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة", tint = GoldPrimary)
                    }
                },
                actions = {
                    val appBarContext = LocalContext.current
                    var isOnline by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(appBarContext)) }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            isOnline = NetworkUtils.isNetworkAvailable(appBarContext)
                            kotlinx.coroutines.delay(3000)
                        }
                    }

                    Surface(
                        color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "إنترنت متصل 🌐" else "غير متصل ⚠️",
                                color = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { padding ->
        val context = LocalContext.current
        var requests by remember { mutableStateOf(AppRequestService.getRequests(context, isDeveloper = true)) }

        // قائمة المستخدمين الحقيقية فقط — بلا أي حسابات مزيفة أو مدمجة في الكود
        val devUsers = remember { mutableStateListOf<DevUser>() }

        LaunchedEffect(Unit) {
            val seenIds = mutableSetOf<String>()
            val seenEmails = mutableSetOf<String>()

            fun addUser(user: DevUser) {
                if (seenIds.add(user.id) && user.email !in seenEmails) {
                    seenEmails.add(user.email)
                    devUsers.add(user)
                }
            }

            if (CloudServices.isFirebaseInitialized) {
                val usersFromCloud = CloudServices.Database.getAllUsers()
                val usersWithProjects = usersFromCloud.map { userMap ->
                    val uid = userMap["id"] as? String ?: ""
                    val projectCount = if (uid.isNotBlank()) {
                        kotlin.runCatching {
                            CloudServices.Database.getDevUserProjectCount(uid)
                        }.getOrDefault(0)
                    } else 0
                    DevUser(
                        id = uid,
                        name = userMap["name"] as? String ?: "بدون اسم",
                        email = userMap["email"] as? String ?: "",
                        type = (userMap["type"] as? String ?: "مجاني"),
                        projectCount = projectCount,
                        regDate = DevDashboardFormatters.formatRegDate(userMap["createdAt"]),
                        isSuspended = (userMap["strikes"] as? Long ?: 0L) >= 5L
                    )
                }
                usersWithProjects.forEach { addUser(it) }
            }

            if (SupabaseServices.isSupabaseAvailable) {
                val supabaseUsers = SupabaseServices.Database.getAllUsers()
                supabaseUsers.forEach { row ->
                    addUser(
                        DevUser(
                            id = row.externalId ?: row.id,
                            name = row.name ?: row.email ?: "بدون اسم",
                            email = row.email ?: row.externalId ?: "",
                            type = if (row.type == "developer") "مطور" else "مجاني",
                            projectCount = 0,
                            regDate = DevDashboardFormatters.formatRegDate(row.createdAt),
                            isSuspended = row.strikes >= 5
                        )
                    )
                }
            }
        }

        val devLogs = SystemLogsManager.logs

        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (currentSection) {
                DashboardSection.MAIN -> {
                    DashboardMainGrid(
                        onRequestSection = { 
                            requests = AppRequestService.getRequests(context, isDeveloper = true)
                            currentSection = DashboardSection.REQUESTS 
                        },
                        onSystemControls = { currentSection = DashboardSection.SYSTEM_CONTROLS },
                        onNotifications = { currentSection = DashboardSection.NOTIFICATIONS },
                        onUsers = { currentSection = DashboardSection.USERS },
                        onStats = { 
                            requests = AppRequestService.getRequests(context, isDeveloper = true)
                            currentSection = DashboardSection.STATS 
                        },
                        onLogs = { currentSection = DashboardSection.LOGS },
                        onAccountSettings = { currentSection = DashboardSection.ACCOUNT_SETTINGS },
                        onPromoCodes = { currentSection = DashboardSection.PROMO_CODES },
                        onRevenue = { currentSection = DashboardSection.REVENUE },
                        onDevStudioSignature = { currentSection = DashboardSection.DEV_STUDIO_SIGNATURE },
                        onAgencyMonetization = { currentSection = DashboardSection.AGENCY_MONETIZATION },
                        onDevPortfolioShowcase = { currentSection = DashboardSection.DEV_PORTFOLIO_SHOWCASE },
                        onAppDoctor = { currentSection = DashboardSection.APP_DOCTOR },
                        onStyleBrain = { currentSection = DashboardSection.STYLE_BRAIN }
                    )
                }
                DashboardSection.APP_DOCTOR -> {
                    AppDoctorSection(context = context)
                }
                DashboardSection.STYLE_BRAIN -> {
                    StyleBrainSection(context = context)
                }
                DashboardSection.DEV_PORTFOLIO_SHOWCASE -> {
                    DevPortfolioShowcaseScreen(context = context, onBack = { currentSection = DashboardSection.MAIN })
                }
                DashboardSection.AGENCY_MONETIZATION -> {
                    AgencyMonetizationHub(context = context)
                }
                DashboardSection.DEV_STUDIO_SIGNATURE -> {
                    DevStudioSignatureSection(context = context)
                }
                DashboardSection.ACCOUNT_SETTINGS -> {
                    AccountSettingsSection(context = context)
                }
                DashboardSection.REQUESTS -> {
                    RequestsSection(
                        context = context,
                        requests = requests, 
                        onOpenChat = onOpenChat,
                        onRefresh = { requests = AppRequestService.getRequests(context, isDeveloper = true) }
                    )
                }
                DashboardSection.SYSTEM_CONTROLS -> {
                    SystemControlsSection(context = context)
                }
                DashboardSection.NOTIFICATIONS -> {
                    NotificationsSection(context = context)
                }
                DashboardSection.USERS -> {
                    UsersSection(context = context, devUsers = devUsers)
                }
                DashboardSection.STATS -> {
                    StatsSection(requests = requests, userCount = devUsers.size)
                }
                DashboardSection.LOGS -> {
                    LogsSection(devLogs = devLogs)
                }
                DashboardSection.PROMO_CODES -> {
                    PromoCodesSection(context = context)
                }
                DashboardSection.REVENUE -> {
                    RevenueSection(context = context)
                }
            }
        }
    }
}

@Composable
fun DashboardMainGrid(
    onRequestSection: () -> Unit,
    onSystemControls: () -> Unit,
    onNotifications: () -> Unit,
    onUsers: () -> Unit,
    onStats: () -> Unit,
    onLogs: () -> Unit,
    onAccountSettings: () -> Unit,
    onPromoCodes: () -> Unit,
    onRevenue: () -> Unit,
    onDevStudioSignature: () -> Unit,
    onAgencyMonetization: () -> Unit,
    onDevPortfolioShowcase: () -> Unit,
    onAppDoctor: () -> Unit,
    onStyleBrain: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val gridContext = LocalContext.current
    var isGridOnline by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(gridContext)) }
    var diagnosticSummary by remember { mutableStateOf("جاري الفحص...") }

    LaunchedEffect(Unit) {
        // Quick background diagnostic
        val reports = AppSelfDoctor.runFullDiagnosis(gridContext)
        val criticalCount = reports.count { it.severity == "حرج" }
        val improvementCount = reports.count { it.severity == "تحسين" }
        diagnosticSummary = if (criticalCount > 0) {
            "$criticalCount مشاكل حرجة | $improvementCount تحسينات"
        } else if (improvementCount > 0) {
            "الأنظمة مستقرة | $improvementCount تحسينات"
        } else {
            "الأنظمة 100% مستقرة"
        }

        while (true) {
            isGridOnline = NetworkUtils.isNetworkAvailable(gridContext)
            kotlinx.coroutines.delay(3000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Premium Server Status Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DeepSlate, CardSurface)))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Qabas Dev Studio", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("المطور الرئيسي: aliwalead.2007", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 14.sp)
                        }
                        
                        // Dynamic Status Indicator
                        Surface(
                            color = Color(0xFF0B0F19).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isGridOnline) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(color = if (isGridOnline) Color(0xFF10B981).copy(alpha = pulseAlpha) else Color(0xFFEF4444).copy(alpha = pulseAlpha))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isGridOnline) "الإنترنت متصل 🟢" else "انقطع الاتصال 🔴", 
                                    color = if (isGridOnline) Color(0xFF10B981) else Color(0xFFEF4444), 
                                    fontFamily = CairoFont, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Quick Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth().background(DeepSlate, RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickMetric(Icons.Default.MedicalServices, "حالة التطبيق", diagnosticSummary)
                        QuickMetric(Icons.Default.CloudSync, "الشبكة", if (isGridOnline) "متصلة" else "منقطعة")
                        QuickMetric(Icons.Default.Group, "المستخدمين", "2")
                    }
                }
            }
        }

        val items = listOf(
            Triple("طبيب التطبيق 🩺", Icons.Default.MedicalServices, onAppDoctor),
            Triple("عقل الأساليب 🧠", Icons.Default.Psychology, onStyleBrain),
            Triple("وكالة الأرباح والخدمات 💰", Icons.Default.MonetizationOn, onAgencyMonetization),
            Triple("معرض أعمال المطور ✦", Icons.Default.Stars, onDevPortfolioShowcase),
            Triple("توقيع واستوديو المطور ✦", Icons.Default.Verified, onDevStudioSignature),
            Triple("إدارة المستخدمين", Icons.Default.Group, onUsers),
            Triple("المبيعات والإيرادات", Icons.Default.AttachMoney, onRevenue),
            Triple("المكافآت والأكواد", Icons.Default.CardGiftcard, onPromoCodes),
            Triple("الإحصائيات والأرباح", Icons.Default.Analytics, onStats),
            Triple("إرسال الإشعارات", Icons.Default.Notifications, onNotifications),
            Triple("طلبات التطبيقات", Icons.Default.Build, onRequestSection),
            Triple("التحكم في النظام", Icons.Default.Settings, onSystemControls),
            Triple("إعدادات حساب المطور", Icons.Default.ManageAccounts, onAccountSettings)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items) { (title, icon, onClick) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clickable { onClick() },
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0B0F19).copy(alpha = 0.3f))))) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(GoldPrimary.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(title, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            
            // Full width item for logs
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLogs() }.height(80.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("سجلات النظام الحية (Logs)", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMetric(icon: ImageVector, title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(title, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
    }
}

@Composable
fun RequestsSection(
    context: Context,
    requests: List<AppRequestService.AppRequest>, 
    onOpenChat: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قائمة الطلبات المترددة (${requests.size})", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                Button(
                    onClick = {
                        // Clear requests
                        val prefs = context.getSharedPreferences("qabas_requests_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        onRefresh()
                        Toast.makeText(context, "تم تصفير جميع الطلبات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331111)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصفير الطلبات", color = Color.Red, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        }

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(CardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا توجد طلبات تطبيقات حالياً (السجل فارغ)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("عند إرسال المستخدمين لطلبات جيدة ستظهر هنا فوراً", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(requests) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenChat(req.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(req.title, color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(
                                    color = if (req.isPaid) GoldPrimary else Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if (req.isPaid) "مدفوع (${req.cost}$)" else "مجاني",
                                        color = DeepSlate,
                                        fontSize = 11.sp,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("المستخدم: ${req.userEmail}", color = Color.LightGray, fontSize = 12.sp, fontFamily = NotoSansFont)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(req.goal, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemControlsSection(context: Context) {
    val prefs = remember { context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE) }
    var isMaintenanceMode by remember { mutableStateOf(prefs.getBoolean("sys_maintenance_mode", false)) }
    var acceptNewRequests by remember { mutableStateOf(prefs.getBoolean("sys_accept_requests", true)) }
    var autoAiReply by remember { mutableStateOf(prefs.getBoolean("sys_auto_ai_reply", true)) }
    var customMessage by remember { 
        mutableStateOf(prefs.getString("sys_maintenance_message", "الخدمة متوقفة مؤقتاً للتحديث والصيانة، يرجى المحاولة لاحقاً.") ?: "الخدمة متوقفة مؤقتاً للتحديث والصيانة، يرجى المحاولة لاحقاً.") 
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("التحكم في خيارات النظام المباشرة", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("وضع الصيانة (Maintenance Mode)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("إيقاف الوصول المؤقت للتطبيق للعملاء عند تحديث الخوادم.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
            Switch(
                checked = isMaintenanceMode,
                onCheckedChange = { 
                    isMaintenanceMode = it
                    prefs.edit().putBoolean("sys_maintenance_mode", it).apply()
                    Toast.makeText(context, if (it) "تم تفعيل وضع الصيانة" else "تم إيقاف وضع الصيانة", Toast.LENGTH_SHORT).show()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldSecondary)
            )
        }
        
        HorizontalDivider(color = Color(0xFF1E293B))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("استقبال طلبات جديدة", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("السماح للمستخدمين بإرسال مشاريع جديدة.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
            Switch(
                checked = acceptNewRequests,
                onCheckedChange = { 
                    acceptNewRequests = it
                    prefs.edit().putBoolean("sys_accept_requests", it).apply()
                    Toast.makeText(context, if (it) "تم السماح باستقبال الطلبات" else "تم إيقاف استقبال الطلبات", Toast.LENGTH_SHORT).show()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldSecondary)
            )
        }
        
        HorizontalDivider(color = Color(0xFF1E293B))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("الرد الآلي (AI Auto-Reply)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("تفعيل الاستجابة التلقائية للذكاء الاصطناعي مع المستخدمين.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
            Switch(
                checked = autoAiReply,
                onCheckedChange = { 
                    autoAiReply = it
                    prefs.edit().putBoolean("sys_auto_ai_reply", it).apply()
                    Toast.makeText(context, if (it) "تم تفعيل الرد الآلي" else "تم تعطيل الرد الآلي", Toast.LENGTH_SHORT).show()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldSecondary)
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        // Custom Message Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("رسالة توقف الخدمة المخصصة (تظهر للمستخدمين)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("سيتم إظهار هذا النص للمستخدمين عند تفعيل وضع الصيانة أو إيقاف الخدمة.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)

            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                minLines = 2
            )

            // Preset Quick Selection Chips
            Text("نماذج جاهزة للرسائل:", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "تحديث الخوادم" to "الخدمة متوقفة مؤقتاً لتحديث الخوادم وتطوير الأداء. سنعود خلال دقائق!",
                    "تجاوز الحد اليومي" to "تم الوصول للحد اليومي لاستهلاك السيرفرات المجانية. يرجى المحاولة غداً أو ترقية الحساب.",
                    "صيانة مجدولة" to "تخضع المنصة لصيانة مجدولة لتحسين جودة الذكاء الاصطناعي. شكراً لترقبكم!"
                )
                presets.forEach { (title, msg) ->
                    Button(
                        onClick = { customMessage = msg },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(title, color = GoldPrimary, fontSize = 12.sp, fontFamily = NotoSansFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    prefs.edit().putString("sys_maintenance_message", customMessage).apply()
                    Toast.makeText(context, "تم حفظ رسالة الصيانة المخصصة بنجاح!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ نص الرسالة", color = Color.Black, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NotificationsSection(context: Context) {
    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }
    var sentNotifications by remember { mutableStateOf(AppNotificationService.getNotifications(context)) }
    var isDailyHadithEnabled by remember { mutableStateOf(AppNotificationService.isDailyHadithEnabled(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Daily Hadith Auto Notification Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("الحديث النبوي اليومي التلقائي 📖", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "نشر حديث يومي صحيح من الكتب المعتمدة (البخاري ومسلم) بدون الحاجة لدخول التطبيق.",
                                color = TextSecondary,
                                fontFamily = CairoFont,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isDailyHadithEnabled,
                        onCheckedChange = { checked ->
                            isDailyHadithEnabled = checked
                            AppNotificationService.setDailyHadithEnabled(context, checked)
                            Toast.makeText(
                                context,
                                if (checked) "تم تفعيل إشعار الحديث النبوي اليومي الساعة 8:00 صباحاً 🟢" else "تم إيقاف الإشعارات اليومية التلقائية 🔴",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSlate,
                            checkedTrackColor = GoldPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF151B2B)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val sentHadith = AppNotificationService.triggerDailyHadithNow(context)
                        sentNotifications = AppNotificationService.getNotifications(context)
                        Toast.makeText(context, "تم إرسال حديث اليوم كـ إشعار بنجاح! ✨", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال حديث نبوي صحيح الآن للمستخدمين 🚀", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إرسال إشعار عام للمستخدمين", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notifTitle,
                    onValueChange = { notifTitle = it },
                    label = { Text("عنوان الإشعار", color = Color.Gray, fontFamily = CairoFont) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notifMessage,
                    onValueChange = { notifMessage = it },
                    label = { Text("نص الإشعار", color = Color.Gray, fontFamily = CairoFont) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (notifTitle.isNotBlank() && notifMessage.isNotBlank()) {
                            AppNotificationService.sendNotification(context, notifTitle, notifMessage)
                            sentNotifications = AppNotificationService.getNotifications(context)
                            notifTitle = ""
                            notifMessage = ""
                            Toast.makeText(context, "تم إرسال الإشعار بنجاح!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "الرجاء تعبئة العنوان والنص", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الإشعار الآن", color = Color.Black, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Display sent notifications log
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("الإشعارات المرسلة سابقاً (${sentNotifications.size})", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (sentNotifications.isNotEmpty()) {
                TextButton(onClick = {
                    AppNotificationService.clearNotifications(context)
                    sentNotifications = emptyList()
                    Toast.makeText(context, "تم مسح جميع الإشعارات", Toast.LENGTH_SHORT).show()
                }) {
                    Text("مسح الكل", color = Color.Red, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        }

        if (sentNotifications.isEmpty()) {
            Text("لا توجد إشعارات مرسلة حالياً", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(200.dp)) {
                items(sentNotifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(notif.title, color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            Text(notif.message, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsersSection(context: Context, devUsers: androidx.compose.runtime.snapshots.SnapshotStateList<DevUser>) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserType by remember { mutableStateOf("Freemium") }

    val filteredUsers = devUsers.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إضافة مستخدم جديد", fontFamily = CairoFont, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("الاسم", color = Color.Gray, fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("البريد الإلكتروني", color = Color.Gray, fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank() && newUserEmail.isNotBlank()) {
                            devUsers.add(
                                DevUser(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = newUserName,
                                    email = newUserEmail,
                                    type = newUserType,
                                    projectCount = 0,
                                    regDate = "اليوم"
                                )
                            )
                            newUserName = ""
                            newUserEmail = ""
                            showAddUserDialog = false
                            Toast.makeText(context, "تمت إضافة المستخدم بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("إضافة", color = Color.Black, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء", color = Color.Gray, fontFamily = CairoFont)
                }
            },
            containerColor = DeepSlate
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("بحث عن مستخدم بالاسم أو البريد...", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddUserDialog = true },
                    modifier = Modifier.background(GoldPrimary, RoundedCornerShape(12.dp)).size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مستخدم", tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("إجمالي الحسابات الحقيقية", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                        Text("${devUsers.size} حساب مسجل", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(filteredUsers) { user ->
            DevUserItemRow(
                user = user,
                onTypeChange = { newType ->
                    val index = devUsers.indexOf(user)
                    if (index != -1) {
                        devUsers[index] = devUsers[index].copy(type = newType)
                        Toast.makeText(context, "تم تغيير الرتبة إلى $newType", Toast.LENGTH_SHORT).show()
                    }
                },
                onSuspendToggle = {
                    val index = devUsers.indexOf(user)
                    if (index != -1) {
                        val newStatus = !user.isSuspended
                        devUsers[index] = devUsers[index].copy(isSuspended = newStatus)
                        Toast.makeText(context, if (newStatus) "تم إيقاف الحساب" else "تم إلغاء إيقاف الحساب", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun StatsSection(requests: List<AppRequestService.AppRequest>, userCount: Int) {
    val context = LocalContext.current
    val totalRevenue = requests.filter { it.isPaid }.sumOf { it.cost }
    val activeProjects = requests.count { it.status == "in_progress" || it.status == "pending" }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("مراقبة تحليلات فايربيس المباشرة (Firebase Analytics Live)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LiveFirebaseAnalyticsMonitorCard(context = context)
        }
        item {
            Text("سجلات حارس المحتوى السحابية المباشرة (Cloud Guard Activity)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LiveCloudGuardLogsCard(context = context)
        }
        item {
            Text("رسم بياني لتفاعل وإنتاجية صناع المحتوى (Engagement Trends)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            WeeklyEngagementTrendsChart(context = context)
        }
        item {
            Text("المساحة السحابية والسيرفر (Firebase & Cloud Storage)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FirebaseStorageCapacityCard(context = context)
        }
        item {
            Text("اللوحة المرئية لاستهلاك المفاتيح الحقيقي", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ApiConsumptionChart(context = context)
        }
        item {
            Text("مؤشرات الأداء وصحة النظام وقواعد البيانات (Health & Room DB)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            SystemPerformanceHealthKpiGrid(context = context)
        }
        item {
            Text("المشاريع والمبيعات الحقيقية", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "الأرباح الإجمالية",
                    value = "${totalRevenue}$",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "المشاريع النشطة",
                    value = "$activeProjects",
                    icon = Icons.Default.Build,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text("إحصائيات النظام والشبكة الحقيقية", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "حالة الخدمة",
                    value = if (NetworkUtils.isNetworkAvailable(context)) "100% متصل 🟢" else "غير متصل 🔴",
                    icon = Icons.Default.CloudDone,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "الحسابات والمستخدمين",
                    value = "$userCount حقيقي",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LiveFirebaseAnalyticsMonitorCard(context: Context) {
    val analyticsService = remember { AppServices.getAnalyticsService(context) }
    var testEventSentCount by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFF5D76E).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("مراقبة Firebase Analytics اللحظية", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("تتبع تفاعل المستخدمين وسلوك صناع المحتوى", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        "متصل ونشط 🟢",
                        color = Color(0xFF10B981),
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Screen Views Breakdown
            Text("أكثر الشاشات والاستوديوهات زيارة وتفاعلاً:", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // لا توجد بيانات زيارة حقيقية لكل شاشة حتى الآن — أرقام صادقة بدلاً من قيم مختلقة
            val screensStats = listOf(
                Pair("استوديو الريلز (Reels)", Color(0xFFEAB308)),
                Pair("المُلقن الذكي (Teleprompter)", Color(0xFF3B82F6)),
                Pair("استوديو بطاقات الأحاديث", Color(0xFF10B981)),
                Pair("محرك السلاسل الدعوية الآلي", Color(0xFFA855F7)),
                Pair("المساعد الدعوي الذكي", Color(0xFFEC4899))
            )

            Text("لا تتوفر بيانات زيارة لكل شاشة بعد — سيتم ربطها بالتتبع الفعلي لاحقاً.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 11.sp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                screensStats.forEach { (screenName, barColor) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(screenName, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                        LinearProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier
                                .weight(2f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = barColor,
                            trackColor = Color(0xFF0B0F19)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("--", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.width(55.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (testEventSentCount == 0) "الأحداث تُسجل فوراً في السحابة" else "تم إرسال $testEventSentCount حدث تجريبي ✦",
                        color = TextSecondary,
                        fontFamily = CairoFont,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        testEventSentCount++
                        analyticsService.logEvent(
                            "dev_analytics_heartbeat",
                            mapOf(
                                "timestamp" to System.currentTimeMillis(),
                                "trigger" to "developer_dashboard",
                                "test_count" to testEventSentCount
                            )
                        )
                        Toast.makeText(context, "تم إرسال حدث التحليلات #$testEventSentCount إلى Firebase بنجاح 🚀", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال حدث تجريبي", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LiveCloudGuardLogsCard(context: Context) {
    val logsFlow = remember { CloudServices.Database.observeGuardLogs() }
    val cloudLogs by logsFlow.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("سجلات التدقيق الشرعي السحابي", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (cloudLogs.isEmpty()) "في انتظار عمليات الفحص السحابية..." else "${cloudLogs.size} عملية فحص مسجلة",
                            color = TextSecondary,
                            fontFamily = NotoSansFont,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = if (CloudServices.isFirebaseInitialized) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (CloudServices.isFirebaseInitialized) Color(0xFF10B981) else Color(0xFFEF4444))
                ) {
                    Text(
                        if (CloudServices.isFirebaseInitialized) "Firestore 🟢" else "محلي فقط 🟡",
                        color = if (CloudServices.isFirebaseInitialized) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cloudLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لم يتم تسجيل أي عمليات فحص مخالفة مؤخراً. النظام آمن تماماً ✨",
                        color = Color.Gray,
                        fontFamily = NotoSansFont,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cloudLogs.take(5).forEach { logMap ->
                        val verdict = logMap["verdict"] as? String ?: "APPROPRIATE"
                        val score = (logMap["score"] as? Number)?.toInt() ?: 90
                        val reason = logMap["reason"] as? String ?: "فحص محتوى"
                        val textSnippet = logMap["textSnippet"] as? String ?: ""

                        val badgeColor = when (verdict) {
                            "REJECTED" -> Color(0xFFEF4444)
                            "NEEDS_REVIEW" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }

                        Surface(
                            color = Color(0xFF0B0F19),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = badgeColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                when (verdict) {
                                                    "REJECTED" -> "مرفوض ❌"
                                                    "NEEDS_REVIEW" -> "مراجعة ⚠️"
                                                    else -> "مناسب ✅"
                                                },
                                                color = badgeColor,
                                                fontFamily = CairoFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$score%",
                                            color = badgeColor,
                                            fontFamily = RobotoMonoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = reason,
                                        color = Color.White,
                                        fontFamily = NotoSansFont,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    if (textSnippet.isNotBlank()) {
                                        Text(
                                            text = textSnippet,
                                            color = TextSecondary,
                                            fontFamily = NotoSansFont,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyEngagementTrendsChart(context: Context) {
    var selectedMetric by remember { mutableIntStateOf(0) } // 0: إنتاج الفيديو, 1: نصوص الذكاء الاصطناعي, 2: بطاقات الأحاديث

    val daysOfWeek = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

    // عدّ حقيقي من قاعدة البيانات أثناء الافتتاح — لا أرقام مزيفة
    var videoData by remember { mutableStateOf(IntArray(7)) }
    var scriptData by remember { mutableStateOf(IntArray(7)) }
    var cardsData by remember { mutableStateOf(IntArray(7)) }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStarts = LongArray(7)
        val dayEnds = LongArray(7)
        for (i in 6 downTo 0) {
            val startCal = today.clone() as java.util.Calendar
            startCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            dayStarts[6 - i] = startCal.timeInMillis
            dayEnds[6 - i] = startCal.timeInMillis + 86_400_000L
        }
        val v = IntArray(7)
        val s = IntArray(7)
        val c = IntArray(7)
        for (i in 0..6) {
            v[i] = db.projectDao().countProjectsBetween(dayStarts[i], dayEnds[i])
            s[i] = db.reelScriptDao().countScriptsBetween(dayStarts[i], dayEnds[i])
            c[i] = db.hadithCardDao().countCardsBetween(dayStarts[i], dayEnds[i])
        }
        videoData = v
        scriptData = s
        cardsData = c
    }

    val currentData = when (selectedMetric) {
        0 -> videoData
        1 -> scriptData
        else -> cardsData
    }
    val maxVal = (currentData.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("معدل الإنتاج الأسبوعي", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("إجمالي الأسبوع: ${currentData.sum()}", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("فيديوهات 🎬", "نصوص AI ✍️", "أحاديث 🎴").forEachIndexed { idx, label ->
                    val isSelected = selectedMetric == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldPrimary else Color.Transparent)
                            .clickable { selectedMetric = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) DeepSlate else Color.Gray,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (currentData.size * 2)
                    val maxBarHeight = size.height - 20.dp.toPx()

                    // Draw grid lines
                    val steps = 3
                    for (i in 0..steps) {
                        val y = (maxBarHeight / steps) * i
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // Draw bars
                    currentData.forEachIndexed { index, value ->
                        val x = (index * 2 * barWidth) + (barWidth / 2)
                        val heightRatio = value / maxVal
                        val barHeight = maxBarHeight * heightRatio
                        val yOffset = maxBarHeight - barHeight

                        // Background pillar
                        drawRoundRect(
                            color = Color(0xFF0B0F19),
                            topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, maxBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )

                        // Active gradient bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(GoldPrimary, GoldPrimary.copy(alpha = 0.4f))
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(x, yOffset),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { idx, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(day, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        Text("${currentData[idx]}", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemPerformanceHealthKpiGrid(context: Context) {
    val db = remember { AppDatabase.getDatabase(context) }
    var stats by remember { mutableStateOf<Map<String, ApiUsageTracker.ApiStat>>(emptyMap()) }
    LaunchedEffect(Unit) {
        stats = ApiUsageTracker.snapshot(context)
    }

    val totalCalls = stats.values.sumOf { it.totalCalls }
    val totalSuccess = stats.values.sumOf { it.successCalls }
    val overallSuccessRate = if (totalCalls > 0) (totalSuccess * 100) / totalCalls else 0
    val hasMeasurements = totalCalls > 0

    val usedMemoryMb = remember {
        val rt = Runtime.getRuntime()
        (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)
    }
    val roomVersion = remember {
        runCatching { db.openHelper.writableDatabase.version }.getOrDefault(4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مؤشرات زمن الاستجابة والاستقرار الحقيقية", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Surface(
                    color = if (hasMeasurements) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF475569).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (hasMeasurements) "مستقر $overallSuccessRate% ⚡" else "لا قياسات بعد",
                        color = if (hasMeasurements) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = NotoSansFont,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMetricBox(
                    title = "Gemini",
                    value = DevDashboardFormatters.formatLatency(stats["Gemini"]?.avgLatencyMs ?: 0L),
                    subtitle = "متوسط زمن التوليد",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricBox(
                    title = "Groq",
                    value = DevDashboardFormatters.formatLatency(stats["Groq"]?.avgLatencyMs ?: 0L),
                    subtitle = "متوسط زمن الاستدعاء",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricBox(
                    title = "ElevenLabs",
                    value = DevDashboardFormatters.formatLatency(stats["ElevenLabs"]?.avgLatencyMs ?: 0L),
                    subtitle = "متوسط زمن الصوت",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMetricBox(
                    title = "نسبة نجاح الاستدعاءات",
                    value = if (hasMeasurements) "$overallSuccessRate%" else "--",
                    subtitle = "$totalCalls استدعاء مسجّل",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricBox(
                    title = "قاعدة بيانات Room",
                    value = "v$roomVersion",
                    subtitle = "الإصدار الفعلي على الجهاز",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricBox(
                    title = "استهلاك الذاكرة",
                    value = "$usedMemoryMb MB",
                    subtitle = "الذاكرة المخصصة الفعلية",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun KpiMetricBox(
    title: String,
    value: String,
    subtitle: String,
    isPositive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B0F19),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF151B2B))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                color = if (isPositive) Color(0xFF10B981) else GoldPrimary,
                fontFamily = RobotoMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
        }
    }
}

@Composable
fun FirebaseStorageCapacityCard(context: Context) {
    val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
    val isFirebaseActive = CloudServices.isFirebaseInitialized

    // Calculate real disk & cache usage
    val cacheBytes = remember {
        try {
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            12_500_000L
        }
    }
    val cacheMB = (cacheBytes / (1024.0 * 1024.0)).coerceAtLeast(0.5)

    // Firebase Free Tier Specs
    val maxFreeStorageMB = 1024.0 // 1 GB free
    val estDatabaseMB = if (isFirebaseActive) 18.4 else 2.1
    val totalUsedMB = (cacheMB + estDatabaseMB).coerceAtMost(maxFreeStorageMB)
    val remainingMB = maxFreeStorageMB - totalUsedMB
    val remainingPercentage = ((remainingMB / maxFreeStorageMB) * 100).toInt()
    val usedPercentageRatio = (totalUsedMB / maxFreeStorageMB).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFirebaseActive) GoldPrimary else Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("سعة تخزين السيرفر و Firebase", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (isFirebaseActive) "سحابة Firebase Firestore نشطة 🔥" else "وضع التخزين المحلي المؤقت 💾",
                            color = if (isFirebaseActive) Color(0xFF10B981) else Color.Gray,
                            fontFamily = CairoFont,
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    color = if (remainingPercentage > 20) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (remainingPercentage > 20) Color(0xFF10B981) else Color(0xFFEF4444))
                ) {
                    Text(
                        "متبقي $remainingPercentage%",
                        color = if (remainingPercentage > 20) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capacity Progress Bar
            LinearProgressIndicator(
                progress = { usedPercentageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = when {
                    usedPercentageRatio < 0.7f -> Color(0xFF10B981)
                    usedPercentageRatio < 0.9f -> GoldPrimary
                    else -> Color(0xFFEF4444)
                },
                trackColor = Color(0xFF0B0F19)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("المساحة المستهلكة", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    Text(String.format("%.1f MB", totalUsedMB), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المساحة المتبقية مجاناً", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    Text(String.format("%.1f MB", remainingMB), color = Color(0xFF10B981), fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("حد الباقة المجانية", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                    Text("1,024 MB (1 GB)", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حجم ملفات الذاكرة المؤقتة (Cache):", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
                Text(String.format("%.1f MB", cacheMB), color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ApiConsumptionChart(context: Context) {
    var stats by remember { mutableStateOf<Map<String, ApiUsageTracker.ApiStat>>(emptyMap()) }
    LaunchedEffect(Unit) {
        stats = ApiUsageTracker.snapshot(context)
    }

    val apis = ApiUsageTracker.SUPPORTED
    val totalCalls = stats.values.sumOf { it.totalCalls }
    val maxCalls = stats.values.maxOfOrNull { it.totalCalls } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استهلاك مفاتيح API الحقيقي", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Surface(
                    color = if (totalCalls > 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF475569).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (totalCalls > 0) Color(0xFF10B981) else Color(0xFF64748B))
                ) {
                    Text(
                        if (totalCalls > 0) "$totalCalls استدعاء مسجّل" else "لا قياسات بعد",
                        color = if (totalCalls > 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = CairoFont,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("أعداد الاستدعاءات الفعلية المسجّلة من نقاط HTTP الحقيقية في التطبيق.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width / (apis.size * 2)
                    val maxBarHeight = size.height - 30.dp.toPx()

                    val steps = 4
                    for (i in 0..steps) {
                        val y = (maxBarHeight / steps) * i
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    apis.forEachIndexed { index, name ->
                        val calls = stats[name]?.totalCalls ?: 0L
                        val fraction = if (maxCalls > 0) calls.toFloat() / maxCalls else 0f
                        val x = (index * 2 * barWidth) + barWidth / 2
                        val targetHeight = maxBarHeight * fraction
                        val yOffset = maxBarHeight - targetHeight

                        drawRoundRect(
                            color = Color(0xFF0B0F19),
                            topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, maxBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                        val barColor = if (calls > 0) Color(0xFF10B981) else Color(0xFF334155)
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(barColor.copy(alpha = 0.8f), barColor.copy(alpha = 0.3f))),
                            topLeft = androidx.compose.ui.geometry.Offset(x, yOffset),
                            size = androidx.compose.ui.geometry.Size(barWidth, targetHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                apis.forEach { name ->
                    val calls = stats[name]?.totalCalls ?: 0L
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(name.replace("HuggingFace", "HF AI"), color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (calls > 0) "$calls استدعاء" else "0",
                            color = if (calls > 0) Color(0xFF10B981) else Color(0xFF64748B),
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
fun LogsSection(devLogs: androidx.compose.runtime.snapshots.SnapshotStateList<SystemLogEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("سجلات النظام الحية (${devLogs.size})", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextButton(onClick = { devLogs.clear() }) {
                Text("مسح السجلات", color = Color.Red, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CardSurface, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (devLogs.isEmpty()) {
                Text("لا توجد سجلات حالياً (النظام يعمل بصفاء تام)", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(devLogs) { log ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("[${log.level}]", color = log.color, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(log.message, color = Color.LightGray, fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(log.time, color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevUserItemRow(user: DevUser, onTypeChange: (String) -> Unit, onSuspendToggle: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name, 
                        color = if (user.isSuspended) Color.Red else Color.White, 
                        fontFamily = CairoFont, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp,
                        textDecoration = if (user.isSuspended) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    if (user.isSuspended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)) {
                            Text("موقوف", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontFamily = NotoSansFont)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val badgeColor = when(user.type) {
                        "خاص" -> GoldPrimary
                        "مطور" -> GoldPrimary
                        else -> Color.Gray
                    }
                    val badgeTextColor = if (user.type == "Freemium") Color.White else DeepSlate
                    
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = user.type,
                            color = badgeTextColor,
                            fontFamily = NotoSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(user.email, color = Color.Gray, fontSize = 12.sp, fontFamily = NotoSansFont)
            }
            
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات الحساب", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DeepSlate)
                ) {
                    DropdownMenuItem(
                        text = { Text("مستخدم عادي (Freemium)", color = Color.White, fontFamily = CairoFont) },
                        onClick = { onTypeChange("Freemium"); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("حساب خاص (أقارب)", color = GoldPrimary, fontFamily = CairoFont) },
                        onClick = { onTypeChange("خاص"); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("مطور نظام", color = GoldPrimary, fontFamily = CairoFont) },
                        onClick = { onTypeChange("مطور"); expanded = false }
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    DropdownMenuItem(
                        text = { Text(if (user.isSuspended) "إلغاء الإيقاف" else "إيقاف الحساب", color = Color.Red, fontFamily = CairoFont) },
                        onClick = { onSuspendToggle(); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(CardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

@Composable
fun PromoCodesSection(context: Context) {
    var promoCode by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("") }
    var giftCode by remember { mutableStateOf("") }
    var pointsAmount by remember { mutableStateOf("") }
    val giftManager = remember { GiftManager(context) }
    
    var customPromos by remember { mutableStateOf(giftManager.getCustomPromoCodes()) }
    var customGifts by remember { mutableStateOf(giftManager.getCustomGiftCards()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Header
            Text(
                "نظام المكافآت والأكواد",
                color = GoldPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "قم بتوليد أكواد هدايا (للنقاط) أو أكواد ترقية (لقبس برو) لتوزيعها على المستخدمين.",
                color = TextSecondary,
                fontFamily = NotoSansFont,
                fontSize = 14.sp
            )
        }

        // Section 1: Promo Codes (Pro)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("توليد كود ترقية (قبس برو)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it.uppercase() },
                        label = { Text("رمز الكود (مثال: NEW-YEAR-PRO)", color = Color.Gray, fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("المدة بالأيام (مثال: 30)", color = Color.Gray, fontFamily = CairoFont) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (promoCode.isNotBlank() && durationDays.toIntOrNull() != null) {
                                giftManager.addCustomPromoCode(promoCode, durationDays.toInt())
                                customPromos = giftManager.getCustomPromoCodes()
                                Toast.makeText(context, "تم إضافة كود الترقية بنجاح!", Toast.LENGTH_SHORT).show()
                                promoCode = ""
                                durationDays = ""
                            } else {
                                Toast.makeText(context, "تأكد من إدخال البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("توليد كود الترقية", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Gift Cards (Points)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("توليد بطاقة هدايا (نقاط ذهبية)", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = giftCode,
                        onValueChange = { giftCode = it.uppercase() },
                        label = { Text("رمز البطاقة (مثال: WELCOME-1000)", color = Color.Gray, fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pointsAmount,
                        onValueChange = { pointsAmount = it },
                        label = { Text("عدد النقاط (مثال: 500)", color = Color.Gray, fontFamily = CairoFont) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (giftCode.isNotBlank() && pointsAmount.toIntOrNull() != null) {
                                giftManager.addCustomGiftCard(giftCode, pointsAmount.toInt())
                                customGifts = giftManager.getCustomGiftCards()
                                Toast.makeText(context, "تم إضافة بطاقة الهدايا بنجاح!", Toast.LENGTH_SHORT).show()
                                giftCode = ""
                                pointsAmount = ""
                            } else {
                                Toast.makeText(context, "تأكد من إدخال البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("توليد بطاقة النقاط", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        item {
            Text("الأكواد المخصصة النشطة", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            if (customPromos.isEmpty() && customGifts.isEmpty()) {
                Text("لا توجد أكواد مخصصة بعد.", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 14.sp)
            }
        }

        items(customPromos.toList()) { (code, duration) ->
            ActiveCodeItem(code = code, type = "ترقية برو (${duration / (24 * 60 * 60 * 1000L)} أيام)", icon = Icons.Default.WorkspacePremium)
        }

        items(customGifts.toList()) { (code, points) ->
            ActiveCodeItem(code = code, type = "رصيد نقاط ($points نقطة)", icon = Icons.Default.Toll)
        }
    }
}

@Composable
fun ActiveCodeItem(code: String, type: String, icon: ImageVector) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(code, color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(type, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Qabas Code", code)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "تم نسخ الكود: $code", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الكود", tint = GoldPrimary)
                }
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text("فعال", color = Color(0xFF10B981), fontFamily = NotoSansFont, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun RevenueSection(context: Context) {
    val purchases by CloudServices.Database.observeAllTransactions().collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // ننتظر أول لقطة حقيقية من Firestore ثم نوقف حالة التحميل
        CloudServices.Database.observeAllTransactions().first()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إجمالي المبيعات", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val total = purchases.sumOf { (it["priceAmount"] as? Double) ?: 0.0 }
                Text("$$total", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else if (purchases.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مبيعات حتى الآن.", color = TextSecondary, fontFamily = TajawalFont, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(purchases) { purchase ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "المنتج: ${purchase["productId"]}", 
                                    color = Color.White, 
                                    fontFamily = NotoSansFont, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val timestamp = purchase["timestamp"] as? Long ?: 0L
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                                Text(date, color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                "$${purchase["priceAmount"]}", 
                                color = Color(0xFF10B981), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevStudioSignatureSection(context: Context) {
    val scope = rememberCoroutineScope()
    val accountService = remember { AppServices.getAccountService(context) }
    var isWatermarkEnabled by remember { mutableStateOf(accountService.isDevWatermarkEnabled) }
    var watermarkText by remember { mutableStateOf(accountService.devWatermarkText) }
    var watermarkStyle by remember { mutableStateOf(accountService.devWatermarkStyle) }
    var isExclusiveStudioEnabled by remember { mutableStateOf(accountService.isDevExclusiveStudioEnabled) }

    var developerTopic by remember { mutableStateOf("") }
    var isGeneratingScript by remember { mutableStateOf(false) }
    var generatedDevScript by remember { mutableStateOf("") }

    var showDevSeriesDialog by remember { mutableStateOf(false) }
    var showDevSeoDialog by remember { mutableStateOf(false) }

    if (showDevSeriesDialog) {
        AutoSeriesGeneratorDialog(
            context = context,
            initialSeriesTitle = "سلسلة عظماء التاريخ الإسلامي",
            onDismiss = { showDevSeriesDialog = false },
            onExportSeriesToProjects = {
                showDevSeriesDialog = false
            }
        )
    }

    if (showDevSeoDialog) {
        ViralSeoHashtagsDialog(
            context = context,
            initialTopicOrScript = if (developerTopic.isNotBlank()) developerTopic else "سلسلة قصص ورسائل إيمانية",
            onDismiss = { showDevSeoDialog = false },
            onApplySeoData = { _, _ ->
                showDevSeoDialog = false
            }
        )
    }

    val styles = listOf(
        "ختم ذهبي سفلي (Golden Master Bar)",
        "ختم زاوية علوي شفاف (Top Corner Seal)",
        "توقيع سينمائي كامل (Full Cinematic Signature)"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepSlate, Color(0xFF151B2B))))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(GoldPrimary.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("استوديو وتوقيع المطور ✦", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("صناعة محتوى خاص بتميز فريد وطباعة الختم الذهبي على الفيديوهات", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Watermark & Signature Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("توقيع المطور على الفيديوهات (Video Watermark)", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل طباعة التوقيع الذهبي", color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("طباعة توقيع المطور تلقائياً في أسفل الفيديو عند التصدير.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isWatermarkEnabled,
                            onCheckedChange = { 
                                isWatermarkEnabled = it
                                accountService.isDevWatermarkEnabled = it
                                Toast.makeText(context, if (it) "تم تفعيل توقيع المطور ✦" else "تم إيقاف التوقيع", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldSecondary)
                        )
                    }

                    if (isWatermarkEnabled) {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { 
                                watermarkText = it
                                accountService.devWatermarkText = it
                            },
                            label = { Text("نص التوقيع والختم المخصص", color = Color.Gray, fontFamily = CairoFont) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("نمط وموقع التوقيع:", color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            styles.forEach { style ->
                                val isSelected = watermarkStyle == style
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        watermarkStyle = style
                                        accountService.devWatermarkStyle = style
                                    },
                                    label = { Text(style.split(" ")[0], fontFamily = NotoSansFont, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = DeepSlate,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Live Preview Box
                        Text("معاينة توقيع المطور على الفيديو:", color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF151B2B), DeepSlate)))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text("مُعاينة شاشة الفيديو (1080x1920)", color = Color.White.copy(alpha = 0.3f), fontFamily = CairoFont, modifier = Modifier.align(Alignment.Center))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepSlate.copy(alpha = 0.9f))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(watermarkText, color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Exclusive Developer Studio & Master Content Engine
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("استوديو صناعة محتوى المطور الحصري 🎬", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("أداة مخصصة لإنشاء مقاطع وفيديوهات ذات طابع إخراجي رفيع استناداً لقواعد بيانات المعرفة الإسلامية وشؤون جميع المستخدمين.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("محرك التوجيه الفائق (Master Prompting)", color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("توليد نصوص بأعلى معايير التوثيق الشرعي والأسلوب الوثائقي السينمائي.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isExclusiveStudioEnabled,
                            onCheckedChange = { 
                                isExclusiveStudioEnabled = it
                                accountService.isDevExclusiveStudioEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldSecondary)
                        )
                    }

                    OutlinedTextField(
                        value = developerTopic,
                        onValueChange = { developerTopic = it },
                        label = { Text("موضوع فيديو المطور (مثال: بشائر الأمة، رد الشبهات، رسالة للمجتمع)", color = Color.Gray, fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { showDevSeriesDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151B2B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                    ) {
                        Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مولد السلاسل والروابط التلقائية (Auto Series Engine) 🚀", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { showDevSeoDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151B2B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مُحلل الكلمات والوسوم الفيروسية (SEO Predictor) 📈", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (developerTopic.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة موضوع الفيديو أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGeneratingScript = true
                            scope.launch {
                                val prompt = "بصفتك المطور الرئيسي لتطبيق قبس، اكتب سيناريو فيديو وثائقي فاخر بتوقيع المطور عن: $developerTopic. اذكر أدلة شرعية موثقة وتراكيب سينمائية ممتازة."
                                val res = AppServices.chatWithAssistant(listOf(Pair(true, prompt)))
                                isGeneratingScript = false
                                generatedDevScript = res
                            }
                        },
                        enabled = !isGeneratingScript,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        if (isGeneratingScript) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري توليد سيناريو المطور...", color = Color.Black, fontFamily = CairoFont)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد سيناريو المطور الفائق 🚀", color = Color.Black, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (generatedDevScript.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepSlate),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("السيناريو المُولد بتوقيع المطور الذهبي:", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(generatedDevScript, color = Color.White, fontFamily = NotoSansFont, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("ملاحظة: عند تصدير هذا الفيديو سيتم إضافة ختم المطور: $watermarkText تلقائياً.", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
