import * as THREE from 'three';

// Уютная «лаборатория»: стол, стена, свет с тенями.
export function labRoom(scene, { wall = 0x2a3b74, table = 0xb07a4a, size = 14, lightPos = [4, 9, 6] } = {}) {
  scene.add(new THREE.HemisphereLight(0xdfe8ff, 0x3a2a20, 1.0));
  const dl = new THREE.DirectionalLight(0xffffff, 2.2);
  dl.position.set(...lightPos); dl.castShadow = true;
  dl.shadow.mapSize.set(1024, 1024);
  const c = dl.shadow.camera; c.left = -size / 2; c.right = size / 2; c.top = size / 2; c.bottom = -size / 2; c.near = 1; c.far = 40;
  dl.shadow.bias = -0.0005;
  scene.add(dl);

  const top = new THREE.Mesh(new THREE.BoxGeometry(size, 0.4, size * 0.7), new THREE.MeshStandardMaterial({ color: table, roughness: 0.8 }));
  top.position.y = -0.2; top.receiveShadow = true; scene.add(top);
  // планки на столе — чтобы было видно дерево
  for (let i = -3; i <= 3; i++) {
    const l = new THREE.Mesh(new THREE.BoxGeometry(size, 0.002, 0.02), new THREE.MeshBasicMaterial({ color: 0x8a5a32 }));
    l.position.set(0, 0.001, i * size * 0.1); scene.add(l);
  }
  const w = new THREE.Mesh(new THREE.PlaneGeometry(size * 2, size), new THREE.MeshStandardMaterial({ color: wall, roughness: 1 }));
  w.position.set(0, size / 2 - 1, -size * 0.35); w.receiveShadow = true; scene.add(w);
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(size * 3, size * 3), new THREE.MeshStandardMaterial({ color: 0x1b2347 }));
  floor.rotation.x = -Math.PI / 2; floor.position.y = -4; scene.add(floor);
  return { light: dl, table: top };
}

export function shadowAll(obj) { obj.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } }); return obj; }

export const rand = (a, b) => a + Math.random() * (b - a);
export const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
export const lerp = (a, b, t) => a + (b - a) * t;
