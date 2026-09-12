package com.qabas.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenContentGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    var notifications by remember { 
        val list = AppNotificationService.getNotifications(context)
        if (list.isEmpty()) {
            // Seed initial Trend Guard alerts
            AppNotificationService.sendNotification(
                context,
                "🚨 تنبيه عاجل من مرصد الثغور!",
                "رصد موجة تشكيك عاجلة تستهدف ثبات الشباب والالتزام. يُرجى الدخول لمرصد الثغور للمدافعة بنشر المقاطع.",
                isTrendAlert = true
            )
            AppNotificationService.sendNotification(
                context,
                "⚡ تريند يحتاج مدافعة سريعة",
                "انتشار تريند ساخر من قيم الأسرة وبر الوالدين. تم إعداد رد قاطع ومحكم بالذكاء الاصطناعي.",
                isTrendAlert = true
            )
            mutableStateOf(AppNotificationService.getNotifications(context))
        } else {
            mutableStateOf(list)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Translator.tr("الإشعارات والمدافعات"), fontFamily = CairoFont, fontWeight = FontWeight.Bold, color = GoldPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة", tint = GoldPrimary)
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = {
                            AppNotificationService.clearNotifications(context)
                            notifications = emptyList()
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "مسح الكل", tint = GoldPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (notifications.isEmpty()) {
                QabasEmptyState(
                    icon = Icons.Default.Notifications,
                    title = Translator.tr("لا توجد إشعارات حالياً"),
                    description = Translator.tr("عندما تصلك إشعارات جديدة أو تنبيهات المدافعة عن الثغور وموجات التريند ستظهر هنا فوراً."),
                    actionButtonText = Translator.tr("مرصد الثغور 🛡️"),
                    onActionClick = onOpenContentGuard
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationCard(notif, onOpenContentGuard)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notif: AppNotificationService.AppNotification,
    onOpenContentGuard: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = if (notif.isTrendAlert) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (notif.isTrendAlert) Color(0xFF7F1D1D) else DeepSlate,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (notif.isTrendAlert) Icons.Default.Shield else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (notif.isTrendAlert) Color(0xFFEF4444) else GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            notif.title, 
                            color = if (notif.isTrendAlert) Color(0xFFEF4444) else GoldPrimary, 
                            fontFamily = CairoFont, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val date = SimpleDateFormat("dd/MM - hh:mm a", Locale.getDefault()).format(Date(notif.timestamp))
                        Text(
                            date, 
                            color = Color.Gray, 
                            fontFamily = NotoSansFont, 
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        notif.message, 
                        color = Color.White.copy(alpha = 0.9f), 
                        fontFamily = NotoSansFont, 
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            if (notif.isTrendAlert) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenContentGuard,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الانتقال لمرصد الثغور والمدافعة 🛡️", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

