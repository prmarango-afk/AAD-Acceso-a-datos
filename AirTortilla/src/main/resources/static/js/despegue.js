/* =========================================================
   Avión de pasajeros 3D (Three.js) que cruza la pantalla al
   crear los ficheros. Se construye con geometrías de Three.js:
   fuselaje, alas en flecha, motores, ventanillas, timón con los
   colores de AirTortilla, luces de navegación y estelas.
   Expone window.Despegue.lanzar().
   ========================================================= */
(function () {
  if (typeof THREE === "undefined") {
    window.Despegue = { lanzar() {} };
    return;
  }

  let lienzo, render, escena, camara, avion, estelas, motores, luces;
  let animando = false;

  /** Entorno de reflejos: un cielo degradado para que el metal brille. */
  function crearEntorno() {
    return null; // PMREM falla en algunas gráficas: los brillos salen de las luces
  }

  /** Extruye una forma 2D plana (para alas, estabilizadores y timón). */
  function placa(puntos, grosor, material) {
    const s = new THREE.Shape();
    s.moveTo(puntos[0][0], puntos[0][1]);
    puntos.slice(1).forEach(([a, b]) => s.lineTo(a, b));
    const g = new THREE.ExtrudeGeometry(s, {
      depth: grosor, bevelEnabled: true, bevelThickness: grosor * 0.4, bevelSize: grosor * 0.5, bevelSegments: 3
    });
    g.translate(0, 0, -grosor / 2);
    return new THREE.Mesh(g, material);
  }

  /** Construye el avión con el morro apuntando a +Z. */
  function crearAvion(env) {
    const g = new THREE.Group();
    const blanco = new THREE.MeshStandardMaterial({ color: 0xf4f6fa, metalness: 0.25, roughness: 0.32, side: THREE.DoubleSide, envMap: env, envMapIntensity: 1.1 });
    const gris = new THREE.MeshStandardMaterial({ color: 0xb9c1cf, metalness: 0.6, roughness: 0.3, envMap: env });
    const ambar = new THREE.MeshStandardMaterial({ color: 0xffb627, metalness: 0.3, roughness: 0.35, side: THREE.DoubleSide, envMap: env });
    const cristal = new THREE.MeshStandardMaterial({ color: 0x0b1424, metalness: 0.9, roughness: 0.08, envMap: env });
    const oscuro = new THREE.MeshStandardMaterial({ color: 0x1a1d24, metalness: 0.5, roughness: 0.5, envMap: env });

    // Fuselaje: perfil girado alrededor de su eje
    const perfil = [[0, -5.2], [0.12, -5.15], [0.32, -4.7], [0.5, -3.9], [0.6, -3.0], [0.62, -1.5], [0.62, 2.8],
      [0.6, 3.6], [0.54, 4.2], [0.42, 4.65], [0.26, 4.95], [0.1, 5.12], [0, 5.16]]
      .map(([r, y]) => new THREE.Vector2(r, y));
    const geoFus = new THREE.LatheGeometry(perfil, 48);
    geoFus.rotateX(Math.PI / 2);
    const fuselaje = new THREE.Mesh(geoFus, blanco);
    fuselaje.scale.set(1, 1.04, 1);
    g.add(fuselaje);

    // Panza gris y franja ámbar a lo largo del fuselaje
    const panza = new THREE.Mesh(new THREE.CylinderGeometry(0.625, 0.625, 7.2, 40, 1, true, Math.PI * 0.62, Math.PI * 0.76), gris);
    panza.rotation.x = Math.PI / 2;
    panza.position.z = -0.1;
    g.add(panza);
    const franja = new THREE.Mesh(new THREE.CylinderGeometry(0.628, 0.628, 7.6, 40, 1, true, Math.PI * 0.42, Math.PI * 0.08), ambar);
    franja.rotation.x = Math.PI / 2;
    const franja2 = franja.clone();
    franja2.rotation.y = Math.PI;
    franja2.rotation.x = -Math.PI / 2;
    g.add(franja, franja2);

    // Ventanillas de pasajeros (instanciadas a ambos lados)
    const geoVent = new THREE.BoxGeometry(0.03, 0.12, 0.085);
    const filas = 26;
    const ventanas = new THREE.InstancedMesh(geoVent, cristal, filas * 2);
    const m = new THREE.Matrix4();
    for (let i = 0; i < filas; i++) {
      const z = -3.1 + i * 0.25;
      m.makeTranslation(0.612, 0.2, z); ventanas.setMatrixAt(i * 2, m);
      m.makeTranslation(-0.612, 0.2, z); ventanas.setMatrixAt(i * 2 + 1, m);
    }
    g.add(ventanas);

    // Cristales de la cabina
    [-1, 1].forEach((lado) => {
      const c = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.12, 0.3), cristal);
      c.position.set(0.2 * lado, 0.28, 4.42);
      c.rotation.set(-0.5, 0.45 * lado, 0.2 * lado);
      g.add(c);
    });
    const cFrente = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.12, 0.2), cristal);
    cFrente.position.set(0, 0.32, 4.5);
    cFrente.rotation.x = -0.6;
    g.add(cFrente);

    // Alas en flecha con un poco de diedro
    const puntosAla = [[0.4, 1.3], [5.4, -1.55], [5.55, -2.05], [5.1, -2.15], [0.4, -1.25]];
    [-1, 1].forEach((lado) => {
      const ala = placa(puntosAla.map(([x, z]) => [x, -z]), 0.08, blanco);
      ala.rotation.x = -Math.PI / 2;
      ala.scale.x = lado;
      ala.position.set(0, -0.32, 0);
      ala.rotation.z = 0.08 * lado;
      g.add(ala);
      // Winglet
      const winglet = placa([[0, 0], [0.45, 0.85], [0.75, 0.85], [0.55, 0]], 0.05, ambar);
      winglet.rotation.y = -Math.PI / 2;
      winglet.position.set(5.45 * lado, -0.1, -1.65);
      g.add(winglet);
      // Motor colgado bajo el ala
      const motor = new THREE.Group();
      const gondola = new THREE.Mesh(new THREE.CylinderGeometry(0.34, 0.28, 1.5, 32, 1, true), blanco);
      gondola.rotation.x = Math.PI / 2;
      gondola.material = blanco;
      const interior = new THREE.Mesh(new THREE.CylinderGeometry(0.33, 0.27, 1.48, 32, 1, true), oscuro);
      interior.rotation.x = Math.PI / 2;
      interior.material = oscuro.clone();
      interior.material.side = THREE.BackSide;
      const borde = new THREE.Mesh(new THREE.TorusGeometry(0.34, 0.035, 12, 32), gris);
      borde.position.z = 0.75;
      const ventilador = new THREE.Mesh(new THREE.CircleGeometry(0.31, 32), oscuro);
      ventilador.position.z = 0.6;
      const cono = new THREE.Mesh(new THREE.ConeGeometry(0.1, 0.25, 16), gris);
      cono.rotation.x = Math.PI / 2;
      cono.position.z = 0.68;
      const tobera = new THREE.Mesh(new THREE.ConeGeometry(0.2, 0.5, 24), gris);
      tobera.rotation.x = -Math.PI / 2;
      tobera.position.z = -0.95;
      motor.add(gondola, interior, borde, ventilador, cono, tobera);
      motor.position.set(1.95 * lado, -0.72, 0.85);
      g.add(motor);
      const pilon = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.32, 1.1), blanco);
      pilon.position.set(1.95 * lado, -0.42, 0.55);
      g.add(pilon);
    });

    // Estabilizadores horizontales
    const puntosEst = [[0.2, -3.85], [2.1, -4.85], [2.15, -5.15], [1.75, -5.2], [0.2, -4.75]];
    [-1, 1].forEach((lado) => {
      const est = placa(puntosEst.map(([x, z]) => [x, -z]), 0.05, blanco);
      est.rotation.x = -Math.PI / 2;
      est.scale.x = lado;
      est.position.y = 0.12;
      est.rotation.z = 0.12 * lado;
      g.add(est);
    });

    // Timón vertical con los colores de AirTortilla
    const timon = placa([[-3.55, 0.45], [-4.85, 2.55], [-5.45, 2.55], [-5.15, 0.35]], 0.07, ambar);
    timon.rotation.y = -Math.PI / 2;
    g.add(timon);
    const logo = new THREE.Mesh(new THREE.CircleGeometry(0.32, 32), blanco);
    logo.position.set(0.075, 1.5, -4.55);
    logo.rotation.y = Math.PI / 2;
    const logo2 = logo.clone();
    logo2.position.x = -0.075;
    logo2.rotation.y = -Math.PI / 2;
    g.add(logo, logo2);

    // Luces de navegación: roja (izquierda), verde (derecha), blanca (cola)
    const luz = (color) => new THREE.Mesh(new THREE.SphereGeometry(0.07, 12, 12), new THREE.MeshBasicMaterial({ color }));
    const roja = luz(0xff2d2d); roja.position.set(-5.5, -0.2, -1.75);
    const verde = luz(0x2dff7a); verde.position.set(5.5, -0.2, -1.75);
    const blanca = luz(0xffffff); blanca.position.set(0, 0.2, -5.2);
    g.add(roja, verde, blanca);
    luces = { roja, verde, blanca };

    // Puntos de salida de los gases (para las estelas)
    motores = [new THREE.Object3D(), new THREE.Object3D()];
    motores[0].position.set(-1.95, -0.72, -1.3);
    motores[1].position.set(1.95, -0.72, -1.3);
    g.add(motores[0], motores[1]);

    g.scale.setScalar(0.26);
    return g;
  }

  /** Estelas: partículas que salen de los motores y se desvanecen. */
  function crearEstelas() {
    const MAX = 900;
    const geo = new THREE.BufferGeometry();
    geo.setAttribute("position", new THREE.BufferAttribute(new Float32Array(MAX * 3), 3));
    geo.setAttribute("vida", new THREE.BufferAttribute(new Float32Array(MAX), 1));
    // Cada partícula lleva su "vida" (1 = recién salida, 0 = desaparecida): crece y se desvanece
    const puntos = new THREE.Points(geo, new THREE.ShaderMaterial({
      transparent: true, depthWrite: false,
      uniforms: { mapa: { value: texturaHumo() }, escala: { value: window.innerHeight / 2 } },
      vertexShader: `
        attribute float vida;
        uniform float escala;
        varying float vVida;
        void main() {
          vVida = vida;
          vec4 mv = modelViewMatrix * vec4(position, 1.0);
          gl_PointSize = vida <= 0.0 ? 0.0 : (0.12 + (1.0 - vida) * 0.5) * escala / -mv.z;
          gl_Position = projectionMatrix * mv;
        }`,
      fragmentShader: `
        uniform sampler2D mapa;
        varying float vVida;
        void main() {
          float a = texture2D(mapa, gl_PointCoord).a * vVida * 0.7;
          if (a < 0.01) discard;
          gl_FragColor = vec4(0.93, 0.95, 1.0, a);
        }`
    }));
    puntos.frustumCulled = false;
    return { puntos, edades: new Float32Array(MAX).fill(99), MAX, siguiente: 0 };
  }

  function texturaHumo() {
    const c = document.createElement("canvas");
    c.width = c.height = 64;
    const ctx = c.getContext("2d");
    const grad = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
    grad.addColorStop(0, "rgba(255,255,255,1)");
    grad.addColorStop(0.4, "rgba(255,255,255,0.35)");
    grad.addColorStop(1, "rgba(255,255,255,0)");
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 64, 64);
    return new THREE.CanvasTexture(c);
  }

  function iniciar() {
    lienzo = document.createElement("canvas");
    lienzo.className = "despegue";
    document.body.appendChild(lienzo);
    render = new THREE.WebGLRenderer({ canvas: lienzo, antialias: true, alpha: true });
    render.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    render.outputEncoding = THREE.sRGBEncoding;
    render.toneMapping = THREE.ACESFilmicToneMapping;

    escena = new THREE.Scene();
    camara = new THREE.PerspectiveCamera(40, 1, 0.1, 200);
    camara.position.set(0, 0, 12);

    const env = crearEntorno();
    const relleno = new THREE.DirectionalLight(0xaecbff, 0.9);
    relleno.position.set(5, -3, 4);
    escena.add(relleno);
    escena.add(new THREE.HemisphereLight(0xbfd4ff, 0x2a2018, 1.1));
    const sol = new THREE.DirectionalLight(0xfff1dc, 2.2);
    sol.position.set(-6, 8, 6);
    escena.add(sol);

    avion = crearAvion(env);
    escena.add(avion);
    estelas = crearEstelas();
    escena.add(estelas.puntos);
    ajustar();
    window.addEventListener("resize", ajustar);
  }

  function ajustar() {
    if (!render) return;
    render.setSize(window.innerWidth, window.innerHeight, false);
    camara.aspect = window.innerWidth / window.innerHeight;
    if (estelas) estelas.puntos.material.uniforms.escala.value = window.innerHeight * render.getPixelRatio() / 2;
    camara.updateProjectionMatrix();
  }

  // Trayectoria: sale por abajo a la izquierda, pasa cerca de la cámara y se aleja arriba a la derecha
  const ruta = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-17, -8, -10), new THREE.Vector3(-6, -2.6, 2),
    new THREE.Vector3(3, 1.2, 3.5), new THREE.Vector3(18, 8, -14)
  ]);
  const DURACION = 4200;
  const tmp = new THREE.Vector3(), mundo = new THREE.Vector3();

  function emitir(motor) {
    motor.getWorldPosition(mundo);
    const e = estelas;
    const i = e.siguiente;
    e.siguiente = (e.siguiente + 1) % e.MAX;
    const pos = e.puntos.geometry.attributes.position;
    pos.setXYZ(i, mundo.x + (Math.random() - 0.5) * 0.04, mundo.y + (Math.random() - 0.5) * 0.04, mundo.z);
    e.edades[i] = 0;
  }

  function lanzar() {
    if (animando) return;
    if (!lienzo) iniciar();
    animando = true;
    lienzo.classList.add("activo");
    estelas.edades.fill(99);
    const inicio = performance.now();
    let anterior = inicio;

    function cuadro(ahora) {
      const dt = Math.min((ahora - anterior) / 1000, 0.05);
      anterior = ahora;
      const t = Math.min((ahora - inicio) / DURACION, 1);
      const k = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; // suavizado

      // Posición y orientación sobre la trayectoria
      ruta.getPointAt(Math.min(k, 0.999), avion.position);
      ruta.getPointAt(Math.min(k + 0.01, 1), tmp);
      avion.lookAt(tmp);
      avion.rotateZ(-0.55 * Math.sin(k * Math.PI)); // alabeo en el giro

      // Luces parpadeantes
      const fase = (ahora / 1000) % 1;
      luces.blanca.visible = fase < 0.08;

      // Estelas
      if (t < 0.97) { emitir(motores[0]); emitir(motores[1]); emitir(motores[0]); emitir(motores[1]); }
      const vidas = estelas.puntos.geometry.attributes.vida;
      for (let i = 0; i < estelas.MAX; i++) {
        estelas.edades[i] += dt;
        vidas.setX(i, Math.max(0, 1 - estelas.edades[i] / 1.6));
      }
      vidas.needsUpdate = true;
      estelas.puntos.geometry.attributes.position.needsUpdate = true;

      render.render(escena, camara);
      if (t < 1 || estelas.edades.some((e) => e < 1.6)) {
        requestAnimationFrame(cuadro);
      } else {
        animando = false;
        lienzo.classList.remove("activo");
        render.clear();
      }
    }
    requestAnimationFrame(cuadro);
  }

  window.Despegue = { lanzar };
})();
