@file:OptIn(ExperimentalMaterial3Api::class)

package com.lab3d.science.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lab3d.science.audio.SoundFx
import com.lab3d.science.engine.Lab3DViewport
import com.lab3d.science.model.ExperimentId
import com.lab3d.science.model.LabCatalog
import com.lab3d.science.model.LabProgress
import com.lab3d.science.sim.LabState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ExperimentScreen(
    state: LabState,
    onBack: () -> Unit,
) {
    val exp = state.experiment
    var showTheory by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }

    // Автоскрытие баннера выполненного задания
    LaunchedEffect(state.completedBanner) {
        if (state.completedBanner != null) {
            delay(3600)
            state.completedBanner = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(LabColors.bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Верхняя панель
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { SoundFx.playClick(); onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад", tint = LabColors.Text)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${exp.emoji} ${exp.title}",
                    color = LabColors.Text,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${exp.subject} · ${LabProgress.starsFor(exp)}/3 ⭐",
                    color = exp.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TopActionPill("🔬 Теория", exp.accentColor) {
                SoundFx.playClick()
                showTheory = true
            }
            Spacer(Modifier.width(6.dp))
            TopActionPill("🧠 Тест", LabColors.Gold) {
                SoundFx.playClick()
                showQuiz = true
            }
            Spacer(Modifier.width(4.dp))
        }

        // Адаптивный макет: на планшете/в альбомной — 3D слева + панель справа; на телефоне — сверху + снизу
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 680.dp
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ViewportBox(state, Modifier.weight(1.25f).fillMaxHeight())
                    ControlPanel(state, Modifier.weight(0.95f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViewportBox(state, Modifier.weight(1.05f).fillMaxWidth())
                    ControlPanel(state, Modifier.weight(0.95f).fillMaxWidth())
                }
            }
        }
    }

    if (showTheory) TheorySheet(exp) { showTheory = false }
    if (showQuiz) QuizSheet(exp) { showQuiz = false }
}

