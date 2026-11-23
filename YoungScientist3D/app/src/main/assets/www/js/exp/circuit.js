import * as THREE from 'three';
import { shadowAll } from '../common.js';

const MATERIALS = [
  { name: 'Медная проволока', emoji: '🟠', cond: 1.0, kind: 'copper' },
  { name: 'Железный гвоздь', emoji: '📌', cond: 0.8, kind: 'nail' },
  { name: 'Грифель карандаша', emoji: '✏️', cond: 0.3, kind: 'graphite' },
  { name: 'Деревянная палочка', emoji: '🪵', cond: 0, kind: 'wood' },
  { name: 'Пластик', emoji: '🧴', cond: 0, kind: 'plastic' },
  { name: 'Ластик', emoji: '🩷', cond: 0, kind: 'rubber' },
];

function sample(kind) {
  const g = new THREE.Group();
  const m = (c, o = {}) => new THREE.MeshStandardMaterial({ color: c, roughness: 0.5, ...o });
  if (kind === 'copper') { const c = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 1.5, 10), m(0xd8783a, { metalness: 0.9, roughness: 0.25 })); c.rotation.z = Math.PI / 2; g.add(c); }
  if (kind === 'nail') {
    const c = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.03, 1.5, 10), m(0x9aa0aa, { metalness: 0.9, roughness: 0.35 })); c.rotation.z = Math.PI / 2; g.add(c);
    const h = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.14, 0.04, 14), c.material); h.rotation.z = Math.PI / 2; h.position.x = -0.75; g.add(h);
  }
  if (kind === 'graphite') {
    const w = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.11, 1.2, 6), m(0xffcf3f)); w.rotation.z = Math.PI / 2; g.add(w);
    const lead = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 1.55, 8), m(0x333338, { metalness: 0.5 })); lead.rotation.z = Math.PI / 2; g.add(lead);
  }
  if (kind === 'wood') { const c = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.12, 0.16), m(0xc8955a, { roughness: 0.9 })); g.add(c); }
  if (kind === 'plastic') { const c = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.12, 0.2), m(0x30a0ff, { roughness: 0.2 })); g.add(c); }
  if (kind === 'rubber') { const c = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.22, 0.32), m(0xff7fa8, { roughness: 0.9 })); g.add(c); }
  shadowAll(g); return g;
}

function glowTexture() {
  const c = document.createElement('canvas'); c.width = c.height = 128; const g = c.getContext('2d');
  const gr = g.createRadialGradient(64, 64, 0, 64, 64, 64);
  gr.addColorStop(0, 'rgba(255,240,180,1)'); gr.addColorStop(0.3, 'rgba(255,210,90,0.6)'); gr.addColorStop(1, 'rgba(255,180,50,0)');
  g.fillStyle = gr; g.fillRect(0, 0, 128, 128);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
}

