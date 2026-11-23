package com.lab3d.science.sim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lab3d.science.audio.SoundFx
import com.lab3d.science.engine.ParticlePool3D
import com.lab3d.science.model.ExperimentId
import com.lab3d.science.model.LabProgress
import com.lab3d.science.model.Mission
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class OrbitBody(
    var x: Float,
    var z: Float,
    var vx: Float,
    var vz: Float,
    val radius: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val playerLaunched: Boolean = false,
) {
    val trailX = FloatArray(48) { x }
    val trailZ = FloatArray(48) { z }
    var trailHead = 0
    var trailStep = 0
    var lastAngle = atan2(z.toDouble(), x.toDouble()).toFloat()
    var accumAngle = 0f
    var alive = true
}

/**
 * Центральное состояние симуляции всех 6 3D-опытов.
 * Физика обновляется в 60 FPS из GL-потока; UI читает Compose-состояния.
 */
class LabState(val experiment: ExperimentId) {

    val particles = ParticlePool3D(340)

    // Уведомление о выполненном задании
    var completedBanner by mutableStateOf<Mission?>(null)
    var statusBanner by mutableStateOf("")

    // ---------------- 1. ВУЛКАН ----------------
    var sodaSpoons by mutableIntStateOf(2)
    var vinegarMl by mutableIntStateOf(50)
    var soapDrops by mutableIntStateOf(2)
    var lavaColorIdx by mutableIntStateOf(0) // 0 Красная, 1 Оранжевая, 2 Зелёная, 3 Фиолетовая
    var erupting by mutableStateOf(false)
    var eruptionLevel by mutableFloatStateOf(0f) // 0..1
    var peakHeightCm by mutableFloatStateOf(0f)
    var co2Pressure by mutableFloatStateOf(1.0f) // атм
    var phValue by mutableFloatStateOf(7.0f)
    private var eruptionTimeLeft = 0f
    private var targetPeakCm = 0f

    // ---------------- 2. ЭЛЕКТРИЧЕСТВО ----------------
    var switchClosed by mutableStateOf(false)
    var voltage by mutableFloatStateOf(9.0f)
    var resistance by mutableFloatStateOf(6.0f)
    var loadType by mutableIntStateOf(0) // 0 Лампа, 1 Мотор, 2 Электромагнит
    var bulbBurned by mutableStateOf(false)
    var currentA by mutableFloatStateOf(0f)
    var powerW by mutableFloatStateOf(0f)
    @Volatile var motorAngle = 0f
    @Volatile var electronPhase = 0f
    @Volatile var magnetLift = 0f

    // ---------------- 3. ОПТИКА ----------------
    var whiteLight by mutableStateOf(true)
    var wavelengthNm by mutableFloatStateOf(530f) // 400..700 нм
    var prismAngleDeg by mutableFloatStateOf(8f)  // -35..35
    var materialIdx by mutableIntStateOf(1)       // 0 Вода 1.33, 1 Стекло 1.52, 2 Алмаз 2.42
    var opticsInteracted by mutableStateOf(false)

    // ---------------- 4. МАГНЕТИЗМ ----------------
    var magnetMode by mutableIntStateOf(1)        // 0 Поле 3D, 1 Левитация
    var samePoles by mutableStateOf(true)         // true N-N (отталкивание), false N-S (притяжение)
    var magnetDistance by mutableFloatStateOf(0.55f) // 0.20..1.0
    var strengthIdx by mutableIntStateOf(1)       // 0 Феррит, 1 Алнико, 2 Неодим N52
    @Volatile var levitationHeight = 0.52f
    private var levitationVel = 0f

    // ---------------- 5. РАКЕТА ----------------
    var planetIdx by mutableIntStateOf(0)         // 0 Земля, 1 Луна, 2 Марс, 3 Юпитер
    var thrustKn by mutableFloatStateOf(95f)      // 30..160 кН
    var fuelLoadPct by mutableFloatStateOf(75f)   // 20..100 %
    var rocketPhase by mutableIntStateOf(0)       // 0 Старт, 1 Полёт, 2 Космос!, 3 Парашют/Посадка
    var altitudeKm by mutableFloatStateOf(0f)
    var velocityMs by mutableFloatStateOf(0f)
    var fuelRemainPct by mutableFloatStateOf(75f)
    var gForce by mutableFloatStateOf(1f)
    var maxAltitudeKm by mutableFloatStateOf(0f)

