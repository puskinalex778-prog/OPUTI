import * as THREE from 'three';
import { labRoom, shadowAll, rand } from '../common.js';

// Профиль вулкана (радиус, высота) — по нему же «течёт» пена.
const PROFILE = [[2.3, 0], [1.7, 0.65], [1.05, 1.45], [0.55, 2.05], [0.42, 2.08]];
function slopeH(r) {
  if (r >= PROFILE[0][0]) return 0;
  for (let i = 0; i < PROFILE.length - 1; i++) {
    const [r1, h1] = PROFILE[i], [r2, h2] = PROFILE[i + 1];
    if (r <= r1 && r >= r2) return h1 + (h2 - h1) * (r1 - r) / (r1 - r2);
  }
  return 2.08;
}
const CRATER_FLOOR = 1.5;

export default {
  id: 'volcano', title: 'Вулкан из соды', emoji: '🌋', subject: 'Химия', color: '#ff6b4a',
  short: 'Смешай соду и уксус — и устрой настоящее извержение пены!',
  why: `<h2>🌋 Почему вулкан «извергается»?</h2>
    <p>Пищевая сода — это <b>щёлочь</b>, а уксус — <b>кислота</b>. Когда они встречаются, начинается <b>химическая реакция</b>.</p>
    <p class="formula">сода + уксус → углекислый газ + вода + соль</p>
    <p>Самое интересное — <b>углекислый газ</b> (CO₂). Он появляется очень быстро и превращается в миллионы пузырьков. Пузырькам тесно в кратере, и они выталкивают пену наружу — получается «лава»!</p>
    <p>Больше соды → больше газа → сильнее извержение. А без соды уксус ни с чем не реагирует — поэтому ничего и не происходит.</p>
    <p class="fact">🌍 <b>Факт:</b> у настоящих вулканов тоже есть газы! Они растворены в раскалённой магме и, вырываясь, помогают ей подняться наверх.</p>
    <p class="home">🏠 <b>Попробуй дома со взрослыми:</b> поставь стакан на поднос, положи 2 ложки соды, каплю жидкого мыла и пищевой краситель, а затем влей полстакана уксуса. Не трогай уксус руками и не пробуй на вкус!</p>`,
  quiz: [
    { q: 'Какой газ получается, когда смешивают соду и уксус?', a: ['Кислород', 'Углекислый газ', 'Водород', 'Гелий'], ok: 1,
      note: 'Это углекислый газ, CO₂ — тот самый, что в газировке.' },
    { q: 'Что будет, если в вулкан налить уксус без соды?', a: ['Огромное извержение', 'Ничего не произойдёт', 'Вулкан замёрзнет', 'Появится дым'], ok: 1,
      note: 'Для реакции нужны оба вещества: и кислота, и щёлочь.' },
    { q: 'Как сделать извержение сильнее?', a: ['Добавить больше соды', 'Покрасить вулкан', 'Выключить свет', 'Налить воду вместо уксуса'], ok: 0,
      note: 'Больше соды — больше газа — больше пены!' },
  ],

  start(ctx) {
    const { scene } = ctx.newScene({ bg: 0x1a2550, camPos: [0, 4.2, 7.5], target: [0, 1.2, 0], minDist: 3.5, maxDist: 14 });
    labRoom(scene);

    // --- вулкан ---
    const outer = PROFILE.map(([r, h]) => new THREE.Vector2(r, h));
    const pts = [...outer, new THREE.Vector2(0.34, 1.7), new THREE.Vector2(0.25, CRATER_FLOOR), new THREE.Vector2(0, CRATER_FLOOR)];
    const volc = new THREE.Mesh(new THREE.LatheGeometry(pts.reverse(), 28), new THREE.MeshStandardMaterial({ color: 0x8b5a3c, roughness: 0.95, flatShading: true, side: THREE.DoubleSide }));
    shadowAll(volc); scene.add(volc);
    // камни и трава вокруг
    for (let i = 0; i < 14; i++) {
      const a = rand(0, Math.PI * 2), r = rand(2.4, 3.6);
      const rock = new THREE.Mesh(new THREE.DodecahedronGeometry(rand(0.08, 0.22)), new THREE.MeshStandardMaterial({ color: i % 3 ? 0x6d6d6d : 0x3f8f3a, flatShading: true }));
      rock.position.set(Math.cos(a) * r, 0.05, Math.sin(a) * r); shadowAll(rock); scene.add(rock);
    }
    // соль соды в кратере
    const pile = new THREE.Mesh(new THREE.ConeGeometry(0.28, 0.25, 16), new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 1 }));
    pile.position.y = CRATER_FLOOR; pile.scale.setScalar(0.001); scene.add(pile);

    // ложка
    const spoon = new THREE.Group();
    const bowl = new THREE.Mesh(new THREE.SphereGeometry(0.22, 16, 8, 0, Math.PI * 2, 0, Math.PI / 2), new THREE.MeshStandardMaterial({ color: 0xd8d8e0, metalness: 0.8, roughness: 0.3, side: THREE.DoubleSide }));
    bowl.rotation.x = Math.PI; spoon.add(bowl);
    const handle = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.04, 0.08), bowl.material); handle.position.x = 0.75; spoon.add(handle);
    const sodaInSpoon = new THREE.Mesh(new THREE.SphereGeometry(0.19, 12, 8), new THREE.MeshStandardMaterial({ color: 0xffffff }));
    sodaInSpoon.scale.y = 0.4; spoon.add(sodaInSpoon);
    spoon.position.set(4, 3.2, 0); scene.add(spoon);

    // бутылка уксуса
    const bottle = new THREE.Group();
    const bp = [[0, 0], [0.32, 0], [0.34, 0.05], [0.34, 0.9], [0.14, 1.25], [0.12, 1.5], [0, 1.5]].map(([x, y]) => new THREE.Vector2(x, y));
    bottle.add(new THREE.Mesh(new THREE.LatheGeometry(bp, 20), new THREE.MeshStandardMaterial({ color: 0xcfe8ff, transparent: true, opacity: 0.45, roughness: 0.1 })));
    const vin = new THREE.Mesh(new THREE.CylinderGeometry(0.3, 0.3, 0.75, 20), new THREE.MeshStandardMaterial({ color: 0xf5efc8, transparent: true, opacity: 0.8 }));
    vin.position.y = 0.42; bottle.add(vin);
    const lab = ctx.makeLabel('УКСУС', { fontSize: 40, bg: '#ffcf3f', color: '#3a2a00', scale: 0.22 });
    lab.position.set(0, 0.55, 0.36); bottle.add(lab);
    bottle.position.set(-3.2, 0, 0.8); shadowAll(bottle); scene.add(bottle);

    // --- частицы (пена / сода / уксус) ---
    const N = 1100;
    const geo = new THREE.IcosahedronGeometry(0.075, 1);
    const mat = new THREE.MeshStandardMaterial({ roughness: 0.6 });
    const inst = new THREE.InstancedMesh(geo, mat, N);
    inst.instanceMatrix.setUsage(THREE.DynamicDrawUsage);
    inst.castShadow = true; inst.frustumCulled = false;
    scene.add(inst);
    const P = Array.from({ length: N }, () => ({ on: false, x: 0, y: 0, z: 0, vx: 0, vy: 0, vz: 0, life: 0, max: 1, s: 1, kind: 0 }));
    const dummy = new THREE.Object3D(); const col = new THREE.Color();
    let cursor = 0;
    function spawn(kind, x, y, z, vx, vy, vz, life, color, s = 1) {
      const idx = cursor; const p = P[idx]; cursor = (cursor + 1) % N;
      Object.assign(p, { on: true, x, y, z, vx, vy, vz, life, max: life, s, kind });
      inst.setColorAt(idx, col.set(color));
    }
    for (let i = 0; i < N; i++) { dummy.position.set(0, -50, 0); dummy.scale.setScalar(0.0001); dummy.updateMatrix(); inst.setMatrixAt(i, dummy.matrix); inst.setColorAt(i, col.set(0xffffff)); }

    // --- состояние ---
    const DYES = { none: 0xfff6e6, red: 0xff3030, green: 0x30d060, blue: 0x3080ff, purple: 0xb040ff };
    let soda = 0, dye = 'none', erupt = 0, eruptTotal = 0, anim = null, fizzT = 0, busy = false;

    function setStep() {
      if (erupt > 0) ctx.ui.step('🌋 Извержение! Пузырьки газа выталкивают пену наружу!');
      else if (soda === 0) ctx.ui.step('Шаг 1. Насыпь в кратер соду 🥄 (можно до 3 ложек).');
      else ctx.ui.step(`Шаг 2. Выбери краску 🎨 и налей уксус 🧪! <br><small>Соды в кратере: ${soda} ${soda === 1 ? 'ложка' : 'ложки'}</small>`);
    }

    ctx.ui.clear();
    const bSoda = ctx.ui.button('🥄 Насыпать соду', () => {
      if (busy || soda >= 3) return;
      busy = true; anim = { type: 'spoon', t: 0 };
    });
    const bVin = ctx.ui.button('🧪 Налить уксус', () => {
      if (busy) return; busy = true; anim = { type: 'pour', t: 0 }; ctx.sfx.pour();
    }, 'orange');
    ctx.ui.button('🔄 Заново', () => { reset(); }, 'gray');
    ctx.ui.choices('Краска для «лавы»:', [
      { label: '⚪ Без', value: 'none' }, { label: '🔴', value: 'red' }, { label: '🟢', value: 'green' }, { label: '🔵', value: 'blue' }, { label: '🟣', value: 'purple' },
    ], (v) => { dye = v; }, 0);

    function reset() {
      soda = 0; erupt = 0; anim = null; busy = false; pile.scale.setScalar(0.001);
      P.forEach((p) => { p.on = false; });
      spoon.position.set(4, 3.2, 0); spoon.rotation.set(0, 0, 0); sodaInSpoon.visible = true;
      bottle.position.set(-3.2, 0, 0.8); bottle.rotation.set(0, 0, 0);
      setStep(); ctx.ui.info('');
    }
    setStep();

    return {
      update(dt, t) {
        // анимации ложки и бутылки
        if (anim && anim.type === 'spoon') {
          anim.t += dt; const a = anim.t;
          if (a < 0.8) { spoon.position.lerp(new THREE.Vector3(0.1, 2.9, 0), 0.12); sodaInSpoon.visible = true; }
          else if (a < 1.6) {
            spoon.rotation.x = Math.min(1.6, (a - 0.8) * 3);
            if (a < 1.3) for (let k = 0; k < 3; k++) spawn(1, rand(-0.12, 0.12), 2.75, rand(-0.12, 0.12), 0, -1, 0, 2, 0xffffff, 0.7);
            sodaInSpoon.visible = false;
          } else if (a < 2.4) { spoon.rotation.x *= 0.85; spoon.position.lerp(new THREE.Vector3(4, 3.2, 0), 0.1); }
          else { soda++; pile.scale.setScalar(0.5 + soda * 0.35); anim = null; busy = false; ctx.sfx.pop(); setStep(); }
        }
        if (anim && anim.type === 'pour') {
          anim.t += dt; const a = anim.t;
          if (a < 0.8) { bottle.position.lerp(new THREE.Vector3(-1.05, 2.4, 0), 0.12); }
          else if (a < 2.0) {
            bottle.rotation.z = Math.max(-2.1, -(a - 0.8) * 4);
            if (a > 1.1) for (let k = 0; k < 3; k++) spawn(2, -0.35 + rand(-0.04, 0.04), 2.6, rand(-0.04, 0.04), 1.2, 0, 0, 1.2, 0xf3eec0, 0.6);
          } else if (a < 3.0) { bottle.rotation.z *= 0.85; bottle.position.lerp(new THREE.Vector3(-3.2, 0, 0.8), 0.1); }
          else {
            bottle.rotation.z = 0; anim = null; busy = false;
            if (soda > 0) {
              eruptTotal = erupt = 1.6 + soda * 2.2; ctx.sfx.fizz(1.5); fizzT = 0; setStep();
            } else {
              ctx.ui.step('🤔 Уксус налит… но ничего не происходит! Без соды реакции нет. Насыпь соду и попробуй ещё раз.');
            }
          }
        }

        // извержение: рождаем пену
        if (erupt > 0) {
          const power = soda / 3;
          const k = erupt / eruptTotal; // 1 → 0
          const rate = (90 + 260 * power) * Math.min(1, k * 2.5);
          let n = rate * dt; const base = new THREE.Color(DYES[dye]);
          while (n > 0) {
            if (Math.random() < n) {
              const a = rand(0, Math.PI * 2), r = rand(0, 0.25);
              const c = base.clone().lerp(new THREE.Color(0xffffff), rand(0, 0.45));
              spawn(0, Math.cos(a) * r, CRATER_FLOOR + 0.4, Math.sin(a) * r,
                Math.cos(a) * rand(0.2, 0.9), rand(2.2, 3.5) * (0.7 + power * 0.6), Math.sin(a) * rand(0.2, 0.9), rand(5, 8), c.getHex(), rand(0.8, 1.6));
            }
            n -= 1;
          }
          pile.scale.multiplyScalar(1 - dt * 0.25);
          fizzT += dt; if (fizzT > 1.3 && erupt > 1) { fizzT = 0; ctx.sfx.fizz(1.4); }
          erupt -= dt;
          if (erupt <= 0) {
            erupt = 0; soda = 0; pile.scale.setScalar(0.001);
            ctx.award('done');
            ctx.ui.step('🎉 Ура, получилось! Попробуй 3 ложки соды или другую краску. А потом нажми «💡 Почему?»');
          }
        }

        // физика частиц
        for (let i = 0; i < N; i++) {
          const p = P[i];
          if (!p.on) continue;
          p.life -= dt;
          if (p.life <= 0) { p.on = false; dummy.position.set(0, -50, 0); dummy.scale.setScalar(0.0001); dummy.updateMatrix(); inst.setMatrixAt(i, dummy.matrix); continue; }
          p.vy -= 6 * dt;
          p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
          const r = Math.hypot(p.x, p.z) || 0.0001;
          if (p.kind !== 0) { // сода и уксус пропадают в кратере
            if (p.y < CRATER_FLOOR + 0.15 && r < 0.45) p.life = 0;
          } else {
            const floorY = r < 0.42 ? CRATER_FLOOR + 0.05 : slopeH(r) + 0.06;
            if (p.y < floorY) {
              p.y = floorY;
              const nx = p.x / r, nz = p.z / r;
              const radial = Math.max(p.vx * nx + p.vz * nz, r < 0.42 ? 0.6 : 0.35);
              const slow = r > 2.3 ? 0.9 : 0.985;
              p.vx = nx * radial * slow; p.vz = nz * radial * slow; p.vy = 0;
              if (r > 2.3) { p.vx *= 0.96; p.vz *= 0.96; }
            }
          }
          const fade = Math.min(1, p.life / 1.0);
          dummy.position.set(p.x, p.y, p.z);
          dummy.scale.setScalar(p.s * fade * (p.kind === 0 ? 1 : 0.6));
          dummy.updateMatrix(); inst.setMatrixAt(i, dummy.matrix);
        }
        inst.instanceMatrix.needsUpdate = true;
        if (inst.instanceColor) inst.instanceColor.needsUpdate = true;
        bSoda.disabled = soda >= 3 || erupt > 0; bVin.disabled = erupt > 0;
      },
    };
  },
};
