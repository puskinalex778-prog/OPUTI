package com.lab3d.science.engine

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Процедурный 3D-меш (x, y, z, nx, ny, nz) с индексным буфером. */
class Mesh3D(vertices: FloatArray, indices: ShortArray) {
    val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
    val indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }
    val indexCount: Int = indices.size

    companion object {
        fun sphere(stacks: Int = 14, slices: Int = 22): Mesh3D {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            for (i in 0..stacks) {
                val phi = PI * i / stacks
                val y = cos(phi).toFloat()
                val r = sin(phi).toFloat()
                for (j in 0..slices) {
                    val th = 2.0 * PI * j / slices
                    val x = (r * cos(th)).toFloat()
                    val z = (r * sin(th)).toFloat()
                    v.add(x); v.add(y); v.add(z); v.add(x); v.add(y); v.add(z)
                }
            }
            val row = slices + 1
            for (i in 0 until stacks) {
                for (j in 0 until slices) {
                    val a = (i * row + j).toShort()
                    val b = ((i + 1) * row + j).toShort()
                    val c = ((i + 1) * row + j + 1).toShort()
                    val d = (i * row + j + 1).toShort()
                    idx.add(a); idx.add(b); idx.add(d)
                    idx.add(b); idx.add(c); idx.add(d)
                }
            }
            return Mesh3D(v.toFloatArray(), idx.toShortArray())
        }

        fun cylinder(slices: Int = 20): Mesh3D = frustum(1f, 1f, slices)

        fun cone(slices: Int = 20): Mesh3D = frustum(0.001f, 1f, slices)

        /** Усечённый конус: верх радиуса topR в y=+0.5, низ радиуса botR в y=-0.5. */
        fun frustum(topR: Float, botR: Float, slices: Int = 22): Mesh3D {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            val dr = botR - topR
            val slopeLen = sqrt(1f + dr * dr)
            val ny = dr / slopeLen
            val nr = 1f / slopeLen

            for (j in 0..slices) {
                val a = 2.0 * PI * j / slices
                val cx = cos(a).toFloat()
                val cz = sin(a).toFloat()
                v.add(cx * topR); v.add(0.5f); v.add(cz * topR); v.add(cx * nr); v.add(ny); v.add(cz * nr)
                v.add(cx * botR); v.add(-0.5f); v.add(cz * botR); v.add(cx * nr); v.add(ny); v.add(cz * nr)
            }
            for (j in 0 until slices) {
                val t0 = (j * 2).toShort()
                val b0 = (j * 2 + 1).toShort()
                val t1 = (j * 2 + 2).toShort()
                val b1 = (j * 2 + 3).toShort()
                idx.add(t0); idx.add(b0); idx.add(t1)
                idx.add(b0); idx.add(b1); idx.add(t1)
            }
            if (topR > 0.01f) {
                val tc = (v.size / 6).toShort()
                v.add(0f); v.add(0.5f); v.add(0f); v.add(0f); v.add(1f); v.add(0f)
                val ts = v.size / 6
                for (j in 0..slices) {
                    val a = 2.0 * PI * j / slices
                    v.add(cos(a).toFloat() * topR); v.add(0.5f); v.add(sin(a).toFloat() * topR)
                    v.add(0f); v.add(1f); v.add(0f)
                }
                for (j in 0 until slices) {
                    idx.add(tc); idx.add((ts + j + 1).toShort()); idx.add((ts + j).toShort())
                }
            }
            if (botR > 0.01f) {
                val bc = (v.size / 6).toShort()
                v.add(0f); v.add(-0.5f); v.add(0f); v.add(0f); v.add(-1f); v.add(0f)
                val bs = v.size / 6
                for (j in 0..slices) {
                    val a = 2.0 * PI * j / slices
                    v.add(cos(a).toFloat() * botR); v.add(-0.5f); v.add(sin(a).toFloat() * botR)
                    v.add(0f); v.add(-1f); v.add(0f)
                }
                for (j in 0 until slices) {
                    idx.add(bc); idx.add((bs + j).toShort()); idx.add((bs + j + 1).toShort())
                }
            }
            return Mesh3D(v.toFloatArray(), idx.toShortArray())
        }

        fun box(): Mesh3D {
            val faces = arrayOf(
                floatArrayOf(0f, 0f, 1f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f),
                floatArrayOf(0f, 0f, -1f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f),
                floatArrayOf(1f, 0f, 0f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f),
                floatArrayOf(-1f, 0f, 0f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f),
                floatArrayOf(0f, 1f, 0f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f),
                floatArrayOf(0f, -1f, 0f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f),
            )
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            faces.forEachIndexed { fi, f ->
                val nx = f[0]; val ny = f[1]; val nz = f[2]
                for (k in 0 until 4) {
                    v.add(f[3 + k * 3]); v.add(f[4 + k * 3]); v.add(f[5 + k * 3])
                    v.add(nx); v.add(ny); v.add(nz)
                }
                val base = (fi * 4).toShort()
                idx.add(base); idx.add((base + 1).toShort()); idx.add((base + 2).toShort())
                idx.add(base); idx.add((base + 2).toShort()); idx.add((base + 3).toShort())
            }
            return Mesh3D(v.toFloatArray(), idx.toShortArray())
        }

        fun torus(majorSegs: Int = 28, minorSegs: Int = 12, tubeR: Float = 0.14f): Mesh3D {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            for (i in 0..majorSegs) {
                val u = 2.0 * PI * i / majorSegs
                val cu = cos(u).toFloat(); val su = sin(u).toFloat()
                for (j in 0..minorSegs) {
                    val w = 2.0 * PI * j / minorSegs
                    val cw = cos(w).toFloat(); val sw = sin(w).toFloat()
                    val r = 1f + tubeR * cw
                    v.add(r * cu); v.add(tubeR * sw); v.add(r * su)
                    v.add(cw * cu); v.add(sw); v.add(cw * su)
                }
            }
            val row = minorSegs + 1
            for (i in 0 until majorSegs) {
                for (j in 0 until minorSegs) {
                    val a = (i * row + j).toShort()
                    val b = ((i + 1) * row + j).toShort()
                    val c = ((i + 1) * row + j + 1).toShort()
                    val d = (i * row + j + 1).toShort()
                    idx.add(a); idx.add(b); idx.add(d)
                    idx.add(b); idx.add(c); idx.add(d)
                }
            }
            return Mesh3D(v.toFloatArray(), idx.toShortArray())
        }

        /** Равносторонняя треугольная 3D-призма (ось Y от -0.5 до +0.5). */
        fun triangularPrism(): Mesh3D {
            val pts = Array(3) { i ->
                val a = Math.toRadians(90.0 + i * 120.0)
                floatArrayOf(cos(a).toFloat(), sin(a).toFloat())
            }
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            fun addQuad(p0: FloatArray, p1: FloatArray) {
                val ex = p1[0] - p0[0]
                val ez = p1[1] - p0[1]
                val len = sqrt(ex * ex + ez * ez)
                val nx = ez / len
                val nz = -ex / len
                val base = (v.size / 6).toShort()
                v.add(p0[0]); v.add(-0.5f); v.add(p0[1]); v.add(nx); v.add(0f); v.add(nz)
                v.add(p1[0]); v.add(-0.5f); v.add(p1[1]); v.add(nx); v.add(0f); v.add(nz)
                v.add(p1[0]); v.add(0.5f); v.add(p1[1]); v.add(nx); v.add(0f); v.add(nz)
                v.add(p0[0]); v.add(0.5f); v.add(p0[1]); v.add(nx); v.add(0f); v.add(nz)
                idx.add(base); idx.add((base + 1).toShort()); idx.add((base + 2).toShort())
                idx.add(base); idx.add((base + 2).toShort()); idx.add((base + 3).toShort())
            }
            addQuad(pts[0], pts[1])
            addQuad(pts[1], pts[2])
            addQuad(pts[2], pts[0])
            // Верхняя крышка
            val tb = (v.size / 6).toShort()
            for (p in pts) { v.add(p[0]); v.add(0.5f); v.add(p[1]); v.add(0f); v.add(1f); v.add(0f) }
            idx.add(tb); idx.add((tb + 1).toShort()); idx.add((tb + 2).toShort())
            // Нижняя крышка
            val bb = (v.size / 6).toShort()
            for (p in pts) { v.add(p[0]); v.add(-0.5f); v.add(p[1]); v.add(0f); v.add(-1f); v.add(0f) }
            idx.add(bb); idx.add((bb + 2).toShort()); idx.add((bb + 1).toShort())
            return Mesh3D(v.toFloatArray(), idx.toShortArray())
        }
    }
}