    // ---------------- 6. ГРАВИТАЦИЯ ----------------
    var sunMass by mutableFloatStateOf(1.0f)      // 0.5..2.5
    var launchSpeedK by mutableFloatStateOf(1.0f) // 0.4..1.8
    var timeScale by mutableFloatStateOf(1.0f)    // 0, 1, 2.5
    var bodyCount by mutableIntStateOf(0)
    val bodies = ArrayList<OrbitBody>()

    init {
        if (experiment == ExperimentId.GRAVITY) resetSolarSystem(0)
        if (experiment == ExperimentId.VOLCANO) updateVolcanoPh()
        if (experiment == ExperimentId.CIRCUIT) updateCircuitMetrics()
    }

    // =====================================================================
    // Действия пользователя
    // =====================================================================
    fun onShake() {
        when (experiment) {
            ExperimentId.VOLCANO -> triggerVolcano(boost = 1.2f)
            ExperimentId.MAGNET -> {
                levitationVel += 2.2f
                SoundFx.playClick()
            }
            ExperimentId.GRAVITY -> launchPlanet()
            else -> Unit
        }
    }

    // --- Вулкан ---
    fun addSoda() {
        sodaSpoons = (sodaSpoons + 1).coerceAtMost(5)
        updateVolcanoPh()
        SoundFx.playClick()
    }

    fun addVinegar() {
        vinegarMl = (vinegarMl + 20).coerceAtMost(100)
        updateVolcanoPh()
        SoundFx.playPour()
    }

    fun addSoap() {
        soapDrops = (soapDrops + 1).coerceAtMost(5)
        SoundFx.playClick()
    }

    fun resetVolcano() {
        sodaSpoons = 0; vinegarMl = 0; soapDrops = 0
        erupting = false; eruptionTimeLeft = 0f; eruptionLevel = 0f
        peakHeightCm = 0f; co2Pressure = 1.0f; phValue = 7.0f
        particles.clear()
        statusBanner = "Вулкан очищен. Добавь соду и уксус!"
        SoundFx.playClick()
    }

    private fun updateVolcanoPh() {
        phValue = (7.0f + sodaSpoons * 0.45f - vinegarMl * 0.042f).coerceIn(2.5f, 9.5f)
    }

    fun triggerVolcano(boost: Float = 1f) {
        if (sodaSpoons == 0 || vinegarMl == 0) {
            statusBanner = "Нужны и сода, и уксус для реакции!"
            SoundFx.playClick()
            return
        }
        val reagentFactor = (minOf(sodaSpoons / 5f, vinegarMl / 100f) * 0.7f + (sodaSpoons / 5f + vinegarMl / 100f) * 0.15f)
        val soapBonus = 1f + soapDrops * 0.14f
        targetPeakCm = (reagentFactor * 95f * soapBonus * boost).coerceIn(20f, 165f)
        eruptionTimeLeft = 4.5f + soapDrops * 0.55f
        erupting = true
        statusBanner = "Бурная реакция! Выделяется газ CO₂!"
        SoundFx.playEruption()
    }

    fun lavaRgb(): FloatArray = when (lavaColorIdx) {
        0 -> floatArrayOf(1.0f, 0.24f, 0.14f) // Красная
        1 -> floatArrayOf(1.0f, 0.56f, 0.10f) // Оранжевая
        2 -> floatArrayOf(0.22f, 0.95f, 0.42f) // Зелёная
        else -> floatArrayOf(0.75f, 0.30f, 1.0f) // Фиолетовая
    }

    // --- Электричество ---
    fun toggleSwitch() {
        switchClosed = !switchClosed
        updateCircuitMetrics()
        if (switchClosed) SoundFx.playZap() else SoundFx.playClick()
    }

    fun replaceBulb() {
        bulbBurned = false
        switchClosed = false
        updateCircuitMetrics()
        statusBanner = "Новая лампочка установлена!"
        SoundFx.playClick()
    }

