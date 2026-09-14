package com.qabas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionPipelineSection(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val events = remember { mutableStateOf(ProductionPipelineTracker.getEvents(context)) }
    val stats = remember(events.value) {
        ProductionPipelineTracker.getStageStats(context)
    }
    var selectedStage by remember { mutableStateOf<ProductionPipelineTracker.Stage?>(null) }

    val filteredEvents = if (selectedStage != null) events.value.filter { it.stage == selectedStage } else events.value

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "مسار الإنتاج 🎬",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFont
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                }
            },
            actions = {
                IconButton(onClick = {
                    ProductionPipelineTracker.clearEvents(context)
                    events.value = emptyList()
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح السجل", tint = Color(0xFFEF4444))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Summary Cards ──
            item {
                val totalSuccess = events.value.count { it.result == ProductionPipelineTracker.Result.SUCCESS }
                val totalFailure = events.value.count { it.result == ProductionPipelineTracker.Result.FAILURE }
                val totalFallback = events.value.count { it.result == ProductionPipelineTracker.Result.FALLBACK }
                val totalTimeout = events.value.count { it.result == ProductionPipelineTracker.Result.TIMEOUT }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMiniCard("✅", "$totalSuccess", "نجاح", Color(0xFF10B981), Modifier.weight(1f))
                    SummaryMiniCard("❌", "$totalFailure", "فشل", Color(0xFFEF4444), Modifier.weight(1f))
                    SummaryMiniCard("⚠️", "$totalFallback", "بديل", Color(0xFFF59E0B), Modifier.weight(1f))
                    SummaryMiniCard("⏱️", "$totalTimeout", "مهلة", Color(0xFF8B5CF6), Modifier.weight(1f))
                }
            }

            // ── Stage Filter Chips ──
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "تصفية حسب المرحلة:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = NotoSansFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStage == null,
                        onClick = { selectedStage = null },
                        label = { Text("الكل", fontSize = 10.sp, fontFamily = CairoFont) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = GoldPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Stage chips in rows of 3
                val stages = ProductionPipelineTracker.Stage.entries.toList()
                stages.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { stage ->
                            val stageEvents = stats[stage]
                            val hasData = stageEvents != null && stageEvents.values.sum() > 0
                            FilterChip(
                                selected = selectedStage == stage,
                                onClick = { selectedStage = if (selectedStage == stage) null else stage },
                                label = {
                                    Text(
                                        "${stage.label} ${stageEvents?.let { "(${it.values.sum()})" } ?: ""}",
                                        fontSize = 9.sp,
                                        fontFamily = CairoFont,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = GoldPrimary,
                                    disabledContainerColor = if (hasData) Color(0xFF1E293B) else Color.Transparent,
                                    disabledLabelColor = if (hasData) TextSecondary else Color.Gray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if row is not full
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Stage Success Rate Summary ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "معدل النجاح لكل مرحلة:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = NotoSansFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(ProductionPipelineTracker.Stage.entries.toList()) { stage ->
                val stageStats = stats[stage]
                val total = stageStats?.values?.sum() ?: 0
                val success = stageStats?.get(ProductionPipelineTracker.Result.SUCCESS) ?: 0
                val rate = if (total > 0) (success.toFloat() / total * 100).toInt() else 0

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stage.label,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = CairoFont,
                        modifier = Modifier.weight(1f)
                    )
                    if (total > 0) {
                        Box(
                            modifier = Modifier.width(100.dp).height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(rate / 100f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            rate >= 80 -> Color(0xFF10B981)
                                            rate >= 50 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$rate%",
                            color = when {
                                rate >= 80 -> Color(0xFF10B981)
                                rate >= 50 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NotoSansFont
                        )
                    } else {
                        Text("—", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            // ── Event Log ──
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "سجل الأحداث (${filteredEvents.size}):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = NotoSansFont,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "لا توجد أحداث بعد",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = CairoFont,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "ابدأ إنتاج فيديو لعرض سجل مسار الإنتاج هنا",
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontFamily = NotoSansFont,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(filteredEvents.take(50)) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D1320)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        Color(ProductionPipelineTracker.resultColor(event.result)).copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ProductionPipelineTracker.resultEmoji(event.result),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    event.stage.label,
                                    color = Color(ProductionPipelineTracker.resultColor(event.result)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoFont
                                )
                            }
                            Text(
                                ProductionPipelineTracker.formatTimestamp(event.timestamp),
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = NotoSansFont
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            event.message,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = CairoFont,
                            lineHeight = 16.sp
                        )
                        if (event.detail.isNotBlank()) {
                            Text(
                                event.detail,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = NotoSansFont,
                                lineHeight = 14.sp
                            )
                        }
                        if (event.durationMs > 0) {
                            Text(
                                "الوقت: ${formatDuration(event.durationMs)}",
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontFamily = NotoSansFont
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryMiniCard(emoji: String, count: String, label: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(count, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = NotoSansFont)
            Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = CairoFont)
        }
    }
}

private fun formatDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> "${ms / 1000}ث"
        ms < 3600_000 -> "${ms / 60000}د ${ms % 60000 / 1000}ث"
        else -> "${ms / 3600000}س ${(ms % 3600000) / 60000}د"
    }
}
