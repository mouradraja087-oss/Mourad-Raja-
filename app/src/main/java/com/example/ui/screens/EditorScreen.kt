package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MontageProject
import com.example.data.model.ClipItem
import com.example.data.model.FilterStyle
import com.example.data.model.TransitionType
import com.example.viewmodel.MontageViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MontageViewModel,
    modifier: Modifier = Modifier
) {
    val projectState by viewModel.activeProject.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val timelineSeconds by viewModel.currentTimelineSeconds.collectAsState()
    val equalizerWaves by viewModel.equalizerWaves.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val project = projectState ?: return

    var activeTab by remember { mutableStateOf(0) } // 0: Text, 1: Filters, 2: Transitions, 3: Options
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddClipSheet by remember { mutableStateOf(false) }

    // Settings state holders
    var projectSettingsName by remember(project.id) { mutableStateOf(project.name) }
    var projectSettingsTrack by remember(project.id) { mutableStateOf(project.musicTrackId) }
    var projectSettingsVolume by remember(project.id) { mutableStateOf(project.musicVolume) }
    var projectSettingsRatio by remember(project.id) { mutableStateOf(project.aspectRatio) }

    // Export properties state holders
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("MP4") }
    var exportResolution by remember { mutableStateOf("1080p") }
    var exportFrameRate by remember { mutableStateOf("30 FPS") }

    val bgDark = Color(0xFF0C0C0E)
    val cardDark = Color(0xFF16161A)
    val accentCyan = Color(0xFF00E5FF)
    val accentOrange = Color(0xFFFF8F00)
    val textLight = Color(0xFFE2E2E9)
    val textMuted = Color(0xFF8F8F9F)
    val borderCol = Color(0xFF25252B)

    // Force RTL layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateToDashboard() },
                            modifier = Modifier.testTag("back_to_dashboard")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textLight)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = project.name,
                                color = textLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "أبعاد الفيديو: ${project.aspectRatio} | ${project.clips.size} عناصر",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("open_project_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textLight)
                        }

                        Button(
                            onClick = { showExportFormatDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("export_video_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, size = 18.dp, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رندر وتصدير", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Main Playback Renderer & Canvas frame Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Compute aspect ratios
                    val aspectBrush = when (project.aspectRatio) {
                        "16:9" -> Modifier.aspectRatio(1.77f)
                        "9:16" -> Modifier.aspectRatio(0.56f)
                        "1:1" -> Modifier.aspectRatio(1.0f)
                        else -> Modifier.aspectRatio(1.77f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .then(aspectBrush)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val renderState = viewModel.getTimelineClipRenderingState()
                        val timelineSecondsFloat = timelineSeconds

                        // Realtime Canvas compositing renderer!
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("montage_preview_canvas")
                        ) {
                            val w = size.width
                            val h = size.height

                            clipRect {
                                if (renderState.currentClip != null) {
                                    if (renderState.nextClip != null && renderState.transitionStyle != TransitionType.NONE) {
                                        // We are in transition zone! Let's double layer make transition render.
                                        val progress = renderState.transitionProgressPercent
                                        when (renderState.transitionStyle) {
                                            TransitionType.CROSSFADE -> {
                                                // Crossfade Alpha blend
                                                drawClipProceduralAnimation(renderState.currentClip, renderState.currentClipProgressPercent, timelineSecondsFloat, 1f - progress, w, h)
                                                drawClipProceduralAnimation(renderState.nextClip, progress * 0.2f, timelineSecondsFloat, progress, w, h)
                                            }
                                            TransitionType.SLIDE_LEFT -> {
                                                // Slide side-by-side transition
                                                val shiftX = w * progress
                                                withTransform({
                                                    translate(left = -shiftX)
                                                }) {
                                                    drawClipProceduralAnimation(renderState.currentClip, renderState.currentClipProgressPercent, timelineSecondsFloat, 1f, w, h)
                                                }
                                                withTransform({
                                                    translate(left = w - shiftX)
                                                }) {
                                                    drawClipProceduralAnimation(renderState.nextClip, progress * 0.2f, timelineSecondsFloat, 1f, w, h)
                                                }
                                            }
                                            TransitionType.ZOOM_IN -> {
                                                // Zoom scaling transitions
                                                val scaleOut = 1f + progress * 0.4f
                                                val alphaOut = (1f - progress).coerceIn(0f, 1f)
                                                withTransform({
                                                    scale(scaleOut, scaleOut, pivot = Offset(w/2, h/2))
                                                }) {
                                                    drawClipProceduralAnimation(renderState.currentClip, renderState.currentClipProgressPercent, timelineSecondsFloat, alphaOut, w, h)
                                                }

                                                val scaleIn = 0.5f + progress * 0.5f
                                                withTransform({
                                                    scale(scaleIn, scaleIn, pivot = Offset(w/2, h/2))
                                                }) {
                                                    drawClipProceduralAnimation(renderState.nextClip, progress * 0.2f, timelineSecondsFloat, progress, w, h)
                                                }
                                            }
                                            else -> {
                                                drawClipProceduralAnimation(renderState.currentClip, renderState.currentClipProgressPercent, timelineSecondsFloat, 1f, w, h)
                                            }
                                        }
                                    } else {
                                        // Single regular clip rendering, apply Ken Burns if enabled
                                        val kbEnabled = renderState.currentClip.isKenBurnsEnabled
                                        val scale = if (kbEnabled) 1f + (renderState.currentClipProgressPercent * 0.22f) else 1f
                                        val transX = if (kbEnabled) (renderState.currentClipProgressPercent * w * 0.04f) else 0f
                                        val transY = if (kbEnabled) (renderState.currentClipProgressPercent * h * 0.02f) else 0f

                                        withTransform({
                                            scale(scale, scale, pivot = Offset(w/2, h/2))
                                            translate(transX, transY)
                                        }) {
                                            drawClipProceduralAnimation(
                                                clip = renderState.currentClip,
                                                progressPercent = renderState.currentClipProgressPercent,
                                                globalTime = timelineSecondsFloat,
                                                alphaMultiplier = 1f,
                                                width = w,
                                                height = h
                                            )
                                        }
                                    }
                                } else {
                                    // Empty state inside player
                                    drawRect(Color(0xFF101013))
                                }
                            }
                        }

                        // Subtitle overlay block
                        renderState.currentClip?.let { activeClip ->
                            if (activeClip.subtitleText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                                        .shadow(4.dp, RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val textCol = when (activeClip.subtitleStyle) {
                                        "Neon" -> accentCyan
                                        "Impact" -> Color(0xFFFFEB3B)
                                        else -> textLight
                                    }
                                    val textWeight = when (activeClip.subtitleStyle) {
                                        "Impact" -> FontWeight.Black
                                        else -> FontWeight.Bold
                                    }
                                    val fontSizeVal = when (activeClip.subtitleStyle) {
                                        "Impact" -> 19.sp
                                        "Neon" -> 16.sp
                                        else -> 15.sp
                                    }

                                    Text(
                                        text = activeClip.subtitleText,
                                        color = textCol,
                                        fontSize = fontSizeVal,
                                        fontWeight = textWeight,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }

                        // Top camera layout details overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color.Red else textMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) "رندر مباشر" else "معاينة موقوفة",
                                    color = textLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "REC [1080p - 60FPS]",
                                color = textLight.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Audio track visual wave synthesis panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardDark.copy(alpha = 0.7f))
                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, size = 16.dp, tint = accentCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = getMusicName(project.musicTrackId),
                                    color = textLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "مستوى الصوت: ${(project.musicVolume * 100).toInt()}%",
                                color = textMuted,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Audio waves bouncing columns container using Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        ) {
                            val space = 4.dp.toPx()
                            val barWidth = (size.width - (equalizerWaves.size - 1) * space) / equalizerWaves.size
                            equalizerWaves.forEachIndexed { i, amp ->
                                val barHeight = size.height * amp
                                val x = i * (barWidth + space)
                                val y = (size.height - barHeight) / 2
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(accentCyan, accentCyan.copy(alpha = 0.3f))
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Timeline Playback controllers & seeking slider slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.togglePlay() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accentCyan)
                                    .testTag("timeline_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format("%.1f", timelineSeconds),
                                color = accentCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = " / " + String.format("%.1f", viewModel.getTotalDuration()) + " ثانية",
                                color = textMuted,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.seekToPosition(0f) }
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "Restart", tint = textLight, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = timelineSeconds,
                        onValueChange = { viewModel.seekToPosition(it) },
                        valueRange = 0f..viewModel.getTotalDuration().coerceAtLeast(0.1f),
                        colors = SliderDefaults.colors(
                            thumbColor = accentCyan,
                            activeTrackColor = accentCyan,
                            inactiveTrackColor = borderCol
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timeline_seeker")
                    )
                }

                // Horizontal list of clip elements timeline
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مسار اللقطات الزمني (${project.clips.size})", color = textLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        IconButton(
                            onClick = { showAddClipSheet = true },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("add_clip_to_timeline")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Clip", tint = accentCyan, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Clips row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(project.clips) { index, clip ->
                            val isSelected = clip.id == selectedClipId
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentOrange.copy(alpha = 0.25f) else cardDark)
                                    .border(2.dp, if (isSelected) accentOrange else borderCol, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectClip(clip.id) }
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(getClipColor(clip.presetType).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getClipPresetIcon(clip.presetType),
                                            contentDescription = null,
                                            tint = getClipColor(clip.presetType),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = clip.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textLight,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${clip.durationSeconds}ث",
                                        fontSize = 10.sp,
                                        color = textMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Workspace Inspector (Properties and editing parameters edit dashboard)
                val selectedClip = project.clips.find { it.id == selectedClipId }
                if (selectedClip != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .padding(horizontal = 16.dp)
                            .testTag("clip_inspector_panel"),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderCol, borderCol))),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Column {
                            // Tab Rows selectors for properties
                            TabRow(
                                selectedTabIndex = activeTab,
                                containerColor = Color.Transparent,
                                contentColor = textLight
                            ) {
                                val tabs = listOf("نص وترجمة 📝", "الفلاتر 🎨", "الانتقالات 🎬", "التحكم ⚙️")
                                tabs.forEachIndexed { i, title ->
                                    val tabSelected = activeTab == i
                                    Tab(
                                        selected = tabSelected,
                                        onClick = { activeTab = i },
                                        text = { Text(title, fontSize = 12.sp, fontWeight = if (tabSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            // Active tab panel content
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                when (activeTab) {
                                    0 -> InspectorTextTab(selectedClip, viewModel, accentOrange, borderCol, textLight, textMuted)
                                    1 -> InspectorFiltersTab(selectedClip, viewModel, accentOrange, borderCol, textLight, textMuted)
                                    2 -> InspectorTransitionsTab(selectedClip, viewModel, accentOrange, borderCol, textLight, textMuted)
                                    3 -> InspectorControlTab(selectedClip, viewModel, accentOrange, borderCol, textLight, textMuted)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("الرجاء إضافة لقطة أو اختيار لقطة من خط الزمن لتعديلها.", color = textMuted, fontSize = 13.sp)
                    }
                }
            }

            // Custom Dialog/Modals implementation
            // 1. Project global Settings Dialog (ASPECT RATIO / SOUNDTRACK / NAME)
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateProjectSettings(
                                    name = projectSettingsName,
                                    trackId = projectSettingsTrack,
                                    volume = projectSettingsVolume,
                                    aspectRatio = projectSettingsRatio
                                )
                                showSettingsDialog = false
                            }
                        ) {
                            Text("تطبيق التعديلات", color = accentCyan, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("إلغاء", color = textMuted)
                        }
                    },
                    title = {
                        Text("إعدادات رندر وتصدير المشروع", color = textLight, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Title edit
                            Column {
                                Text("اسم المونتاج:", color = textLight, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = projectSettingsName,
                                    onValueChange = { projectSettingsName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentCyan,
                                        unfocusedBorderColor = borderCol,
                                        focusedTextColor = textLight,
                                        unfocusedTextColor = textLight
                                    ),
                                    singleLine = true
                                )
                            }

                            // Aspect Ratio select
                            Column {
                                Text("توجيه شاشة العرض (Aspect Ratio):", color = textLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val formats = listOf("16:9" to "أفقي (تلفاز)", "9:16" to "عمودي (تيك توك)", "1:1" to "مربع (انستغرام)")
                                    formats.forEach { form ->
                                        val selected = projectSettingsRatio == form.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) accentCyan.copy(alpha = 0.2f) else borderCol)
                                                .border(1.dp, if (selected) accentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { projectSettingsRatio = form.first }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(form.first, color = textLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(form.second, color = textMuted, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Soundtrack select
                            Column {
                                Text("الموسيقار الخلفي المصاحب:", color = textLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val tracks = listOf("m1" to "سينمائي ملحمي", "m2" to "سايبر غلو", "m3" to "لوفاي هادئ", "m4" to "صوت الطبيعة")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tracks.forEach { track ->
                                        val selected = projectSettingsTrack == track.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) accentCyan.copy(alpha = 0.2f) else borderCol)
                                                .border(1.dp, if (selected) accentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { projectSettingsTrack = track.first }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(track.second, color = textLight, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            // Volume slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("حجم صوت الموسيقى:", color = textLight, fontSize = 12.sp)
                                    Text("${(projectSettingsVolume * 100).toInt()}%", color = accentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = projectSettingsVolume,
                                    onValueChange = { projectSettingsVolume = it },
                                    colors = SliderDefaults.colors(thumbColor = accentCyan, activeTrackColor = accentCyan)
                                )
                            }
                        }
                    },
                    containerColor = cardDark
                )
            }

            // 2. Choose export resolution / frame rates modal dialog
            if (showExportFormatDialog) {
                AlertDialog(
                    onDismissRequest = { showExportFormatDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExportFormatDialog = false
                                viewModel.startExportSimulation(exportResolution, exportFormat, exportFrameRate)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                        ) {
                            Text("تأكيد رندر وتصدير المونتاج", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportFormatDialog = false }) {
                            Text("إلغاء", color = textMuted)
                        }
                    },
                    title = {
                        Text("خيارات المعالجة والترميز النهائي", color = textLight, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // File format
                            Column {
                                Text("صيغة التصدير المستهدفة:", color = textLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("MP4", "MKV", "MOV").forEach { f ->
                                        val active = exportFormat == f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) accentCyan.copy(alpha = 0.2f) else borderCol)
                                                .border(1.dp, if (active) accentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { exportFormat = f }
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(f, color = textLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Resolution preset
                            Column {
                                Text("دقة الإخراج البصري والمخرجات:", color = textLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("1080p", "4K (فائق)", "720p (سريع)").forEach { res ->
                                        val active = exportResolution == res
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) accentCyan.copy(alpha = 0.2f) else borderCol)
                                                .border(1.dp, if (active) accentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { exportResolution = res }
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(res, color = textLight, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }

                            // FPS frame rates selector
                            Column {
                                Text("معدل تدفق الإطارات:", color = textLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("30 FPS", "60 FPS (سلس)", "24 FPS (سينمائي)").forEach { fps ->
                                        val active = exportFrameRate == fps
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) accentCyan.copy(alpha = 0.2f) else borderCol)
                                                .border(1.dp, if (active) accentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { exportFrameRate = fps }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(fps, color = textLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = cardDark
                )
            }

            // 3. Quick sheet helper to add clips to timeline
            if (showAddClipSheet) {
                AlertDialog(
                    onDismissRequest = { showAddClipSheet = false },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAddClipSheet = false }) {
                            Text("إلغاء", color = accentOrange)
                        }
                    },
                    title = {
                        Text("إضافة مشهد جديد لخط المونتاج", color = textLight, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text("اختر المظهر البصري لطبقة اللقطة المضافة:", color = textMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            val scenePresets = listOf(
                                Triple("NATURE", "🌲 جلال الطبيعة وصوت السكون", Color(0xFF4CAF50)),
                                Triple("NEON", "🌌 الممرات العصرية الضوئية", Color(0xFF00E5FF)),
                                Triple("VINTAGE", "📽️ ذكريات قديمة مغبرّة عتيقة", Color(0xFF8D6E63)),
                                Triple("SPACE", "🚀 رحلة الفضاء والنجوم الساطعة", Color(0xFF9575CD)),
                                Triple("CYBERPUNK", "⚡ نمط سايبر المستقبلي المتراقص", Color(0xFFFF4081)),
                                Triple("OCEAN", "🌊 نسيم البحر وموج المحيط الأزرق", Color(0xFF29B6F6))
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                scenePresets.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(borderCol.copy(alpha = 0.6f))
                                            .clickable {
                                                viewModel.addClipToActive(preset.first)
                                                showAddClipSheet = false
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.third.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(getClipPresetIcon(preset.first), contentDescription = null, size = 16.dp, tint = preset.third)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(preset.second, color = textLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = cardDark
                )
            }

            // 4. Export progress compiler modal rendering overlay!
            if (isExporting) {
                ExportModalScreen(viewModel, accentCyan, textLight, textMuted, cardDark, borderCol)
            }
        }
    }
}

// Sub-components: Tab views inside inspector Card
@Composable
fun InspectorTextTab(
    clip: ClipItem,
    viewModel: MontageViewModel,
    accentColor: Color,
    borderCol: Color,
    textLight: Color,
    textMuted: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("كتابة نصوص الترجمة المرافقة (Subtitles):", color = textLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = clip.subtitleText,
            onValueChange = { viewModel.updateClipInActive(clip.copy(subtitleText = it)) },
            placeholder = { Text("مثال: في هدوء الطبيعة تكتمل السكينة...", color = textMuted, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clip_subtitle_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderCol,
                focusedTextColor = textLight,
                unfocusedTextColor = textLight
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text("تصميم وتلوين خط النص بأساليب ممتازة:", color = textLight, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val subtitleStyles = listOf("Elegant" to "كلاسيكي هادئ 📖", "Impact" to "تأثير السوشال ⚠️", "Neon" to "توهج نيون مستقبلي 🌌")
            subtitleStyles.forEach { style ->
                val active = clip.subtitleStyle == style.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accentColor.copy(alpha = 0.15f) else borderCol.copy(alpha = 0.6f))
                        .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { viewModel.updateClipInActive(clip.copy(subtitleStyle = style.first)) }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(style.second, color = textLight, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun InspectorFiltersTab(
    clip: ClipItem,
    viewModel: MontageViewModel,
    accentColor: Color,
    borderCol: Color,
    textLight: Color,
    textMuted: Color
) {
    Column {
        Text("تطبيق مرشحات الألوان التخليقية (Filters):", color = textLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        val filters = listOf(
            FilterStyle.ORIGINAL to "الأصل الطبيعي 🌈",
            FilterStyle.GRAYSCALE to "سينما كلاسيك 📽️",
            FilterStyle.SEPIA to "حرارة الذكريات 🍂",
            FilterStyle.WARM_SUNSET to "غروب شمس دافئ 🌅",
            FilterStyle.CYBERPUNK to "سايبر نيون متوهج ⚡",
            FilterStyle.VINTAGE_GRAIN to "رول قديم تالف 🎥"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Divide into two groups or small boxes
            val firstHalf = filters.take(3)
            val secondHalf = filters.takeLast(3)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                firstHalf.forEach { f ->
                    val active = clip.filterStyle == f.first
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) accentColor.copy(alpha = 0.15f) else borderCol.copy(alpha = 0.6f))
                            .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateClipInActive(clip.copy(filterStyle = f.first)) }
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(f.second, color = textLight, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                secondHalf.forEach { f ->
                    val active = clip.filterStyle == f.first
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) accentColor.copy(alpha = 0.15f) else borderCol.copy(alpha = 0.6f))
                            .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateClipInActive(clip.copy(filterStyle = f.first)) }
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(f.second, color = textLight, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun InspectorTransitionsTab(
    clip: ClipItem,
    viewModel: MontageViewModel,
    accentColor: Color,
    borderCol: Color,
    textLight: Color,
    textMuted: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("طريقة الانتقال إلى اللقطة اللاحقة (Transitions):", color = textLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        val trans = listOf(
            TransitionType.NONE to "قطع مفاجئ حاسم ✂️",
            TransitionType.CROSSFADE to "تلاشي تدريجي مدمج 🌫️",
            TransitionType.SLIDE_LEFT to "إزاحة بصرية من الجانب ↔️",
            TransitionType.ZOOM_IN to "تقريب بؤري سينمائي 🔍"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            trans.forEach { t ->
                val active = clip.transitionType == t.first
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accentColor.copy(alpha = 0.15f) else borderCol.copy(alpha = 0.6f))
                        .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { viewModel.updateClipInActive(clip.copy(transitionType = t.first)) }
                        .padding(12.dp)
                ) {
                    Text(t.second, color = textLight, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun InspectorControlTab(
    clip: ClipItem,
    viewModel: MontageViewModel,
    accentColor: Color,
    borderCol: Color,
    textLight: Color,
    textMuted: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Duration slider adjusting
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مدة ديمومة وعرض اللقطة:", color = textLight, fontSize = 12.sp)
                Text("${clip.durationSeconds} ثانية", color = accentColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = clip.durationSeconds,
                onValueChange = { viewModel.updateClipInActive(clip.copy(durationSeconds = Math.round(it * 2f) / 2f)) },
                valueRange = 1.0f..15.0f,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )
        }

        // Toggle Ken Burns motion zoom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(borderCol.copy(alpha = 0.4f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("حركة تقريب بؤري ذاتي (Ken Burns):", color = textLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("حركة دوران وتقريب ديناميكي تضفي طابعاً تفاعلياً.", color = textMuted, fontSize = 10.sp)
            }
            Switch(
                checked = clip.isKenBurnsEnabled,
                onCheckedChange = { viewModel.updateClipInActive(clip.copy(isKenBurnsEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.4f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Delete Clip Action
        Button(
            onClick = { viewModel.deleteClipFromActive(clip.id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_clip_btn")
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, size = 18.dp, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("احذف هذه اللقطة من خط المونتاج", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// Procedural visual components generator inside Canvas
fun DrawScope.drawClipProceduralAnimation(
    clip: ClipItem,
    progressPercent: Float,
    globalTime: Float,
    alphaMultiplier: Float,
    width: Float,
    height: Float
) {
    // Determine active colors based on filter values
    val originalColors = when (clip.presetType) {
        "NATURE" -> listOf(Color(0xFF2E7D32), Color(0xFF81C784), Color(0xFFFB8C00))
        "NEON" -> listOf(Color(0xFF1A1A24), Color(0xFF00E5FF), Color(0xFFE040FB))
        "VINTAGE" -> listOf(Color(0xFF4E342E), Color(0xFFD7CCC8), Color(0xFFF57C00))
        "SPACE" -> listOf(Color(0xFF0D0D1B), Color(0xFFECEFF1), Color(0xFF90CAF9))
        "CYBERPUNK" -> listOf(Color(0xFF21102A), Color(0xFFFF007F), Color(0xFF00FFFF))
        "OCEAN" -> listOf(Color(0xFF01579B), Color(0xFF29B6F6), Color(0xFFE0F7FA))
        else -> listOf(Color(0xFF1E1E24), Color(0xFF8E8E93))
    }

    // Transform colors with active Filters
    val processedColors = originalColors.map { col ->
        val tinted = applyFilterToColor(col, clip.filterStyle)
        tinted.copy(alpha = tinted.alpha * alphaMultiplier)
    }

    val primaryCol = processedColors.getOrNull(1) ?: Color.Gray
    val darkBg = processedColors.getOrNull(0) ?: Color.Black
    val lighterCol = processedColors.getOrNull(2) ?: Color.LightGray

    // 1. Draw backdrop layer
    drawRect(darkBg)

    // 2. Draw animated assets based on scene design types
    when (clip.presetType) {
        "NATURE" -> {
            // Draw hills/forest rolling and sun
            drawCircle(
                color = lighterCol,
                radius = width * 0.12f,
                center = Offset(width * 0.25f, height * 0.35f + sin(globalTime) * 10f)
            )

            // Draw rolling wavy layers
            val wavePath = Path().apply {
                val waveY = height * 0.65f
                moveTo(0f, height)
                lineTo(0f, waveY)
                cubicTo(
                    width * 0.35f, waveY - 45f + sin(globalTime + 1.0f) * 20f,
                    width * 0.7f, waveY + 55f - cos(globalTime) * 15f,
                    width, waveY
                )
                lineTo(width, height)
                close()
            }
            drawPath(wavePath, primaryCol)

            val forestPath = Path().apply {
                val waveY = height * 0.72f
                moveTo(0f, height)
                lineTo(0f, waveY)
                cubicTo(
                    width * 0.25f, waveY + 40f - cos(globalTime * 0.8f) * 20f,
                    width * 0.65f, waveY - 50f + sin(globalTime * 1.5f) * 15f,
                    width, waveY
                )
                lineTo(width, height)
                close()
            }
            drawPath(forestPath, primaryCol.copy(alpha = primaryCol.alpha * 0.8f))

            // Draw floating birds
            val birdX = width * 0.7f - (progressPercent * width * 0.4f)
            val birdY = height * 0.25f + cos(globalTime * 3f) * 8f
            drawPath(
                Path().apply {
                    moveTo(birdX, birdY)
                    quadraticTo(birdX + 15f, birdY - 12f, birdX + 30f, birdY)
                    quadraticTo(birdX + 45f, birdY - 12f, birdX + 60f, birdY)
                    quadraticTo(birdX + 30f, birdY + 10f, birdX, birdY)
                },
                lighterCol.copy(alpha = lighterCol.alpha * 0.8f)
            )
        }

        "NEON" -> {
            // Perspective lines speeding down grid
            val offsetRoad = (globalTime * 140f) % 250f
            drawPath(
                Path().apply {
                    moveTo(width / 2f, height * 0.4f)
                    lineTo(width / 2f - 240f - offsetRoad, height)
                    lineTo(width / 2f + 240f + offsetRoad, height)
                    close()
                },
                primaryCol.copy(alpha = primaryCol.alpha * 0.2f)
            )

            // Horizontal neon bars crossing
            val lineY = height * 0.4f + (progressPercent * height * 0.6f)
            drawLine(
                color = lighterCol,
                start = Offset(0f, lineY),
                end = Offset(width, lineY),
                strokeWidth = 6.dp.toPx()
            )

            // Blinking high glow orb
            drawCircle(
                color = primaryCol,
                radius = width * 0.04f + sin(globalTime * 12.0f) * 6f,
                center = Offset(width / 2f, height * 0.4f)
            )
        }

        "VINTAGE" -> {
            // Draw film camera frame overlay and circular gear
            val radius = width * 0.22f
            val radAngle = globalTime * 2.2f
            val centerOff = Offset(width * 0.72f, height * 0.5f)

            // Spinning retro film spool wheel
            drawCircle(lighterCol.copy(alpha = lighterCol.alpha * 0.15f), radius = radius, center = centerOff)
            drawCircle(lighterCol, radius = radius * 0.15f, center = centerOff)
            for (i in 0..4) {
                val angleNow = radAngle + (i * (2 * Math.PI / 5))
                val spokeX = centerOff.x + cos(angleNow).toFloat() * (radius * 0.7f)
                val spokeY = centerOff.y + sin(angleNow).toFloat() * (radius * 0.7f)
                drawLine(
                    color = primaryCol,
                    start = centerOff,
                    end = Offset(spokeX, spokeY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Vignette shadow around borders
            drawRect(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f * alphaMultiplier)),
                    center = Offset(width/2, height/2),
                    radius = width * 0.8f
                )
            )
        }

        "SPACE" -> {
            // Starfield procedural particle speeds expansion
            val starCount = 35
            for (i in 0 until starCount) {
                val seed = i * 632.42f
                val life = (progressPercent + (seed % 1.0f)) % 1.0f
                val distance = life * (width * 0.6f)
                val angle = (seed * 11.23f) % (2 * Math.PI)
                val x = width / 2f + cos(angle).toFloat() * distance
                val y = height / 2f + sin(angle).toFloat() * distance
                val sizeVal = (life * 6.dp.toPx()).coerceAtLeast(1f)
                drawCircle(
                    color = primaryCol.copy(alpha = primaryCol.alpha * (1f - life)),
                    radius = sizeVal,
                    center = Offset(x, y)
                )
            }

            // Spaceship glowing engine thrust ball
            drawCircle(
                color = lighterCol,
                radius = width * 0.08f,
                center = Offset(width/2f, height/2f)
            )
        }

        "CYBERPUNK" -> {
            // Draw vertical dynamic equalizer bars
            val barCount = 12
            val spacing = width * 0.06f
            val startX = (width - (barCount - 1) * spacing) / 2f

            for (i in 0 until barCount) {
                // Synthesize heights using cosine waves helper
                val heightPercent = (0.2f + 0.7f * sin(globalTime * 6f + i * 1.5f)).coerceIn(0.1f, 1.0f)
                val barLen = height * 0.55f * heightPercent
                val x = startX + i * spacing
                val y = height * 0.75f - barLen

                drawRoundRect(
                    color = primaryCol,
                    topLeft = Offset(x, y),
                    size = Size(width * 0.035f, barLen),
                    cornerRadius = CornerRadius(5f, 5f)
                )

                // High spark dots
                drawCircle(
                    color = lighterCol,
                    radius = width * 0.012f,
                    center = Offset(x + width * 0.017f, y - 10f)
                )
            }
        }

        "OCEAN" -> {
            // Gentle azure rolling waves
            val waveY = height * 0.60f
            val waveOffset = globalTime * 3.5f

            drawPath(
                Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waveY)
                    for (i in 0..10) {
                        val sliceX = width * (i / 10f)
                        val sliceY = waveY + sin(waveOffset + i) * 20f
                        lineTo(sliceX, sliceY)
                    }
                    lineTo(width, height)
                    close()
                },
                primaryCol
            )

            drawPath(
                Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waveY + 25f)
                    for (i in 0..10) {
                        val sliceX = width * (i / 10f)
                        val sliceY = waveY + 25f + cos(waveOffset * 0.8f + i) * 15f
                        lineTo(sliceX, sliceY)
                    }
                    lineTo(width, height)
                    close()
                },
                primaryCol.copy(alpha = primaryCol.alpha * 0.65f)
            )

            // Sailing yacht boat mockup floating
            val boatX = width * 0.28f + sin(globalTime * 0.6f) * (width * 0.15f)
            val boatY = waveY - 5f + sin(waveOffset + 3) * 10f

            drawPath(
                Path().apply {
                    moveTo(boatX, boatY)
                    lineTo(boatX + 50f, boatY)
                    lineTo(boatX + 40f, boatY + 18f)
                    lineTo(boatX + 10f, boatY + 18f)
                    close()
                },
                lighterCol
            )
            // Boat Sail
            drawPath(
                Path().apply {
                    moveTo(boatX + 25f, boatY)
                    lineTo(boatX + 25f, boatY - 35f)
                    lineTo(boatX + 45f, boatY - 5f)
                    close()
                },
                Color.White.copy(alpha = 0.9f * alphaMultiplier)
            )
        }
    }

    // 3. Draw active visual artifacts for Vintage film specs
    if (clip.filterStyle == FilterStyle.VINTAGE_GRAIN) {
        val rand = java.util.Random(globalTime.toRawBits().toLong())
        // Draw vertical hairline scratch lines
        if (rand.nextFloat() < 0.45f) {
            val scratchX = rand.nextFloat() * width
            drawLine(
                color = Color(0x3B000000),
                start = Offset(scratchX, 0f),
                end = Offset(scratchX + (rand.nextFloat() * 12f - 6f), height),
                strokeWidth = 1.dp.toPx()
            )
        }
        // Draw random speckle circles
        for (i in 0..5) {
            if (rand.nextFloat() < 0.3f) {
                val pointX = rand.nextFloat() * width
                val pointY = rand.nextFloat() * height
                drawCircle(
                    color = if (rand.nextBoolean()) Color(0x87FFFFFF) else Color(0x73000000),
                    radius = rand.nextFloat() * 3.dp.toPx() + 0.5f,
                    center = Offset(pointX, pointY)
                )
            }
        }
    }
}

// Map standard Colors according to Filters
fun applyFilterToColor(original: Color, filter: FilterStyle): Color {
    return when (filter) {
        FilterStyle.ORIGINAL -> original
        FilterStyle.GRAYSCALE -> {
            val luma = (original.red * 0.299f + original.green * 0.587f + original.blue * 0.114f).coerceIn(0f, 1f)
            Color(luma, luma, luma, original.alpha)
        }
        FilterStyle.SEPIA -> {
            val r = (original.red * 0.393f + original.green * 0.769f + original.blue * 0.189f).coerceIn(0f, 1f)
            val g = (original.red * 0.349f + original.green * 0.686f + original.blue * 0.168f).coerceIn(0f, 1f)
            val b = (original.red * 0.272f + original.green * 0.534f + original.blue * 0.131f).coerceIn(0f, 1f)
            Color(r, g, b, original.alpha)
        }
        FilterStyle.WARM_SUNSET -> {
            Color(
                red = (original.red * 1.15f + 0.1f).coerceIn(0f, 1f),
                green = (original.green * 0.85f).coerceIn(0f, 1f),
                blue = (original.blue * 0.70f).coerceIn(0f, 1f),
                alpha = original.alpha
            )
        }
        FilterStyle.CYBERPUNK -> {
            Color(
                red = (original.red * 0.7f + 0.3f).coerceIn(0f, 1f),
                green = (original.green * 0.6f).coerceIn(0f, 1f),
                blue = (original.blue * 1.15f + 0.15f).coerceIn(0f, 1f),
                alpha = original.alpha
            )
        }
        FilterStyle.VINTAGE_GRAIN -> {
            // Brownish film look
            Color(
                red = (original.red * 0.90f + 0.05f).coerceIn(0f, 1f),
                green = (original.green * 0.85f + 0.04f).coerceIn(0f, 1f),
                blue = (original.blue * 0.75f + 0.02f).coerceIn(0f, 1f),
                alpha = original.alpha
            )
        }
    }
}

// Helpers
fun getClipPresetIcon(preset: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (preset.uppercase()) {
        "NATURE" -> Icons.Default.FilterHdr
        "NEON" -> Icons.Default.FlashOn
        "VINTAGE" -> Icons.Default.Movie
        "SPACE" -> Icons.Default.AutoAwesome
        "CYBERPUNK" -> Icons.Default.GraphicEq
        "OCEAN" -> Icons.Default.Waves
        else -> Icons.Default.VideoCall
    }
}

fun getClipColor(preset: String): Color {
    return when (preset.uppercase()) {
        "NATURE" -> Color(0xFF4CAF50)
        "NEON" -> Color(0xFF00E5FF)
        "VINTAGE" -> Color(0xFF8D6E63)
        "SPACE" -> Color(0xFF9575CD)
        "CYBERPUNK" -> Color(0xFFFF4081)
        "OCEAN" -> Color(0xFF29B6F6)
        else -> Color.Gray
    }
}

fun getMusicName(id: String): String {
    return when (id) {
        "m1" -> "صوت الملحمة السينمائية 🎻"
        "m2" -> "نبض المستقبل الكهربي 🌌"
        "m3" -> "لحن رقيق لوفاي هادئ ☕"
        "m4" -> "صوت الرياح والغابات المطيرة 🌲"
        else -> "مسار صوتي أساسي 🎵"
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
