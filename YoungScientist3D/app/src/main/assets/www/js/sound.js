// Простые звуки, синтезированные Web Audio API (без файлов).
let ac = null;
function ctx() {
  if (!ac) { try { ac = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { ac = null; } }
  return ac;
}
function tone(freq, dur = 0.12, type = 'sine', vol = 0.15, slideTo = null, delay = 0) {
  const a = ctx(); if (!a) return;
  const t0 = a.currentTime + delay;
  const o = a.createOscillator(), g = a.createGain();
  o.type = type; o.frequency.setValueAtTime(freq, t0);
  if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, t0 + dur);
  g.gain.setValueAtTime(vol, t0); g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  o.connect(g).connect(a.destination); o.start(t0); o.stop(t0 + dur + 0.02);
}
function noise(dur = 0.5, vol = 0.12, filterFreq = 2000) {
  const a = ctx(); if (!a) return;
  const len = Math.floor(a.sampleRate * dur);
  const buf = a.createBuffer(1, len, a.sampleRate); const d = buf.getChannelData(0);
  for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / len);
  const src = a.createBufferSource(); src.buffer = buf;
  const f = a.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = filterFreq;
  const g = a.createGain(); g.gain.value = vol;
  src.connect(f).connect(g).connect(a.destination); src.start();
}
export const sfx = {
  unlock() { const a = ctx(); if (a && a.state === 'suspended') a.resume(); },
  click() { tone(660, 0.05, 'triangle', 0.08); },
  pop() { tone(300, 0.12, 'sine', 0.2, 900); },
  plop() { tone(500, 0.18, 'sine', 0.2, 120); },
  thud() { tone(120, 0.2, 'triangle', 0.25, 50); },
  fizz(d = 1.5) { noise(d, 0.15, 3000); },
  pour() { noise(0.8, 0.08, 900); },
  click2() { tone(1200, 0.03, 'square', 0.06); },
  good() { tone(660, 0.12, 'triangle', 0.15); tone(880, 0.18, 'triangle', 0.15, null, 0.1); },
  bad() { tone(220, 0.25, 'sawtooth', 0.08, 160); },
  star() { [784, 988, 1175, 1568].forEach((f, i) => tone(f, 0.15, 'triangle', 0.12, null, i * 0.07)); },
  hum() { tone(110, 0.4, 'sine', 0.05); },
};
