package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MontageProject
import com.example.viewmodel.MontageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MontageViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val isGeminiLoading by viewModel.isGeminiLoading.collectAsState()
    val geminiError by viewModel.geminiError.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    // AI wizard state
    var aiThemePrompt by remember { mutableStateOf("") }
    var selectedMusicCategory by remember { mutableStateOf("SYNTHWAVE") }

    // Colors
    val bgDark = Color(0xFF0C0C0E)
    val cardDark = Color(0xFF16161A)
    val accentCyan = Color(0xFF00E5FF)
    val accentOrange = Color(0xFFFF8F00)
    val textLight = Color(0xFFE2E2E9)
    val textMuted = Color(0xFF8F8F9F)
    val borderCol = Color(0xFF2E2E38)

    // Force RTL for Arabic layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgDark)
                .padding(bottom = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                ,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CardDefaults.cardColors().containerColor, Color(0xFF1D2930))
                                )
                            )
                            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MovieFilter,
                                        contentDescription = "Studio Icon",
                                        tint = accentCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "استوديو المونتاج",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textLight,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "النسخة الذكية v2.0",
                                        color = accentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "قم بتلقيم أفكارك للذكاء الاصطناعي وبناء خطوط سينمائية كاملة وتصدير مخرجات بصرية ساحرة بلمسة واحدة.",
                                color = textMuted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // AI Storyboard Assistant (Gemini Wizard)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_generator_box"),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = RowDefaults.cardBorder(borderColor = accentOrange.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Wizard",
                                    tint = accentOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مساعد السيناريو والقصة بالذكاء الاصطناعي (Gemini)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textLight
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "اكتب الفكرة الأساسية للفيديو وسيقوم المولد الذكي بترتيب المشاهد وتطبيق الفلاتر والتحويلات وصياغة نصوص المشهد تلقائياً:",
                                fontSize = 13.sp,
                                color = textMuted,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = aiThemePrompt,
                                onValueChange = { aiThemePrompt = it },
                                placeholder = {
                                    Text(
                                        "مثال: رحلة برية مع الأصدقاء في الجبال، أو يوم عمل هادئ...",
                                        fontSize = 13.sp,
                                        color = textMuted
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_prompt_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentOrange,
                                    unfocusedBorderColor = borderCol,
                                    focusedTextColor = textLight,
                                    unfocusedTextColor = textLight
                                ),
                                maxLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "اختر الموسيقى الملحقة للمشاهد:",
                                fontSize = 13.sp,
                                color = textLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val musicOptions = listOf(
                                    Pair("SYNTHWAVE", "نبض المستبقل 🌌"),
                                    Pair("LOFI", "قهوة لوفاي ☕"),
                                    Pair("CINEMATIC", "تراك ملحمي 🎻"),
                                    Pair("NATURE", "صوت الغابة 🌲")
                                )
                                musicOptions.forEach { opt ->
                                    val isSelected = selectedMusicCategory == opt.first
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentOrange.copy(alpha = 0.2f) else borderCol.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) accentOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedMusicCategory = opt.first }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.second,
                                            fontSize = 11.sp,
                                            color = if (isSelected) textLight else textMuted,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (aiThemePrompt.isBlank()) {
                                        viewModel.generateAiStoryboard("مونتاج سريع عشوائي", selectedMusicCategory)
                                    } else {
                                        viewModel.generateAiStoryboard(aiThemePrompt, selectedMusicCategory)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_ai_storyboard"),
                                colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("صمم سيناريو المونتاج بالذكاء الاصطناعي", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            if (geminiError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = geminiError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Quick Tools Row & Project Title header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مشاريع المونتاج الحالية (${projects.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textLight
                        )

                        ElevatedButton(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = accentCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("create_new_project_btn")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشروع يدوي جديد", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Projects List items
                if (projects.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "No projects",
                                tint = textMuted,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا يوجد مشاريع حتى الآن",
                                fontSize = 16.sp,
                                color = textLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك البدء بإنشاء مشروع فارغ أو استخدام مولد الذكاء الاصطناعي السحري بالتعليمات بالأعلى.",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 30.dp)
                            )
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onOpen = { viewModel.navigateToEditor(project) },
                            onDelete = { viewModel.deleteProject(project.id) },
                            accentCyan = accentCyan,
                            textLight = textLight,
                            textMuted = textMuted,
                            cardDark = cardDark,
                            borderCol = borderCol
                        )
                    }
                }

                // Learn Academy Section / Tutorial guidelines
                item {
                    Text(
                        text = "أكاديمية صناعة المونتاج",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textLight,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AcademyCard(
                            title = "دليل المشاهد المتناسقة",
                            desc = "تعرّف على توقيت الانتقالات لجذب نظر المتابع.",
                            icon = Icons.Default.Edit,
                            accentColor = accentCyan,
                            cardDark = cardDark,
                            borderCol = borderCol,
                            textLight = textLight,
                            textMuted = textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        AcademyCard(
                            title = "مزامنة الصوت والصورة",
                            desc = "اجعل ذروة الضربة الموسيقية تتزامن مع تغيير الفيلتر.",
                            icon = Icons.Default.MusicVideo,
                            accentColor = accentOrange,
                            cardDark = cardDark,
                            borderCol = borderCol,
                            textLight = textLight,
                            textMuted = textMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // High Fidelity loading overlay for Gemini AI Storyboarding
            if (isGeminiLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accentOrange,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "جاري تفعيل ذكاء المخرج السينمائي...",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "يقوم Gemini بتحليل الكلمات المفتاحية لمشروعك، وابتكار قائمة مشاهد مترابطة مع صياغة نصوص الترجمة العربية المتناسقة وتطبيق فلاتر دافئة...",
                            fontSize = 13.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Create New Project Dialog modal
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newProjectName.isNotBlank()) {
                                    viewModel.createAndLoadNewProject(newProjectName)
                                    newProjectName = ""
                                    showCreateDialog = false
                                }
                            }
                        ) {
                            Text("إنشاء وابدأ التعديل", color = accentCyan, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("إلغاء", color = textMuted)
                        }
                    },
                    title = {
                        Text("مشروع مونتاج يدوي جديد", color = textLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    text = {
                        Column {
                            Text("أدخل اسماً لمشروعك الجديد لتميزه في قائمة المعالجة:", color = textMuted, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newProjectName,
                                onValueChange = { newProjectName = it },
                                placeholder = { Text("مثال: فلوغ السفر الاسبوعي", fontSize = 13.sp, color = textMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("project_name_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentCyan,
                                    unfocusedBorderColor = borderCol,
                                    focusedTextColor = textLight,
                                    unfocusedTextColor = textLight
                                ),
                                maxLines = 1,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    },
                    containerColor = cardDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProjectItemCard(
    project: MontageProject,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    cardDark: Color,
    borderCol: Color
) {
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val dateStr = formatter.format(Date(project.createdAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardDark)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (project.isExported) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "تصدير ناجح ✨",
                                color = accentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(textMuted.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "قيد التصميم ✏️",
                                color = textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, size = 14.dp, tint = textMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${project.clips.size} لقطات", color = textMuted, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, size = 14.dp, tint = textMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${project.clips.sumOf { it.durationSeconds.toDouble() }} ثانية", color = textMuted, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, size = 12.dp, tint = textMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, color = textMuted, fontSize = 11.sp)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_btn_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete project",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AcademyCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    cardDark: Color,
    borderCol: Color,
    textLight: Color,
    textMuted: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderCol, borderCol)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textLight)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, fontSize = 11.sp, color = textMuted, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}

object RowDefaults {
    @Composable
    fun cardBorder(borderColor: Color) = CardDefaults.outlinedCardBorder().copy(
        brush = Brush.linearGradient(listOf(borderColor, borderColor))
    )
}