@Composable
private fun ViewportBox(state: LabState, modifier: Modifier) {
    val exp = state.experiment
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, exp.accentColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
    ) {
        Lab3DViewport(state, Modifier.fillMaxSize())

        // Подсказка камеры слева сверху
        Text(
            "3D · крути / зум",
            color = LabColors.Text2,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xAA0A0F24))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        // Живая телеметрия прибора справа сверху
        TelemetryHud(
            state = state,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        )

        // Статус-строка снизу 3D-окна
        if (state.statusBanner.isNotBlank()) {
            Text(
                state.statusBanner,
                color = LabColors.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC0D1530))
                    .border(1.dp, LabColors.CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // Баннер выполненного задания
        AnimatedVisibility(
            visible = state.completedBanner != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp, start = 14.dp, end = 14.dp),
        ) {
            state.completedBanner?.let { m ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4))))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⭐", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Задание выполнено! +${m.xp} XP", color = Color(0xFF051018), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(m.title, color = Color(0xFF071A24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryHud(state: LabState, modifier: Modifier) {
    val lines: List<String> = when (state.experiment) {
        ExperimentId.VOLCANO -> listOf(
            "CO₂: ${"%.1f".format(state.co2Pressure)} атм",
            "pH: ${"%.1f".format(state.phValue)}",
            "Пик: ${state.peakHeightCm.toInt()} см",
        )
        ExperimentId.CIRCUIT -> listOf(
            "U: ${"%.1f".format(state.voltage)} В · R: ${state.resistance.toInt()} Ом",
            "I: ${"%.2f".format(state.currentA)} А",
            "P: ${"%.1f".format(state.powerW)} Вт",
        )
        ExperimentId.OPTICS -> listOf(
            "n = ${"%.2f".format(state.refractiveIndex())}",
            "Угол: ${state.prismAngleDeg.roundToInt()}°",
            if (state.whiteLight) "Спектр: 7 цветов" else "λ = ${state.wavelengthNm.roundToInt()} нм",
        )
        ExperimentId.MAGNET -> listOf(
            if (state.samePoles) "Полюса: N ⟷ N" else "Полюса: N ↔ S",
            if (state.magnetMode == 1) "Высота: ${(state.levitationHeight * 100).toInt()} мм"
            else "Дист.: ${(state.magnetDistance * 10).toInt()} см",
        )
        ExperimentId.ROCKET -> listOf(
            "Высота: ${"%.1f".format(state.altitudeKm)} км",
            "Скор.: ${state.velocityMs.toInt()} м/с",
            "Топливо: ${state.fuelRemainPct.toInt()}% · ${"%.1f".format(state.gForce)}G",
        )
        ExperimentId.GRAVITY -> listOf(
            "Солнце: ${"%.1f".format(state.sunMass)} M☉",
            "Планет: ${state.bodyCount}",
            "Скорость: ${"%.2f".format(state.launchSpeedK)}×",
        )
    }

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xBB0A0F24))
            .border(1.dp, LabColors.Cyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        lines.forEach { line ->
            Text(line, color = LabColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlPanel(state: LabState, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(LabColors.Card)
            .border(1.dp, LabColors.CardBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state.experiment) {
            ExperimentId.VOLCANO -> VolcanoControls(state)
            ExperimentId.CIRCUIT -> CircuitControls(state)
            ExperimentId.OPTICS -> OpticsControls(state)
            ExperimentId.MAGNET -> MagnetControls(state)
            ExperimentId.ROCKET -> RocketControls(state)
            ExperimentId.GRAVITY -> GravityControls(state)
        }

        Spacer(Modifier.height(2.dp))
        MissionsBlock(state.experiment)
    }
}

// =====================================================================
// Панели управления для каждого из 6 опытов
// =====================================================================
@Composable
private fun VolcanoControls(s: LabState) {
    Text("Реагенты (или потряси телефон!)", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionBtn("🥄 Сода (${s.sodaSpoons}/5)", LabColors.Cyan, Modifier.weight(1f)) { s.addSoda() }
        ActionBtn("🧪 Уксус (${s.vinegarMl}мл)", LabColors.Pink, Modifier.weight(1f)) { s.addVinegar() }
        ActionBtn("🫧 Мыло (${s.soapDrops}/5)", LabColors.Gold, Modifier.weight(1f)) { s.addSoap() }
    }
    Text("Цвет лавы / пены", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val colors = listOf("🔴 Красный", "🟠 Оранж", "🟢 Зелёный", "🟣 Фиол.")
        colors.forEachIndexed { i, label ->
            ChoiceChip(label, s.lavaColorIdx == i, Modifier.weight(1f)) {
                s.lavaColorIdx = i
                SoundFx.playClick()
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryBigButton("🌋 ЗАПУСТИТЬ РЕАКЦИЮ!", Color(0xFFFF5A36), Modifier.weight(1.7f)) { s.triggerVolcano() }
        ActionBtn("🧹 Сброс", LabColors.Text2, Modifier.weight(0.75f)) { s.resetVolcano() }
    }
}

@Composable
private fun CircuitControls(s: LabState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryBigButton(
            if (s.switchClosed) "⚡ ЦЕПЬ ЗАМКНУТА (ВЫКЛ)" else "🔌 ЗАМКНУТЬ РУБИЛЬНИК",
            if (s.switchClosed) LabColors.Green else LabColors.Gold,
            Modifier.weight(1.5f),
        ) { s.toggleSwitch() }
        if (s.bulbBurned) {
            ActionBtn("💡 Новая лампа", LabColors.Pink, Modifier.weight(0.9f)) { s.replaceBulb() }
        }
    }
    Text("Подключённый прибор", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("💡 Лампа", "🌀 Мотор", "🧲 Магнит").forEachIndexed { idx, title ->
            ChoiceChip(title, s.loadType == idx, Modifier.weight(1f)) {
                s.loadType = idx
                s.updateCircuitMetrics()
                SoundFx.playClick()
            }
        }
    }
    LabSlider("Напряжение батареи (U)", "${"%.1f".format(s.voltage)} В", s.voltage, 1.5f..24f, LabColors.Gold) {
        s.voltage = it
        s.updateCircuitMetrics()
    }
    LabSlider("Сопротивление резистора (R)", "${s.resistance.roundToInt()} Ом", s.resistance, 1f..30f, LabColors.Cyan) {
        s.resistance = it
        s.updateCircuitMetrics()
    }
}

@Composable
private fun OpticsControls(s: LabState) {
    Text("Источник света", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("🌈 Белый свет", s.whiteLight, Modifier.weight(1f)) {
            s.whiteLight = true; s.opticsInteracted = true; SoundFx.playClick()
        }
        ChoiceChip("🔦 Моно-лазер", !s.whiteLight, Modifier.weight(1f)) {
            s.whiteLight = false; s.opticsInteracted = true; SoundFx.playClick()
        }
    }
    Text("Материал 3D-призмы (показатель преломления n)", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("💧 Вода 1.33", "🪟 Стекло 1.52", "💎 Алмаз 2.42").forEachIndexed { idx, label ->
            ChoiceChip(label, s.materialIdx == idx, Modifier.weight(1f)) {
                s.materialIdx = idx; s.opticsInteracted = true; SoundFx.playClick()
            }
        }
    }
    LabSlider("Поворот призмы", "${s.prismAngleDeg.roundToInt()}°", s.prismAngleDeg, -32f..32f, LabColors.Cyan) {
        s.prismAngleDeg = it; s.opticsInteracted = true
    }
    if (!s.whiteLight) {
        LabSlider("Длина волны лазера (цвет)", "${s.wavelengthNm.roundToInt()} нм", s.wavelengthNm, 400f..700f, LabColors.Pink) {
            s.wavelengthNm = it; s.opticsInteracted = true
        }
    }
}

@Composable
private fun MagnetControls(s: LabState) {
    Text("Режим эксперимента", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("✨ Левитация 3D", s.magnetMode == 1, Modifier.weight(1f)) {
            s.magnetMode = 1; SoundFx.playClick()
        }
        ChoiceChip("🧭 Поле и Компасы", s.magnetMode == 0, Modifier.weight(1f)) {
            s.magnetMode = 0; SoundFx.playClick()
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("N ⟷ N Отталкивание", s.samePoles, Modifier.weight(1f)) {
            s.samePoles = true; SoundFx.playClick()
        }
        ChoiceChip("N ↔ S Притяжение", !s.samePoles, Modifier.weight(1f)) {
            s.samePoles = false; SoundFx.playClick()
        }
    }
    Text("Тип магнита", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Феррит", "Алнико", "Неодим N52").forEachIndexed { idx, title ->
            ChoiceChip(title, s.strengthIdx == idx, Modifier.weight(1f)) {
                s.strengthIdx = idx; SoundFx.playClick()
            }
        }
    }
    if (s.magnetMode == 1) {
        PrimaryBigButton("⬆️ ПОДБРОСИТЬ МАГНИТ!", LabColors.Pink, Modifier.fillMaxWidth()) { s.bounceMagnet() }
    } else {
        LabSlider("Расстояние между магнитами", "${(s.magnetDistance * 10).roundToInt()} см", s.magnetDistance, 0.20f..1.0f, LabColors.Cyan) {
            s.magnetDistance = it
        }
    }
}

@Composable
private fun RocketControls(s: LabState) {
    Text("Планета запуска (гравитация g)", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("🌍 Земля", "🌕 Луна", "🔴 Марс", "🟠 Юпитер").forEachIndexed { idx, label ->
            ChoiceChip(label, s.planetIdx == idx, Modifier.weight(1f)) {
                s.planetIdx = idx
                s.resetRocket()
            }
        }
    }
    LabSlider("Тяга двигателя", "${s.thrustKn.roundToInt()} кН", s.thrustKn, 30f..160f, LabColors.Green) {
        s.thrustKn = it
    }
    LabSlider("Запас топлива на старте", "${s.fuelLoadPct.roundToInt()}%", s.fuelLoadPct, 20f..100f, LabColors.Gold) {
        s.fuelLoadPct = it
        if (s.rocketPhase == 0) s.fuelRemainPct = it
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryBigButton(
            if (s.rocketPhase == 1) "🚀 В ПОЛЁТЕ..." else "🚀 ПУСК РАКЕТЫ!",
            LabColors.Green,
            Modifier.weight(1.6f),
        ) { s.launchRocket() }
        ActionBtn("🔄 На старт", LabColors.Cyan, Modifier.weight(0.85f)) { s.resetRocket() }
    }
}

@Composable
private fun GravityControls(s: LabState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryBigButton("🪐 ЗАПУСТИТЬ ПЛАНЕТУ!", LabColors.Pink, Modifier.weight(1.5f)) { s.launchPlanet() }
        ActionBtn(
            if (s.timeScale == 0f) "▶️ Старт" else "⏸️ Пауза",
            LabColors.Cyan,
            Modifier.weight(0.85f),
        ) {
            s.timeScale = if (s.timeScale == 0f) 1f else 0f
            SoundFx.playClick()
        }
    }
    LabSlider("Скорость запуска планеты", "${"%.2f".format(s.launchSpeedK)}×", s.launchSpeedK, 0.4f..1.8f, LabColors.Pink) {
        s.launchSpeedK = it
    }
    LabSlider("Масса Солнца", "${"%.1f".format(s.sunMass)} M☉", s.sunMass, 0.5f..2.5f, LabColors.Gold) {
        s.sunMass = it
    }
    Text("Готовые космические системы", color = LabColors.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("☀️ Солнечная", false, Modifier.weight(1f)) { s.resetSolarSystem(0) }
        ChoiceChip("☄️ Комета", false, Modifier.weight(1f)) { s.resetSolarSystem(1) }
        ChoiceChip("⚡ Быстрая", false, Modifier.weight(1f)) { s.resetSolarSystem(2) }
    }
}

// =====================================================================
// Блок заданий (миссий) + Теория + Викторина
// =====================================================================
@Composable
private fun MissionsBlock(exp: ExperimentId) {
    val list = LabCatalog.missionsFor(exp)
    val done = LabProgress.completedMissions
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LabColors.Bg.copy(alpha = 0.55f))
            .border(1.dp, LabColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🎯 Задания эксперимента", color = LabColors.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        list.forEach { m ->
            val ok = m.id in done
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (ok) LabColors.Green else LabColors.Text2,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "${m.title} (+${m.xp} XP)",
                        color = if (ok) LabColors.Green else LabColors.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    if (!ok) {
                        Text(m.hint, color = LabColors.Text2, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TheorySheet(exp: ExperimentId, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LabColors.Sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, null, tint = exp.accentColor)
                Spacer(Modifier.width(8.dp))
                Text("Как это работает?", color = LabColors.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(exp.accentColor.copy(alpha = 0.16f))
                    .border(1.dp, exp.accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(exp.formula, color = exp.accentColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Text(exp.explanation, color = LabColors.Text, fontSize = 15.sp, lineHeight = 22.sp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LabColors.Gold.copy(alpha = 0.14f))
                    .border(1.dp, LabColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("💡 Интересный факт", color = LabColors.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(exp.funFact, color = LabColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun QuizSheet(exp: ExperimentId, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val questions = remember(exp) { LabCatalog.quizzesFor(exp) }
    var qIdx by remember { mutableIntStateOf(0) }
    var selected by remember(qIdx) { mutableStateOf<Int?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LabColors.Sheet) {
        val q = questions[qIdx]
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Quiz, null, tint = LabColors.Gold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Викторина · вопрос ${qIdx + 1} из ${questions.size}",
                    color = LabColors.Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
            }
            Text(q.question, color = LabColors.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp)

            q.options.forEachIndexed { i, opt ->
                val isPicked = selected == i
                val isCorrect = i == q.correctIndex
                val bg = when {
                    selected == null -> LabColors.Card
                    isCorrect -> LabColors.Green.copy(alpha = 0.25f)
                    isPicked -> Color(0xFFFF4D6D).copy(alpha = 0.28f)
                    else -> LabColors.Card.copy(alpha = 0.5f)
                }
                val brd = when {
                    selected == null -> LabColors.CardBorder
                    isCorrect -> LabColors.Green
                    isPicked -> Color(0xFFFF4D6D)
                    else -> LabColors.CardBorder
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.5.dp, brd, RoundedCornerShape(16.dp))
                        .clickable(enabled = selected == null) {
                            selected = i
                            if (i == q.correctIndex) {
                                LabProgress.markQuizSolved(exp, qIdx)
                                SoundFx.playSuccess()
                            } else {
                                SoundFx.playClick()
                            }
                        }
                        .padding(14.dp),
                ) {
                    Text(opt, color = LabColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (selected != null) {
                val ok = selected == q.correctIndex
                Text(
                    if (ok) "✅ ${q.feedback}" else "❌ Попробуй запомнить: ${q.feedback}",
                    color = if (ok) LabColors.Green else LabColors.Gold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                PrimaryBigButton(
                    if (qIdx + 1 < questions.size) "Следующий вопрос →" else "Готово! Вернуться к опыту",
                    LabColors.Cyan,
                    Modifier.fillMaxWidth(),
                ) {
                    if (qIdx + 1 < questions.size) qIdx++ else onDismiss()
                }
            }
        }
    }
}

// =====================================================================
// Переиспользуемые кнопки и слайдеры
// =====================================================================
@Composable
private fun TopActionPill(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PrimaryBigButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.78f))))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color(0xFF07101C), fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActionBtn(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LabColors.Text, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) LabColors.Cyan.copy(alpha = 0.25f) else LabColors.Bg.copy(alpha = 0.5f))
            .border(1.dp, if (selected) LabColors.Cyan else LabColors.CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) LabColors.Cyan else LabColors.Text2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LabSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = LabColors.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
    }
}
