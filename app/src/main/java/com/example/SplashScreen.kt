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
 * شاشة البداية الرسمية لاستوديو قبس (Official Qabas Studio Splash Screen)
 * - هوية قبس الرسمية الكاملة: الشعلة الذهبية المتوهجة فوق الكتاب المفتوح + قبس + الآية الكريمة
 * - أنيميشن سينمائي خفيف وفائق السلاسة (Fade + Scale + Soft Ambient Glow)
 * - بدون أي تشغيل صوتي
 * - دعم التخطي السريع ولمس الشاشة مع مهلة أمان قصوى
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

    // Animation Controllers (Lightweight, GPU-friendly Animatable properties)
    val glowAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.92f) }
    val exitAlpha = remember { Animatable(1f) }
    val skipButtonAlpha = remember { Animatable(0f) }

    // Controlled Timeline Choreography
    LaunchedEffect(Unit) {
        // 1. Soft ambient glow emergence
        launch {
            glowAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }

        // 2. Cinematic Logo Entry (Smooth Spring + Fade)
        delay(150)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // 3. Reveal Skip button after logo stabilizes
        delay(400)
        launch {
            skipButtonAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // 4. Steady presentation hold (~1.5s)
        delay(1500)

        // 5. Smooth dissolve exit into Studio
        exitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        close()
    }

    // Safety timeout fallback (never get stuck)
    LaunchedEffect(Unit) {
        delay(4500)
        close()
    }

    val skipInteraction = remember { MutableInteractionSource() }

    // Brand Palette
    val voidBlack = Color(0xFF03060C)
    val deepNavy = Color(0xFF060D19)
    val goldPrimary = Color(0xFFE8C547)
    val goldLight = Color(0xFFFFF1A8)
    val goldWarm = Color(0xFFB88E28)

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
        // 1. Gentle Ambient Golden Halo Glow (Behind Logo)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glowAlpha.value * exitAlpha.value * 0.85f
                }
        ) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.40f)
            val glowRadius = size.minDimension * 0.55f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldLight.copy(alpha = 0.22f),
                        goldPrimary.copy(alpha = 0.12f),
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

        // 2. Main Center Stage: Official Qabas Visual Identity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = logoAlpha.value * exitAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            // Attempt to load the official identity artwork (R.drawable.qabas_logo)
            val logoPainter = runCatching {
                painterResource(id = R.drawable.qabas_logo)
            }.getOrNull()

            if (logoPainter != null) {
                Image(
                    painter = logoPainter,
                    contentDescription = "شعار قبس الرسمي",
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.92f),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Graceful fallback if resource is unavailable: Elegant Calligraphy & Verse
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✦",
                        color = goldLight,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "قَـبَـسْ",
                        color = goldPrimary,
                        fontSize = 54.sp,
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "استوديو الإنتاج وصناعة الأثر",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontFamily = TajawalFont,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "« ادْعُ إِلَىٰ سَبِيلِ رَبِّكَ بِالْحِكْمَةِ وَالْمَوْعِظَةِ الْحَسَنَةِ »",
                        color = goldPrimary.copy(alpha = 0.95f),
                        fontSize = 15.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Elegant Skip Pill at Bottom
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
