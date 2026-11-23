package com.lab3d.science.engine

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lab3d.science.model.ExperimentId
import com.lab3d.science.sim.LabState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Встраиваемая 3D-сцена опыта с вращением камеры на 360° и зумом щипком. */
@Composable
fun Lab3DViewport(state: LabState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val renderer = remember(state) { LabRenderer(state) }
    val glView = remember(state) { LabGLSurfaceView(context, renderer) }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@SuppressLint("ViewConstructor")
private class LabGLSurfaceView(
    context: Context,
    private val renderer: LabRenderer,
) : GLSurfaceView(context) {

    private var prevX = 0f
    private var prevY = 0f
    private var lastTapMs = 0L

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.zoomBy(detector.scaleFactor)
                return true
            }
        },
    )

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                prevX = event.x
                prevY = event.y
                val now = System.currentTimeMillis()
                if (now - lastTapMs < 280L) renderer.resetCamera()
                lastTapMs = now
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - prevX
                val dy = event.y - prevY
                prevX = event.x
                prevY = event.y
                renderer.orbitBy(dx * 0.45f, dy * 0.35f)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

class LabRenderer(private val state: LabState) : GLSurfaceView.Renderer {

    private val gl = Gl3DContext()
    private var lastNs = 0L
    private var time = 0f

    @Volatile private var camYaw = defaultYaw(state.experiment)
    @Volatile private var camPitch = defaultPitch(state.experiment)
    @Volatile private var camDist = defaultDist(state.experiment)

    fun orbitBy(dYaw: Float, dPitch: Float) {
        camYaw = (camYaw + dYaw) % 360f
        camPitch = (camPitch + dPitch).coerceIn(4f, 76f)
    }

    fun zoomBy(scaleFactor: Float) {
        camDist = (camDist / scaleFactor).coerceIn(3.2f, 15.0f)
    }

    fun resetCamera() {
        camYaw = defaultYaw(state.experiment)
        camPitch = defaultPitch(state.experiment)
        camDist = defaultDist(state.experiment)
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        gl.initGl()
        lastNs = System.nanoTime()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        gl.resize(width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.045f)
        lastNs = now
        time += dt

        state.step(dt)

        val yawRad = Math.toRadians(camYaw.toDouble())
        val pitchRad = Math.toRadians(camPitch.toDouble())
        val lookY = when (state.experiment) {
            ExperimentId.GRAVITY -> 0f
            ExperimentId.ROCKET -> 0.4f
            else -> -0.1f
        }
        val ex = (camDist * cos(pitchRad) * sin(yawRad)).toFloat()
        val ey = (lookY + camDist * sin(pitchRad)).toFloat()
        val ez = (camDist * cos(pitchRad) * cos(yawRad)).toFloat()

        val (bgR, bgG, bgB) = computeBackground()
        val acc = state.experiment.accentColor
        gl.beginFrame(bgR, bgG, bgB, ex, ey, ez, 0f, lookY, 0f, acc.red, acc.green, acc.blue)

        when (state.experiment) {
            ExperimentId.VOLCANO -> renderVolcano()
            ExperimentId.CIRCUIT -> renderCircuit()
            ExperimentId.OPTICS -> renderOptics()
            ExperimentId.MAGNET -> renderMagnet()
            ExperimentId.ROCKET -> renderRocket()
            ExperimentId.GRAVITY -> renderGravity()
        }

        gl.drawParticles(state.particles)
    }

    private fun computeBackground(): Triple<Float, Float, Float> {
        if (state.experiment == ExperimentId.ROCKET) {
            val spaceK = (state.altitudeKm / 85f).coerceIn(0f, 1f)
            val (sr, sg, sb) = when (state.planetIdx) {
                0 -> Triple(0.14f, 0.36f, 0.68f) // Земля
                1 -> Triple(0.03f, 0.04f, 0.08f) // Луна
                2 -> Triple(0.45f, 0.20f, 0.14f) // Марс
                else -> Triple(0.46f, 0.32f, 0.16f) // Юпитер
            }
            return Triple(
                sr * (1f - spaceK) + 0.02f * spaceK,
                sg * (1f - spaceK) + 0.025f * spaceK,
                sb * (1f - spaceK) + 0.06f * spaceK,
            )
        }
        return when (state.experiment) {
            ExperimentId.GRAVITY -> Triple(0.02f, 0.025f, 0.07f)
            ExperimentId.OPTICS -> Triple(0.03f, 0.04f, 0.09f)
            else -> Triple(0.055f, 0.075f, 0.15f)
        }
    }

    private fun drawLabBench(y: Float = -1.25f) {
        gl.push {
            gl.tr(0f, y - 0.12f, 0f)
            gl.sc(5.8f, 0.24f, 4.2f)
            gl.draw(gl.box, 0.13f, 0.17f, 0.27f, shininess = 60f)
        }
        // Неоновая окантовка лабораторного стола
        gl.push {
            gl.tr(0f, y + 0.005f, 2.08f)
            gl.sc(5.8f, 0.03f, 0.04f)
            gl.draw(gl.box, 0.22f, 0.90f, 1.0f, emissive = 0.6f)
        }
    }

