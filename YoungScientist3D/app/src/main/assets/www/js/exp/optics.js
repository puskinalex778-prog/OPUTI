// 2D-трассировка луча через треугольную призму (закон преломления Снеллиуса).
// Координаты: (x, z) — плоскость стола.

export function prismVerts(cx, cz, side, angle) {
  const R = side / Math.sqrt(3); const v = [];
  for (let k = 0; k < 3; k++) {
    const a = angle + Math.PI / 2 + k * (2 * Math.PI / 3);
    v.push([cx + R * Math.cos(a), cz + R * Math.sin(a)]);
  }
  return v;
}

function hitSegment(o, d, a, b) {
  const ex = b[0] - a[0], ez = b[1] - a[1];
  const den = d[0] * ez - d[1] * ex;
  if (Math.abs(den) < 1e-9) return null;
  const wx = a[0] - o[0], wz = a[1] - o[1];
  const t = (wx * ez - wz * ex) / den;
  const u = (wx * d[1] - wz * d[0]) / den;
  if (t > 1e-6 && u >= 0 && u <= 1) return t;
  return null;
}

// Возвращает массив точек пути луча.
export function trace(origin, dir, verts, n, screenX, maxLen = 14) {
  const pts = [origin.slice()];
  let o = origin.slice(), d = norm(dir), inside = false, lastEdge = -1;
  const cx = (verts[0][0] + verts[1][0] + verts[2][0]) / 3, cz = (verts[0][1] + verts[1][1] + verts[2][1]) / 3;
  for (let bounce = 0; bounce < 8; bounce++) {
    let best = null, bi = -1;
    for (let i = 0; i < 3; i++) {
      if (i === lastEdge) continue;
      const t = hitSegment(o, d, verts[i], verts[(i + 1) % 3]);
      if (t !== null && (best === null || t < best)) { best = t; bi = i; }
    }
    if (best === null) break;
    const p = [o[0] + d[0] * best, o[1] + d[1] * best];
    pts.push(p);
    const a = verts[bi], b = verts[(bi + 1) % 3];
    let nx = b[1] - a[1], nz = -(b[0] - a[0]);
    const l = Math.hypot(nx, nz); nx /= l; nz /= l;
    // нормаль наружу
    const mx = (a[0] + b[0]) / 2 - cx, mz = (a[1] + b[1]) / 2 - cz;
    if (nx * mx + nz * mz < 0) { nx = -nx; nz = -nz; }
    // для преломления нормаль против направления луча
    let N = [nx, nz]; if (d[0] * nx + d[1] * nz > 0) N = [-nx, -nz];
    const eta = inside ? n : 1 / n;
    const cosi = -(N[0] * d[0] + N[1] * d[1]);
    const k = 1 - eta * eta * (1 - cosi * cosi);
    if (k < 0) { // полное внутреннее отражение
      d = norm([d[0] + 2 * cosi * N[0], d[1] + 2 * cosi * N[1]]);
    } else {
      const c = eta * cosi - Math.sqrt(k);
      d = norm([eta * d[0] + c * N[0], eta * d[1] + c * N[1]]);
      inside = !inside;
    }
    o = p; lastEdge = bi;
    if (!inside) break;
  }
  // финальный отрезок до экрана
  let hitScreen = false;
  if (!inside) {
    let L = maxLen;
    if (d[0] > 1e-6) { const t = (screenX - o[0]) / d[0]; if (t > 0 && t < L) { L = t; hitScreen = true; } }
    pts.push([o[0] + d[0] * L, o[1] + d[1] * L]);
  }
  return { pts, hitScreen, end: pts[pts.length - 1], passed: pts.length >= 4 };
}

function norm(v) { const l = Math.hypot(v[0], v[1]) || 1; return [v[0] / l, v[1] / l]; }

// Показатели преломления (дисперсия чуть усилена, чтобы радуга была заметнее).
export const SPECTRUM = [
  { name: 'красный', color: 0xff2020, n: 1.48 },
  { name: 'оранжевый', color: 0xff8a00, n: 1.495 },
  { name: 'жёлтый', color: 0xffe000, n: 1.51 },
  { name: 'зелёный', color: 0x20e040, n: 1.525 },
  { name: 'голубой', color: 0x20c8ff, n: 1.54 },
  { name: 'синий', color: 0x2050ff, n: 1.555 },
  { name: 'фиолетовый', color: 0x9a30ff, n: 1.575 },
];
