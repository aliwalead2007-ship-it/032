package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class CovenantRule(
    val number: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badge: String
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var isAgreed by remember { mutableStateOf(true) }

    val covenantRules = remember {
        listOf(
            CovenantRule(
                number = "١",
                title = "إخلاص القصد ونفع الأمة",
                description = "جعل صناعة المحتوى سبيلاً لنشر الفضيلة وتزكية النفوس وبناء الوعي الراشد، بعيداً عن الابتذال والتفاهة.",
                icon = Icons.Default.VolunteerActivism,
                badge = "النية والأثر"
            ),
            CovenantRule(
                number = "٢",
                title = "النقاء الصوتي التام",
                description = "الاعتماد الحصري على التلاوات القرآنية الندية، والأناشيد العذبة بدون معازف، والمؤثرات الصوتية الطبيعية المباحة.",
                icon = Icons.Default.GraphicEq,
                badge = "بلا موسيقى"
            ),
            CovenantRule(
                number = "٣",
                title = "صون الحياء والوقار الشرعي",
                description = "الالتزام بالحشمة والسمت الإسلامي، واجتناب اللقطات الخادشة، مع طمس وتجريد وجوه الرموز المولدة بالذكاء الاصطناعي.",
                icon = Icons.Default.Security,
                badge = "عفة وحشمة"
            ),
            CovenantRule(
                number = "٤",
                title = "التوثيق والأمانة العلمية",
                description = "عزو الآيات القرآنية بدقة متناهية، والاعتماد الحصري على الأحاديث الصحيحة والحسنة وتجنب المنكر والموضوع.",
                icon = Icons.Default.MenuBook,
                badge = "صحة الرواية"
            ),
            CovenantRule(
                number = "٥",
                title = "الخطاب الرصين ونشر الألفة",
                description = "ترسيخ قيم الرحمة والتوبة والأمل، واجتناب السباب، والتكفير، والجدل العقيم، وإثارة الشائعات والفتن.",
                icon = Icons.Default.Diversity3,
                badge = "أدب الحوار"
            )
        )
    }

    val goldGradient = Brush.horizontalGradient(
        listOf(GoldSecondary, GoldPrimary, Color(0xFFFFF9C4), GoldPrimary)
    )

    val luxuryBorder = Brush.verticalGradient(
        listOf(GoldSecondary.copy(alpha = 0.8f), GoldPrimary.copy(alpha = 0.3f))
    )

    val infiniteTransition = rememberInfiniteTransition(label = "covenantPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        DeepSlate,
                        Color(0xFF070B14)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header Badge
            Surface(
                color = GoldPrimary.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ميثاق استوديو قبس الدعوي",
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ضوابط صناعة المحتوى الهادف",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = CairoFont,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "عهـد وأمانـة بين يـدي الله لنشـر الـهدى والـنـور",
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Scrollable Rules Container Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, luxuryBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    covenantRules.forEach { rule ->
                        Surface(
                            color = Color(0xFF0B0F19).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Number & Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(GoldPrimary.copy(alpha = 0.12f), CircleShape)
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = rule.icon,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rule.number}. ${rule.title}",
                                            color = Color.White,
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Surface(
                                            color = GoldPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = rule.badge,
                                                color = GoldPrimary,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = rule.description,
                                        color = Color(0xFFCBD5E1),
                                        fontFamily = NotoSansFont,
                                        fontSize = 12.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pledge Checkbox Row
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isAgreed) GoldPrimary.copy(alpha = 0.5f) else Color(0xFF334155)),
                onClick = { isAgreed = !isAgreed },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { isAgreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GoldPrimary,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = DeepSlate
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "أقر وأتعهد بالالتزام بهذا الميثاق في جميع مشاريعي",
                        color = if (isAgreed) Color.White else Color.Gray,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Pledge Button
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("has_seen_onboarding", true)
                        .putBoolean("has_seen_welcome_rules", true)
                        .putBoolean("has_accepted_covenant", true)
                        .apply()
                    onFinish()
                },
                enabled = isAgreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .scale(if (isAgreed) pulseScale else 1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isAgreed) goldGradient else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "أوافق وأتعهد بالالتزام ✦",
                            color = if (isAgreed) DeepSlate else Color.Gray,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "متابعة",
                            tint = if (isAgreed) DeepSlate else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
