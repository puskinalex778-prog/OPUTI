import * as THREE from 'three';
import { labRoom, shadowAll, rand, clamp } from '../common.js';

const TANK_W = 5.6, TANK_D = 2.2, TANK_H = 3.0, WATER = 2.3, BOTTOM = 0.06;

function std(color, extra = {}) { return new THREE.MeshStandardMaterial({ color, roughness: 0.6, ...extra }); }
const ITEMS = [
  { name: 'Мячик', emoji: '🏓', rho: 0.08, h: 0.44, make: () => new THREE.Mesh(new THREE.SphereGeometry(0.22, 24, 16), std(0xffffff, { roughness: 0.3 })) },
  { name: 'Брусок', emoji: '🪵', rho: 0.6, h: 0.36, make: () => new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.36, 0.36), std(0xc8904f, { roughness: 0.9 })) },
  { name: 'Яблоко', emoji: '🍎', rho: 0.8, h: 0.5, make: () => {
      const g = new THREE.Group();
      const a = new THREE.Mesh(new THREE.SphereGeometry(0.25, 24, 16), std(0xe0302a, { roughness: 0.35 })); a.scale.y = 0.92; g.add(a);
      const st = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.02, 0.14), std(0x5a3a1a)); st.position.y = 0.27; g.add(st);
      const lf = new THREE.Mesh(new THREE.SphereGeometry(0.07, 8, 6), std(0x3faa3a)); lf.scale.set(1, 0.3, 0.5); lf.position.set(0.07, 0.3, 0); g.add(lf);
      return g; } },
  { name: 'Лёд', emoji: '🧊', rho: 0.92, h: 0.42, make: () => { const m = new THREE.Mesh(new THREE.BoxGeometry(0.42, 0.42, 0.42), std(0xcff4ff, { transparent: true, opacity: 0.7, roughness: 0.05 })); m.renderOrder = 1; return m; } },
  { name: 'Яйцо', emoji: '🥚', rho: 1.03, h: 0.44, make: () => { const m = new THREE.Mesh(new THREE.SphereGeometry(0.17, 24, 16), std(0xf5e6c8, { roughness: 0.5 })); m.scale.set(1, 1.3, 1); return m; } },
  { name: 'Камень', emoji: '🪨', rho: 2.6, h: 0.42, make: () => new THREE.Mesh(new THREE.DodecahedronGeometry(0.24), std(0x7d7d85, { flatShading: true })) },
  { name: 'Гайка', emoji: '🔩', rho: 7.8, h: 0.2, make: () => { const m = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.2, 0.2, 6), std(0xb8bcc8, { metalness: 0.9, roughness: 0.25 })); return m; } },
];

