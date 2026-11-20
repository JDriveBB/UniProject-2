// Prueba de carga RNF2: módulo de evaluación (500 usuarios simultáneos, respuesta < 3 s).
// Imagen de Docker fijada: grafana/k6:2.3.0 (sin dependencias remotas).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = (__ENV.BASE || 'http://host.docker.internal:18080').replace(/\/+$/, '');
const SALIDA = (__ENV.SALIDA || '/salida').replace(/\/+$/, '');
const DOCENTE_EMAIL = __ENV.DOCENTE_EMAIL || 'docente@demo.uniproject.test';
const VUS_MAX = Math.max(1, Number.parseInt(__ENV.VUS_MAX || '500', 10) || 500);
const DURACION_SUBIDA = __ENV.DURACION_SUBIDA || '1m';
const DURACION_SOSTENIDA = __ENV.DURACION_SOSTENIDA || '3m';
const DURACION_BAJADA = __ENV.DURACION_BAJADA || '30s';
const MAX_ESTUDIANTES_DEMO = 5;

const ENDPOINTS = [
  'panel_docente',
  'entregas_hito',
  'proyecto_detalle',
  'evaluacion_hito',
];

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    evaluacion_rnf2: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: DURACION_SUBIDA, target: VUS_MAX },
        { duration: DURACION_SOSTENIDA, target: VUS_MAX },
        { duration: DURACION_BAJADA, target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    'http_req_duration{fase:evaluacion}': ['p(95)<3000', 'max<3000'],
    'http_req_duration{endpoint:panel_docente}': ['p(95)<3000', 'max<3000'],
    'http_req_duration{endpoint:proyecto_detalle}': ['p(95)<3000', 'max<3000'],
    'http_req_duration{endpoint:entregas_hito}': ['p(95)<3000', 'max<3000'],
    'http_req_duration{endpoint:evaluacion_hito}': ['p(95)<3000', 'max<3000'],
  },
};

function extraerCookies(jar, url) {
  const crudas = jar.cookiesForURL(url);
  const planas = {};
  for (const [nombre, valores] of Object.entries(crudas)) {
    if (Array.isArray(valores) && valores.length > 0 && valores[0]) {
      planas[nombre] = valores[0];
    }
  }
  return planas;
}

function iniciarSesion(email, contrasena) {
  const jar = new http.CookieJar();
  const resCsrf = http.get(`${BASE}/api/sesion/csrf`, {
    jar,
    tags: { fase: 'setup', name: 'GET /api/sesion/csrf' },
  });
  if (resCsrf.status !== 204) {
    throw new Error(`No se pudo obtener el token CSRF en setup() (HTTP ${resCsrf.status}).`);
  }

  const xsrf = extraerCookies(jar, `${BASE}/`)['XSRF-TOKEN'];
  if (!xsrf) {
    throw new Error('La respuesta de /api/sesion/csrf no dejó la cookie XSRF-TOKEN.');
  }

  const resLogin = http.post(
    `${BASE}/api/sesion/login`,
    JSON.stringify({ email, contrasena }),
    {
      jar,
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': xsrf,
      },
      tags: { fase: 'setup', name: 'POST /api/sesion/login' },
    }
  );
  if (resLogin.status !== 200) {
    throw new Error(`No se pudo iniciar sesión en setup() (HTTP ${resLogin.status}).`);
  }

  const cookies = extraerCookies(jar, `${BASE}/api/`);
  if (!cookies.JSESSIONID) {
    throw new Error('El inicio de sesión no dejó cookie JSESSIONID.');
  }
  return cookies;
}

function getJsonSetup(url, cookies, nombreOperacion) {
  const res = http.get(url, {
    cookies,
    tags: { fase: 'setup', name: nombreOperacion },
  });
  if (res.status !== 200) {
    throw new Error(`Error en setup() al consultar ${nombreOperacion} (HTTP ${res.status}).`);
  }
  return res.json();
}