    // =====================================================================
    // 1. 3D ВУЛКАН И ХИМИЯ
    // =====================================================================
    private fun renderVolcano() {
        drawLabBench(-1.25f)
        // Металлический лабораторный поддон
        gl.push {
            gl.tr(0f, -1.20f, 0f)
            gl.sc(2.15f, 0.08f, 2.15f)
            gl.draw(gl.cylinder, 0.34f, 0.38f, 0.46f, shininess = 90f)
        }
        gl.push {
            gl.tr(0f, -1.14f, 0f)
            gl.sc(2.12f, 0.28f, 2.12f)
            gl.draw(gl.thinTorus, 0.45f, 0.50f, 0.58f, shininess = 90f)
        }

        // Гора 3D-вулкана (2 слоя породы)
        gl.push {
            gl.tr(0f, -0.35f, 0f)
            gl.sc(1.55f, 1.65f, 1.55f)
            gl.draw(gl.volcanoCone, 0.34f, 0.24f, 0.19f, shininess = 18f)
        }
        gl.push {
            gl.tr(0f, 0.18f, 0f)
            gl.sc(0.95f, 0.65f, 0.95f)
            gl.draw(gl.volcanoCone, 0.24f, 0.19f, 0.18f, shininess = 16f)
        }

        // Светящийся кратер с лавой/пеной
        val rgb = state.lavaRgb()
        val er = state.eruptionLevel
        gl.push {
            gl.tr(0f, 0.49f, 0f)
            gl.sc(0.32f, 0.06f, 0.32f)
            gl.draw(gl.cylinder, rgb[0], rgb[1], rgb[2], emissive = 0.45f + 0.5f * er)
        }
        if (er > 0.03f) {
            // Пенящаяся шапка, выходящая из кратера
            gl.push {
                gl.tr(0f, 0.56f + er * 0.25f, 0f)
                gl.sc(0.32f + er * 0.24f, 0.24f + er * 0.36f, 0.32f + er * 0.24f)
                gl.draw(gl.sphere, rgb[0], rgb[1], rgb[2], emissive = 0.65f)
            }
            // Потоки пены по склонам вулкана
            for (k in 0 until 5) {
                val a = k * 1.256f + 0.3f
                val len = (0.5f + er * 0.85f).coerceAtMost(1.2f)
                gl.drawSegment(
                    cos(a) * 0.25f, 0.46f, sin(a) * 0.25f,
                    cos(a) * (0.25f + len * 0.88f), 0.46f - len * 1.15f, sin(a) * (0.25f + len * 0.88f),
                    0.07f + er * 0.04f, rgb[0], rgb[1], rgb[2], emissive = 0.55f,
                )
            }
        }

        // Колба с уксусом слева (жидкость внутри + полупрозрачное 3D-стекло)
        gl.push {
            gl.tr(-2.1f, -0.78f, 0.65f)
            val fill = (state.vinegarMl / 100f).coerceIn(0f, 1f)
            if (fill > 0.02f) {
                gl.push {
                    gl.tr(0f, -0.40f + fill * 0.28f, 0f)
                    gl.sc(0.42f, fill * 0.56f, 0.42f)
                    gl.draw(gl.flaskCone, 0.72f, 0.92f, 1.0f, a = 0.82f, emissive = 0.15f)
                }
            }
            gl.push { gl.sc(0.50f, 0.85f, 0.50f); gl.draw(gl.flaskCone, 0.82f, 0.94f, 1f, a = 0.32f, shininess = 120f) }
            gl.push { gl.tr(0f, 0.56f, 0f); gl.sc(0.18f, 0.32f, 0.18f); gl.draw(gl.cylinder, 0.82f, 0.94f, 1f, a = 0.35f) }
        }

        // Баночка с содой справа (белый порошок)
        gl.push {
            gl.tr(2.1f, -0.86f, 0.65f)
            val fill = (state.sodaSpoons / 5f).coerceIn(0f, 1f)
            if (fill > 0.02f) {
                gl.push {
                    gl.tr(0f, -0.32f + fill * 0.25f, 0f)
                    gl.sc(0.36f, fill * 0.52f, 0.36f)
                    gl.draw(gl.cylinder, 0.96f, 0.96f, 0.98f)
                }
            }
            gl.push { gl.sc(0.42f, 0.72f, 0.42f); gl.draw(gl.cylinder, 0.85f, 0.90f, 1f, a = 0.30f, shininess = 120f) }
        }

        // Флакон красителя спереди справа
        gl.push {
            gl.tr(1.7f, -0.98f, 1.35f)
            gl.push { gl.sc(0.22f, 0.42f, 0.22f); gl.draw(gl.cylinder, rgb[0], rgb[1], rgb[2], emissive = 0.25f) }
            gl.push { gl.tr(0f, 0.32f, 0f); gl.sc(0.10f, 0.22f, 0.10f); gl.draw(gl.cone, 0.2f, 0.2f, 0.25f) }
        }
    }

