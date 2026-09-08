package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.CairoFont
import com.example.ui.theme.NotoSansFont
import com.example.ui.theme.TajawalFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة البداية الرسمية لاستوديو قبس — نسخة «الكتاب المفتوح» (Book Opening Splash)
 * - كتاب قوّل يُفتح من المنتصف (الغلافان يدوران حول محور العمود الفقري) يكشف شعلة قبس + الآية
 * - تمثيل ثلاثي الأبعاد خفيف (rotationY + cameraDistance) سباع الحركة بسلاسة GPU
 * - نفس التزامات الشاشة السابقة: التخطي، لمس الشاشة، مهلة أمان قصوى، بدون صوت
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val closed = remember { mutableStateOf(false) }

    fun close() {
        if (closed.value) return
        closed.value = true
        onSplashFinished()
    }

    val glowAlpha = remember { Animatable(0f) }
    val bookAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val skipButtonAlpha = remember { Animatable(0f) }

    // 0f = مغلق .. 1f = مفتوح بالكامل
    val openProgress = remember { Animatable(0f) }
    // توهج خلفي غامق أثناء الإغلاق -> ذهبي مشع عند الاكتمال
    val revealGlow = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // 1. ظهور مرحلي ناعم للكتاب المغلق مع هالة أساسية
        launch {
            bookAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            glowAlpha.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }

        delay(250)

        // 2. فتح الكتاب من العمود الفقري
        launch {
            openProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1100, easing = FastOutSlowInEasing)
            )
        }

        // 3. الشعلة والآية تظهران أثناء اكتمال الفتح
        delay(450)
        launch {
            logoAlpha.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        }

        // 4. إشعاع الذهب عند الفتح الكامل
        delay(380)
        launch {
            revealGlow.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }

        // 5. ظهور زر التخطي بعد استقرار المشهد
        delay(350)
        launch {
            skipButtonAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }

        // 6. فترة عرض مستقرة
        delay(1100)

        // 7. ذوبان الخروج نحو الاستوديو
        exitAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        close()
    }

    // مهلة أمان قصوى — لا يُعلَّق التطبيق أبداً
    LaunchedEffect(Unit) {
        delay(4500)
        close()
    }

    val skipInteraction = remember { MutableInteractionSource() }

    // هوية قبس
    val voidBlack = Color(0xFF03060C)
    val deepNavy = Color(0xFF060D19)
    val goldPrimary = Color(0xFFE8C547)
    val goldLight = Color(0xFFFFF1A8)
    val goldWarm = Color(0xFFB88E28)
    val spineBase = Color(0xFF3A2E10)
    val invoice = Color(0xFFF8F1DD)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(deepNavy, voidBlack),
                    center = Offset(0.5f, 0.42f),
                    radius = 1600f
                )
            )
            .clickable(
                interactionSource = skipInteraction,
                indication = null
            ) { close() },
        contentAlignment = Alignment.Center
    ) {
        // 1. هالة ذهبية ضبابية خلف المشهد
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glowAlpha.value * exitAlpha.value * 0.85f + revealGlow.value * 0.35f
                }
        ) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.40f)
            val glowRadius = size.minDimension * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldLight.copy(alpha = 0.22f + revealGlow.value * 0.35f),
                        goldPrimary.copy(alpha = 0.12f + revealGlow.value * 0.25f),
                        goldWarm.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = centerOffset
            )
        }

        // 2. الكتاب المفتوح — نصفان يدوران حول العمود الفقري
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .graphicsLayer {
                    alpha = bookAlpha.value * exitAlpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            // ارسم النصفين جنباً إلى جنب داخل صندوق عرض واحد
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                val halfWidth = 0.46f

                // النصف الأيمن من الكتاب (غلاف فاخر داكن بحواف ذهبية)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(halfWidth)
                        .padding(horizontal = 2.dp)
                        .graphicsLayer {
                            val angle = -90f * (1f - openProgress.value)
                            rotationY = angle
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            cameraDistance = 36f * density
                            val backsideShown = openProgress.value in 0.4f..0.6f
                            alpha = if (backsideShown) 0.35f else 1f
                        }
                        .background(
                            Brush.linearGradient(
                                listOf(goldWarm, spineBase, goldWarm.copy(alpha = 0.7f))
                            ),
                            RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)
                        )
                        .border(1.5.dp, goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                ) {}

                // الصفحة اليمنى (ورقية فاتحة) — الجانب المعروض داخل الغلاف الأيمن
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(halfWidth)
                        .padding(horizontal = 2.dp)
                        .graphicsLayer {
                            val angle = -90f * (1f - openProgress.value)
                            rotationY = angle
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            cameraDistance = 40f * density
                        }
                        .background(
                            Brush.linearGradient(
                                listOf(invoice, Color(0xFFEFE6CC), Color(0xFFD9CBA4))
                            ),
                            RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        )
                        .border(0.5.dp, goldWarm.copy(alpha = 0.25f), RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "۞",
                        color = goldWarm,
                        fontSize = 18.sp,
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                // النصف الأيسر من الكتاب (غلاف)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(halfWidth)
                        .padding(horizontal = 2.dp)
                        .graphicsLayer {
                            val angle = 90f * (1f - openProgress.value)
                            rotationY = angle
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                            cameraDistance = 36f * density
                        }
                        .background(
                            Brush.linearGradient(
                                listOf(goldWarm, spineBase, goldWarm.copy(alpha = 0.7f))
                            ),
                            RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                        )
                        .border(1.5.dp, goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                ) {}

                // الصفحة اليسرى (ورقية)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(halfWidth)
                        .padding(horizontal = 2.dp)
                        .graphicsLayer {
                            val angle = 90f * (1f - openProgress.value)
                            rotationY = angle
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                            cameraDistance = 40f * density
                        }
                        .background(
                            Brush.linearGradient(
                                listOf(invoice, Color(0xFFEFE6CC), Color(0xFFD9CBA4))
                            ),
                            RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        )
                        .border(0.5.dp, goldWarm.copy(alpha = 0.25f), RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "﴾أَلَا بِذِكْرِ اللهِ تَطْمَئِنُّ الْقُلُوبُ﴿",
                        color = goldWarm.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // 3. محتوى الكتاب العلوي: شعلة قبس + شعار + الآية — يطفو كشفاً فوق الصفحات
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = logoAlpha.value * exitAlpha.value
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // شعلة قبس الذهبية المتوهجة (رمزية مرسومة بسيطة بدل صورة إن توفرت)
                    Text("✦", color = goldLight, fontSize = 42.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("قَـبَـسْ", color = goldPrimary, fontSize = 52.sp, fontFamily = AmiriFont, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "استوديو الإنتاج وصناعة الأثر",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontFamily = TajawalFont,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // 4. الآية السفلية داخل الكتاب
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value * exitAlpha.value * 0.92f
                        }
                ) {
                    Text(
                        text = "« ادْعُ إِلَىٰ سَبِيلِ رَبِّكَ بِالْحِكْمَةِ وَالْمَوْعِظَةِ الْحَسَنَةِ »",
                        color = goldPrimary.copy(alpha = 0.95f),
                        fontSize = 14.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // 5. زر التخطي أسفل الشاشة
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .graphicsLayer {
                    alpha = skipButtonAlpha.value * exitAlpha.value * 0.9f
                }
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = goldPrimary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    interactionSource = skipInteraction,
                    indication = null
                ) { close() }
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "تخطي",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontFamily = NotoSansFont,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "✦",
                    color = goldPrimary,
                    fontSize = 11.sp
                )
            }
        }
    }
}