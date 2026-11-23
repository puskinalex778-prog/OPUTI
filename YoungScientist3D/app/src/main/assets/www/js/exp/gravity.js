import * as THREE from 'three';
import { shadowAll, rand } from '../common.js';

const H = 10; // высота башни, м
const PLANETS = [
  { id: 'earth', name: 'Земля', emoji: '🌍', g: 9.8, air: 1, sky: 0x87c8f0, ground: 0x5aa04a, note: 'Есть воздух — он мешает перу падать.' },
  { id: 'moon', name: 'Луна', emoji: '🌕', g: 1.62, air: 0, sky: 0x020208, ground: 0x9c9c9c, note: 'Воздуха нет совсем!' },
  { id: 'mars', name: 'Марс', emoji: '🔴', g: 3.71, air: 0.01, sky: 0xd99a6c, ground: 0xb4552d, note: 'Воздух очень разреженный.' },
  { id: 'jupiter', name: 'Юпитер', emoji: '🪐', g: 24.8, air: 1, sky: 0xd8b07a, ground: 0xc9a47a, note: 'Представим, что можно стоять на облаках!' },
];
const FEATHER_VT = 1.25; // предельная скорость пера на Земле, м/с
const fmt = (x, d = 2) => x.toFixed(d).replace('.', ',');