function buscarEscenarioEvaluado(cookiesDocente) {
  const proyectos = getJsonSetup(`${BASE}/api/proyectos`, cookiesDocente, 'GET /api/proyectos');
  if (!Array.isArray(proyectos) || proyectos.length === 0) {
    throw new Error('No hay proyectos disponibles para la cuenta docente demo.');
  }

  for (const proyecto of proyectos) {
    const detalle = getJsonSetup(
      `${BASE}/api/proyectos/${proyecto.idProyecto}`,
      cookiesDocente,
      'GET /api/proyectos/{idProyecto}'
    );
    const equipos = Array.isArray(detalle.equipos) ? detalle.equipos : [];
    for (const equipo of equipos) {
      const hitos = Array.isArray(equipo.hitos) ? equipo.hitos : [];
      const evaluado = hitos.find((h) => h.estado === 'EVALUADO');
      if (evaluado) {
        return {
          idProyecto: detalle.proyecto.idProyecto,
          idEquipo: equipo.idEquipo,
          idHitoEquipo: evaluado.idHitoEquipo,
        };
      }
    }
  }

  throw new Error('No se encontró ningún hito en estado EVALUADO en los proyectos demo.');
}

export function setup() {
  const contrasena = __ENV.DEMO_CONTRASENA || '';
  if (!contrasena) {
    throw new Error('Falta la variable de entorno DEMO_CONTRASENA.');
  }

  const cookiesDocente = iniciarSesion(DOCENTE_EMAIL, contrasena);
  const escenario = buscarEscenarioEvaluado(cookiesDocente);

  // Verificación previa para fallar rápido si la sesión de pruebas no tiene los datos esperados.
  getJsonSetup(
    `${BASE}/api/proyectos/${escenario.idProyecto}/panel`,
    cookiesDocente,
    'GET /api/proyectos/{idProyecto}/panel'
  );
  getJsonSetup(
    `${BASE}/api/hitos-equipo/${escenario.idHitoEquipo}/tareas`,
    cookiesDocente,
    'GET /api/hitos-equipo/{idHitoEquipo}/tareas'
  );
  getJsonSetup(
    `${BASE}/api/hitos-equipo/${escenario.idHitoEquipo}/evaluacion`,
    cookiesDocente,
    'GET /api/hitos-equipo/{idHitoEquipo}/evaluacion'
  );

  const inscritos = getJsonSetup(
    `${BASE}/api/proyectos/${escenario.idProyecto}/estudiantes`,
    cookiesDocente,
    'GET /api/proyectos/{idProyecto}/estudiantes'
  );
  const candidatos = Array.isArray(inscritos)
    ? inscritos
        .filter((e) => e.registrado === true && e.idEquipo === escenario.idEquipo && e.email)
        .slice(0, MAX_ESTUDIANTES_DEMO)
    : [];

  const sesionesEstudiantes = [];
  for (const est of candidatos) {
    sesionesEstudiantes.push(iniciarSesion(est.email, contrasena));
  }

  console.log(
    `Escenario listo: proyecto=${escenario.idProyecto}, equipo=${escenario.idEquipo}, hitoEquipo=${escenario.idHitoEquipo}, sesionesEstudiante=${sesionesEstudiantes.length}`
  );

  return {
    idProyecto: escenario.idProyecto,
    idEquipo: escenario.idEquipo,
    idHitoEquipo: escenario.idHitoEquipo,
    docente: cookiesDocente,
    estudiantes: sesionesEstudiantes,
  };
}

function pausaEntrePasos() {
  sleep(1 + Math.random() * 2);
}

function consultarEndpoint(url, cookies, etiqueta, nombreOperacion) {
  const res = http.get(url, {
    cookies,
    tags: {
      fase: 'evaluacion',
      endpoint: etiqueta,
      name: nombreOperacion,
    },
  });
  check(res, {
    [`${etiqueta}: estado 200`]: (r) => r.status === 200,
  });
  return res;
}

