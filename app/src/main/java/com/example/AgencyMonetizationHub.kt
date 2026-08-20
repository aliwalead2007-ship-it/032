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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class ClientRetainer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val clientName: String,
    val serviceType: String,
    val monthlyAmount: Double,
    val paymentStatus: String, // "مدفوع 🟢", "معلق 🟡", "مطلوب تجديد 🔴"
    val deliveryProgress: Float // 0.0f to 1.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgencyMonetizationHub(context: Context) {
    var monthlyTarget by remember { mutableStateOf(3000f) } // Default $3000/mo
    var activeTab by remember { mutableStateOf(0) } // 0: Income Strategy, 1: Quote Generator, 2: Client Tracker, 3: White-Label Preset

    // Client Quote Generator State
    var clientName by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf("باقة إنتاج ريلز شهرياً (30 فيديو)") }
    var packagePrice by remember { mutableStateOf("600") }
    var clientLogoName by remember { mutableStateOf("") }
    var generatedQuoteText by remember { mutableStateOf("") }

    // Client Retainers
    var clientsList by remember {
        mutableStateOf(
            listOf(
                ClientRetainer(clientName = "قناة هداية الدعوية", serviceType = "باقة 30 فيديو Reels", monthlyAmount = 600.0, paymentStatus = "مدفوع 🟢", deliveryProgress = 0.8f),
                ClientRetainer(clientName = "جمعية البر والتقوى", serviceType = "تطبيق مخصص White-Label", monthlyAmount = 1500.0, paymentStatus = "مدفوع 🟢", deliveryProgress = 1.0f),
                ClientRetainer(clientName = "منصة تدبر القرآن", serviceType = "باقة السكربتات والتعليق الصوتي", monthlyAmount = 350.0, paymentStatus = "معلق 🟡", deliveryProgress = 0.4f)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "مركز أرباح المطور والوكالة الرقمية 💰",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "حوّل تطبيق قبس إلى وكالة ربحية فائقة لإنتاج المحتوى وبناء تطبيقات العملاء بضغطات زر.",
                                color = TextSecondary,
                                fontFamily = CairoFont,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Selector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) {
                            Text("خطة الدخل", fontSize = 12.sp, fontFamily = NotoSansFont)
                        }
                        SegmentedButton(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) {
                            Text("الفواتير", fontSize = 12.sp, fontFamily = NotoSansFont)
                        }
                        SegmentedButton(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) {
                            Text("العملاء", fontSize = 12.sp, fontFamily = NotoSansFont)
                        }
                        SegmentedButton(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) {
                            Text("White-Label", fontSize = 12.sp, fontFamily = NotoSansFont)
                        }
                    }
                }
            }
        }

        // TAB 0: Income Calculator & Target Simulator
        if (activeTab == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حاسبة الدخل الشهري المستهدف 🎯", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الهدف الشهري:", color = TextSecondary, fontFamily = NotoSansFont, fontSize = 14.sp)
                            Text("$${monthlyTarget.toInt()} / شهرياً", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }

                        Slider(
                            value = monthlyTarget,
                            onValueChange = { monthlyTarget = it },
                            valueRange = 500f..10000f,
                            steps = 19,
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val reelPackagesNeeded = (monthlyTarget * 0.50 / 600).toInt().coerceAtLeast(1)
                        val whiteLabelAppsNeeded = (monthlyTarget * 0.35 / 1500).toInt().coerceAtLeast(1)
                        val vipSubscriptionsNeeded = (monthlyTarget * 0.15 / 50).toInt().coerceAtLeast(2)

                        Text("خريطة العمل المقترحة لتحقيق هذا الهدف سهلاً:", color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        StrategyRequirementRow(
                            icon = Icons.Default.VideoLibrary,
                            title = "باقات إنتاج Reels شهرياً (600$/شهرياً):",
                            subtitle = "تحتاج فقط إلى $reelPackagesNeeded عملاء/جمعيات (ينتجون 30 فيديو شهرياً بضغطات زر من قبس)."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StrategyRequirementRow(
                            icon = Icons.Default.AppSettingsAlt,
                            title = "تطبيقات مخصصة للجهات White-Label (1500$):",
                            subtitle = "تحتاج إلى $whiteLabelAppsNeeded جهات ترغب بتطبيق أندرويد مستقل بشعارها وهويتها."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StrategyRequirementRow(
                            icon = Icons.Default.CardMembership,
                            title = "اشتراكات أفراد ومؤثرين VIP (50$):",
                            subtitle = "تحتاج لبيع $vipSubscriptionsNeeded كود تفعيل سنوياً للمستخدمين وصناع المحتوى."
                        )
                    }
                }
            }
        }

        // TAB 1: Quote & Invoice Generator
        if (activeTab == 1) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مولد العروض والفواتير الاحترافية للعملاء 📄", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("اسم العميل أو الجهة (مثال: جمعية البر)", color = Color.Gray, fontFamily = CairoFont) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF1E293B), focusedTextColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = packagePrice,
                            onValueChange = { packagePrice = it },
                            label = { Text("السعر المترتب ($)", color = Color.Gray, fontFamily = CairoFont) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF1E293B), focusedTextColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val price = packagePrice.toDoubleOrNull() ?: 600.0
                                val cName = clientName.ifBlank { "العميل المحترم" }
                                generatedQuoteText = """
                                    ✦ عرض سعر واتفاقية تقديم خدمات إعلامية ✦
                                    الجهة المستفيدة: $cName
                                    الخدمة: $selectedPackage
                                    التكلفة الإجمالية: $price$
                                    
                                    المميزات المشمولة:
                                    1. كتابة وتوثيق النصوص بالذكاء الاصطناعي (Gemini 1.5 Flash).
                                    2. تعليق صوتي سينمائي مع هندسة صوتية ومؤثرات بدقة عالية.
                                    3. مونتاج تلقائي ومطابقة مرئيات B-Roll التراثية والدعوية.
                                    4. إضافة التوقيع واللوجو الخاص بجهة $cName.
                                    
                                    للتأكيد والدفع المباشر، يُرجى استخدام رابط الدفع المعتمد أو التواصل معنا فوراً.
                                """.trimIndent()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد صيغة الاتفاقية والفاتورة ✦", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                        }

                        if (generatedQuoteText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFF0B0F19),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(generatedQuoteText, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp, lineHeight = 20.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("Quote", generatedQuoteText)
                                                clipboard?.setPrimaryClip(clip)
                                                Toast.makeText(context, "تم نسخ العرض للحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, GoldPrimary)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ", color = GoldPrimary, fontFamily = NotoSansFont, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, generatedQuoteText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة عبر"))
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إرسال", color = DeepSlate, fontFamily = NotoSansFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: Active Clients Tracker
        if (activeTab == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة العقود والعملاء النشطين", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val totalMonthlyIncome = clientsList.sumOf { it.monthlyAmount }
                    Text("الدخل الحالي: $$totalMonthlyIncome", color = Color(0xFF10B981), fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            items(clientsList) { client ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(client.clientName, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(client.serviceType, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                            }
                            Text("$${client.monthlyAmount}", color = GoldPrimary, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الحالة: ${client.paymentStatus}", fontSize = 12.sp, fontFamily = NotoSansFont)
                            Text("نسبة الإنجاز: ${(client.deliveryProgress * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp, fontFamily = NotoSansFont)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { client.deliveryProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = GoldPrimary,
                            trackColor = Color(0xFF151B2B)
                        )
                    }
                }
            }
        }

        // TAB 3: White-Label Preset Injector
        if (activeTab == 3) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تجهيز التطبيق للعملاء (White-Label Injector) 🏷️", color = Color.White, fontFamily = TajawalFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "يمكنك حقن اسم العميل وشعاره وعلامته المائية لجميع الفيديوهات المنتجة بضغطة زر واحدة قبل تصدير النسخة للعميل.",
                            color = TextSecondary,
                            fontFamily = CairoFont,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = clientLogoName,
                            onValueChange = { clientLogoName = it },
                            label = { Text("نص الختم المائي للعميل (مثال: ✦ قناة البر الدعوية ✦)", color = Color.Gray, fontFamily = CairoFont) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF1E293B), focusedTextColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (clientLogoName.isNotBlank()) {
                                    val accountService = AppServices.getAccountService(context)
                                    accountService.isDevWatermarkEnabled = true
                                    accountService.devWatermarkText = clientLogoName
                                    accountService.devWatermarkStyle = "GOLD"
                                    Toast.makeText(context, "تم تفعيل هوية العميل ($clientLogoName) بنجاح على كافة المحركات! ✦", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DeepSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تطبيق هوية العميل على جميع معالجات الفيديو", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyRequirementRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(GoldPrimary.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