export default {
  id: 'gravity', title: 'Гравитация на планетах', emoji: '🪐', subject: 'Астрономия', color: '#ffb02e',
  short: 'Брось молоток и перо на Земле, Луне, Марсе и Юпитере. Что упадёт быстрее?',
  why: `<h2>🪐 Почему всё падает по-разному?</h2>
    <p><b>Гравитация</b> — это сила, с которой планета притягивает к себе всё вокруг. Чем больше и тяжелее планета, тем сильнее она тянет.</p>
    <ul><li>🌍 Земля: 9,8 м/с² — привычная нам сила.</li>
    <li>🌕 Луна: в 6 раз слабее! Поэтому космонавты там прыгают, как кенгуру.</li>
    <li>🔴 Марс: почти в 3 раза слабее, чем на Земле.</li>
    <li>🪐 Юпитер: в 2,5 раза сильнее — там ты весил бы как маленький медведь!</li></ul>
    <p><b>А почему перо падает медленно?</b> Не из-за того, что оно лёгкое, а из-за <b>воздуха</b>. Воздух мешает перу, как будто оно летит на маленьком парашюте. На Луне воздуха нет — и молоток с пером падают <b>одновременно</b>!</p>
    <p class="fact">🚀 <b>Факт:</b> в 1971 году астронавт Дэвид Скотт на Луне уронил молоток и соколиное перо. Они упали вместе — точно как в нашем опыте!</p>
    <p class="home">🏠 <b>Попробуй дома:</b> урони одновременно книгу и лист бумаги — лист отстанет. А теперь положи лист <b>на</b> книгу и урони их вместе. Лист не отстанет — книга «закрывает» его от воздуха!</p>`,
  quiz: [
    { q: 'Где молоток и перо упадут одновременно?', a: ['На Земле', 'На Луне', 'На Юпитере', 'Нигде'], ok: 1,
      note: 'На Луне нет воздуха, который тормозил бы перо.' },
    { q: 'Почему на Земле перо падает медленнее молотка?', a: ['Перо ничего не весит', 'Ему мешает воздух', 'Земля не притягивает перья', 'Перо волшебное'], ok: 1,
      note: 'Сопротивление воздуха тормозит лёгкое и широкое перо.' },
    { q: 'На какой планете ты будешь весить больше всего?', a: ['На Луне', 'На Марсе', 'На Земле', 'На Юпитере'], ok: 3,
      note: 'Гравитация Юпитера в 2,5 раза сильнее земной.' },
  ],

  start(ctx) {
    const { scene } = ctx.newScene({ bg: 0x87c8f0, camPos: [13, 7, 17], target: [0, 5, 0], minDist: 8, maxDist: 40 });
    const hemi = new THREE.HemisphereLight(0xffffff, 0x404040, 1.2); scene.add(hemi);
    const sun = new THREE.DirectionalLight(0xffffff, 2.4); sun.position.set(10, 20, 8); sun.castShadow = true;
    sun.shadow.mapSize.set(1024, 1024); Object.assign(sun.shadow.camera, { left: -12, right: 12, top: 14, bottom: -4, far: 60 }); scene.add(sun);

    const groundMat = new THREE.MeshStandardMaterial({ color: 0x5aa04a, roughness: 1, flatShading: true });
    const gg = new THREE.PlaneGeometry(120, 120, 40, 40);
    const gp = gg.attributes.position; for (let i = 0; i < gp.count; i++) { const x = gp.getX(i), y = gp.getY(i); if (Math.hypot(x, y) > 6) gp.setZ(i, Math.sin(x * 0.3) * Math.cos(y * 0.25) * 0.6); }
    gg.computeVertexNormals();
    const ground = new THREE.Mesh(gg, groundMat); ground.rotation.x = -Math.PI / 2; ground.receiveShadow = true; scene.add(ground);

    // башня
    const tower = new THREE.Group();
    const steel = new THREE.MeshStandardMaterial({ color: 0xff6a3a, metalness: 0.3, roughness: 0.5 });
    [[-1.2, -1.2], [1.2, -1.2], [-1.2, -2.6], [1.2, -2.6]].forEach(([x, z]) => { const p = new THREE.Mesh(new THREE.BoxGeometry(0.18, H, 0.18), steel); p.position.set(x, H / 2, z); tower.add(p); });
    for (let y = 1; y < H; y += 1.5) { const b = new THREE.Mesh(new THREE.BoxGeometry(2.6, 0.1, 0.1), steel); b.position.set(0, y, -1.2); tower.add(b); const b2 = b.clone(); b2.position.z = -2.6; tower.add(b2); }
    const plat = new THREE.Mesh(new THREE.BoxGeometry(3.2, 0.2, 2.2), new THREE.MeshStandardMaterial({ color: 0x506080 })); plat.position.set(0, H, -1.7); tower.add(plat);
    const rail = new THREE.Mesh(new THREE.BoxGeometry(3.2, 0.06, 0.06), steel); rail.position.set(0, H + 0.9, -2.75); tower.add(rail);
    // метки высоты
    for (let y = 0; y <= H; y += 2) { const l = ctx.makeLabel(y + ' м', { fontSize: 40, scale: 0.55 }); l.position.set(-2.2, y + 0.3, -1.2); tower.add(l); }
    shadowAll(tower); scene.add(tower);

    // молоток и перо
    const hammer = new THREE.Group();
    const hh = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.07, 1.0, 10), new THREE.MeshStandardMaterial({ color: 0x9a6a3a })); hammer.add(hh);
    const hd = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.22, 0.22), new THREE.MeshStandardMaterial({ color: 0x8890a0, metalness: 0.8, roughness: 0.3 })); hd.position.y = 0.5; hammer.add(hd);
    shadowAll(hammer); scene.add(hammer);
    const feather = new THREE.Group();
    const fs = new THREE.Mesh(new THREE.SphereGeometry(0.35, 16, 8), new THREE.MeshStandardMaterial({ color: 0xfff4e0, side: THREE.DoubleSide, roughness: 1 }));
    fs.scale.set(0.35, 1.5, 0.04); feather.add(fs);
    const quill = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 1.2, 6), new THREE.MeshStandardMaterial({ color: 0xc0a080 })); quill.position.y = -0.1; feather.add(quill);
    shadowAll(feather); scene.add(feather);

    // декорации планет
    const deco = {};
    deco.earth = new THREE.Group();
    for (let i = 0; i < 18; i++) {
      const tr = new THREE.Group(); const a = rand(0, 6.28), r = rand(12, 40);
      const trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.3, 1.6), new THREE.MeshStandardMaterial({ color: 0x7a4a2a })); trunk.position.y = 0.8; tr.add(trunk);
      const crown = new THREE.Mesh(new THREE.ConeGeometry(1.2, 3, 8), new THREE.MeshStandardMaterial({ color: 0x2f8a3a, flatShading: true })); crown.position.y = 3; tr.add(crown);
      tr.position.set(Math.cos(a) * r, 0, Math.sin(a) * r - 8); deco.earth.add(tr);
    }
    for (let i = 0; i < 5; i++) { const c = new THREE.Mesh(new THREE.SphereGeometry(2, 10, 8), new THREE.MeshStandardMaterial({ color: 0xffffff, flatShading: true })); c.scale.set(2, 0.6, 1); c.position.set(rand(-40, 40), rand(18, 26), rand(-50, -30)); deco.earth.add(c); }
    deco.moon = new THREE.Group();
    const earthBall = new THREE.Mesh(new THREE.SphereGeometry(4, 32, 16), new THREE.MeshStandardMaterial({ color: 0x3a7bd5, emissive: 0x0a2050 }));
    earthBall.position.set(-30, 28, -60); deco.moon.add(earthBall);
    for (let i = 0; i < 25; i++) { const cr = new THREE.Mesh(new THREE.RingGeometry(rand(0.6, 1.2), rand(1.3, 2.5), 24), new THREE.MeshStandardMaterial({ color: 0x777777, side: THREE.DoubleSide })); cr.rotation.x = -Math.PI / 2; const a = rand(0, 6.28), r = rand(5, 35); cr.position.set(Math.cos(a) * r, 0.05, Math.sin(a) * r); deco.moon.add(cr); }
    const sg = new THREE.BufferGeometry(); const sa = []; for (let i = 0; i < 600; i++) { const a = rand(0, 6.28), e = rand(0.05, 1.4); sa.push(Math.cos(a) * Math.cos(e) * 90, Math.sin(e) * 90, Math.sin(a) * Math.cos(e) * 90); }
    sg.setAttribute('position', new THREE.Float32BufferAttribute(sa, 3)); deco.moon.add(new THREE.Points(sg, new THREE.PointsMaterial({ color: 0xffffff, size: 0.4 })));
    deco.mars = new THREE.Group();
    for (let i = 0; i < 30; i++) { const rk = new THREE.Mesh(new THREE.DodecahedronGeometry(rand(0.2, 1.2)), new THREE.MeshStandardMaterial({ color: 0x8a3a1a, flatShading: true })); const a = rand(0, 6.28), r = rand(5, 40); rk.position.set(Math.cos(a) * r, 0.2, Math.sin(a) * r); deco.mars.add(rk); }
    deco.jupiter = new THREE.Group();
    for (let i = 0; i < 6; i++) { const band = new THREE.Mesh(new THREE.TorusGeometry(80, 3, 6, 60), new THREE.MeshBasicMaterial({ color: i % 2 ? 0xb07840 : 0xe8c89a })); band.rotation.x = Math.PI / 2; band.position.y = 10 + i * 8; deco.jupiter.add(band); }
    Object.values(deco).forEach((g) => { shadowAll(g); scene.add(g); });

    // пыль при ударе
    const dust = []; const dustGeo = new THREE.SphereGeometry(0.12, 6, 4);
    function puff(x, z, color) {
      for (let i = 0; i < 14; i++) {
        const m = new THREE.Mesh(dustGeo, new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.8 }));
        m.position.set(x, 0.1, z); scene.add(m);
        dust.push({ m, vx: rand(-1.5, 1.5), vy: rand(0.5, 2), vz: rand(-1.5, 1.5), life: 1.2 });
      }
    }

    let planet = PLANETS[0], weight = 35;
    const st = { running: false, t: 0, h: { y: H + 0.6, v: 0, done: false, time: 0 }, f: { y: H + 0.6, v: 0, done: false, time: 0 } };
    const visited = new Set();
    const labels = [];

    function applyPlanet() {
      scene.background = new THREE.Color(planet.sky);
      groundMat.color.set(planet.ground);
      Object.entries(deco).forEach(([k, g]) => { g.visible = k === planet.id; });
      hemi.intensity = planet.id === 'moon' ? 0.5 : 1.2;
      scene.fog = planet.air > 0.5 ? new THREE.Fog(planet.sky, 40, 110) : null;
      resetDrop();
      showInfo();
    }
    function resetDrop() {
      st.running = false;
      st.h = { y: H + 0.5, v: 0, done: false, time: 0 }; st.f = { y: H + 0.5, v: 0, done: false, time: 0 };
      labels.forEach((l) => scene.remove(l)); labels.length = 0;
      place();
    }
    function place() {
      hammer.position.set(-0.8, st.h.y, -0.4); hammer.rotation.set(0, 0, st.h.done ? Math.PI / 2 : 0);
      if (st.h.done) hammer.position.y = 0.12;
      feather.position.set(0.8 + (st.running && !st.f.done && planet.air > 0.5 ? Math.sin(st.t * 3) * 0.4 : 0), st.f.y, -0.4);
      if (st.f.done) { feather.position.y = 0.08; feather.rotation.set(Math.PI / 2, 0, 0.3); }
      else feather.rotation.set(0, 0, st.running && planet.air > 0.5 ? Math.sin(st.t * 3) * 0.6 : 0.2);
    }
    function showInfo() {
      const wOn = weight * planet.g / 9.8;
      const jump = 0.5 * 9.8 / planet.g;
      ctx.ui.info(`${planet.emoji} <b>${planet.name}</b>: сила тяжести <b>${fmt(planet.g, 2)} м/с²</b>. ${planet.note}<br>
        ⚖️ Весы покажут: <b>${fmt(wOn, 1)} кг</b> &nbsp; 🦘 Прыжок: <b>${fmt(jump, 1)} м</b><br>
        🔨 Молоток: <b>${st.h.done ? fmt(st.h.time) + ' с' : '—'}</b> &nbsp; 🪶 Перо: <b>${st.f.done ? fmt(st.f.time) + ' с' : '—'}</b>`);
    }

    ctx.ui.clear();
    ctx.ui.choices('Выбери планету:', PLANETS.map((p) => ({ label: `${p.emoji} ${p.name}`, value: p })), (p) => { planet = p; applyPlanet(); ctx.ui.step(`Ты на планете ${p.emoji} <b>${p.name}</b>. Бросай!`); }, 0);
    ctx.ui.button('⬇️ Бросить молоток и перо!', () => {
      resetDrop(); st.running = true; visited.add(planet.id);
      ctx.ui.step('Падают… Кто первый? 👀');
    }, 'green');
    ctx.ui.slider('⚖️ Твой вес на Земле', 20, 70, 1, weight, (v) => { weight = v; showInfo(); }, (v) => v + ' кг');
    ctx.ui.step('Как думаешь, что упадёт первым — молоток или перо? Нажми «Бросить»!');
    applyPlanet();

    function landLabel(x, txt) { const l = ctx.makeLabel(txt, { fontSize: 42, scale: 0.7, bg: '#ffcf3f', color: '#3a2a00' }); l.position.set(x, 1.4, -0.4); scene.add(l); labels.push(l); }

    return {
      update(dt) {
        if (st.running) {
          st.t += dt;
          // молоток: воздух почти не мешает
          if (!st.h.done) { st.h.v += planet.g * dt; st.h.y -= st.h.v * dt; if (st.h.y <= 0.5) { st.h.done = true; st.h.time = st.t; ctx.sfx.thud(); puff(-0.8, -0.4, planet.ground); landLabel(-0.8, '🔨 ' + fmt(st.t) + ' с'); showInfo(); } }
          // перо: сопротивление воздуха ~ v²
          if (!st.f.done) {
            const k = planet.air * 9.8 / (FEATHER_VT * FEATHER_VT);
            st.f.v += (planet.g - k * st.f.v * st.f.v) * dt; st.f.y -= st.f.v * dt;
            if (st.f.y <= 0.5) { st.f.done = true; st.f.time = st.t; ctx.sfx.pop(); landLabel(0.8, '🪶 ' + fmt(st.t) + ' с'); showInfo(); }
          }
          if (st.h.done && st.f.done) {
            st.running = false;
            const diff = Math.abs(st.f.time - st.h.time);
            ctx.ui.step(diff < 0.05 ? `😮 Упали <b>одновременно</b>! На планете ${planet.name} нет воздуха, который мешал бы перу.`
              : `Молоток упал на <b>${fmt(diff, 1)} с</b> раньше. Перу мешает ${planet.air < 0.5 ? 'даже тонкий' : ''} воздух. А что будет на Луне? 🌕`);
            if (visited.has('moon') && visited.size >= 2) ctx.award('done');
          }
        }
        place();
        for (let i = dust.length - 1; i >= 0; i--) {
          const d = dust[i]; d.life -= dt; d.vy -= 2 * dt;
          d.m.position.x += d.vx * dt; d.m.position.y = Math.max(0.05, d.m.position.y + d.vy * dt); d.m.position.z += d.vz * dt;
          d.m.material.opacity = Math.max(0, d.life / 1.2) * 0.8;
          if (d.life <= 0) { scene.remove(d.m); d.m.material.dispose(); dust.splice(i, 1); }
        }
      },
    };
  },
};