    // =====================================================================
    // 2. 3D ЭЛЕКТРИЧЕСКАЯ ЦЕПЬ И ЗАКОН ОМА
    // =====================================================================
    private fun renderCircuit() {
        drawLabBench(-1.25f)
        val by = -1.08f
        // Изолированная монтажная плата
        gl.push {
            gl.tr(0f, by - 0.06f, 0f)
            gl.sc(4.8f, 0.14f, 3.2f)
            gl.draw(gl.box, 0.10f, 0.24f, 0.22f, shininess = 50f)
        }

        // Контур медных проводов (прямоугольник X: -1.8..1.8, Z: -1.0..1.0)
        val wy = by + 0.08f
        val wireR = 0.045f
        val cr = 0.88f; val cg = 0.52f; val cb = 0.22f
        gl.drawSegment(-1.8f, wy, -1.0f, 1.8f, wy, -1.0f, wireR, cr, cg, cb)
        gl.drawSegment(1.8f, wy, -1.0f, 1.8f, wy, 1.0f, wireR, cr, cg, cb)
        gl.drawSegment(1.8f, wy, 1.0f, -1.8f, wy, 1.0f, wireR, cr, cg, cb)
        gl.drawSegment(-1.8f, wy, 1.0f, -1.8f, wy, -1.0f, wireR, cr, cg, cb)

        // Бегущие 3D-электроны по цепи (скорость пропорциональна току I = U/R!)
        val iCur = state.currentA
        val electronCount = 24
        val perim = 2f * (3.6f + 2.0f)
        for (k in 0 until electronCount) {
            val u = ((k.toFloat() / electronCount) + state.electronPhase) % 1f
            val d = u * perim
            val ex: Float; val ez: Float
            when {
                d < 3.6f -> { ex = -1.8f + d; ez = -1.0f }
                d < 5.6f -> { ex = 1.8f; ez = -1.0f + (d - 3.6f) }
                d < 9.2f -> { ex = 1.8f - (d - 5.6f); ez = 1.0f }
                else -> { ex = -1.8f; ez = 1.0f - (d - 9.2f) }
            }
            gl.push {
                gl.tr(ex, wy + 0.03f, ez)
                gl.sc(if (iCur > 0f) 0.085f else 0.055f)
                gl.draw(gl.sphere, 0.22f, 0.94f, 1.0f, emissive = if (iCur > 0f) 0.95f else 0.2f)
            }
        }

        // Батарея питания слева (-1.8, z=0)
        gl.push {
            gl.tr(-1.8f, by + 0.42f, 0f)
            gl.push { gl.sc(0.65f, 0.78f, 0.95f); gl.draw(gl.box, 0.18f, 0.22f, 0.34f) }
            // Полюс + (красный) и - (синий)
            gl.push { gl.tr(0f, 0.45f, -0.26f); gl.sc(0.14f, 0.16f, 0.14f); gl.draw(gl.cylinder, 0.95f, 0.22f, 0.22f, emissive = 0.25f) }
            gl.push { gl.tr(0f, 0.45f, 0.26f); gl.sc(0.14f, 0.16f, 0.14f); gl.draw(gl.cylinder, 0.22f, 0.55f, 0.98f, emissive = 0.25f) }
            // Индикатор напряжения (столбик)
            val vFill = (state.voltage / 24f).coerceIn(0.08f, 1f)
            gl.push { gl.tr(0.34f, -0.35f + vFill * 0.35f, 0f); gl.sc(0.04f, vFill * 0.70f, 0.25f); gl.draw(gl.box, 1f, 0.85f, 0.2f, emissive = 0.85f) }
        }

        // Рубильник сзади (z=-1.0, x=-0.5)
        gl.push {
            gl.tr(-0.5f, by + 0.12f, -1.0f)
            gl.push { gl.sc(0.75f, 0.14f, 0.36f); gl.draw(gl.box, 0.25f, 0.25f, 0.30f) }
            // Подвижный рычаг
            gl.push {
                gl.tr(-0.28f, 0.12f, 0f)
                gl.rot(if (state.switchClosed) 0f else 48f, 0f, 0f, 1f)
                gl.drawSegment(0f, 0f, 0f, 0.62f, 0f, 0f, 0.05f, 0.95f, 0.72f, 0.28f)
                gl.push { gl.tr(0.62f, 0.06f, 0f); gl.sc(0.11f); gl.draw(gl.sphere, 1f, 0.25f, 0.25f) }
            }
        }

        // Резистор справа (x=1.8, z=0) — греется красным при высоком токе
        val heat = (iCur / 3.8f).coerceIn(0f, 1f)
        gl.push {
            gl.tr(1.8f, by + 0.28f, 0f)
            gl.rot(90f, 1f, 0f, 0f)
            gl.push { gl.sc(0.24f, 1.05f, 0.24f); gl.draw(gl.cylinder, 0.85f, 0.74f - heat * 0.45f, 0.56f - heat * 0.45f, emissive = heat * 0.55f) }
            for (k in -1..1) {
                gl.push { gl.tr(0f, k * 0.24f, 0f); gl.sc(0.255f, 0.07f, 0.255f); gl.draw(gl.cylinder, 0.2f + (k + 1) * 0.35f, 0.45f, 0.9f) }
            }
        }

        // Сменная 3D-нагрузка спереди (x=0, z=1.0)
        gl.push {
            gl.tr(0f, by + 0.12f, 1.0f)
            when (state.loadType) {
                0 -> {
                    // 3D Лампочка накаливания
                    gl.push { gl.sc(0.45f, 0.18f, 0.45f); gl.draw(gl.cylinder, 0.32f, 0.34f, 0.40f) }
                    gl.push { gl.tr(0f, 0.24f, 0f); gl.sc(0.24f, 0.28f, 0.24f); gl.draw(gl.cylinder, 0.75f, 0.75f, 0.78f, shininess = 100f) }
                    val glow = if (state.bulbBurned) 0f else (iCur / 3.0f).coerceIn(0f, 1.2f)
                    // Нить накала
                    gl.push {
                        gl.tr(0f, 0.65f, 0f)
                        gl.sc(0.14f, 0.22f, 0.14f)
                        gl.draw(gl.torus, 1f, 0.86f, 0.35f, emissive = if (state.bulbBurned) 0f else (0.15f + glow * 0.85f))
                    }
                    // Стеклянная колба + ореол света
                    gl.push {
                        gl.tr(0f, 0.72f, 0f)
                        gl.sc(0.46f)
                        if (state.bulbBurned) {
                            gl.draw(gl.sphere, 0.28f, 0.28f, 0.32f, a = 0.55f)
                        } else {
                            gl.draw(gl.sphere, 1f, 0.95f, 0.65f, a = 0.30f + glow * 0.45f, emissive = glow * 0.9f, shininess = 120f)
                        }
                    }
                    if (glow > 0.15f) {
                        gl.push {
                            gl.tr(0f, 0.72f, 0f)
                            gl.sc(0.68f + glow * 0.28f)
                            gl.draw(gl.sphere, 1f, 0.88f, 0.35f, a = (glow * 0.22f).coerceAtMost(0.32f), emissive = 1f)
                        }
                    }
                }
                1 -> {
                    // 3D Электромотор с 3-лопастным пропеллером
                    gl.push { gl.tr(0f, 0.45f, 0f); gl.rot(90f, 1f, 0f, 0f); gl.sc(0.34f, 0.75f, 0.34f); gl.draw(gl.cylinder, 0.22f, 0.50f, 0.92f) }
                    gl.push { gl.tr(0f, 0.18f, 0f); gl.sc(0.44f, 0.34f, 0.52f); gl.draw(gl.box, 0.25f, 0.28f, 0.36f) }
                    gl.push {
                        gl.tr(0f, 0.45f, 0.46f)
                        gl.rot(state.motorAngle, 0f, 0f, 1f)
                        gl.push { gl.sc(0.13f); gl.draw(gl.sphere, 1f, 0.82f, 0.25f) }
                        for (b in 0 until 3) {
                            gl.push {
                                gl.rot(b * 120f, 0f, 0f, 1f)
                                gl.tr(0f, 0.40f, 0f)
                                gl.rot(18f, 0f, 1f, 0f)
                                gl.sc(0.14f, 0.72f, 0.03f)
                                gl.draw(gl.box, 0.22f, 0.94f, 1.0f, emissive = 0.35f)
                            }
                        }
                    }
                }
                else -> {
                    // 3D Электромагнит с медной катушкой, поднимающий железные гайки!
                    gl.push { gl.tr(-0.55f, 0.55f, 0f); gl.sc(0.08f, 1.15f, 0.08f); gl.draw(gl.cylinder, 0.45f, 0.48f, 0.55f) }
                    gl.drawSegment(-0.55f, 1.10f, 0f, 0f, 1.10f, 0f, 0.05f, 0.45f, 0.48f, 0.55f)
                    // Сердечник + 6 витков катушки
                    gl.push { gl.tr(0f, 0.85f, 0f); gl.sc(0.18f, 0.55f, 0.18f); gl.draw(gl.cylinder, 0.52f, 0.56f, 0.62f) }
                    val magGlow = (iCur / 3.0f).coerceIn(0f, 1f)
                    for (k in 0 until 6) {
                        gl.push {
                            gl.tr(0f, 0.66f + k * 0.075f, 0f)
                            gl.sc(0.24f, 0.18f, 0.24f)
                            gl.draw(gl.torus, 0.90f, 0.52f + magGlow * 0.35f, 0.22f + magGlow * 0.7f, emissive = magGlow * 0.6f)
                        }
                    }
                    // 5 железных гаек, взлетающих к катушке при сильном токе
                    val lift = state.magnetLift
                    for (k in 0 until 5) {
                        val ox = (k - 2) * 0.18f * (1f - lift * 0.55f)
                        val oz = sin(k * 2.1f) * 0.14f * (1f - lift * 0.55f)
                        val oy = 0.06f + lift * (0.45f - abs(k - 2) * 0.03f)
                        gl.push {
                            gl.tr(ox, oy, oz)
                            gl.rot(time * 40f * lift + k * 35f, 0f, 1f, 0f)
                            gl.sc(0.11f, 0.08f, 0.11f)
                            gl.draw(gl.box, 0.72f, 0.76f, 0.84f, shininess = 110f)
                        }
                    }
                }
            }
        }
    }

