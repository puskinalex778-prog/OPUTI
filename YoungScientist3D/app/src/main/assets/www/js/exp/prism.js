import * as THREE from 'three';
import { shadowAll } from '../common.js';
import { prismVerts, trace, SPECTRUM } from './optics.js';

const SIDE = 2.4, BEAM_Y = 0.55, SRC = [-4.5, 0.9], SCREEN_X = 4.5;

export default {
  id: 'prism', title: 'Радуга из призмы', emoji: '🌈', subject: 'Оптика', color: '#b26bff',
  short: 'Посвети фонариком на стеклянную призму и поймай радугу на экране.',
  why: `<h2>🌈 Откуда в призме радуга?</h2>
    <p>Белый свет только кажется одним цветом. На самом деле это <b>смесь всех цветов радуги</b>!</p>
    <p>Когда свет переходит из воздуха в стекло, он <b>преломляется</b> — меняет направление. Разные цвета преломляются по-разному: <b>фиолетовый</b> поворачивает сильнее всех, а <b>красный</b> — слабее всех. Поэтому лучи расходятся веером, и мы видим радугу. Это называется <b>дисперсия</b>.</p>
    <p class="formula">Каждый Охотник Желает Знать, Где Сидит Фазан</p>
    <p>Первые буквы — это цвета радуги по порядку: <b>к</b>расный, <b>о</b>ранжевый, <b>ж</b>ёлтый, <b>з</b>елёный, <b>г</b>олубой, <b>с</b>иний, <b>ф</b>иолетовый.</p>
    <p>А красный лазер — это свет <b>одного</b> цвета, поэтому делиться ему не на что.</p>
    <p class="fact">🔬 <b>Факт:</b> этот опыт впервые провёл Исаак Ньютон в 1666 году. А настоящая радуга на небе получается так же — только призмами служат капельки дождя!</p>
    <p class="home">🏠 <b>Попробуй дома:</b> в солнечный день поставь на подоконник стакан с водой или положи в воду зеркальце под углом — на стене появится маленькая радуга.</p>`,
  quiz: [
    { q: 'Из чего состоит белый свет?', a: ['Только из белого цвета', 'Из всех цветов радуги', 'Из красного и синего', 'Из чёрного и белого'], ok: 1,
      note: 'Белый свет — смесь всех цветов радуги.' },
    { q: 'Какой цвет преломляется (отклоняется) в призме сильнее всего?', a: ['Красный', 'Жёлтый', 'Зелёный', 'Фиолетовый'], ok: 3,
      note: 'Фиолетовый сильнее всех, красный — слабее всех.' },
    { q: 'Почему луч красного лазера не превращается в радугу?', a: ['Он слишком яркий', 'В нём только один цвет', 'Призма сломалась', 'Лазер холодный'], ok: 1,
      note: 'Лазер светит одним цветом — разделять нечего.' },
  ],

  start(ctx) {
    const { scene, camera } = ctx.newScene({ bg: 0x05070f, camPos: [0.5, 7.5, 7.0], target: [0.3, 0.3, 0.6], minDist: 4, maxDist: 16 });
    scene.add(new THREE.HemisphereLight(0x8090c0, 0x201810, 0.35));
    const top = new THREE.Mesh(new THREE.BoxGeometry(12, 0.3, 8), new THREE.MeshStandardMaterial({ color: 0x2a2f45, roughness: 0.9 }));
    top.position.y = -0.15; top.receiveShadow = true; scene.add(top);

    // призма
    const v0 = prismVerts(0, 0, SIDE, 0);
    const shape = new THREE.Shape(); shape.moveTo(v0[0][0], -v0[0][1]); shape.lineTo(v0[1][0], -v0[1][1]); shape.lineTo(v0[2][0], -v0[2][1]); shape.closePath();
    const pg = new THREE.ExtrudeGeometry(shape, { depth: 1.0, bevelEnabled: false }); pg.rotateX(-Math.PI / 2);
    const prismMesh = new THREE.Mesh(pg, new THREE.MeshStandardMaterial({ color: 0xbfe8ff, transparent: true, opacity: 0.28, roughness: 0.05, metalness: 0.1, depthWrite: false, side: THREE.DoubleSide }));
    prismMesh.position.y = 0.02; prismMesh.renderOrder = 5; scene.add(prismMesh);
    const pEdges = new THREE.LineSegments(new THREE.EdgesGeometry(pg), new THREE.LineBasicMaterial({ color: 0xe8f8ff }));
    prismMesh.add(pEdges);

    // фонарик
    const torch = new THREE.Group();
    const body = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.18, 1.1, 20), new THREE.MeshStandardMaterial({ color: 0x3355ff, metalness: 0.4, roughness: 0.4 }));
    body.rotation.z = Math.PI / 2; body.position.x = -0.55; torch.add(body);
    const head = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.2, 0.3, 20), new THREE.MeshStandardMaterial({ color: 0xc0c8d8, metalness: 0.8, roughness: 0.3 }));
    head.rotation.z = Math.PI / 2; head.position.x = 0.1; torch.add(head);
    const lens = new THREE.Mesh(new THREE.CircleGeometry(0.25, 20), new THREE.MeshBasicMaterial({ color: 0xffffff }));
    lens.rotation.y = Math.PI / 2; lens.position.x = 0.26; torch.add(lens);
    torch.position.set(SRC[0], BEAM_Y, SRC[1]); shadowAll(torch); scene.add(torch);

    // экран
    const screen = new THREE.Mesh(new THREE.BoxGeometry(0.1, 1.8, 6), new THREE.MeshStandardMaterial({ color: 0xf4f4f4, roughness: 1 }));
    screen.position.set(SCREEN_X + 0.05, 0.9, 0.5); scene.add(screen);
    const screenLabel = ctx.makeLabel('ЭКРАН', { fontSize: 36, scale: 0.3 }); screenLabel.position.set(SCREEN_X + 0.1, 2.1, 0.5); scene.add(screenLabel);

    // лучи
    const beamGeo = new THREE.BoxGeometry(1, 0.05, 0.06);
    const beams = SPECTRUM.map((s) => {
      const segs = [];
      for (let k = 0; k < 9; k++) {
        const m = new THREE.Mesh(beamGeo, new THREE.MeshBasicMaterial({ color: new THREE.Color(s.color).multiplyScalar(0.75), transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false }));
        m.visible = false; m.renderOrder = 6; scene.add(m); segs.push(m);
      }
      const spot = new THREE.Mesh(new THREE.PlaneGeometry(0.28, 1.0), new THREE.MeshBasicMaterial({ color: s.color, transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false }));
      spot.rotation.y = -Math.PI / 2; spot.visible = false; spot.renderOrder = 7; scene.add(spot);
      return { s, segs, spot };
    });
    const glow = new THREE.PointLight(0xffffff, 0, 6); glow.position.set(0, 1, 0); scene.add(glow);

    let prismAngle = 15, beamAngle = -20, laser = false, dirty = true, rainbowShown = false;

    ctx.ui.clear();
    ctx.ui.choices('Источник света:', [{ label: '🔦 Белый фонарик', value: false }, { label: '🔴 Красный лазер', value: true }], (v) => { laser = v; dirty = true; }, 0);
    const sP = ctx.ui.slider('🔺 Поверни призму', 0, 120, 1, prismAngle, (v) => { prismAngle = v; dirty = true; }, (v) => v + '°');
    ctx.ui.slider('🔦 Наклон фонарика', -30, 30, 1, beamAngle, (v) => { beamAngle = v; dirty = true; }, (v) => v + '°');
    ctx.ui.step('Двигай ползунки, чтобы луч прошёл через призму. Что появится на экране?');
    ctx.onTap([prismMesh], () => { sP.set((prismAngle + 10) % 121); ctx.sfx.click2(); });

    function rebuild() {
      const ang = prismAngle * Math.PI / 180, b = beamAngle * Math.PI / 180;
      prismMesh.rotation.y = -ang;
      torch.rotation.y = -b;
      const verts = prismVerts(0, 0, SIDE, ang);
      const dir = [Math.cos(b), Math.sin(b)];
      const o = [SRC[0] + dir[0] * 0.3, SRC[1] + dir[1] * 0.3];
      const ends = [];
      beams.forEach((B, i) => {
        const on = !laser || i === 0;
        const r = on ? trace(o, dir, verts, B.s.n, SCREEN_X) : null;
        B.segs.forEach((m, k) => {
          if (!r || k >= r.pts.length - 1) { m.visible = false; return; }
          const a = r.pts[k], c = r.pts[k + 1];
          const dx = c[0] - a[0], dz = c[1] - a[1], L = Math.hypot(dx, dz);
          m.visible = L > 0.001;
          m.position.set((a[0] + c[0]) / 2, BEAM_Y, (a[1] + c[1]) / 2);
          m.scale.set(L, laser ? 1.4 : 1, laser ? 1.4 : 1);
          m.rotation.y = -Math.atan2(dz, dx);
        });
        if (r && r.hitScreen && Math.abs(r.end[1] - 0.5) < 3) {
          B.spot.visible = true; B.spot.position.set(SCREEN_X - 0.01, 0.9, r.end[1]);
          ends.push({ z: r.end[1], passed: r.passed });
        } else B.spot.visible = false;
      });
      const all = ends.length === 7 && ends.every((e) => e.passed);
      const spread = all ? Math.abs(ends[6].z - ends[0].z) : 0;
      glow.intensity = 0;
      if (laser) {
        ctx.ui.info('🔴 Лазер светит только <b>одним цветом</b>. Он преломляется, но радуги нет!');
      } else if (all && spread > 0.3) {
        glow.intensity = 0.6; glow.position.set(SCREEN_X - 0.5, 1, (ends[0].z + ends[6].z) / 2);
        ctx.ui.info('🌈 <b>Радуга!</b> Порядок цветов: <span style="color:#ff5050">К</span><span style="color:#ff9a20">О</span><span style="color:#ffe030">Ж</span><span style="color:#40e060">З</span><span style="color:#40d0ff">Г</span><span style="color:#5080ff">С</span><span style="color:#b060ff">Ф</span> — «Каждый Охотник Желает Знать, Где Сидит Фазан»');
        if (!rainbowShown) { rainbowShown = true; ctx.sfx.star(); ctx.ui.step('🎉 Ты поймал радугу! Теперь попробуй красный лазер.'); ctx.award('done'); }
      } else if (ends.length && ends.every((e) => e.passed)) {
        ctx.ui.info('Почти! Цвета ещё слиплись — поверни призму ещё чуть-чуть.');
      } else {
        ctx.ui.info('Луч не попадает на экран 🤔 Поверни призму или фонарик, чтобы свет прошёл сквозь стекло и упал на экран.');
      }
    }

    return {
      update(dt, t) {
        if (dirty) { dirty = false; rebuild(); }
        lens.material.color.set(laser ? 0xff3030 : 0xffffff);
      },
    };
  },
};