export default function (data) {
  const usarDocente = data.estudiantes.length === 0 || (__VU - 1) % 4 === 0;
  const cookies = usarDocente
    ? data.docente
    : data.estudiantes[(__VU - 1) % data.estudiantes.length];

  if (usarDocente) {
    consultarEndpoint(
      `${BASE}/api/proyectos/${data.idProyecto}/panel`,
      cookies,
      'panel_docente',
      'GET /api/proyectos/{idProyecto}/panel'
    );
    pausaEntrePasos();
  }

  consultarEndpoint(
    `${BASE}/api/hitos-equipo/${data.idHitoEquipo}/tareas`,
    cookies,
    'entregas_hito',
    'GET /api/hitos-equipo/{idHitoEquipo}/tareas'
  );
  pausaEntrePasos();

  consultarEndpoint(
    `${BASE}/api/proyectos/${data.idProyecto}`,
    cookies,
    'proyecto_detalle',
    'GET /api/proyectos/{idProyecto}'
  );
  pausaEntrePasos();

  consultarEndpoint(
    `${BASE}/api/hitos-equipo/${data.idHitoEquipo}/evaluacion`,
    cookies,
    'evaluacion_hito',
    'GET /api/hitos-equipo/{idHitoEquipo}/evaluacion'
  );
  pausaEntrePasos();
}

function fmtMs(valor) {
  return typeof valor === 'number' ? `${valor.toFixed(1)} ms` : 'N/D';
}

function fmtPct(valor) {
  return typeof valor === 'number' ? `${(valor * 100).toFixed(2)} %` : 'N/D';
}

function fmtNum(valor, decimales = 2) {
  return typeof valor === 'number' ? valor.toFixed(decimales) : 'N/D';
}

function recolectarChecks(grupo, destino) {
  if (!grupo) {
    return;
  }
  const listaChecks = Array.isArray(grupo.checks)
    ? grupo.checks
    : Object.values(grupo.checks || {});
  for (const ch of listaChecks) {
    if (ch && typeof ch.name === 'string') {
      destino.push(ch);
    }
  }
  const subgrupos = Array.isArray(grupo.groups)
    ? grupo.groups
    : Object.values(grupo.groups || {});
  for (const sub of subgrupos) {
    recolectarChecks(sub, destino);
  }
}

function estadoUmbral(metrica, expresion) {
  if (!metrica || !metrica.thresholds || !metrica.thresholds[expresion]) {
    return 'SIN DATOS';
  }
  return metrica.thresholds[expresion].ok ? 'CUMPLE' : 'FALLA';
}