/** Пул 3D-частиц (лава, пена, искры, огонь ракеты, звёзды) без аллокаций в кадре. */
class ParticlePool3D(val capacity: Int = 320) {
    val active = BooleanArray(capacity)
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val z = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val vz = FloatArray(capacity)
    val r = FloatArray(capacity)
    val g = FloatArray(capacity)
    val b = FloatArray(capacity)
    val size = FloatArray(capacity)
    val life = FloatArray(capacity)
    val maxLife = FloatArray(capacity)
    val gravity = FloatArray(capacity)
    private var cursor = 0

    fun clear() = active.fill(false)

    fun spawn(
        px: Float, py: Float, pz: Float,
        pvx: Float, pvy: Float, pvz: Float,
        cr: Float, cg: Float, cb: Float,
        pSize: Float, pLife: Float, pGrav: Float = -6.5f,
    ) {
        val i = cursor
        cursor = (cursor + 1) % capacity
        active[i] = true
        x[i] = px; y[i] = py; z[i] = pz
        vx[i] = pvx; vy[i] = pvy; vz[i] = pvz
        r[i] = cr; g[i] = cg; b[i] = cb
        size[i] = pSize
        life[i] = pLife
        maxLife[i] = pLife.coerceAtLeast(0.05f)
        gravity[i] = pGrav
    }

