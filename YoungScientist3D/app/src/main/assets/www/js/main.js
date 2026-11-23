import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { sfx } from './sound.js';
import volcano from './exp/volcano.js';
import buoyancy from './exp/buoyancy.js';
import prism from './exp/prism.js';
import gravity from './exp/gravity.js';
import circuit from './exp/circuit.js';
import earth from './exp/earth.js';
import menuScene from './menuScene.js';

const EXPERIMENTS = [volcano, buoyancy, prism, gravity, circuit, earth];
const $ = (id) => document.getElementById(id);

/* ---------------- progress (звёзды) ---------------- */
const KEY = 'lab3d_progress_v1';
let progress = {};
try { progress = JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { progress = {}; }
function saveProgress() { try { localStorage.setItem(KEY, JSON.stringify(progress)); } catch (e) {} }
function starsOf(id) { const p = progress[id] || {}; return (p.done ? 1 : 0) + (p.why ? 1 : 0) + (p.quiz ? 1 : 0); }
function totalStars() { return EXPERIMENTS.reduce((s, e) => s + starsOf(e.id), 0); }

/* ---------------- renderer ---------------- */
const stage = $('stage');
const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
stage.appendChild(renderer.domElement);

let scene = null, camera = null, controls = null, current = null, currentExp = null;
const tapHandlers = [];

function resize() {
  const w = stage.clientWidth || 1, h = stage.clientHeight || 1;
  renderer.setSize(w, h, false);
  if (camera) { camera.aspect = w / h; camera.updateProjectionMatrix(); }
}
new ResizeObserver(resize).observe(stage);
window.addEventListener('resize', resize);

/* ---------------- helpers for experiments ---------------- */
function disposeScene(s) {
  if (!s) return;
  s.traverse((o) => {
    if (o.geometry) o.geometry.dispose();
    if (o.material) {
      const mats = Array.isArray(o.material) ? o.material : [o.material];
      mats.forEach((m) => { for (const k in m) { if (m[k] && m[k].isTexture) m[k].dispose(); } m.dispose(); });
    }
  });
}

function toast(msg, ms = 2200) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toast._t); toast._t = setTimeout(() => t.classList.remove('show'), ms);
}

function award(kind) {
  if (!currentExp) return;
  const p = progress[currentExp.id] || (progress[currentExp.id] = {});
  if (p[kind]) return;
  p[kind] = true; saveProgress(); sfx.star();
  const names = { done: 'Опыт проведён!', why: 'Ты узнал, почему!', quiz: 'Тест пройден!' };
  toast('⭐ +1 звезда — ' + names[kind]);
  updateTopButtons();
}

function makeLabel(text, opts = {}) {
  const fs = opts.fontSize || 48, pad = 18;
  const c = document.createElement('canvas'); const g = c.getContext('2d');
  g.font = `800 ${fs}px sans-serif`;
  const lines = String(text).split('\n');
  const w = Math.max(...lines.map((l) => g.measureText(l).width)) + pad * 2;
  const h = lines.length * fs * 1.2 + pad * 2;
  c.width = Math.ceil(w); c.height = Math.ceil(h);
  g.font = `800 ${fs}px sans-serif`;
  g.fillStyle = opts.bg || 'rgba(15,22,55,0.85)';
  const r = 24; g.beginPath(); g.roundRect ? g.roundRect(0, 0, c.width, c.height, r) : g.rect(0, 0, c.width, c.height); g.fill();
  g.fillStyle = opts.color || '#fff'; g.textBaseline = 'middle'; g.textAlign = 'center';
  lines.forEach((l, i) => g.fillText(l, c.width / 2, pad + fs * 0.6 + i * fs * 1.2));
  const tex = new THREE.CanvasTexture(c); tex.colorSpace = THREE.SRGBColorSpace;
  const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, depthTest: opts.depthTest ?? true, transparent: true }));
  const s = opts.scale || 0.5; sp.scale.set(s * c.width / c.height, s, 1);
  sp.renderOrder = 10;
  return sp;
}