    fun updateCircuitMetrics() {
        if (!switchClosed || (loadType == 0 && bulbBurned)) {
            currentA = 0f
            powerW = 0f
            return
        }
        val i = voltage / resistance.coerceAtLeast(1f)
        currentA = i
        powerW = voltage * i
        if (loadType == 0 && i > 4.0f) {
            bulbBurned = true
            currentA = 0f
            powerW = 0f
            statusBanner = "💥 Перегрузка (> 4 А)! Лампочка перегорела — увеличь сопротивление!"
            SoundFx.playZap()
            // Искры
            for (k in 0 until 32) {
                val a = Random.nextFloat() * 6.28f
                val sp = 1.5f + Random.nextFloat() * 2.5f
                particles.spawn(
                    0f, 0.45f, 0.9f,
                    cos(a) * sp, 1.2f + Random.nextFloat() * 2.4f, sin(a) * sp,
                    1f, 0.85f, 0.25f, 0.06f, 0.7f, -7.5f,
                )
            }
        }
    }

    // --- Оптика ---
    fun refractiveIndex(): Float = when (materialIdx) {
        0 -> 1.33f // Вода
        1 -> 1.52f // Стекло
        else -> 2.42f // Алмаз
    }

    // --- Магнетизм ---
    fun magnetStrengthFactor(): Float = when (strengthIdx) {
        0 -> 0.55f // Феррит
        1 -> 1.0f  // Алнико
        else -> 1.65f // Неодим N52
    }

    fun bounceMagnet() {
        levitationVel = 2.4f
        SoundFx.playClick()
    }

    // --- Ракета ---
    fun planetGravity(): Float = when (planetIdx) {
        0 -> 9.81f  // Земля
        1 -> 1.62f  // Луна
        2 -> 3.72f  // Марс
        else -> 24.79f // Юпитер
    }

    fun planetName(): String = when (planetIdx) {
        0 -> "Земля"
        1 -> "Луна"
        2 -> "Марс"
        else -> "Юпитер"
    }

    fun launchRocket() {
        if (rocketPhase == 1) return
        altitudeKm = 0f
        velocityMs = 0f
        maxAltitudeKm = 0f
        fuelRemainPct = fuelLoadPct
        rocketPhase = 1
        particles.clear()
        statusBanner = "🔥 Двигатели на полную! Старт с планеты ${planetName()}!"
        SoundFx.playRocket()
    }

    fun resetRocket() {
        rocketPhase = 0
        altitudeKm = 0f
        velocityMs = 0f
        maxAltitudeKm = 0f
        fuelRemainPct = fuelLoadPct
        gForce = planetGravity() / 9.81f
        particles.clear()
        statusBanner = "Ракета готова к запуску!"
        SoundFx.playClick()
    }

    // --- Гравитация ---
    fun resetSolarSystem(preset: Int) {
        synchronized(bodies) {
            bodies.clear()
            when (preset) {
                0 -> {
                    // Мини Солнечная система (3 стабильные планеты)
                    bodies += OrbitBody(1.9f, 0f, 0f, sqrt(5.2f * sunMass / 1.9f), 0.16f, 0.35f, 0.78f, 1.0f)
                    bodies += OrbitBody(3.0f, 0f, 0f, sqrt(5.2f * sunMass / 3.0f), 0.20f, 1.0f, 0.48f, 0.25f)
                    bodies += OrbitBody(-4.2f, 0f, 0f, -sqrt(5.2f * sunMass / 4.2f), 0.24f, 0.85f, 0.65f, 1.0f)
                    statusBanner = "Солнечная система запущена!"
                }
                1 -> {
                    // Комета с вытянутой эллиптической орбитой
                    bodies += OrbitBody(2.2f, 0f, 0f, sqrt(5.2f * sunMass / 2.2f), 0.17f, 0.30f, 0.90f, 0.65f)
                    bodies += OrbitBody(4.4f, 0f, 0f, sqrt(5.2f * sunMass / 4.4f) * 0.62f, 0.13f, 0.45f, 0.95f, 1.0f)
                    statusBanner = "Комета на вытянутой орбите!"
                }
                else -> {
                    // Тесная быстрая система
                    bodies += OrbitBody(1.5f, 0f, 0f, sqrt(5.2f * sunMass / 1.5f), 0.15f, 1.0f, 0.85f, 0.3f)
                    bodies += OrbitBody(-2.6f, 0f, 0f, -sqrt(5.2f * sunMass / 2.6f), 0.19f, 1.0f, 0.35f, 0.65f)
                    statusBanner = "Быстрая орбитальная система!"
                }
            }
            bodyCount = bodies.size
        }
        SoundFx.playClick()
    }

