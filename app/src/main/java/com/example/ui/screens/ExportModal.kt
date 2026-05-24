package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MontageViewModel

@Composable
fun ExportModalScreen(
    viewModel: MontageViewModel,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    cardDark: Color,
    borderCol: Color,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.exportProgress.collectAsState()
    val logs by viewModel.exportLog.collectAsState()
    val finishedVideoName by viewModel.exportFinishedVideo.collectAsState()

    val isFinished = progress >= 100

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(enabled = false) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardDark)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFinished) {
                // Compile loading status
                Icon(
                    imageVector = Icons.Default.Cyclone,
                    contentDescription = "Compiling",
                    tint = accentCyan,
                    modifier = Modifier
                        .size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "جاري تحزيم ورندر خط الزمن...",
                    color = textLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يرجى عدم إغلاق التطبيق أثناء ترميز ملفات الفيديو وتحويل الأصول البصرية والمؤثرات.",
                    color = textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(borderCol)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentCyan, Color(0xFF00B0FF))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$progress%",
                    color = accentCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "سجل معالجة الرندر:",
                    color = textLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Log trace terminal
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = "> $log",
                            color = if (log.contains("نجاح")) accentCyan else textMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // SUCCESS EXPORT RENDER
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = accentCyan,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "تم رندر المونتاج بنجاح! ✨",
                    color = textLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اسم الملف: ${finishedVideoName ?: "montage_video.mp4"}",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = borderCol, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "مشاركة المقطع فوراً:",
                    color = textLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Share options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shares = listOf(
                        Triple("تيك توك", Icons.Default.QueueMusic, Color(0xFF000000)),
                        Triple("انستغرام", Icons.Default.PhotoCamera, Color(0xFFE1306C)),
                        Triple("واتساب", Icons.Default.Send, Color(0xFF25D366))
                    )

                    shares.forEach { share ->
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = share.third),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(share.second, contentDescription = null, size = 16.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(share.first, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissExport() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dismiss_export_dialog"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textLight),
                        border = RowDefaults.cardBorder(borderColor = borderCol)
                    ) {
                        Text("الرجوع للمحرر")
                    }

                    ElevatedButton(
                        onClick = { viewModel.dismissExport(); viewModel.navigateToDashboard() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = accentCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("شاشة المشاريع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