const ui = {
  step(html) { $('step').innerHTML = html; },
  info(html) { $('info').innerHTML = html || ''; },
  clear() { $('controls').innerHTML = ''; },
  button(label, onClick, cls = '') {
    const b = document.createElement('button'); b.className = 'btn ' + cls; b.innerHTML = label;
    b.addEventListener('click', () => { sfx.click(); onClick(b); });
    $('controls').appendChild(b); return b;
  },
  choices(label, items, onPick, initial = null) {
    const wrap = document.createElement('div'); wrap.className = 'seg';
    if (label) { const l = document.createElement('div'); l.className = 'lbl'; l.textContent = label; wrap.appendChild(l); }
    const btns = items.map((it, i) => {
      const b = document.createElement('button'); b.innerHTML = it.label;
      b.addEventListener('click', () => { sfx.click(); btns.forEach((x) => x.classList.remove('on')); b.classList.add('on'); onPick(it.value, i, b); });
      wrap.appendChild(b); return b;
    });
    if (initial !== null && btns[initial]) btns[initial].classList.add('on');
    $('controls').appendChild(wrap);
    return { el: wrap, buttons: btns, mark(i) { btns[i] && btns[i].classList.add('tested'); },
      select(i) { btns.forEach((x) => x.classList.remove('on')); btns[i] && btns[i].classList.add('on'); } };
  },
  slider(label, min, max, step, value, onInput, fmt = (v) => v) {
    const wrap = document.createElement('div'); wrap.className = 'slider';
    wrap.innerHTML = `<div class="lbl"><span>${label}</span><b></b></div><input type="range" min="${min}" max="${max}" step="${step}" value="${value}">`;
    const inp = wrap.querySelector('input'), out = wrap.querySelector('b');
    const upd = () => { const v = parseFloat(inp.value); out.textContent = fmt(v); onInput(v); };
    inp.addEventListener('input', upd);
    out.textContent = fmt(value);
    $('controls').appendChild(wrap);
    return { el: wrap, input: inp, set(v) { inp.value = v; upd(); } };
  },
};