    // =====================================================================
    // 3. 3D ОПТИКА — ПРИЗМА НЬЮТОНА И ЛАЗЕР
    // =====================================================================
    private fun renderOptics() {
        drawLabBench(-1.25f)
        val beamY = -0.38f

        // 3D Лазерный излучатель слева
        gl.push {
            gl.tr(-2.45f, beamY, 0f)
            gl.push { gl.tr(0f, -0.44f, 0f); gl.sc(0.10f, 0.85f, 0.10f); gl.draw(gl.cylinder, 0.35f, 0.38f, 0.45f) }
            gl.push { gl.rot(90f, 0f, 0f, 1f); gl.sc(0.22f, 0.78f, 0.22f); gl.draw(gl.cylinder, 0.22f, 0.26f, 0.36f) }
            gl.push {
                gl.tr(0.40f, 0f, 0f); gl.rot(90f, 0f, 0f, 1f); gl.sc(0.11f, 0.08f, 0.11f)
                val c = if (state.whiteLight) floatArrayOf(1f, 1f, 1f) else wavelengthToRgb(state.wavelengthNm)
                gl.draw(gl.cylinder, c[0], c[1], c[2], emissive = 1f)
            }
        }

        // 3D Экран проекции справа
        val screenX = 2.55f
        gl.push {
            gl.tr(screenX, beamY + 0.12f, 0.25f)
            gl.rot(-12f, 0f, 1f, 0f)
            gl.push { gl.sc(0.10f, 1.55f, 2.55f); gl.draw(gl.box, 0.16f, 0.19f, 0.28f) }
            gl.push { gl.tr(-0.06f, 0f, 0f); gl.sc(0.02f, 1.38f, 2.38f); gl.draw(gl.box, 0.86f, 0.88f, 0.92f) }
        }

        // Поворотный столик + 3D Треугольная Стеклянная Призма в центре
        gl.push {
            gl.tr(0f, -1.16f, 0f)
            gl.sc(0.95f, 0.16f, 0.95f)
            gl.draw(gl.cylinder, 0.65f, 0.52f, 0.24f, shininess = 90f)
        }

        // Вычисляем 3D-лучи (Закон Снеллиуса + дисперсия)
        val nBase = state.refractiveIndex()
        val pAng = state.prismAngleDeg
        val entryX = -0.35f; val entryZ = 0f
        val exitX = 0.35f; val exitZ = 0.06f + pAng * 0.004f

        val inCol = if (state.whiteLight) floatArrayOf(1f, 1f, 1f) else wavelengthToRgb(state.wavelengthNm)
        // Падающий луч от лазера до призмы
        gl.drawSegment(-2.05f, beamY, 0f, entryX, beamY, entryZ, 0.045f, inCol[0], inCol[1], inCol[2], emissive = 1f)

        if (state.whiteLight) {
            // 7 лучей радуги с реалистичной дисперсией (фиолетовый отклоняется сильнее красного!)
            val rainbow = arrayOf(
                floatArrayOf(1.0f, 0.15f, 0.12f), // Красный
                floatArrayOf(1.0f, 0.55f, 0.08f), // Оранжевый
                floatArrayOf(1.0f, 0.94f, 0.15f), // Жёлтый
                floatArrayOf(0.18f, 0.95f, 0.35f), // Зелёный
                floatArrayOf(0.15f, 0.90f, 1.0f), // Голубой
                floatArrayOf(0.22f, 0.42f, 1.0f), // Синий
                floatArrayOf(0.68f, 0.20f, 1.0f), // Фиолетовый
            )
            val dispStrength = (nBase - 1.0f) * 0.85f
            rainbow.forEachIndexed { idx, c ->
                val frac = idx / 6f // 0 = красный, 1 = фиолетовый
                val bendZ = (0.18f + dispStrength * (0.45f + frac * 0.92f)) + pAng * 0.022f
                val hitZ = (exitZ + bendZ).coerceIn(-0.95f, 1.35f)
                // Луч внутри призмы
                gl.drawSegment(entryX, beamY, entryZ, exitX, beamY, exitZ + frac * 0.10f, 0.024f, c[0], c[1], c[2], a = 0.85f, emissive = 1f)
                // Луч от призмы к экрану
                gl.drawSegment(exitX, beamY, exitZ + frac * 0.10f, screenX - 0.12f, beamY, hitZ, 0.032f, c[0], c[1], c[2], emissive = 1f)
                // Светящееся пятно спектра на экране
                gl.push {
                    gl.tr(screenX - 0.10f, beamY, hitZ)
                    gl.sc(0.05f, 0.34f, 0.11f)
                    gl.draw(gl.box, c[0], c[1], c[2], emissive = 1f)
                }
            }
        } else {
            val frac = ((700f - state.wavelengthNm) / 300f).coerceIn(0f, 1f)
            val dispStrength = (nBase - 1.0f) * 0.85f
            val hitZ = (exitZ + 0.18f + dispStrength * (0.45f + frac * 0.92f) + pAng * 0.022f).coerceIn(-0.95f, 1.35f)
            gl.drawSegment(entryX, beamY, entryZ, exitX, beamY, exitZ, 0.038f, inCol[0], inCol[1], inCol[2], emissive = 1f)
            gl.drawSegment(exitX, beamY, exitZ, screenX - 0.12f, beamY, hitZ, 0.044f, inCol[0], inCol[1], inCol[2], emissive = 1f)
            gl.push {
                gl.tr(screenX - 0.10f, beamY, hitZ)
                gl.sc(0.07f, 0.25f, 0.25f)
                gl.draw(gl.sphere, inCol[0], inCol[1], inCol[2], emissive = 1f)
            }
        }

        // Полупрозрачная 3D-призма рисуется ПОСЛЕ лучей для правильного эффекта стекла!
        val (pr, pg, pb, pa) = when (state.materialIdx) {
            0 -> floatArrayOf(0.35f, 0.85f, 1.0f, 0.36f) // Вода
            1 -> floatArrayOf(0.82f, 0.94f, 1.0f, 0.40f) // Стекло
            else -> floatArrayOf(0.75f, 0.98f, 1.0f, 0.52f) // Алмаз
        }
        gl.push {
            gl.tr(0f, beamY, 0f)
            gl.rot(pAng, 0f, 1f, 0f)
            gl.sc(0.85f, 1.45f, 0.85f)
            gl.draw(gl.prism, pr, pg, pb, a = pa, emissive = if (state.materialIdx == 2) 0.22f else 0.08f, shininess = 128f)
        }
    }

