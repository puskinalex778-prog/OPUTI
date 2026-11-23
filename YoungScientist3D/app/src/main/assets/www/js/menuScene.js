// Фоновая 3D-сцена главного меню: летающие колбы, атомы и планеты.
export default function menuScene(ctx) {
  const { THREE } = ctx;
  const { scene, camera } = ctx.newScene({ bg: 0x0d1530, camPos: [0, 0, 12], target: [0, 0, 0], orbit: false });
  scene.fog = new THREE.Fog(0x0d1530, 10, 30);
  scene.add(new THREE.HemisphereLight(0xbfd4ff, 0x302050, 1.2));
  const dl = new THREE.DirectionalLight(0xffffff, 2); dl.position.set(4, 6, 8); scene.add(dl);

  const items = [];
  const colors = [0xff6b6b, 0xffcf3f, 0x4dd4ff, 0x7be0a8, 0xb38bff, 0xff9f43];
  function atom() {
    const g = new THREE.Group();
    g.add(new THREE.Mesh(new THREE.SphereGeometry(0.35, 24, 16), new THREE.MeshStandardMaterial({ color: 0xff6b6b, emissive: 0x401010 })));
    for (let i = 0; i < 3; i++) {
      const ring = new THREE.Mesh(new THREE.TorusGeometry(1, 0.03, 8, 64), new THREE.MeshBasicMaterial({ color: 0x9fd8ff }));
      ring.rotation.set(Math.random() * 3, i * 1.05, 0); g.add(ring);
      const e = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 8), new THREE.MeshBasicMaterial({ color: 0xffffff }));
      e.position.x = 1; ring.add(e);
    }
    return g;
  }
  function flask(color) {
    const g = new THREE.Group();
    const glass = new THREE.MeshStandardMaterial({ color: 0xddeeff, transparent: true, opacity: 0.35, roughness: 0.1 });
    const pts = [new THREE.Vector2(0, -0.8), new THREE.Vector2(0.7, -0.7), new THREE.Vector2(0.75, -0.5), new THREE.Vector2(0.2, 0.4), new THREE.Vector2(0.2, 0.9)];
    g.add(new THREE.Mesh(new THREE.LatheGeometry(pts, 24), glass));
    const liq = [new THREE.Vector2(0, -0.78), new THREE.Vector2(0.66, -0.68), new THREE.Vector2(0.62, -0.35), new THREE.Vector2(0, -0.35)];
    g.add(new THREE.Mesh(new THREE.LatheGeometry(liq, 24), new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.3 })));
    return g;
  }
  function planet(color) {
    const g = new THREE.Group();
    g.add(new THREE.Mesh(new THREE.SphereGeometry(0.6, 24, 16), new THREE.MeshStandardMaterial({ color, flatShading: true })));
    const ring = new THREE.Mesh(new THREE.RingGeometry(0.85, 1.15, 40), new THREE.MeshBasicMaterial({ color: 0xffe9b0, side: THREE.DoubleSide, transparent: true, opacity: 0.7 }));
    ring.rotation.x = 1.2; g.add(ring); return g;
  }
  for (let i = 0; i < 14; i++) {
    const k = i % 3;
    const o = k === 0 ? atom() : k === 1 ? flask(colors[i % colors.length]) : planet(colors[(i + 2) % colors.length]);
    o.position.set((Math.random() - 0.5) * 18, (Math.random() - 0.5) * 14, -Math.random() * 12);
    o.userData = { sp: 0.2 + Math.random() * 0.5, ph: Math.random() * 6, rs: (Math.random() - 0.5) * 0.8 };
    scene.add(o); items.push(o);
  }
  const starG = new THREE.BufferGeometry(); const arr = [];
  for (let i = 0; i < 400; i++) arr.push((Math.random() - 0.5) * 60, (Math.random() - 0.5) * 40, -10 - Math.random() * 15);
  starG.setAttribute('position', new THREE.Float32BufferAttribute(arr, 3));
  scene.add(new THREE.Points(starG, new THREE.PointsMaterial({ color: 0xffffff, size: 0.08 })));

  return {
    update(dt, t) {
      items.forEach((o) => {
        const u = o.userData;
        o.position.y += Math.sin(t * u.sp + u.ph) * dt * 0.3;
        o.rotation.y += u.rs * dt; o.rotation.x += u.rs * 0.5 * dt;
        o.children.forEach((c) => { if (c.geometry && c.geometry.type === 'TorusGeometry') c.rotation.z += dt * 2; });
      });
      camera.position.x = Math.sin(t * 0.1) * 1.5; camera.lookAt(0, 0, 0);
    },
  };
}