export function handleSummary(data) {
  const metricas = data.metrics || {};
  const umbrales = [];
  for (const [nombreMetrica, info] of Object.entries(metricas)) {
    if (info && info.thresholds) {
      for (const [expr, resultado] of Object.entries(info.thresholds)) {
        umbrales.push({
          metrica: nombreMetrica,
          expresion: expr,
          ok: Boolean(resultado && resultado.ok),
        });
      }
    }
  }
  const cumpleTodo = umbrales.length > 0 && umbrales.every((u) => u.ok);

  const duracionTotalMs = data.state && typeof data.state.testRunDurationMs === 'number'
    ? data.state.testRunDurationMs
    : null;
  const httpReqs = metricas.http_reqs ? metricas.http_reqs.values : null;
  const httpFailed = metricas.http_req_failed ? metricas.http_req_failed.values : null;
  const checksMetrica = metricas.checks ? metricas.checks.values : null;
  const vusMax = metricas.vus_max ? metricas.vus_max.values : null;
  const iteraciones = metricas.iterations ? metricas.iterations.values : null;

  const lineas = [
    '========================================================================',
    'UniProject - Prueba de carga RNF2 (modulo de evaluacion)',
    '========================================================================',
    `Fecha UTC        : ${new Date().toISOString()}`,
    `Imagen k6        : grafana/k6:2.3.0`,
    `URL base         : ${BASE}`,
    `Escenario        : ramping-vus (VUS_MAX=${VUS_MAX}, ${DURACION_SUBIDA} -> ${DURACION_SOSTENIDA} -> ${DURACION_BAJADA})`,
    `Duracion real    : ${duracionTotalMs !== null ? `${(duracionTotalMs / 1000).toFixed(1)} s` : 'N/D'}`,
    `Resultado global : ${cumpleTodo ? 'CUMPLE RNF2' : 'NO CUMPLE RNF2'}`,
    '',
    '--- Resumen general ---',
    `Solicitudes HTTP : ${httpReqs ? `${httpReqs.count} (${fmtNum(httpReqs.rate)} req/s)` : 'N/D'}`,
    `Fallos HTTP      : ${httpFailed ? `${fmtPct(httpFailed.rate)} (fallidas=${httpFailed.passes}, exitosas=${httpFailed.fails})` : 'N/D'}`,
    `Checks 200       : ${checksMetrica ? `${fmtPct(checksMetrica.rate)} (ok=${checksMetrica.passes}, fallo=${checksMetrica.fails})` : 'N/D'}`,
    `VUs maximos      : ${vusMax ? vusMax.max : 'N/D'}`,
    `Iteraciones      : ${iteraciones ? iteraciones.count : 'N/D'}`,
    '',
    '--- Latencia por endpoint evaluado (umbral RNF2: p(95) < 3000 ms y max < 3000 ms) ---',
  ];

  const clavesLatencia = [
    ...ENDPOINTS.map((ep) => ({
      titulo: ep,
      clave: `http_req_duration{endpoint:${ep}}`,
    })),
    {
      titulo: 'total_evaluacion',
      clave: 'http_req_duration{fase:evaluacion}',
    },
  ];

  for (const item of clavesLatencia) {
    const metrica = metricas[item.clave];
    const v = metrica ? metrica.values : null;
    if (!v) {
      lineas.push(`* ${item.titulo}: sin mediciones`);
      continue;
    }
    const estP95 = estadoUmbral(metrica, 'p(95)<3000');
    const estMax = estadoUmbral(metrica, 'max<3000');
    lineas.push(
      `* ${item.titulo}: min=${fmtMs(v.min)}, avg=${fmtMs(v.avg)}, med=${fmtMs(v.med)}, p(90)=${fmtMs(v['p(90)'])}, p(95)=${fmtMs(v['p(95)'])} [${estP95}], p(99)=${fmtMs(v['p(99)'])}, max=${fmtMs(v.max)} [${estMax}]`
    );
  }

  const listaChecks = [];
  recolectarChecks(data.root_group, listaChecks);
  if (listaChecks.length > 0) {
    lineas.push('', '--- Detalle de checks ---');
    for (const ch of listaChecks) {
      lineas.push(`* ${ch.name}: ok=${ch.passes}, fallo=${ch.fails}`);
    }
  }

  if (umbrales.length > 0) {
    lineas.push('', '--- Umbrales evaluados ---');
    for (const u of umbrales) {
      lineas.push(`* [${u.ok ? 'CUMPLE' : 'FALLA'}] ${u.metrica} -> ${u.expresion}`);
    }
  }

  lineas.push('========================================================================', '');
  const textoLegible = lineas.join('\n');

  // Excluir setup_data para no persistir cookies de sesión (JSESSIONID, XSRF-TOKEN) en resumen.json.
  const { setup_data, ...sinSesiones } = data;

  return {
    [`${SALIDA}/resumen.json`]: JSON.stringify(sinSesiones, null, 2),
    [`${SALIDA}/resumen.txt`]: textoLegible,
    stdout: textoLegible,
  };
}