    // =====================================================================
    // 4. 3D МАГНИТНОЕ ПОЛЕ И ЛЕВИТАЦИЯ
    // =====================================================================
    private fun renderMagnet() {
        drawLabBench(-1.25f)
        val str = state.magnetStrengthFactor()

        if (state.magnetMode == 1) {
            // Режим ЛЕВИТАЦИЯ: вертикальная направляющая + два 3D-кольцевых магнита!
            gl.push { gl.tr(0f, -1.16f, 0f); gl.sc(1.35f, 0.16f, 1.35f); gl.draw(gl.cylinder, 0.26f, 0.28f, 0.36f) }
            gl.push { gl.tr(0f, 0.05f, 0f); gl.sc(0.11f, 2.45f, 0.11f); gl.draw(gl.cylinder, 0.82f, 0.88f, 0.96f, a = 0.55f) }

            // Нижний неподвижный кольцевой магнит (сверху N красный, снизу S синий)
            val botY = -0.92f
            gl.push { gl.tr(0f, botY + 0.07f, 0f); gl.sc(0.68f, 0.65f, 0.68f); gl.draw(gl.torus, 0.95f, 0.22f, 0.26f) }
            gl.push { gl.tr(0f, botY - 0.07f, 0f); gl.sc(0.68f, 0.65f, 0.68f); gl.draw(gl.torus, 0.20f, 0.48f, 0.98f) }

            // Верхний парящий 3D-магнит
            val topY = botY + 0.22f + state.levitationHeight * 1.15f
            val topBottomIsRed = state.samePoles
            gl.push {
                gl.tr(0f, topY, 0f)
                gl.rot(sin(time * 2.5f) * 3.5f, 0f, 0f, 1f)
                gl.push {
                    gl.tr(0f, -0.07f, 0f); gl.sc(0.68f, 0.65f, 0.68f)
                    if (topBottomIsRed) gl.draw(gl.torus, 0.95f, 0.22f, 0.26f) else gl.draw(gl.torus, 0.20f, 0.48f, 0.98f)
                }
                gl.push {
                    gl.tr(0f, 0.07f, 0f); gl.sc(0.68f, 0.65f, 0.68f)
                    if (topBottomIsRed) gl.draw(gl.torus, 0.20f, 0.48f, 0.98f) else gl.draw(gl.torus, 0.95f, 0.22f, 0.26f)
                }
            }

            // Светящиеся силовые кольца магнитной подушки между магнитами
            if (state.samePoles && state.levitationHeight > 0.22f) {
                for (k in 0 until 4) {
                    val f = (k + ((time * 1.4f) % 1f)) / 4f
                    val fy = botY + 0.18f + f * (topY - botY - 0.34f)
                    val rr = 0.45f + sin(f * PI).toFloat() * (0.45f + str * 0.22f)
                    gl.push {
                        gl.tr(0f, fy, 0f)
                        gl.sc(rr, 0.5f, rr)
                        gl.draw(gl.thinTorus, 0.25f, 0.92f, 1.0f, a = 0.55f, emissive = 0.95f)
                    }
                }
            }
        } else {
            // Режим ПОЛЕ 3D: два горизонтальных 3D-магнита + 36 живых 3D-стрелок компасов!
            val halfGap = 0.52f + state.magnetDistance * 1.45f
            val my = -0.35f

            // Левый магнит (правый полюс — всегда N красный)
            gl.push {
                gl.tr(-halfGap - 0.55f, my, 0f)
                gl.push { gl.tr(-0.30f, 0f, 0f); gl.sc(0.60f, 0.36f, 0.36f); gl.draw(gl.box, 0.20f, 0.48f, 0.98f) }
                gl.push { gl.tr(0.30f, 0f, 0f); gl.sc(0.60f, 0.36f, 0.36f); gl.draw(gl.box, 0.95f, 0.22f, 0.26f) }
            }
            // Правый магнит (левый полюс — N красный если samePoles, или S синий если притяжение)
            gl.push {
                gl.tr(halfGap + 0.55f, my, 0f)
                val leftRed = state.samePoles
                gl.push {
                    gl.tr(-0.30f, 0f, 0f); gl.sc(0.60f, 0.36f, 0.36f)
                    if (leftRed) gl.draw(gl.box, 0.95f, 0.22f, 0.26f) else gl.draw(gl.box, 0.20f, 0.48f, 0.98f)
                }
                gl.push {
                    gl.tr(0.30f, 0f, 0f); gl.sc(0.60f, 0.36f, 0.36f)
                    if (leftRed) gl.draw(gl.box, 0.20f, 0.48f, 0.98f) else gl.draw(gl.box, 0.95f, 0.22f, 0.26f)
                }
            }

            // 4 магнитных полюса (заряды +q / -q) для вычисления истинного 3D-поля B(x,y,z)
            val poles = arrayOf(
                floatArrayOf(-halfGap - 0.85f, my, 0f, -str), // S1
                floatArrayOf(-halfGap - 0.25f, my, 0f, +str), // N1
                floatArrayOf(halfGap + 0.25f, my, 0f, if (state.samePoles) +str else -str),
                floatArrayOf(halfGap + 0.85f, my, 0f, if (state.samePoles) -str else +str),
            )

            // 36 плавающих 3D-стрелок компасов, поворачивающихся точно по вектору B(r)!
            for (ix in -2..2) {
                for (ring in 0 until 6) {
                    val ang = ring * (2.0 * PI / 6.0) + 0.35
                    val rr = 0.88f + (abs(ix) % 2) * 0.26f
                    val px = ix * 0.82f
                    val py = my + (sin(ang) * rr).toFloat()
                    val pz = (cos(ang) * rr).toFloat()
                    if (py < -1.08f) continue

                    var bx = 0f; var by = 0f; var bz = 0f
                    for (p in poles) {
                        val dx = px - p[0]; val dy = py - p[1]; val dz = pz - p[2]
                        val d2 = dx * dx + dy * dy + dz * dz + 0.12f
                        val invD3 = p[3] / (d2 * sqrt(d2))
                        bx += dx * invD3; by += dy * invD3; bz += dz * invD3
                    }
                    val len = sqrt(bx * bx + by * by + bz * bz).coerceAtLeast(0.001f)
                    val nx = bx / len * 0.16f; val ny = by / len * 0.16f; val nz = bz / len * 0.16f
                    gl.drawSegment(px, py, pz, px + nx, py + ny, pz + nz, 0.026f, 1.0f, 0.28f, 0.34f, emissive = 0.55f)
                    gl.drawSegment(px, py, pz, px - nx, py - ny, pz - nz, 0.026f, 0.25f, 0.72f, 1.0f, emissive = 0.45f)
                }
            }

            // Визуализация силы притяжения / отталкивания между полюсами
            val midColor = if (state.samePoles) floatArrayOf(1f, 0.35f, 0.35f) else floatArrayOf(0.25f, 0.95f, 0.65f)
            for (k in 0 until 3) {
                val p = ((time * 1.8f + k * 0.33f) % 1f)
                val xOff = if (state.samePoles) (p * 0.42f) else ((1f - p) * 0.42f)
                gl.push {
                    gl.tr(0f, my, 0f)
                    gl.rot(90f, 0f, 0f, 1f)
                    gl.sc(0.22f + xOff * 0.8f, 0.4f, 0.22f + xOff * 0.8f)
                    gl.draw(gl.thinTorus, midColor[0], midColor[1], midColor[2], a = (1f - p) * 0.65f, emissive = 0.9f)
                }
            }
        }
    }