export default {
  id: 'circuit', title: 'Электрическая цепь', emoji: '💡', subject: 'Электричество', color: '#22b573',
  short: 'Собери цепь с батарейкой и лампочкой. Какие предметы пропускают ток?',
  why: `<h2>💡 Почему лампочка загорается?</h2>
    <p>Электрический ток — это движение крошечных частиц <b>электронов</b> по проводам. Батарейка работает как насос: она толкает электроны по кругу.</p>
    <p>Ток может течь, только если цепь <b>замкнута</b> — то есть провода образуют круг без разрывов. Выключатель как раз размыкает и замыкает этот круг.</p>
    <ul><li><b>Проводники</b> — материалы, по которым ток течёт: медь, железо, алюминий, графит.</li>
    <li><b>Изоляторы</b> (диэлектрики) — не пропускают ток: дерево, пластик, резина, стекло.</li></ul>
    <p>Грифель карандаша — проводник, но «плохой»: он сильно сопротивляется току, поэтому лампочка горит тускло.</p>
    <p>Провода делают из меди, а покрывают пластиком или резиной — чтобы ток шёл только по проводу и не ударил нас.</p>
    <p class="fact">⚡ <b>Факт:</b> электроны в проводе ползут медленнее улитки — меньше миллиметра в секунду! Но лампочка загорается мгновенно, потому что они начинают двигаться сразу по всей цепи.</p>
    <p class="home">⚠️ <b>Важно:</b> опыты с электричеством можно делать <b>только с маленькой батарейкой</b> и вместе со взрослыми. Никогда не трогай розетку и провода от неё — это опасно для жизни!</p>`,
  quiz: [
    { q: 'Что нужно, чтобы лампочка загорелась?', a: ['Разомкнутая цепь', 'Замкнутая цепь', 'Только лампочка', 'Много проводов'], ok: 1,
      note: 'Ток течёт только по замкнутому кругу.' },
    { q: 'Какой материал — изолятор?', a: ['Медь', 'Железо', 'Резина', 'Графит'], ok: 2,
      note: 'Резина, пластик и дерево не пропускают ток.' },
    { q: 'Почему провода покрывают пластиком?', a: ['Для красоты', 'Чтобы ток не ушёл наружу и не ударил нас', 'Чтобы провода были тяжелее', 'Чтобы ток шёл быстрее'], ok: 1,
      note: 'Пластик — изолятор, он защищает нас от тока.' },
  ],

  start(ctx) {
    const { scene } = ctx.newScene({ bg: 0x1a2550, camPos: [0, 6.2, 6.5], target: [0, 0.4, 0], minDist: 3.5, maxDist: 14 });
    scene.add(new THREE.HemisphereLight(0xdfe8ff, 0x3a2a20, 0.9));
    const dl = new THREE.DirectionalLight(0xffffff, 1.6); dl.position.set(3, 8, 5); dl.castShadow = true; dl.shadow.mapSize.set(1024, 1024);
    Object.assign(dl.shadow.camera, { left: -6, right: 6, top: 6, bottom: -6 }); scene.add(dl);

    const board = new THREE.Mesh(new THREE.BoxGeometry(7.5, 0.2, 4.8), new THREE.MeshStandardMaterial({ color: 0xd9b07a, roughness: 0.9 }));
    board.position.y = 0.1; board.receiveShadow = true; scene.add(board);
    const table = new THREE.Mesh(new THREE.BoxGeometry(14, 0.3, 10), new THREE.MeshStandardMaterial({ color: 0x5a3a2a })); table.position.y = -0.15; table.receiveShadow = true; scene.add(table);

    // батарейка
    const bat = new THREE.Group();
    const bb = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.35, 1.2, 24), new THREE.MeshStandardMaterial({ color: 0x222222, metalness: 0.3 })); bb.rotation.z = Math.PI / 2; bat.add(bb);
    const band = new THREE.Mesh(new THREE.CylinderGeometry(0.355, 0.355, 0.5, 24), new THREE.MeshStandardMaterial({ color: 0xffa020 })); band.rotation.z = Math.PI / 2; band.position.x = 0.3; bat.add(band);
    const tip = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.12, 0.12, 16), new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0.9 })); tip.rotation.z = Math.PI / 2; tip.position.x = 0.66; bat.add(tip);
    bat.position.set(-2.4, 0.55, 0.6); shadowAll(bat); scene.add(bat);
    const lp = ctx.makeLabel('+', { fontSize: 60, scale: 0.35, bg: '#e9475b' }); lp.position.set(-1.65, 1.15, 0.6); scene.add(lp);
    const lm = ctx.makeLabel('−', { fontSize: 60, scale: 0.35, bg: '#3b64e8' }); lm.position.set(-3.15, 1.15, 0.6); scene.add(lm);
    const lv = ctx.makeLabel('Батарейка 4,5 В', { fontSize: 34, scale: 0.3 }); lv.position.set(-2.4, 1.2, 0.95); scene.add(lv);

    // выключатель
    const sw = new THREE.Group();
    const swBase = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.12, 0.5), new THREE.MeshStandardMaterial({ color: 0x334488 })); sw.add(swBase);
    const metal = new THREE.MeshStandardMaterial({ color: 0xc9ccd6, metalness: 0.9, roughness: 0.3 });
    const c1 = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.2, 0.2), metal); c1.position.set(-0.45, 0.15, 0); sw.add(c1);
    const c2 = c1.clone(); c2.position.x = 0.45; sw.add(c2);
    const leverPivot = new THREE.Group(); leverPivot.position.set(-0.45, 0.25, 0); sw.add(leverPivot);
    const lever = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.05, 0.12), metal); lever.position.x = 0.47; leverPivot.add(lever);
    const knob = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 8), new THREE.MeshStandardMaterial({ color: 0xe9475b })); knob.position.x = 0.9; leverPivot.add(knob);
    sw.position.set(-0.5, 0.26, -1.4); shadowAll(sw); scene.add(sw);
    const swl = ctx.makeLabel('Выключатель', { fontSize: 34, scale: 0.3 }); swl.position.set(-0.5, 1.1, -1.4); scene.add(swl);

    // лампочка
    const bulb = new THREE.Group();
    const socket = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.26, 0.4, 20), new THREE.MeshStandardMaterial({ color: 0xb0b4c0, metalness: 0.8, roughness: 0.3 })); socket.position.y = 0.2; bulb.add(socket);
    const glassMat = new THREE.MeshStandardMaterial({ color: 0xffffff, transparent: true, opacity: 0.35, emissive: 0xffd060, emissiveIntensity: 0, roughness: 0.05, depthWrite: false });
    const glass = new THREE.Mesh(new THREE.SphereGeometry(0.42, 24, 16), glassMat); glass.position.y = 0.75; glass.renderOrder = 3; bulb.add(glass);
    const filMat = new THREE.MeshStandardMaterial({ color: 0x553311, emissive: 0xffaa30, emissiveIntensity: 0 });
    const fil = new THREE.Mesh(new THREE.TorusGeometry(0.12, 0.015, 6, 20, Math.PI), filMat); fil.position.y = 0.72; bulb.add(fil);
    const halo = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowTexture(), blending: THREE.AdditiveBlending, transparent: true, depthWrite: false, opacity: 0 }));
    halo.scale.setScalar(2.6); halo.position.y = 0.75; bulb.add(halo);
    const bulbLight = new THREE.PointLight(0xffd080, 0, 8, 1.5); bulbLight.position.y = 0.8; bulb.add(bulbLight);
    bulb.position.set(1.8, 0.2, -1.4); shadowAll(bulb); glass.castShadow = false; scene.add(bulb);

    // зажимы для образца
    const clipMat = new THREE.MeshStandardMaterial({ color: 0xe9475b, roughness: 0.4 });
    [0.5, 1.9].forEach((x) => { const cl = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.3, 0.3), clipMat); cl.position.set(x, 0.35, 1.4); shadowAll(cl); scene.add(cl); });
    const tl = ctx.makeLabel('Проверяем материал', { fontSize: 34, scale: 0.3 }); tl.position.set(1.2, 1.0, 1.85); scene.add(tl);
    let sampleObj = null;

    // провода
    const P = [[-1.7, 0.55, 0.6], [-1.4, 0.26, -0.4], [-0.95, 0.5, -1.4], [-0.05, 0.5, -1.4], [0.8, 0.26, -1.4], [1.8, 0.4, -1.4], [2.7, 0.26, -0.4], [2.5, 0.26, 1.4], [1.9, 0.4, 1.4],
      [0.5, 0.4, 1.4], [-0.6, 0.26, 1.7], [-3.2, 0.26, 1.4], [-3.15, 0.55, 0.6]].map((p) => new THREE.Vector3(...p));
    const wireMat = new THREE.MeshStandardMaterial({ color: 0xe03030, roughness: 0.5 });
    const wireMat2 = new THREE.MeshStandardMaterial({ color: 0x2050e0, roughness: 0.5 });
    [[0, 1, 2, 'r'], [3, 4, 5, 'r'], [5, 6, 7, 8, 'r'], [9, 10, 11, 12, 'b']].forEach((seg) => {
      const pts = seg.filter((x) => typeof x === 'number').map((i) => P[i]);
      const curve = new THREE.CatmullRomCurve3(pts);
      const w = new THREE.Mesh(new THREE.TubeGeometry(curve, 40, 0.045, 8, false), seg[seg.length - 1] === 'b' ? wireMat2 : wireMat);
      w.castShadow = true; scene.add(w);
    });
    // путь электронов (замкнутый)
    const loop = new THREE.CatmullRomCurve3(P, true);
    const NE = 60; const eGeo = new THREE.SphereGeometry(0.05, 8, 6);
    const eMat = new THREE.MeshBasicMaterial({ color: 0x80e0ff });
    const electrons = Array.from({ length: NE }, (_, i) => { const m = new THREE.Mesh(eGeo, eMat); m.userData.u = i / NE; scene.add(m); return m; });

    let closed = false, matIdx = 0, current = 0, testedTimer = 0;
    const tested = {};
    function setSample(i) {
      matIdx = i; if (sampleObj) scene.remove(sampleObj);
      sampleObj = sample(MATERIALS[i].kind); sampleObj.position.set(1.2, 0.45, 1.4); scene.add(sampleObj);
      testedTimer = 0; update();
    }
    function toggle() { closed = !closed; ctx.sfx.click2(); testedTimer = 0; update(); }
    function update() {
      const m = MATERIALS[matIdx];
      current = closed ? m.cond : 0;
      bSwitch.innerHTML = closed ? '🔌 Выключить' : '⚡ Включить';
      if (!closed) ctx.ui.step('Цепь <b>разомкнута</b> — ток не течёт. Нажми на выключатель!');
      else if (m.cond > 0.5) ctx.ui.step(`💡 Горит! <b>${m.name}</b> — хороший <b>проводник</b>.`);
      else if (m.cond > 0) ctx.ui.step(`🔅 Горит тускло. <b>${m.name}</b> проводит ток, но плохо.`);
      else ctx.ui.step(`🌑 Не горит! <b>${m.name}</b> — <b>изолятор</b>, ток через него не проходит.`);
      if (closed) ctx.sfx.hum();
      renderInfo();
    }
    function renderInfo() {
      const rows = MATERIALS.map((m, i) => tested[i] !== undefined ? `${m.emoji} ${m.name}: <b>${m.cond > 0 ? '✅ проводник' : '❌ изолятор'}</b>` : null).filter(Boolean);
      ctx.ui.info(rows.length ? 'Проверено:<br>' + rows.join('<br>') : 'Положи разные предметы в зажимы и включи ток.');
    }

    ctx.ui.clear();
    const bSwitch = ctx.ui.button('⚡ Включить', toggle, 'green');
    const pick = ctx.ui.choices('Что положить в зажимы:', MATERIALS.map((m, i) => ({ label: `${m.emoji} ${m.name}`, value: i })), (i) => setSample(i), 0);
    ctx.onTap([sw], toggle);
    setSample(0);

    return {
      update(dt, t) {
        leverPivot.rotation.z += ((closed ? 0 : 0.6) - leverPivot.rotation.z) * Math.min(1, dt * 12);
        const flick = current > 0 && current < 0.5 ? 0.85 + Math.sin(t * 40) * 0.08 : 1;
        const I = current * flick;
        filMat.emissiveIntensity = I * 4; glassMat.emissiveIntensity = I * 1.2; halo.material.opacity = I;
        bulbLight.intensity = I * 6;
        electrons.forEach((e) => {
          e.userData.u = (e.userData.u - dt * 0.04 * current + 1) % 1; // от «−» к «+»
          e.position.copy(loop.getPointAt(e.userData.u));
        });
        eMat.color.setHSL(0.55, 1, current > 0 ? 0.65 + Math.sin(t * 8) * 0.1 : 0.45);
        if (closed) {
          testedTimer += dt;
          if (testedTimer > 0.6 && tested[matIdx] === undefined) {
            tested[matIdx] = MATERIALS[matIdx].cond > 0; pick.mark(matIdx); renderInfo();
            if (Object.keys(tested).length >= 4) ctx.award('done');
          }
        }
      },
    };
  },
};
