import * as THREE from 'three';
import { rand } from '../common.js';

const ORBIT = 9, ER = 1.1, TILT = 23.44;
const MONTHS = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
const MDAYS = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
function dateName(n) { let d = Math.round(n); for (let m = 0; m < 12; m++) { if (d <= MDAYS[m]) return `${d} ${MONTHS[m]}`; d -= MDAYS[m]; } return '31 декабря'; }
const CITIES = [
  { name: 'Москва', emoji: '🏙️', lat: 55.75 },
  { name: 'Мурманск', emoji: '❄️', lat: 68.97 },
  { name: 'Сочи', emoji: '🌴', lat: 43.6 },
  { name: 'Экватор', emoji: '🌍', lat: 0 },
  { name: 'Сидней', emoji: '🦘', lat: -33.87 },
];

// Процедурная «карта» планеты (не настоящая география — для наглядности).
function planetTexture() {
  const W = 512, H = 256; const c = document.createElement('canvas'); c.width = W; c.height = H;
  const g = c.getContext('2d'); const img = g.createImageData(W, H);
  const S = 64; const grid = Array.from({ length: S * S }, () => Math.random());
  const val = (x, y) => { const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
    const at = (i, j) => grid[((j % S + S) % S) * S + ((i % S + S) % S)];
    const sx = xf * xf * (3 - 2 * xf), sy = yf * yf * (3 - 2 * yf);
    return (at(xi, yi) * (1 - sx) + at(xi + 1, yi) * sx) * (1 - sy) + (at(xi, yi + 1) * (1 - sx) + at(xi + 1, yi + 1) * sx) * sy; };
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    let n = 0, amp = 0.5, f = 4 / W * 2;
    for (let o = 0; o < 5; o++) { n += val(x * f * 2, y * f * 2) * amp; amp *= 0.5; f *= 2; }
    const lat = Math.abs(y / H - 0.5) * 2;
    const i = (y * W + x) * 4; let r, gg, b;
    if (lat > 0.86) { r = gg = b = 245; }
    else if (n > 0.52) { const d = n - 0.52; r = 60 + d * 300; gg = 140 + d * 150; b = 60; if (lat < 0.35 && d > 0.08) { r = 200; gg = 170; b = 100; } }
    else { const d = 0.52 - n; r = 20; gg = 80 + (0.2 - Math.min(d, 0.2)) * 300; b = 170 + (0.2 - Math.min(d, 0.2)) * 300; }
    img.data[i] = r; img.data[i + 1] = gg; img.data[i + 2] = b; img.data[i + 3] = 255;
  }
  g.putImageData(img, 0, 0);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
}
function sunGlow() {
  const c = document.createElement('canvas'); c.width = c.height = 256; const g = c.getContext('2d');
  const gr = g.createRadialGradient(128, 128, 20, 128, 128, 128);
  gr.addColorStop(0, 'rgba(255,230,120,1)'); gr.addColorStop(0.4, 'rgba(255,170,40,0.45)'); gr.addColorStop(1, 'rgba(255,120,0,0)');
  g.fillStyle = gr; g.fillRect(0, 0, 256, 256);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
}

