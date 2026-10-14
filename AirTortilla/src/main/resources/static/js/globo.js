/* =========================================================
   Globo terráqueo 3D (Three.js) con texturas reales:
   - cara de día (Blue Marble) y cara de noche (luces de ciudades)
   - reflejo del sol en el agua y atmósfera
   - rutas de las reservas como arcos con aviones volando
   Expone window.Globo.setRutas([{origen, paisOrigen, destino, paisDestino, n}]).
   ========================================================= */
(function () {
  // Coordenadas (latitud, longitud) de las ciudades de los datos
  const CIUDADES = {
    "madrid": [40.42, -3.70], "barcelona": [41.39, 2.17], "valencia": [39.47, -0.38],
    "paris": [48.86, 2.35], "lyon": [45.76, 4.84], "roma": [41.90, 12.50], "milan": [45.46, 9.19],
    "berlin": [52.52, 13.40], "munich": [48.14, 11.58], "londres": [51.51, -0.13], "lisboa": [38.72, -9.14],
    "nueva york": [40.71, -74.01], "atlanta": [33.75, -84.39], "mexico df": [19.43, -99.13]
  };
  // Si una ciudad no está en la lista, se usa la capital de su país
  const PAISES = {
    ES: [40.42, -3.70], FR: [48.86, 2.35], IT: [41.90, 12.50], DE: [52.52, 13.40], GB: [51.51, -0.13],
    PT: [38.72, -9.14], US: [38.90, -77.04], MX: [19.43, -99.13], NL: [52.37, 4.90], BE: [50.85, 4.35]
  };
  const RADIO = 1;
  const AMBAR = 0xffb627;

  const cont = document.getElementById("globo");
  if (!cont || typeof THREE === "undefined") {
    window.Globo = { setRutas() {} };
    if (cont) cont.classList.add("sin-3d");
    return;
  }

  // ---------- Escena, cámara y renderizador ----------
  const escena = new THREE.Scene();
  const camara = new THREE.PerspectiveCamera(32, 1, 0.1, 100);
  camara.position.set(0, 0, 4.3);
  const render = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  render.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  cont.appendChild(render.domElement);

  const globo = new THREE.Group();
  escena.add(globo);
  // Orientación inicial: Europa y el Atlántico mirando a la cámara
  globo.rotation.y = -1.05;
  globo.rotation.x = 0.5;

  // Dirección del sol (fija respecto a la cámara): ilumina desde arriba a la izquierda
  const SOL = new THREE.Vector3(-0.55, 0.45, 0.7).normalize();

  /** Convierte latitud/longitud en un punto 3D sobre la esfera (coincide con la textura). */
  function aPunto(lat, lon, r = RADIO) {
    const phi = (90 - lat) * Math.PI / 180;
    const theta = (lon + 180) * Math.PI / 180;
    return new THREE.Vector3(
      -r * Math.sin(phi) * Math.cos(theta),
      r * Math.cos(phi),
      r * Math.sin(phi) * Math.sin(theta)
    );
  }

  // ---------- La Tierra ----------
  const cargador = new THREE.TextureLoader();
  const textura = (url) => {
    const t = cargador.load(url, () => cont.classList.add("cargado"));
    t.anisotropy = render.capabilities.getMaxAnisotropy();
    return t;
  };

  const tierra = new THREE.Mesh(
    new THREE.SphereGeometry(RADIO, 96, 96),
    new THREE.ShaderMaterial({
      uniforms: {
        dia: { value: textura("img/earth-blue-marble.jpg") },
        noche: { value: textura("img/earth-night.jpg") },
        agua: { value: textura("img/earth-water.png") },
        sol: { value: SOL }
      },
      vertexShader: `
        varying vec2 vUv;
        varying vec3 vNormal;
        varying vec3 vPos;
        void main() {
          vUv = uv;
          vNormal = normalize(mat3(modelMatrix) * normal);
          vPos = (modelMatrix * vec4(position, 1.0)).xyz;
          gl_Position = projectionMatrix * viewMatrix * vec4(vPos, 1.0);
        }`,
      fragmentShader: `
        uniform sampler2D dia;
        uniform sampler2D noche;
        uniform sampler2D agua;
        uniform vec3 sol;
        varying vec2 vUv;
        varying vec3 vNormal;
        varying vec3 vPos;
        void main() {
          vec3 n = normalize(vNormal);
          vec3 vista = normalize(cameraPosition - vPos);
          float luz = dot(n, sol);
          float mezcla = smoothstep(-0.18, 0.22, luz);

          vec3 colDia = texture2D(dia, vUv).rgb * (0.35 + 0.9 * max(luz, 0.0));
          vec3 colNoche = texture2D(noche, vUv).rgb * vec3(1.35, 1.1, 0.75) * 1.6;
          vec3 color = mix(colNoche, colDia, mezcla);

          // Reflejo del sol solo en el agua
          float esAgua = texture2D(agua, vUv).r;
          vec3 reflejo = reflect(-sol, n);
          float brillo = pow(max(dot(reflejo, vista), 0.0), 28.0) * esAgua * mezcla;
          color += vec3(1.0, 0.92, 0.75) * brillo * 0.65;

          // Borde azulado (atmósfera vista de cerca)
          float fresnel = pow(1.0 - max(dot(n, vista), 0.0), 3.0);
          color += vec3(0.30, 0.55, 1.0) * fresnel * (0.25 + 0.75 * mezcla);

          gl_FragColor = vec4(color, 1.0);
        }`
    })
  );
  globo.add(tierra);

  // Atmósfera: halo exterior
  escena.add(new THREE.Mesh(
    new THREE.SphereGeometry(RADIO * 1.16, 64, 64),
    new THREE.ShaderMaterial({
      transparent: true, side: THREE.BackSide, depthWrite: false, blending: THREE.AdditiveBlending,
      uniforms: { sol: { value: SOL } },
      vertexShader: `
        varying vec3 vNormal;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }`,
      fragmentShader: `
        uniform vec3 sol;
        varying vec3 vNormal;
        void main() {
          float i = pow(0.68 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 4.0);
          float lado = 0.45 + 0.55 * max(dot(-vNormal, sol) * 0.5 + 0.5, 0.0);
          gl_FragColor = vec4(0.35, 0.62, 1.0, 1.0) * i * lado * 1.6;
        }`
    })
  ));

  // ---------- Rutas y aviones ----------
  const capaRutas = new THREE.Group();
  globo.add(capaRutas);
  let aviones = [];
  let arcos = [];

  /** Silueta de avión vista desde arriba, apuntando hacia +Y. */
  const geoAvion = (() => {
    const s = new THREE.Shape();
    const p = [[0, 1], [0.12, 0.35], [0.95, -0.1], [0.95, -0.28], [0.12, -0.05], [0.1, -0.62],
      [0.35, -0.85], [0.35, -0.98], [0, -0.88], [-0.35, -0.98], [-0.35, -0.85], [-0.1, -0.62],
      [-0.12, -0.05], [-0.95, -0.28], [-0.95, -0.1], [-0.12, 0.35]];
    s.moveTo(p[0][0], p[0][1]);
    p.slice(1).forEach(([x, y]) => s.lineTo(x, y));
    const g = new THREE.ShapeGeometry(s);
    g.scale(0.03, 0.03, 0.03);
    return g;
  })();
  const matAvion = new THREE.MeshBasicMaterial({ color: 0xffffff, side: THREE.DoubleSide });

  function coordenadas(ciudad, pais) {
    return CIUDADES[(ciudad || "").toLowerCase()] || PAISES[pais] || null;
  }

  function limpiar() {
    while (capaRutas.children.length) {
      const o = capaRutas.children.pop();
      if (o.geometry && o.geometry !== geoAvion) o.geometry.dispose();
    }
    aviones = [];
    arcos = [];
  }

  /** Dibuja las rutas. Las que tienen más reservas brillan más y llevan más aviones. */
  function setRutas(rutas) {
    limpiar();
    const ciudadesUsadas = new Map();
    const maxN = Math.max(1, ...rutas.map((r) => r.n));

    rutas.slice(0, 60).forEach((ruta, i) => {
      const a = coordenadas(ruta.origen, ruta.paisOrigen);
      const b = coordenadas(ruta.destino, ruta.paisDestino);
      if (!a || !b || (a[0] === b[0] && a[1] === b[1])) return;
      const pa = aPunto(a[0], a[1], RADIO * 1.003);
      const pb = aPunto(b[0], b[1], RADIO * 1.003);
      ciudadesUsadas.set(ruta.origen, pa);
      ciudadesUsadas.set(ruta.destino, pb);

      // Arco que sube más cuanto más lejos están las ciudades
      const distancia = pa.distanceTo(pb);
      const medio = pa.clone().add(pb).multiplyScalar(0.5).normalize().multiplyScalar(RADIO + distancia * 0.42);
      const curva = new THREE.QuadraticBezierCurve3(pa, medio, pb);
      const intensidad = 0.3 + 0.7 * (ruta.n / maxN);

      // Línea base tenue + "estela" que recorre el arco
      const puntos = curva.getPoints(80);
      const base = new THREE.Line(new THREE.BufferGeometry().setFromPoints(puntos),
        new THREE.LineBasicMaterial({ color: AMBAR, transparent: true, opacity: 0.22 * intensidad + 0.08 }));
      capaRutas.add(base);
      const estela = new THREE.Line(new THREE.BufferGeometry().setFromPoints(puntos),
        new THREE.LineBasicMaterial({ color: 0xffd27a, transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending }));
      estela.geometry.setDrawRange(0, 0);
      capaRutas.add(estela);

      const cuantos = ruta.n >= maxN * 0.6 ? 2 : 1;
      for (let k = 0; k < cuantos; k++) {
        const avion = new THREE.Mesh(geoAvion, matAvion);
        capaRutas.add(avion);
        const vel = 0.0014 + Math.random() * 0.0016;
        const t = (i * 0.137 + k * 0.5) % 1;
        aviones.push({ mesh: avion, curva, t, vel });
        if (k === 0) arcos.push({ estela, avion: aviones[aviones.length - 1] });
      }
    });

    // Ciudades: punto + anillo que late
    const geoC = new THREE.SphereGeometry(0.011, 12, 12);
    const matC = new THREE.MeshBasicMaterial({ color: AMBAR });
    ciudadesUsadas.forEach((p) => {
      const punto = new THREE.Mesh(geoC, matC);
      punto.position.copy(p);
      capaRutas.add(punto);
      const anillo = new THREE.Mesh(
        new THREE.RingGeometry(0.016, 0.022, 32),
        new THREE.MeshBasicMaterial({ color: AMBAR, transparent: true, opacity: 0.6, side: THREE.DoubleSide })
      );
      anillo.position.copy(p).multiplyScalar(1.001);
      anillo.lookAt(p.clone().multiplyScalar(2));
      anillo.userData.pulso = Math.random() * Math.PI * 2;
      capaRutas.add(anillo);
    });
  }

  // Rutas de ejemplo mientras no hay datos cargados
  setRutas([
    { origen: "Madrid", destino: "Nueva York", n: 3 }, { origen: "Madrid", destino: "Paris", n: 5 },
    { origen: "Barcelona", destino: "Roma", n: 3 }, { origen: "Lisboa", destino: "Mexico DF", n: 2 },
    { origen: "Londres", destino: "Berlin", n: 2 }, { origen: "Valencia", destino: "Atlanta", n: 2 },
    { origen: "Milan", destino: "Londres", n: 1 }, { origen: "Munich", destino: "Madrid", n: 1 }
  ]);

  // ---------- Arrastrar para girar (con inercia) ----------
  let arrastrando = false, ultX = 0, ultY = 0, inercia = 0;
  cont.addEventListener("pointerdown", (e) => { arrastrando = true; ultX = e.clientX; ultY = e.clientY; cont.setPointerCapture(e.pointerId); });
  cont.addEventListener("pointerup", () => { arrastrando = false; });
  cont.addEventListener("pointercancel", () => { arrastrando = false; });
  cont.addEventListener("pointermove", (e) => {
    if (!arrastrando) return;
    const dx = (e.clientX - ultX) * 0.005;
    globo.rotation.y += dx;
    inercia = dx;
    globo.rotation.x = Math.max(-0.7, Math.min(1.2, globo.rotation.x + (e.clientY - ultY) * 0.004));
    ultX = e.clientX; ultY = e.clientY;
  });

  // ---------- Tamaño ----------
  function ajustar() {
    const w = cont.clientWidth, h = cont.clientHeight;
    if (!w || !h) return;
    render.setSize(w, h, false);
    camara.aspect = w / h;
    camara.updateProjectionMatrix();
  }
  new ResizeObserver(ajustar).observe(cont);
  ajustar();

  // ---------- Bucle de animación ----------
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const m = new THREE.Matrix4(), x = new THREE.Vector3(), dir = new THREE.Vector3(), normal = new THREE.Vector3();
  let reloj = 0;

  function animar() {
    reloj += 0.016;
    if (!arrastrando && !reduce) {
      inercia *= 0.95;
      globo.rotation.y += 0.0007 + inercia;
    }

    for (const av of aviones) {
      if (!reduce) av.t = (av.t + av.vel) % 1;
      const p = av.curva.getPoint(av.t);
      const sig = av.curva.getPoint(Math.min(av.t + 0.01, 1));
      av.mesh.position.copy(p);
      normal.copy(p).normalize();
      dir.copy(sig).sub(p).normalize();
      x.crossVectors(dir, normal).normalize();
      m.makeBasis(x, dir, normal);
      av.mesh.quaternion.setFromRotationMatrix(m);
      av.mesh.scale.setScalar(0.35 + 0.65 * Math.sin(av.t * Math.PI));
    }
    // La estela dibuja el tramo de arco que ya ha recorrido el avión
    for (const a of arcos) {
      const fin = Math.floor(a.avion.t * 80);
      const inicio = Math.max(0, fin - 22);
      a.estela.geometry.setDrawRange(inicio, fin - inicio + 1);
    }
    capaRutas.children.forEach((o) => {
      if (o.userData.pulso !== undefined) {
        const k = 1 + 0.8 * ((Math.sin(reloj * 2.2 + o.userData.pulso) + 1) / 2);
        o.scale.setScalar(k);
        o.material.opacity = 0.75 - 0.6 * (k - 1) / 0.8;
      }
    });

    render.render(escena, camara);
    requestAnimationFrame(animar);
  }
  animar();

  window.Globo = { setRutas };
})();