    fun launchPlanet() {
        val r0 = 2.6f + Random.nextFloat() * 1.1f
        val circularV = sqrt(5.2f * sunMass / r0)
        val v0 = circularV * launchSpeedK
        val colors = arrayOf(
            floatArrayOf(0.25f, 0.92f, 1.0f),
            floatArrayOf(1.0f, 0.42f, 0.72f),
            floatArrayOf(0.45f, 1.0f, 0.50f),
            floatArrayOf(1.0f, 0.78f, 0.25f),
        )
        val c = colors[Random.nextInt(colors.size)]
        val ang = Random.nextFloat() * (2f * PI.toFloat())
        val px = cos(ang) * r0
        val pz = sin(ang) * r0
        val vx = -sin(ang) * v0
        val vz = cos(ang) * v0
        synchronized(bodies) {
            if (bodies.size >= 8) bodies.removeAt(0)
            bodies += OrbitBody(px, pz, vx, vz, 0.15f + Random.nextFloat() * 0.07f, c[0], c[1], c[2], playerLaunched = true)
            bodyCount = bodies.size
        }
        statusBanner = "🪐 Планета запущена со скоростью ${"%.2f".format(launchSpeedK)}×!"
        SoundFx.playClick()
    }

    // =====================================================================
    // Физический шаг (вызывается каждый кадр из 3D-рендерера)
    // =====================================================================
    fun step(dt: Float) {
        when (experiment) {
            ExperimentId.VOLCANO -> stepVolcano(dt)
            ExperimentId.CIRCUIT -> stepCircuit(dt)
            ExperimentId.OPTICS -> stepOptics()
            ExperimentId.MAGNET -> stepMagnet(dt)
            ExperimentId.ROCKET -> stepRocket(dt)
            ExperimentId.GRAVITY -> stepGravity(dt)
        }
        particles.update(dt, floorY = if (experiment == ExperimentId.ROCKET) -100f else -1.18f)
    }

    private fun stepVolcano(dt: Float) {
        if (erupting) {
            eruptionTimeLeft -= dt
            if (eruptionTimeLeft <= 0f) {
                erupting = false
                statusBanner = "Реакция завершена! Высота: ${peakHeightCm.toInt()} см"
            }
            val targetLevel = (targetPeakCm / 120f).coerceIn(0.25f, 1.3f)
            eruptionLevel += (targetLevel - eruptionLevel) * (dt * 3.5f).coerceAtMost(1f)
            peakHeightCm = maxOf(peakHeightCm, eruptionLevel * 105f)
            co2Pressure = 1.0f + eruptionLevel * 4.5f

            val rgb = lavaRgb()
            val count = (3 + (eruptionLevel * 5).toInt()).coerceAtMost(9)
            for (k in 0 until count) {
                val a = Random.nextFloat() * 6.283f
                val spread = 0.45f + Random.nextFloat() * 0.95f
                val up = 2.6f + eruptionLevel * 3.2f + Random.nextFloat() * 1.4f
                val foamMix = if (soapDrops > 0 && Random.nextFloat() < 0.45f) 0.42f else 0f
                particles.spawn(
                    cos(a) * 0.14f, 0.58f, sin(a) * 0.14f,
                    cos(a) * spread, up, sin(a) * spread,
                    (rgb[0] + foamMix).coerceAtMost(1f),
                    (rgb[1] + foamMix).coerceAtMost(1f),
                    (rgb[2] + foamMix).coerceAtMost(1f),
                    0.07f + soapDrops * 0.012f + Random.nextFloat() * 0.05f,
                    1.3f + Random.nextFloat() * 0.8f,
                    -6.2f,
                )
            }
            if (peakHeightCm > 25f) checkMission("vol_1")
            if (peakHeightCm >= 90f) checkMission("vol_2")
            if (lavaColorIdx == 2 || lavaColorIdx == 3) checkMission("vol_3")
        } else {
            eruptionLevel = (eruptionLevel - dt * 0.9f).coerceAtLeast(0f)
            co2Pressure = 1.0f + eruptionLevel * 3.0f
        }
    }