/* ---------------- tap / raycast ---------------- */
const raycaster = new THREE.Raycaster(); const ndc = new THREE.Vector2();
let downPos = null;
renderer.domElement.addEventListener('pointerdown', (e) => { downPos = { x: e.clientX, y: e.clientY, t: performance.now() }; sfx.unlock(); });
renderer.domElement.addEventListener('pointerup', (e) => {
  if (!downPos || !camera) return;
  const moved = Math.hypot(e.clientX - downPos.x, e.clientY - downPos.y);
  if (moved > 12 || performance.now() - downPos.t > 600) return;
  const r = renderer.domElement.getBoundingClientRect();
  ndc.set(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
  raycaster.setFromCamera(ndc, camera);
  for (const h of tapHandlers) {
    const hits = raycaster.intersectObjects(h.objects, true);
    if (hits.length) { h.cb(hits[0]); break; }
  }
});

/* ---------------- modal ---------------- */
function modal(html, buttons) {
  $('modalBody').innerHTML = html;
  const box = $('modalBtns'); box.innerHTML = '';
  (buttons || [{ label: 'Понятно! 👍', cls: 'green' }]).forEach((b) => {
    const el = document.createElement('button'); el.className = b.cls === 'q' ? 'qopt' : 'btn ' + (b.cls || '');
    el.innerHTML = b.label;
    el.addEventListener('click', () => { if (b.onClick) { if (b.onClick(el) === false) return; } closeModal(); });
    box.appendChild(el);
  });
  $('modal').classList.remove('hidden');
}
function closeModal() { $('modal').classList.add('hidden'); }

function showWhy() {
  if (!currentExp) return;
  sfx.click();
  modal(currentExp.why, [{ label: 'Понятно! 👍', cls: 'green', onClick: () => award('why') }]);
}

function showQuiz() {
  if (!currentExp) return;
  sfx.click();
  const qs = currentExp.quiz; let i = 0, score = 0;
  const ask = () => {
    const q = qs[i];
    const btns = q.a.map((txt, k) => ({
      label: txt, cls: 'q',
      onClick: (el) => {
        const all = $('modalBtns').querySelectorAll('.qopt');
        if (all[0].disabled) return false;
        all.forEach((b) => { b.disabled = true; });
        all[q.ok].classList.add('right');
        if (k === q.ok) { score++; sfx.good(); } else { el.classList.add('wrong'); sfx.bad(); }
        const note = document.createElement('p'); note.innerHTML = (k === q.ok ? '✅ <b>Верно!</b> ' : '❌ <b>Не совсем.</b> ') + (q.note || '');
        $('modalBody').appendChild(note);
        const next = document.createElement('button'); next.className = 'btn';
        next.textContent = i < qs.length - 1 ? 'Дальше →' : 'Результат';
        next.addEventListener('click', () => { i++; if (i < qs.length) ask(); else finish(); });
        $('modalBtns').appendChild(next);
        return false;
      },
    }));
    modal(`<h2>❓ Вопрос ${i + 1} из ${qs.length}</h2><p><b>${q.q}</b></p>`, btns);
  };
  const finish = () => {
    const passed = score >= Math.ceil(qs.length * 0.66);
    modal(`<h2>${passed ? '🏆 Отлично!' : '🙂 Почти!'}</h2><p>Правильных ответов: <b>${score} из ${qs.length}</b>.</p>
      <p>${passed ? 'Ты настоящий учёный!' : 'Загляни в «Почему?» и попробуй ещё раз.'}</p>`,
      [{ label: passed ? 'Ура! 🎉' : 'Хорошо', cls: 'green', onClick: () => { if (passed) award('quiz'); } }]);
  };
  ask();
}

function updateTopButtons() {
  if (!currentExp) return;
  const p = progress[currentExp.id] || {};
  $('whyBtn').classList.toggle('done', !!p.why);
  $('quizBtn').classList.toggle('done', !!p.quiz);
  $('title').textContent = `${currentExp.emoji} ${currentExp.title}  ${'⭐'.repeat(starsOf(currentExp.id))}`;
}
$('whyBtn').addEventListener('click', showWhy);
$('quizBtn').addEventListener('click', showQuiz);
$('backBtn').addEventListener('click', () => { sfx.click(); openMenu(); });

/* ---------------- scene switching ---------------- */
function newScene({ bg = 0x0d1530, fov = 50, camPos = [0, 4, 9], target = [0, 1, 0], orbit = true, minDist = 2, maxDist = 30 } = {}) {
  if (current && current.dispose) current.dispose();
  if (controls) controls.dispose();
  disposeScene(scene);
  tapHandlers.length = 0;
  scene = new THREE.Scene();
  scene.background = new THREE.Color(bg);
  camera = new THREE.PerspectiveCamera(fov, 1, 0.05, 500);
  camera.position.set(...camPos);
  controls = null;
  if (orbit) {
    controls = new OrbitControls(camera, renderer.domElement);
    controls.target.set(...target);
    controls.enableDamping = true; controls.dampingFactor = 0.08;
    controls.minDistance = minDist; controls.maxDistance = maxDist;
    controls.maxPolarAngle = Math.PI * 0.49;
    controls.enablePan = false;
    controls.update();
  } else {
    camera.lookAt(...target);
  }
  resize();
  return { scene, camera, controls };
}

function makeCtx() {
  return {
    THREE, renderer, ui, sfx, toast, makeLabel,
    get scene() { return scene; }, get camera() { return camera; }, get controls() { return controls; },
    newScene, award: (k) => award(k),
    onTap(objects, cb) { tapHandlers.push({ objects, cb }); },
    progress: () => progress[currentExp.id] || {},
  };
}

function openExperiment(exp) {
  sfx.unlock(); sfx.click();
  currentExp = exp;
  $('menu').classList.add('hidden');
  $('topbar').classList.remove('hidden');
  $('panel').classList.remove('hidden');
  ui.clear(); ui.info(''); ui.step('');
  updateTopButtons();
  resize();
  current = exp.start(makeCtx());
}

function openMenu() {
  currentExp = null;
  $('topbar').classList.add('hidden');
  $('panel').classList.add('hidden');
  $('menu').classList.remove('hidden');
  renderCards();
  current = menuScene(makeCtx());
}

function renderCards() {
  const box = $('cards'); box.innerHTML = '';
  EXPERIMENTS.forEach((e) => {
    const s = starsOf(e.id);
    const c = document.createElement('button'); c.className = 'ecard';
    c.style.borderBottom = `6px solid ${e.color}`;
    c.innerHTML = `<div class="em">${e.emoji}</div><span class="tag" style="background:${e.color}">${e.subject}</span>
      <h2>${e.title}</h2><p>${e.short}</p><div class="st">${'⭐'.repeat(s)}${'☆'.repeat(3 - s)}</div>`;
    c.addEventListener('click', () => openExperiment(e));
    box.appendChild(c);
  });
  $('totalStars').textContent = `⭐ ${totalStars()} / ${EXPERIMENTS.length * 3}`;
}

/* ---------------- loop ---------------- */
const clock = new THREE.Clock();
function loop() {
  requestAnimationFrame(loop);
  const dt = Math.min(clock.getDelta(), 0.05);
  const t = clock.elapsedTime;
  try { if (current && current.update) current.update(dt, t); } catch (err) { console.error(err); }
  if (controls) controls.update();
  if (scene && camera) renderer.render(scene, camera);
}

// Android "назад" (вызывается из MainActivity)
window.onAndroidBack = function () {
  if (!$('modal').classList.contains('hidden')) { closeModal(); return true; }
  if (currentExp) { openMenu(); return true; }
  return false;
};

openMenu();
loop();
$('loading').classList.add('hidden');