    // =====================================================================
    // 5. 3D РАКЕТА И КОСМОНАВТИКА
    // =====================================================================
    private fun renderRocket() {
        val alt = state.altitudeKm
        val padDrop = (alt * 2.4f).coerceAtMost(28f)

        // Поверхность планеты и стартовая башня (уходят вниз при наборе высоты!)
        if (padDrop < 24f) {
            val (gr, gg, gb) = when (state.planetIdx) {
                0 -> floatArrayOf(0.18f, 0.48f, 0.24f) // Земля
                1 -> floatArrayOf(0.55f, 0.56f, 0.60f) // Луна
                2 -> floatArrayOf(0.72f, 0.32f, 0.18f) // Марс
                else -> floatArrayOf(0.72f, 0.54f, 0.32f) // Юпитер
            }
            gl.push {
                gl.tr(0f, -1.25f - padDrop, 0f)
                gl.sc(14f, 0.4f, 14f)
                gl.draw(gl.cylinder, gr, gg, gb, shininess = 14f)
            }
            // Бетонная площадка + красная стартовая мачта
            gl.push { gl.tr(0f, -1.03f - padDrop, 0f); gl.sc(1.8f, 0.12f, 1.8f); gl.draw(gl.cylinder, 0.36f, 0.38f, 0.42f) }
            gl.push { gl.tr(-1.25f, 0.35f - padDrop, -0.35f); gl.sc(0.24f, 2.75f, 0.24f); gl.draw(gl.box, 0.88f, 0.26f, 0.20f) }
            gl.drawSegment(-1.15f, 0.95f - padDrop, -0.35f, -0.42f, 0.95f - padDrop, -0.10f, 0.05f, 0.80f, 0.80f, 0.84f)
        }

        // Звёзды в космосе (проявляются с высотой или всегда на Луне)
        val starVis = if (state.planetIdx == 1) 1f else ((alt - 18f) / 45f).coerceIn(0f, 1f)
        if (starVis > 0.02f) {
            for (i in 0 until 45) {
                val a = i * 2.39996f
                val rr = 7.5f + (i % 5) * 1.4f
                val sy = (((i * 1.7f - alt * 0.35f) % 10f + 10f) % 10f) - 4.5f
                gl.push {
                    gl.tr(cos(a) * rr, sy, sin(a) * rr)
                    gl.sc(0.06f + (i % 3) * 0.025f)
                    gl.draw(gl.sphere, 1f, 1f, 0.92f, a = starVis, emissive = 1f)
                }
            }
        }

        // Пролетающие 3D-облака в атмосфере (Земля/Юпитер)
        if ((state.planetIdx == 0 || state.planetIdx == 3) && alt in 0.5f..42f) {
            for (i in 0 until 8) {
                val cy = (((i * 2.5f - alt * 0.9f) % 14f + 14f) % 14f) - 6f
                val cx = if (i % 2 == 0) -2.8f - (i % 3) * 0.6f else 2.8f + (i % 3) * 0.6f
                val cz = -2.2f + (i % 4) * 1.1f
                gl.push { gl.tr(cx, cy, cz); gl.sc(1.2f, 0.45f, 0.9f); gl.draw(gl.sphere, 0.94f, 0.96f, 1f, a = 0.62f) }
            }
        }

        // Детализированная 3D-ракета (лёгкая вибрация при работе двигателя!)
        val shake = if (state.rocketPhase == 1 && state.fuelRemainPct > 0f) 0.035f else 0f
        val rx = sin(time * 55f) * shake
        val rz = cos(time * 49f) * shake

        gl.push {
            gl.tr(rx, 0.22f, rz)
            // Белый корпус + синяя полоса
            gl.push { gl.sc(0.36f, 1.55f, 0.36f); gl.draw(gl.cylinder, 0.94f, 0.95f, 0.98f, shininess = 85f) }
            gl.push { gl.tr(0f, 0.42f, 0f); gl.sc(0.37f, 0.16f, 0.37f); gl.draw(gl.cylinder, 0.18f, 0.52f, 0.98f) }
            // Красный обтекатель (конус)
            gl.push { gl.tr(0f, 1.18f, 0f); gl.sc(0.36f, 0.82f, 0.36f); gl.draw(gl.cone, 0.96f, 0.24f, 0.22f, shininess = 85f) }
            // Иллюминатор
            gl.push { gl.tr(0f, 0.18f, 0.34f); gl.sc(0.14f, 0.14f, 0.08f); gl.draw(gl.sphere, 0.24f, 0.92f, 1.0f, emissive = 0.65f) }
            // 4 стабилизатора
            for (k in 0 until 4) {
                gl.push {
                    gl.rot(k * 90f, 0f, 1f, 0f)
                    gl.tr(0.42f, -0.52f, 0f)
                    gl.rot(-20f, 0f, 0f, 1f)
                    gl.sc(0.36f, 0.56f, 0.06f)
                    gl.draw(gl.box, 0.95f, 0.24f, 0.22f)
                }
            }
            // Сопло двигателя
            gl.push { gl.tr(0f, -0.88f, 0f); gl.sc(0.26f, 0.28f, 0.26f); gl.draw(gl.volcanoCone, 0.28f, 0.30f, 0.35f) }

            // Светящийся факел тяги
            if (state.rocketPhase == 1 && state.fuelRemainPct > 0f) {
                val fLen = 0.75f + (state.thrustKn / 160f) * 1.15f + sin(time * 40f) * 0.14f
                gl.push {
                    gl.tr(0f, -1.02f - fLen * 0.5f, 0f)
                    gl.rot(180f, 1f, 0f, 0f)
                    gl.sc(0.24f, fLen, 0.24f)
                    gl.draw(gl.cone, 1f, 0.68f, 0.18f, a = 0.88f, emissive = 1f)
                }
            }

            // Парашют при спуске
            if (state.rocketPhase == 1 && state.fuelRemainPct <= 0f && state.velocityMs < -3f) {
                gl.drawSegment(-0.18f, 1.35f, 0f, -0.75f, 2.55f, 0f, 0.02f, 0.9f, 0.9f, 0.9f)
                gl.drawSegment(0.18f, 1.35f, 0f, 0.75f, 2.55f, 0f, 0.02f, 0.9f, 0.9f, 0.9f)
                gl.drawSegment(0f, 1.35f, 0.18f, 0f, 2.55f, 0.75f, 0.02f, 0.9f, 0.9f, 0.9f)
                gl.push { gl.tr(0f, 2.68f, 0f); gl.sc(0.95f, 0.45f, 0.95f); gl.draw(gl.sphere, 1f, 0.50f, 0.15f, emissive = 0.25f) }
            }
        }
    }