    private fun stepCircuit(dt: Float) {
        updateCircuitMetrics()
        val i = currentA
        if (i > 0f) {
            electronPhase = (electronPhase + dt * (0.45f + i * 0.85f)) % 1f
            if (loadType == 1) {
                motorAngle = (motorAngle + dt * (180f + i * 320f)) % 360f
            }
            val targetLift = if (loadType == 2 && i > 1.2f) ((i - 1.2f) / 1.8f).coerceIn(0f, 1f) else 0f
            magnetLift += (targetLift - magnetLift) * (dt * 5f).coerceAtMost(1f)

            if (loadType == 0 && !bulbBurned && i in 1.0f..3.5f) checkMission("cir_1")
            if (loadType == 1 && i > 2.0f) checkMission("cir_2")
            if (loadType == 2 && i > 1.8f) checkMission("cir_3")
        } else {
            magnetLift = (magnetLift - dt * 4f).coerceAtLeast(0f)
        }
    }

    private fun stepOptics() {
        if (whiteLight && opticsInteracted && abs(prismAngleDeg) <= 22f) checkMission("opt_1")
        if (whiteLight && materialIdx == 2) checkMission("opt_2")
        if (!whiteLight && wavelengthNm < 450f) checkMission("opt_3")
    }

    private fun stepMagnet(dt: Float) {
        val str = magnetStrengthFactor()
        if (magnetMode == 1) {
            if (samePoles) {
                // Магнитная левитация: отталкивание ~ str / h^2 уравновешивает вес
                val eqH = (0.36f + str * 0.34f).coerceIn(0.32f, 1.15f)
                val force = (eqH - levitationHeight) * 26f - levitationVel * 4.5f
                levitationVel += force * dt
                levitationHeight = (levitationHeight + levitationVel * dt).coerceIn(0.16f, 1.55f)
                if (levitationHeight > 0.35f) checkMission("mag_1")
                if (strengthIdx == 2 && levitationHeight > 0.60f) checkMission("mag_2")
            } else {
                // Притяжение — падает и прилипает к нижнему магниту
                levitationVel -= 14f * dt
                levitationHeight = (levitationHeight + levitationVel * dt).coerceAtLeast(0.14f)
                if (levitationHeight <= 0.14f) levitationVel = 0f
            }
        } else {
            if (!samePoles && magnetDistance < 0.38f) checkMission("mag_3")
        }
    }

    private fun stepRocket(dt: Float) {
        val g = planetGravity()
        if (rocketPhase == 1) {
            val dryMassKg = 2200f
            val fuelMassKg = (fuelRemainPct / 100f) * 6800f
            val totalMass = dryMassKg + fuelMassKg

            val thrustN = if (fuelRemainPct > 0f) thrustKn * 1000f else 0f
            if (fuelRemainPct > 0f) {
                // Расход топлива пропорционален тяге
                val burnPctPerSec = (thrustKn / 100f) * 8.2f
                fuelRemainPct = (fuelRemainPct - burnPctPerSec * dt).coerceAtLeast(0f)

                // Пламя и дым двигателя
                for (k in 0 until 5) {
                    val rx = (Random.nextFloat() - 0.5f) * 0.25f
                    val rz = (Random.nextFloat() - 0.5f) * 0.25f
                    val hot = Random.nextFloat() < 0.65f
                    particles.spawn(
                        rx, -0.78f, rz,
                        rx * 2.5f, -(3.8f + thrustKn * 0.03f), rz * 2.5f,
                        if (hot) 1f else 0.78f,
                        if (hot) (0.45f + Random.nextFloat() * 0.45f) else 0.78f,
                        if (hot) 0.12f else 0.82f,
                        if (hot) 0.13f else 0.19f,
                        0.55f + Random.nextFloat() * 0.35f,
                        0f,
                    )
                }
            }

            // Атмосферное сопротивление (на Луне почти нет)
            val atmDensity = when (planetIdx) {
                1 -> 0f
                2 -> 0.12f * exp(-altitudeKm / 14f)
                else -> 1.0f * exp(-altitudeKm / 18f)
            }
            val dragN = atmDensity * 0.08f * velocityMs * abs(velocityMs)
            val accel = (thrustN - dragN) / totalMass - g
            gForce = ((accel + g) / 9.81f).coerceIn(0f, 12f)

            // Ускорение времени (1с симуляции ≈ 3.2с реального полёта) для динамичности
            val simDt = dt * 3.2f
            velocityMs += accel * simDt
            altitudeKm = (altitudeKm + (velocityMs * simDt) / 1000f).coerceAtLeast(0f)
            if (altitudeKm > maxAltitudeKm) maxAltitudeKm = altitudeKm

            if (altitudeKm <= 0f && velocityMs < 0f && fuelRemainPct <= 0f) {
                altitudeKm = 0f
                velocityMs = 0f
                rocketPhase = 3
                statusBanner = "🪂 Ракета вернулась. Макс. высота: ${"%.1f".format(maxAltitudeKm)} км"
            } else if (altitudeKm >= 100f) {
                rocketPhase = 2
                statusBanner = "🌌 КОСМОС ДОСТИГНУТ! Высота > 100 км!"
                SoundFx.playSuccess()
                when (planetIdx) {
                    0 -> checkMission("roc_2")
                    1 -> checkMission("roc_1")
                    2 -> checkMission("roc_3")
                }
            } else if (fuelRemainPct <= 0f && velocityMs < -5f) {
                // Парашют замедляет падение
                velocityMs *= (1f - dt * 1.4f).coerceAtLeast(0.5f)
            }
        }
    }