export default {
  id: 'buoyancy', title: 'Тонет — не тонет', emoji: '🛟', subject: 'Физика', color: '#3fa9f5',
  short: 'Бросай предметы в аквариум и узнай, кто утонет. А что будет, если посолить воду?',
  why: `<h2>🛟 Почему одни предметы плавают, а другие тонут?</h2>
    <p>Всё дело в <b>плотности</b> — это сколько вещества «упаковано» в одном кусочке объёма. У воды плотность <b>1 г/см³</b>.</p>
    <ul><li>Если плотность предмета <b>меньше</b>, чем у воды, — он <b>плавает</b> (дерево, лёд, яблоко).</li>
    <li>Если <b>больше</b> — он <b>тонет</b> (камень, железная гайка).</li></ul>
    <p>Вода выталкивает всё, что в неё погружено. Эту силу открыл древнегреческий учёный <b>Архимед</b>, поэтому она называется <b>силой Архимеда</b>.</p>
    <p>Посмотри: чем легче предмет, тем выше он торчит из воды. Лёд (0,92) почти весь под водой — поэтому у айсберга над водой видна только верхушка!</p>
    <p>🥚 <b>Яйцо с солью.</b> Плотность яйца — 1,03, чуть больше, чем у воды, поэтому оно тонет. Соль делает воду плотнее (≈1,15) — и яйцо всплывает!</p>
    <p class="fact">🌊 <b>Факт:</b> в Мёртвом море вода такая солёная, что человек лежит на ней, как на матрасе, и не тонет.</p>
    <p class="home">🏠 <b>Попробуй дома:</b> положи сырое яйцо в стакан воды. Потом добавляй по ложке соли и мешай. Сколько ложек понадобилось, чтобы яйцо всплыло?</p>`,
  quiz: [
    { q: 'Почему деревянный брусок плавает?', a: ['Он квадратный', 'Его плотность меньше, чем у воды', 'Он сухой', 'Дерево боится воды'], ok: 1,
      note: 'Плотность дерева ≈ 0,6 г/см³ — меньше, чем у воды (1).' },
    { q: 'Как заставить яйцо всплыть?', a: ['Подогреть воду', 'Насыпать в воду соль', 'Покрасить яйцо', 'Бросить сильнее'], ok: 1,
      note: 'Солёная вода плотнее пресной и сильнее выталкивает яйцо.' },
    { q: 'Как называется сила, с которой вода выталкивает предметы?', a: ['Сила Ньютона', 'Сила тяжести', 'Сила Архимеда', 'Сила трения'], ok: 2,
      note: 'Её открыл Архимед более 2000 лет назад.' },
  ],

  start(ctx) {
    const { scene } = ctx.newScene({ bg: 0x1a2550, camPos: [0, 3.6, 8.2], target: [0, 1.4, 0], minDist: 4, maxDist: 15 });
    labRoom(scene);

    // аквариум
    const glassMat = new THREE.MeshStandardMaterial({ color: 0xcfe6ff, transparent: true, opacity: 0.12, roughness: 0.05, depthWrite: false, side: THREE.DoubleSide });
    const tank = new THREE.Mesh(new THREE.BoxGeometry(TANK_W, TANK_H, TANK_D), glassMat);
    tank.position.y = TANK_H / 2; tank.renderOrder = 3; scene.add(tank);
    const edges = new THREE.LineSegments(new THREE.EdgesGeometry(tank.geometry), new THREE.LineBasicMaterial({ color: 0xe8f4ff }));
    edges.position.copy(tank.position); scene.add(edges);
    const base = new THREE.Mesh(new THREE.BoxGeometry(TANK_W + 0.1, BOTTOM, TANK_D + 0.1), std(0x23305e)); base.position.y = BOTTOM / 2; base.receiveShadow = true; scene.add(base);
    // песок
    const sand = new THREE.Mesh(new THREE.BoxGeometry(TANK_W - 0.02, 0.04, TANK_D - 0.02), std(0xe6cf8f, { roughness: 1 })); sand.position.y = BOTTOM + 0.02; sand.receiveShadow = true; scene.add(sand);

    const waterMat = new THREE.MeshStandardMaterial({ color: 0x3aa6ff, transparent: true, opacity: 0.38, roughness: 0.1, depthWrite: false });
    const water = new THREE.Mesh(new THREE.BoxGeometry(TANK_W - 0.04, WATER - BOTTOM, TANK_D - 0.04), waterMat);
    water.position.y = BOTTOM + (WATER - BOTTOM) / 2; water.renderOrder = 2; scene.add(water);
    const surfMat = new THREE.MeshStandardMaterial({ color: 0x8fd2ff, transparent: true, opacity: 0.35, roughness: 0.05, depthWrite: false, side: THREE.DoubleSide });
    const surf = new THREE.Mesh(new THREE.PlaneGeometry(TANK_W - 0.04, TANK_D - 0.04, 40, 16), surfMat);
    surf.rotation.x = -Math.PI / 2; surf.position.y = WATER; surf.renderOrder = 2; scene.add(surf);
    const surfPos = surf.geometry.attributes.position;

    // брызги
    const NS = 240; const drops = new THREE.InstancedMesh(new THREE.SphereGeometry(0.035, 6, 4), new THREE.MeshBasicMaterial({ color: 0xbfe6ff }), NS);
    drops.frustumCulled = false; scene.add(drops); const D = Array.from({ length: NS }, () => ({ on: false })); let dc = 0;
    const dummy = new THREE.Object3D();
    function splash(x, z, power) {
      for (let k = 0; k < 24 * power; k++) {
        const d = D[dc]; dc = (dc + 1) % NS; const a = rand(0, 6.28), s = rand(0.5, 1.6) * power;
        Object.assign(d, { on: true, x, y: WATER, z, vx: Math.cos(a) * s * 0.6, vy: rand(1.5, 3) * power, vz: Math.sin(a) * s * 0.6 });
      }
      ripples.push({ x, z, t: 0, a: 0.08 * power });
      ctx.sfx.plop();
    }
    const ripples = [];

    // предметы
    let rhoW = 1.0, salt = false;
    const objs = ITEMS.map((it, i) => {
      const mesh = it.make(); shadowAll(mesh); mesh.visible = false; scene.add(mesh);
      const label = ctx.makeLabel(`${it.name}\n${String(it.rho).replace('.', ',')} г/см³`, { fontSize: 36, scale: 0.42 });
      label.visible = false; scene.add(label);
      const x = -TANK_W / 2 + 0.5 + i * ((TANK_W - 1) / (ITEMS.length - 1));
      return { it, mesh, label, x, y: 5, v: 0, active: false, inWater: false, settle: 0, reported: false, spin: rand(-1, 1) };
    });
    const results = {};

    function drop(i) {
      const o = objs[i];
      o.active = true; o.mesh.visible = true; o.label.visible = true;
      o.y = 4.3; o.v = 0; o.inWater = false; o.settle = 0; o.reported = false;
      o.mesh.rotation.set(rand(-0.3, 0.3), rand(0, 3), rand(-0.3, 0.3));
      ctx.ui.step(`Летит ${o.it.emoji} <b>${o.it.name}</b>… Как думаешь, утонет?`);
    }

    function report() {
      const lines = Object.entries(results).map(([k, r]) => `${r.emoji} ${r.name}${r.salt ? ' (солёная)' : ''}: <b>${r.floats ? '🟢 плавает' : '🔴 тонет'}</b>`);
      ctx.ui.info(`Плотность воды: <b>${String(rhoW.toFixed(2)).replace('.', ',')} г/см³</b> ${salt ? '🧂' : ''}<br>` + lines.join('<br>'));
    }

    ctx.ui.clear();
    const pick = ctx.ui.choices('Брось в воду:', ITEMS.map((it, i) => ({ label: `${it.emoji} ${it.name}`, value: i })), (i) => drop(i));
    const bSalt = ctx.ui.button('🧂 Насыпать соль', () => {
      salt = !salt; rhoW = salt ? 1.15 : 1.0;
      waterMat.color.set(salt ? 0x9fd0f0 : 0x3aa6ff); waterMat.opacity = salt ? 0.5 : 0.38;
      bSalt.innerHTML = salt ? '💧 Сделать воду пресной' : '🧂 Насыпать соль';
      objs.forEach((o) => { o.reported = false; o.settle = 0; });
      ctx.sfx.fizz(0.6);
      ctx.ui.step(salt ? 'Вода стала солёной и <b>плотнее</b>! Посмотри на яйцо 🥚' : 'Вода снова пресная.');
      report();
    }, 'purple');
    ctx.ui.button('🧹 Убрать всё', () => { objs.forEach((o) => { o.active = false; o.mesh.visible = false; o.label.visible = false; }); }, 'gray');
    ctx.ui.step('Выбери предмет и брось его в аквариум. Угадай заранее: <b>утонет или нет?</b>');
    report();

    let eggToast = false;
    return {
      update(dt, t) {
        const g = 9.8;
        objs.forEach((o) => {
          if (!o.active) return;
          const h = o.it.h;
          const bottomY = o.y - h / 2;
          const f = clamp((WATER - bottomY) / h, 0, 1);
          const wasIn = o.inWater; o.inWater = f > 0.05;
          if (!wasIn && o.inWater && Math.abs(o.v) > 1) splash(o.x, 0, clamp(Math.abs(o.v) / 6, 0.4, 1.3));
          let a = g * ((rhoW / o.it.rho) * f - 1);
          const drag = f > 0 ? (f < 1 ? 3.5 : 1.2) : 0.1;
          a -= drag * o.v;
          o.v += a * dt; o.y += o.v * dt;
          const floor = BOTTOM + 0.04 + h / 2;
          if (o.y < floor) { if (o.v < -0.5) ctx.sfx.thud(); o.y = floor; o.v = 0; }
          // покачивание на воде
          let bob = 0;
          if (f > 0 && f < 1) { bob = Math.sin(t * 2 + o.x) * 0.015; o.mesh.rotation.z = Math.sin(t * 1.5 + o.x) * 0.06; }
          o.mesh.position.set(o.x, o.y + bob, 0);
          if (o.inWater) o.mesh.rotation.y += o.spin * dt * 0.2 * Math.min(1, Math.abs(o.v) * 2);
          o.label.position.set(o.x, Math.max(o.y + h / 2 + 0.45, WATER + 0.55), 0.9);
          // фиксируем результат
          if (o.inWater && Math.abs(o.v) < 0.03) o.settle += dt; else o.settle = 0;
          if (o.settle > 0.8 && !o.reported) {
            o.reported = true;
            const floats = o.y > floor + 0.01 || (o.it.rho < rhoW);
            results[o.it.name + (salt ? '_s' : '')] = { name: o.it.name, emoji: o.it.emoji, floats, salt };
            pick.mark(ITEMS.indexOf(o.it));
            ctx.ui.step(`${o.it.emoji} <b>${o.it.name}</b> ${floats ? 'плавает! 🟢' : 'утонул(а)! 🔴'} Плотность: ${String(o.it.rho).replace('.', ',')} — ${floats ? 'меньше' : 'больше'}, чем у воды (${String(rhoW.toFixed(2)).replace('.', ',')}).`);
            if (floats) ctx.sfx.good(); else ctx.sfx.thud();
            report();
            const tested = new Set(Object.values(results).map((r) => r.name)).size;
            if (tested >= 5) ctx.award('done');
            if (o.it.name === 'Яйцо' && salt && floats && !eggToast) { eggToast = true; ctx.toast('🥚 Ого! В солёной воде яйцо всплыло!'); }
          }
        });

        // брызги
        for (let i = 0; i < NS; i++) {
          const d = D[i];
          if (d.on) { d.vy -= 9 * dt; d.x += d.vx * dt; d.y += d.vy * dt; d.z += d.vz * dt; if (d.y < WATER) d.on = false; }
          dummy.position.set(d.on ? d.x : 0, d.on ? d.y : -50, d.on ? d.z : 0); dummy.updateMatrix(); drops.setMatrixAt(i, dummy.matrix);
        }
        drops.instanceMatrix.needsUpdate = true;

        // рябь на поверхности
        for (let k = ripples.length - 1; k >= 0; k--) { ripples[k].t += dt; if (ripples[k].t > 3) ripples.splice(k, 1); }
        for (let i = 0; i < surfPos.count; i++) {
          const x = surfPos.getX(i), y = surfPos.getY(i);
          let zv = Math.sin(x * 2 + t * 1.5) * 0.008 + Math.cos(y * 3 + t) * 0.006;
          for (const r of ripples) { const dd = Math.hypot(x - r.x, y + r.z); zv += Math.sin(dd * 12 - r.t * 10) * r.a * Math.exp(-r.t * 1.5) * Math.exp(-dd * 1.2); }
          surfPos.setZ(i, zv);
        }
        surfPos.needsUpdate = true;
      },
    };
  },
};
