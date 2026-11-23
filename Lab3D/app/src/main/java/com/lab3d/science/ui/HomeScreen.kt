package com.lab3d.science.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lab3d.science.audio.SoundFx
import com.lab3d.science.model.ExperimentId
import com.lab3d.science.model.LabCatalog
import com.lab3d.science.model.LabProgress

@Composable
fun HomeScreen(onOpenExperiment: (ExperimentId) -> Unit) {
    val totalMissions = LabCatalog.missions.size
    val doneMissions = LabProgress.completedMissions.size
    val totalQuizzes = LabCatalog.quizzes.size
    val doneQuizzes = LabProgress.solvedQuizzes.size
    val nextXp = LabProgress.nextRankXp()
    val xpFrac = (LabProgress.xp.toFloat() / nextXp.coerceAtLeast(1)).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(LabColors.bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Заголовок
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Лаборатория 3D",
                            style = TextStyle(
                                brush = Brush.linearGradient(listOf(LabColors.Cyan, LabColors.Pink)),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            "Интерактивные 3D-опыты по физике, химии и космосу",
                            color = LabColors.Text2,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = { LabProgress.toggleSound(); SoundFx.playClick() }) {
                        Icon(
                            if (LabProgress.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = "Звук",
                            tint = if (LabProgress.soundEnabled) LabColors.Cyan else LabColors.Text2,
                        )
                    }
                }
            }

            // Карточка прогресса юного учёного
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1D2C5E), Color(0xFF2B1B54)),
                            ),
                        )
                        .border(1.dp, LabColors.Cyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(LabProgress.rankTitle(), color = LabColors.Text, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("${LabProgress.xp} / $nextXp XP", color = LabColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        StatPill("⭐ $doneMissions/$totalMissions", LabColors.Gold)
                        Spacer(Modifier.width(8.dp))
                        StatPill("🧠 $doneQuizzes/$totalQuizzes", LabColors.Green)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(xpFrac)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(LabColors.Cyan, LabColors.Pink))),
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Выбери 3D-опыт",
                    color = LabColors.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // 6 карточек 3D-экспериментов
            items(ExperimentId.entries.toList()) { exp ->
                ExperimentCard(exp, stars = LabProgress.starsFor(exp)) {
                    SoundFx.playClick()
                    onOpenExperiment(exp)
                }
            }
        }
    }
}

@Composable
private fun StatPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ExperimentCard(exp: ExperimentId, stars: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(exp.accentColor.copy(alpha = 0.24f), exp.secondaryColor.copy(alpha = 0.14f), LabColors.Card),
                ),
            )
            .border(1.dp, exp.accentColor.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(exp.emoji, fontSize = 36.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(exp.accentColor.copy(alpha = 0.22f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(exp.subject, color = exp.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            exp.title,
            color = LabColors.Text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            exp.subtitle,
            color = LabColors.Text2,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 3) {
                Text(
                    if (i < stars) "⭐" else "☆",
                    fontSize = 15.sp,
                    color = if (i < stars) LabColors.Gold else LabColors.Text2,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("3D →", color = exp.accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