    fun update(dt: Float, floorY: Float = -1.25f) {
        for (i in 0 until capacity) {
            if (!active[i]) continue
            life[i] -= dt
            if (life[i] <= 0f) {
                active[i] = false
                continue
            }
            vy[i] += gravity[i] * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            z[i] += vz[i] * dt
            if (y[i] < floorY && gravity[i] < 0f) {
                y[i] = floorY
                vy[i] = -vy[i] * 0.25f
                vx[i] *= 0.72f
                vz[i] *= 0.72f
            }
        }
    }
}

/** Базовый движок рендеринга OpenGL ES 2.0 с матричным стеком и Blinn-Phong + Rim шейдером. */
class Gl3DContext {
    private var program = 0
    private var aPos = 0
    private var aNormal = 0
    private var uMVP = 0
    private var uModel = 0
    private var uNormalMat = 0
    private var uColor = 0
    private var uEmissive = 0
    private var uShininess = 0
    private var uEyePos = 0
    private var uAccentLight = 0

    lateinit var sphere: Mesh3D; private set
    lateinit var cylinder: Mesh3D; private set
    lateinit var cone: Mesh3D; private set
    lateinit var volcanoCone: Mesh3D; private set
    lateinit var flaskCone: Mesh3D; private set
    lateinit var box: Mesh3D; private set
    lateinit var torus: Mesh3D; private set
    lateinit var thinTorus: Mesh3D; private set
    lateinit var prism: Mesh3D; private set

    val proj = FloatArray(16)
    val view = FloatArray(16)
    val viewProj = FloatArray(16)
    val eyePos = FloatArray(3)

    private val stack = Array(20) { FloatArray(16) }
    private var sp = 0
    private val mvp = FloatArray(16)
    private val inv = FloatArray(16)
    private val norm4 = FloatArray(16)
    private val norm3 = FloatArray(9)