    // =====================================================================
    // 6. 3D ГРАВИТАЦИЯ И ОРБИТЫ ПЛАНЕТ
    // =====================================================================
    private fun renderGravity() {
        // Фоновые 3D-звёзды
        for (i in 0 until 55) {
            val a = i * 2.39996f
            val rr = 9.5f + (i % 5) * 1.6f
            val sy = sin(i * 1.7f) * 5.2f
            gl.push {
                gl.tr(cos(a) * rr, sy, sin(a) * rr)
                gl.sc(0.055f + (i % 3) * 0.02f)
                gl.draw(gl.sphere, 0.95f, 0.96f, 1f, emissive = 1f)
            }
        }

        // Тонкие орбитальные ориентиры
        for (rRing in floatArrayOf(2.0f, 3.2f, 4.5f)) {
            gl.push {
                gl.sc(rRing, 0.18f, rRing)
                gl.draw(gl.thinTorus, 0.35f, 0.48f, 0.85f, a = 0.22f, emissive = 0.5f)
            }
        }

        // 3D Солнце + пульсирующая корона (размер зависит от массы Солнца!)
        val sunR = 0.48f + (state.sunMass - 1f) * 0.16f
        gl.push {
            gl.sc(sunR)
            gl.draw(gl.sphere, 1.0f, 0.82f, 0.18f, emissive = 1f)
        }
        gl.push {
            gl.sc(sunR * (1.28f + 0.06f * sin(time * 4f)))
            gl.draw(gl.sphere, 1.0f, 0.52f, 0.10f, a = 0.28f, emissive = 1f)
        }

        // Все живые 3D-планеты и их светящиеся орбитальные следы
        synchronized(state.bodies) {
            for (b in state.bodies) {
                // Хвост орбиты
                val n = b.trailX.size
                for (k in 0 until n - 1) {
                    val i0 = (b.trailHead + 1 + k) % n
                    val i1 = (b.trailHead + 2 + k) % n
                    val x0 = b.trailX[i0]; val z0 = b.trailZ[i0]
                    val x1 = b.trailX[i1]; val z1 = b.trailZ[i1]
                    val d2 = (x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0)
                    if (d2 in 0.0004f..1.5f) {
                        val fade = k.toFloat() / n
                        gl.drawSegment(x0, 0f, z0, x1, 0f, z1, 0.022f, b.r, b.g, b.b, a = fade * 0.72f, emissive = 0.9f)
                    }
                }
                // Сама 3D-планета
                gl.push {
                    gl.tr(b.x, 0f, b.z)
                    gl.rot(time * 45f, 0f, 1f, 0f)
                    gl.push { gl.sc(b.radius); gl.draw(gl.sphere, b.r, b.g, b.b, emissive = 0.22f, shininess = 70f) }
                    if (b.radius > 0.20f) {
                        gl.push {
                            gl.rot(24f, 1f, 0f, 0f)
                            gl.sc(b.radius * 1.65f, 0.22f, b.radius * 1.65f)
                            gl.draw(gl.thinTorus, 0.95f, 0.85f, 0.65f, a = 0.75f, emissive = 0.25f)
                        }
                    }
                }
            }
        }
    }