export default {
  id: 'earth', title: 'День, ночь и времена года', emoji: '🌞', subject: 'География', color: '#ff7ac0',
  short: 'Покрути Землю вокруг Солнца и узнай, откуда берутся лето, зима и полярная ночь.',
  why: `<h2>🌞 Откуда берутся день, ночь и времена года?</h2>
    <p><b>День и ночь.</b> Земля вращается вокруг своей оси, делая один оборот за <b>24 часа</b>. Сторона, повёрнутая к Солнцу, освещена — там день. Другая сторона в тени — там ночь.</p>
    <p><b>Времена года.</b> Земля летит вокруг Солнца целый <b>год</b>. А её ось <b>наклонена</b> на 23,5° и всё время смотрит в одну сторону.</p>
    <ul><li>Летом северная половина Земли наклонена <b>к Солнцу</b>: лучи падают круче, греют сильнее, а день длиннее ночи.</li>
    <li>Зимой она наклонена <b>от Солнца</b>: лучи скользят по земле, греют слабо, дни короткие.</li></ul>
    <p>Поэтому, когда в Москве зима, в Австралии — <b>лето</b>! А за Полярным кругом (в Мурманске) зимой бывает <b>полярная ночь</b> — солнце не встаёт несколько недель, а летом — <b>полярный день</b>.</p>
    <p>Убери наклон оси в опыте — и времена года исчезнут: день везде будет длиться 12 часов круглый год.</p>
    <p class="fact">🛰️ <b>Факт:</b> зимой Земля находится даже <b>ближе</b> к Солнцу, чем летом! Значит, дело не в расстоянии, а именно в наклоне оси.</p>
    <p class="home">🏠 <b>Попробуй дома:</b> в тёмной комнате посвети фонариком на мяч и медленно обходи с ним вокруг лампы, держа мяч наклонённым. Смотри, какая половина освещена больше.</p>`,
  quiz: [
    { q: 'Почему бывают день и ночь?', a: ['Солнце выключается ночью', 'Земля вращается вокруг своей оси', 'Луна закрывает Солнце', 'Облака закрывают небо'], ok: 1,
      note: 'Один оборот Земли вокруг оси — это одни сутки.' },
    { q: 'Почему бывают времена года?', a: ['Земля то ближе, то дальше от Солнца', 'Ось Земли наклонена', 'Солнце светит то сильнее, то слабее', 'Из-за ветра'], ok: 1,
      note: 'Именно наклон оси меняет, как падают солнечные лучи.' },
    { q: 'Когда в Москве зима, в Австралии…', a: ['Тоже зима', 'Лето', 'Всегда весна', 'Полярная ночь'], ok: 1,
      note: 'Северное и южное полушария получают больше света по очереди.' },
  ],

  start(ctx) {
    const { scene, camera, controls } = ctx.newScene({ bg: 0x02030a, camPos: [0, 7, 22], target: [0, 0, 0], minDist: 3, maxDist: 45 });
    controls.maxPolarAngle = Math.PI; controls.enablePan = false;
    scene.add(new THREE.AmbientLight(0x334466, 0.25));
    const sunLight = new THREE.PointLight(0xffffff, 4, 0, 0); scene.add(sunLight);

    const sun = new THREE.Mesh(new THREE.SphereGeometry(2, 32, 16), new THREE.MeshBasicMaterial({ color: 0xffd040 })); scene.add(sun);
    const glow = new THREE.Sprite(new THREE.SpriteMaterial({ map: sunGlow(), blending: THREE.AdditiveBlending, depthWrite: false, transparent: true }));
    glow.scale.setScalar(9); scene.add(glow);
    const sunL = ctx.makeLabel('☀️ Солнце', { fontSize: 40, scale: 0.6 }); sunL.position.set(0, 3, 0); scene.add(sunL);

    // звёзды
    const sg = new THREE.BufferGeometry(); const sa = [];
    for (let i = 0; i < 1500; i++) { const v = new THREE.Vector3(rand(-1, 1), rand(-1, 1), rand(-1, 1)).normalize().multiplyScalar(rand(80, 140)); sa.push(v.x, v.y, v.z); }
    sg.setAttribute('position', new THREE.Float32BufferAttribute(sa, 3));
    scene.add(new THREE.Points(sg, new THREE.PointsMaterial({ color: 0xffffff, size: 0.35, sizeAttenuation: true })));

    // орбита
    const op = []; for (let i = 0; i <= 128; i++) { const a = i / 128 * Math.PI * 2; op.push(new THREE.Vector3(Math.cos(a) * ORBIT, 0, Math.sin(a) * ORBIT)); }
    scene.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(op), new THREE.LineBasicMaterial({ color: 0x405080 })));
    // подписи сезонов на орбите
    [['21 июня', 172], ['23 сентября', 266], ['22 декабря', 356], ['20 марта', 79]].forEach(([t, n]) => {
      const th = Math.PI + 2 * Math.PI * (n - 172) / 365;
      const l = ctx.makeLabel(t, { fontSize: 32, scale: 0.55, bg: 'rgba(40,60,120,0.7)' });
      l.position.set(Math.cos(th) * (ORBIT + 2.2), 0.4, -Math.sin(th) * (ORBIT + 2.2)); scene.add(l);
    });

    // Земля
    const earthPos = new THREE.Group(); scene.add(earthPos);
    const tiltG = new THREE.Group(); earthPos.add(tiltG);
    const spin = new THREE.Group(); tiltG.add(spin);
    const globe = new THREE.Mesh(new THREE.SphereGeometry(ER, 48, 32), new THREE.MeshStandardMaterial({ map: planetTexture(), roughness: 0.85 }));
    spin.add(globe);
    const atm = new THREE.Mesh(new THREE.SphereGeometry(ER * 1.04, 32, 16), new THREE.MeshStandardMaterial({ color: 0x80b8ff, transparent: true, opacity: 0.15, depthWrite: false }));
    spin.add(atm);
    const axis = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, ER * 3.2, 8), new THREE.MeshBasicMaterial({ color: 0xff5060 })); tiltG.add(axis);
    const nl = ctx.makeLabel('С', { fontSize: 40, scale: 0.2, bg: '#e9475b' }); nl.position.y = ER * 1.75; tiltG.add(nl);
    // экватор
    const eq = new THREE.Mesh(new THREE.TorusGeometry(ER * 1.01, 0.012, 6, 80), new THREE.MeshBasicMaterial({ color: 0xffe080 })); eq.rotation.x = Math.PI / 2; tiltG.add(eq);
    // город
    const marker = new THREE.Group(); spin.add(marker);
    const pin = new THREE.Mesh(new THREE.SphereGeometry(0.08, 12, 8), new THREE.MeshBasicMaterial({ color: 0xff2040 })); marker.add(pin);
    const pinL = ctx.makeLabel('📍', { fontSize: 40, scale: 0.35, bg: 'rgba(0,0,0,0)', depthTest: true }); pinL.position.y = 0.25; marker.add(pinL);
    // параллель города
    const par = new THREE.Mesh(new THREE.TorusGeometry(1, 0.008, 4, 80), new THREE.MeshBasicMaterial({ color: 0xff8090 })); par.rotation.x = Math.PI / 2; tiltG.add(par);

    let day = 172, tilt = TILT, city = CITIES[0], spinSpeed = 1, playing = false, view = 'earth';
    let changedDay = false, changedCity = false;
    const prev = new THREE.Vector3();

    function placeCity() {
      const la = city.lat * Math.PI / 180;
      marker.position.set(Math.cos(la) * ER * 1.01, Math.sin(la) * ER * 1.01, 0);
      par.scale.setScalar(Math.cos(la) * ER * 1.012); par.position.y = Math.sin(la) * ER * 1.01;
    }
    function orbitPos(n) { const th = Math.PI + 2 * Math.PI * (n - 172) / 365; return new THREE.Vector3(Math.cos(th) * ORBIT, 0, -Math.sin(th) * ORBIT); }
    function info() {
      const decl = -tilt * Math.cos(2 * Math.PI * (day + 10) / 365) * Math.PI / 180;
      const la = city.lat * Math.PI / 180;
      const x = -Math.tan(la) * Math.tan(decl);
      let len, special = '';
      if (x <= -1) { len = 24; special = ' ☀️ <b>Полярный день</b> — солнце совсем не заходит!'; }
      else if (x >= 1) { len = 0; special = ' 🌑 <b>Полярная ночь</b> — солнце не встаёт!'; }
      else len = 24 / Math.PI * Math.acos(x);
      const h = Math.floor(len), m = Math.round((len - h) * 60);
      const north = city.lat >= 0; const dm = Math.round(day);
      let season;
      if (tilt === 0) season = 'Времён года нет 😶';
      else { const s = dm >= 80 && dm < 172 ? 0 : dm >= 172 && dm < 266 ? 1 : dm >= 266 && dm < 356 ? 2 : 3;
        const names = north ? ['🌸 весна', '☀️ лето', '🍂 осень', '❄️ зима'] : ['🍂 осень', '❄️ зима', '🌸 весна', '☀️ лето'];
        season = 'Сейчас ' + names[s]; }
      if (city.lat === 0 && tilt !== 0) season = 'На экваторе всегда тепло 🌴';
      ctx.ui.info(`📍 <b>${city.name}</b>, ${dateName(day)}<br>☀️ День длится: <b>${h} ч ${String(m).padStart(2, '0')} мин</b>${special}<br>${season}`);
    }

    ctx.ui.clear();
    const sDay = ctx.ui.slider('📅 День года', 1, 365, 1, day, (v) => { day = v; changedDay = true; info(); check(); }, (v) => dateName(v));
    ctx.ui.choices('Выбери город:', CITIES.map((c) => ({ label: `${c.emoji} ${c.name}`, value: c })), (c) => { city = c; changedCity = true; placeCity(); info(); check(); }, 0);
    const bPlay = ctx.ui.button('▶️ Запустить год', () => { playing = !playing; bPlay.innerHTML = playing ? '⏸ Пауза' : '▶️ Запустить год'; });
    const bTilt = ctx.ui.button('📐 Убрать наклон оси', () => {
      tilt = tilt ? 0 : TILT; bTilt.innerHTML = tilt ? '📐 Убрать наклон оси' : '📐 Вернуть наклон 23,5°';
      ctx.ui.step(tilt ? 'Наклон вернулся — снова есть лето и зима!' : '😮 Без наклона день и ночь везде по 12 часов. Времён года больше нет!'); info();
    }, 'purple');
    ctx.ui.choices('Камера:', [{ label: '🌍 У Земли', value: 'earth' }, { label: '🔭 Вся орбита', value: 'orbit' }], (v) => { view = v; setView(); }, 0);
    ctx.ui.slider('🔄 Скорость вращения Земли', 0, 5, 0.5, spinSpeed, (v) => { spinSpeed = v; }, (v) => v === 0 ? 'стоп' : '×' + v);

    function setView() {
      const ep = orbitPos(day);
      if (view === 'earth') {
        const toSun = ep.clone().negate().normalize();
        const side = new THREE.Vector3(-toSun.z, 0, toSun.x);
        camera.position.copy(ep).addScaledVector(side, 4.2).addScaledVector(toSun, -1.2).add(new THREE.Vector3(0, 1.6, 0));
        controls.target.copy(ep); controls.minDistance = 2.2;
      } else {
        camera.position.set(0, 14, 20); controls.target.set(0, 0, 0); controls.minDistance = 3;
      }
      prev.copy(ep); controls.update();
    }
    function check() { if (changedDay && changedCity) ctx.award('done'); }

    ctx.ui.step('Двигай <b>день года</b> и выбирай города. Где летом солнце не заходит? 🤔');
    placeCity(); info(); setView();

    return {
      update(dt) {
        if (playing) { day += dt * 18; if (day > 365) day -= 365; sDay.input.value = Math.round(day); sDay.el.querySelector('b').textContent = dateName(day); changedDay = true; info(); check(); }
        const ep = orbitPos(day);
        earthPos.position.copy(ep);
        tiltG.rotation.z = -tilt * Math.PI / 180;
        spin.rotation.y += dt * spinSpeed * 0.8;
        if (view === 'earth') { const d = ep.clone().sub(prev); camera.position.add(d); controls.target.copy(ep); }
        prev.copy(ep);
        glow.material.rotation += dt * 0.05;
      },
    };
  },
};