    fun initGl() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = buildProgram(VS, FS)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        uMVP = GLES20.glGetUniformLocation(program, "uMVP")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uNormalMat = GLES20.glGetUniformLocation(program, "uNormalMat")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uEmissive = GLES20.glGetUniformLocation(program, "uEmissive")
        uShininess = GLES20.glGetUniformLocation(program, "uShininess")
        uEyePos = GLES20.glGetUniformLocation(program, "uEyePos")
        uAccentLight = GLES20.glGetUniformLocation(program, "uAccentLight")

        sphere = Mesh3D.sphere(14, 22)
        cylinder = Mesh3D.cylinder(20)
        cone = Mesh3D.cone(20)
        volcanoCone = Mesh3D.frustum(0.32f, 1.0f, 24)
        flaskCone = Mesh3D.frustum(0.36f, 0.92f, 22)
        box = Mesh3D.box()
        torus = Mesh3D.torus(28, 12, 0.22f)
        thinTorus = Mesh3D.torus(36, 10, 0.055f)
        prism = Mesh3D.triangularPrism()
    }

    fun resize(w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(proj, 0, 44f, w.toFloat() / max(h, 1).toFloat(), 0.1f, 120f)
    }

    fun beginFrame(
        bgR: Float, bgG: Float, bgB: Float,
        ex: Float, ey: Float, ez: Float,
        tx: Float, ty: Float, tz: Float,
        accentR: Float, accentG: Float, accentB: Float,
    ) {
        GLES20.glClearColor(bgR, bgG, bgB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        eyePos[0] = ex; eyePos[1] = ey; eyePos[2] = ez
        Matrix.setLookAtM(view, 0, ex, ey, ez, tx, ty, tz, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)

        GLES20.glUniform3fv(uEyePos, 1, eyePos, 0)
        GLES20.glUniform3f(uAccentLight, accentR, accentG, accentB)

        sp = 0
        Matrix.setIdentityM(stack[0], 0)
    }

    inline fun push(block: () -> Unit) {
        pushMatrix()
        block()
        popMatrix()
    }

    fun pushMatrix() {
        if (sp + 1 < stack.size) {
            System.arraycopy(stack[sp], 0, stack[sp + 1], 0, 16)
            sp++
        }
    }

    fun popMatrix() {
        if (sp > 0) sp--
    }

    fun tr(x: Float, y: Float, z: Float) = Matrix.translateM(stack[sp], 0, x, y, z)
    fun rot(deg: Float, x: Float, y: Float, z: Float) = Matrix.rotateM(stack[sp], 0, deg, x, y, z)
    fun sc(x: Float, y: Float, z: Float) =
        Matrix.scaleM(stack[sp], 0, max(x, 0.002f), max(y, 0.002f), max(z, 0.002f))
    fun sc(s: Float) = sc(s, s, s)

    fun draw(
        mesh: Mesh3D,
        r: Float, g: Float, b: Float,
        a: Float = 1f,
        emissive: Float = 0f,
        shininess: Float = 42f,
    ) {
        val model = stack[sp]
        Matrix.multiplyMM(mvp, 0, viewProj, 0, model, 0)
        if (Matrix.invertM(inv, 0, model, 0)) {
            Matrix.transposeM(norm4, 0, inv, 0)
            norm3[0] = norm4[0]; norm3[1] = norm4[1]; norm3[2] = norm4[2]
            norm3[3] = norm4[4]; norm3[4] = norm4[5]; norm3[5] = norm4[6]
            norm3[6] = norm4[8]; norm3[7] = norm4[9]; norm3[8] = norm4[10]
        } else {
            norm3[0] = 1f; norm3[1] = 0f; norm3[2] = 0f
            norm3[3] = 0f; norm3[4] = 1f; norm3[5] = 0f
            norm3[6] = 0f; norm3[7] = 0f; norm3[8] = 1f
        }

        if (a < 0.99f) GLES20.glDepthMask(false)

        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniformMatrix3fv(uNormalMat, 1, false, norm3, 0)
        GLES20.glUniform4f(uColor, r, g, b, a)
        GLES20.glUniform1f(uEmissive, emissive)
        GLES20.glUniform1f(uShininess, shininess)

        val vb = mesh.vertexBuffer
        vb.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, vb)
        vb.position(3)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, vb)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        if (a < 0.99f) GLES20.glDepthMask(true)
    }

    /** Рисует 3D-отрезок (провод, луч света, стрелку, штангу) от A до B. */
    fun drawSegment(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        radius: Float,
        r: Float, g: Float, b: Float,
        a: Float = 1f,
        emissive: Float = 0f,
    ) {
        val dx = x2 - x1; val dy = y2 - y1; val dz = z2 - z1
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 0.001f) return
        push {
            tr((x1 + x2) * 0.5f, (y1 + y2) * 0.5f, (z1 + z2) * 0.5f)
            val ny = (dy / len).coerceIn(-1f, 1f)
            if (ny < -0.9995f) {
                rot(180f, 1f, 0f, 0f)
            } else if (ny < 0.9995f) {
                val ax = dz / len
                val az = -dx / len
                rot(Math.toDegrees(acos(ny.toDouble())).toFloat(), ax, 0f, az)
            }
            sc(radius, len, radius)
            draw(cylinder, r, g, b, a, emissive)
        }
    }

    /** Рисует пул 3D-частиц. */
    fun drawParticles(pool: ParticlePool3D) {
        for (i in 0 until pool.capacity) {
            if (!pool.active[i]) continue
            val k = (pool.life[i] / pool.maxLife[i]).coerceIn(0f, 1f)
            push {
                tr(pool.x[i], pool.y[i], pool.z[i])
                sc(pool.size[i] * (0.35f + 0.65f * k))
                draw(sphere, pool.r[i], pool.g[i], pool.b[i], a = (k * 1.15f).coerceAtMost(0.95f), emissive = 0.72f)
            }
        }
    }

    private fun buildProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    companion object {
        private const val VS = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform mat3 uNormalMat;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            void main() {
                vec4 wp = uModel * vec4(aPos, 1.0);
                vWorldPos = wp.xyz;
                vNormal = normalize(uNormalMat * aNormal);
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """

        private const val FS = """
            precision mediump float;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            uniform vec4 uColor;
            uniform float uEmissive;
            uniform float uShininess;
            uniform vec3 uEyePos;
            uniform vec3 uAccentLight;

            void main() {
                vec3 N = normalize(vNormal);
                vec3 V = normalize(uEyePos - vWorldPos);

                // Ключевой лабораторный свет
                vec3 L1 = normalize(vec3(3.5, 5.5, 4.5));
                float d1 = max(dot(N, L1), 0.0);
                vec3 H1 = normalize(L1 + V);
                float sp1 = pow(max(dot(N, H1), 0.0), uShininess) * 0.48;

                // Заполняющий акцентный свет
                vec3 L2 = normalize(vec3(-4.0, 2.0, 2.5));
                float d2 = max(dot(N, L2), 0.0) * 0.35;

                // Контровой свет сзади
                vec3 L3 = normalize(vec3(2.0, 3.0, -4.5));
                float d3 = max(dot(N, L3), 0.0) * 0.25;

                float hemi = 0.5 + 0.5 * N.y;
                vec3 amb = mix(vec3(0.16, 0.18, 0.28), vec3(0.42, 0.44, 0.52), hemi);

                float rim = pow(1.0 - max(dot(N, V), 0.0), 2.6) * 0.30;
                vec3 lit = uColor.rgb * (amb + vec3(0.95) * d1 + uAccentLight * d2 + vec3(0.7, 0.85, 1.0) * d3)
                         + vec3(1.0) * sp1
                         + uAccentLight * rim;
                vec3 finalCol = mix(lit, uColor.rgb * 1.18, clamp(uEmissive, 0.0, 1.0));
                gl_FragColor = vec4(finalCol, uColor.a);
            }
        """
    }
}
