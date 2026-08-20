package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<ProjectService.Project>,
    onBack: () -> Unit,
    onOpenProject: (ProjectService.Project) -> Unit,
    onDeleteProject: (String) -> Unit,
    onViewSettings: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var projectToDelete by remember { mutableStateOf<ProjectService.Project?>(null) }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = {
                Text(
                    Translator.tr("حذف المشروع"),
                    color = GoldPrimary,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    Translator.tr("هل أنت متأكد من حذف مشروع: \"${projectToDelete?.title}\"؟ لا يمكن التراجع عن هذا الإجراء."),
                    color = Color.White,
                    fontFamily = NotoSansFont
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it.id) }
                        projectToDelete = null
                    }
                ) {
                    Text(Translator.tr("حذف نهائي"), color = Color(0xFFEF4444), fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text(Translator.tr("إلغاء"), color = TextSecondary, fontFamily = CairoFont)
                }
            },
            containerColor = CardSurface
        )
    }

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            Translator.tr("مشاريعي"),
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (projects.isEmpty()) Translator.tr("لا توجد مشاريع") else "${projects.size} " + Translator.tr("مشاريع محفوظة"),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = NotoSansFont
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Translator.tr("العودة"),
                            tint = GoldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(DeepSlate)) {
            if (projects.isEmpty()) {
                QabasEmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = Translator.tr("لا توجد مشاريع سابقة"),
                    description = Translator.tr("ابدأ الآن بتحويل أفكارك الدعوية إلى مقاطع سينمائية مؤثرة باستخدام الذكاء الاصطناعي."),
                    actionButtonText = Translator.tr("إنشاء فيديو جديد ✨"),
                    onActionClick = onBack
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        count = projects.size,
                        key = { index -> projects[index].id.ifEmpty { index.toString() } }
                    ) { index ->
                        val project = projects[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProject(project) }
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                                .size(36.dp)
                                                .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Movie,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            project.title.ifBlank { Translator.tr("مشروع بدون عنوان") },
                                            color = GoldPrimary,
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { projectToDelete = project }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = Translator.tr("حذف"),
                                            tint = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    project.idea.takeIf { it.isNotBlank() } ?: Translator.tr("بدون فكرة مدخلة"),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontFamily = NotoSansFont,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                project.status.ifBlank { Translator.tr("مسودة") },
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val date = if (project.updatedAt > 0) {
                                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(project.updatedAt))
                                            } else {
                                                Translator.tr("اليوم")
                                            }
                                            Text(date, color = TextSecondary, fontSize = 12.sp, fontFamily = NotoSansFont)
                                        }
                                    }

                                    // Direct Resume / Open Button
                                    Surface(
                                        onClick = { onOpenProject(project) },
                                        color = GoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                Translator.tr("متابعة العمل"),
                                                color = GoldPrimary,
                                                fontSize = 11.sp,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.Bold
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
}