    private fun stepGravity(dt: Float) {
        if (sunMass >= 1.8f) checkMission("grv_3")
        val effDt = dt * timeScale
        if (effDt <= 0.0001f) return
        val gm = 5.2f * sunMass
        val sub = 4
        val h = effDt / sub

        synchronized(bodies) {
            val iter = bodies.iterator()
            while (iter.hasNext()) {
                val b = iter.next()
                for (s in 0 until sub) {
                    val r2 = b.x * b.x + b.z * b.z
                    val r = sqrt(r2).coerceAtLeast(0.35f)
                    if (r < 0.62f) {
                        // Падение в Солнце!
                        b.alive = false
                        statusBanner = "☀️ Планета упала на Солнце! Увеличь скорость запуска."
                        for (k in 0 until 20) {
                            val a = Random.nextFloat() * 6.28f
                            particles.spawn(
                                b.x, 0f, b.z,
                                cos(a) * 2.2f, (Random.nextFloat() - 0.5f) * 2f, sin(a) * 2.2f,
                                1f, 0.72f, 0.2f, 0.09f, 0.8f, 0f,
                            )
                        }
                        break
                    }
                    val aMag = -gm / (r2 + 0.04f)
                    b.vx += aMag * (b.x / r) * h
                    b.vz += aMag * (b.z / r) * h
                    b.x += b.vx * h
                    b.z += b.vz * h
                }
                if (!b.alive) {
                    iter.remove()
                    bodyCount = bodies.size
                    continue
                }
                val dist = sqrt(b.x * b.x + b.z * b.z)
                if (dist > 9.5f) {
                    if (b.playerLaunched && launchSpeedK >= 1.45f) checkMission("grv_2")
                    statusBanner = "🚀 Планета преодолела гравитацию и улетела в космос!"
                    iter.remove()
                    bodyCount = bodies.size
                    continue
                }
                val ang = atan2(b.z.toDouble(), b.x.toDouble()).toFloat()
                var da = ang - b.lastAngle
                if (da > PI) da -= (2 * PI).toFloat()
                if (da < -PI) da += (2 * PI).toFloat()
                b.accumAngle += abs(da)
                b.lastAngle = ang
                if (b.playerLaunched && b.accumAngle >= 5.8f) {
                    checkMission("grv_1")
                }

                b.trailStep++
                if (b.trailStep >= 2) {
                    b.trailStep = 0
                    b.trailHead = (b.trailHead + 1) % b.trailX.size
                    b.trailX[b.trailHead] = b.x
                    b.trailZ[b.trailHead] = b.z
                }
            }
        }
    }

    private fun checkMission(id: String) {
        val newlyCompleted = LabProgress.completeMission(id)
        if (newlyCompleted != null) {
            completedBanner = newlyCompleted
            SoundFx.playSuccess()
        }
    }
}