    /** Переводит длину волны света (400..700 нм) в спектральный RGB-цвет. */
    private fun wavelengthToRgb(nm: Float): FloatArray {
        val w = nm.coerceIn(400f, 700f)
        val r: Float; val g: Float; val b: Float
        when {
            w < 440f -> { r = (440f - w) / 60f; g = 0f; b = 1f }
            w < 490f -> { r = 0f; g = (w - 440f) / 50f; b = 1f }
            w < 510f -> { r = 0f; g = 1f; b = (510f - w) / 20f }
            w < 580f -> { r = (w - 510f) / 70f; g = 1f; b = 0f }
            w < 645f -> { r = 1f; g = (645f - w) / 65f; b = 0f }
            else -> { r = 1f; g = 0f; b = 0f }
        }
        return floatArrayOf(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
    }

    companion object {
        private fun defaultYaw(exp: ExperimentId): Float = when (exp) {
            ExperimentId.OPTICS -> 10f
            ExperimentId.MAGNET -> 18f
            ExperimentId.GRAVITY -> 22f
            else -> 28f
        }

        private fun defaultPitch(exp: ExperimentId): Float = when (exp) {
            ExperimentId.GRAVITY -> 44f
            ExperimentId.CIRCUIT -> 34f
            ExperimentId.OPTICS -> 28f
            ExperimentId.ROCKET -> 16f
            else -> 24f
        }

        private fun defaultDist(exp: ExperimentId): Float = when (exp) {
            ExperimentId.GRAVITY -> 9.6f
            ExperimentId.ROCKET -> 6.8f
            ExperimentId.OPTICS -> 6.8f
            else -> 6.4f
        }
    }
}
