import { solicitar } from './api.js';
import { encabezado } from './pacientes.js';
import { boton, campo, elemento, entrada, errorVisible, esqueleto, ficha, mensajeVacio } from './ui.js';

function urlPublica(valor) {
  if (typeof valor !== 'string' || !valor.trim()) return null;
  try {
    const url = new URL(valor);
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password ? url : null;
  } catch {
    return null;
  }
}

async function consultarRecurso(ruta, parametros, signal) {
  const config = await solicitar('/api/configuracion', { signal });
  const base = urlPublica(config.externalApiUrl);
  if (!base || base.search || base.hash) throw new Error('La dirección de la API externa no es válida.');
  if (!base.pathname.endsWith('/')) base.pathname += '/';
  const destino = new URL(ruta, base);
  if (parametros) destino.search = new URLSearchParams(parametros);
  return solicitar(destino, { externa: true, signal });
}

function falloConsulta(destino, error, intentar) {
  const alerta = elemento('div', { class: 'alert', role: 'alert' });
  errorVisible(alerta, error);
  destino.replaceChildren(elemento('div', { class: 'section-feedback' }, alerta,
    boton('Volver a intentar', intentar)));
}

function textoValido(valor) {
  return typeof valor === 'string' && valor.trim().length > 0;
}

const camposMedicamento = [
  ['principioActivo', 'Principio activo'],
  ['presentacion', 'Presentación'],
  ['advertencias', 'Advertencias'],
  ['fuente', 'Fuente'],
];

function listaMedicamentos(registros) {
  if (!Array.isArray(registros) || registros.some(medicamento =>
    !textoValido(medicamento?.nombre) || camposMedicamento.some(([clave]) => !textoValido(medicamento[clave])))) {
    throw new Error('La API externa devolvió un catálogo con formato inesperado.');
  }
  if (!registros.length) return mensajeVacio('Sin coincidencias', 'Prueba con otro nombre.');

  return elemento('div', { class: 'external-results appear' }, registros.map(medicamento =>
    elemento('article', { class: 'external-card' }, elemento('h3', {}, medicamento.nombre),
      ficha(camposMedicamento.map(([clave, etiqueta]) => [etiqueta, medicamento[clave]])))));
}

export function medicamentos(vista) {
  const nombre = entrada('nombre', '', { type: 'search', maxlength: 120, placeholder: 'Ej. Amoxicilina' });
  const alerta = elemento('div', { class: 'alert', role: 'alert', hidden: true });
  const resultados = elemento('div', { class: 'external-list' });
  const form = elemento('form', { class: 'external-search' }, campo('Nombre del medicamento', nombre),
    elemento('button', { type: 'submit', class: 'button primary' }, 'Buscar'));
  let consulta;

  async function buscar(termino) {
    consulta?.abort();
    consulta = new AbortController();
    if (!termino || termino.length > 120) {
      errorVisible(alerta, new Error('Ingresa un nombre de medicamento de entre 1 y 120 caracteres.'));
      resultados.replaceChildren();
      nombre.focus();
      return;
    }

    const signal = AbortSignal.any([vista, consulta.signal]);
    alerta.hidden = true;
    resultados.replaceChildren(esqueleto('Buscando medicamentos…'));
    try {
      const registros = await consultarRecurso('api/medicamentos', { nombre: termino }, signal);
      if (!signal.aborted) resultados.replaceChildren(listaMedicamentos(registros));
    } catch (error) {
      if (signal.aborted) return;
      if (error.status === 404 && error.codigo === 'SIN_RESULTADOS') {
        resultados.replaceChildren(mensajeVacio('Sin coincidencias', 'Prueba con otro nombre.'));
        return;
      }
      falloConsulta(resultados, error, () => buscar(termino));
    }
  }

  form.addEventListener('submit', evento => {
    evento.preventDefault();
    void buscar(nombre.value.trim());
  });
  return [
    encabezado('Medicamentos', 'Busca por nombre o por parte del nombre.'),
    elemento('section', { class: 'surface external-surface', 'aria-label': 'Catálogo de medicamentos' }, form, alerta, resultados),
  ];
}

function fichaClinica(datos) {
  const mapa = urlPublica(datos?.urlMapa);
  function coordenadaValida(coordenada, limite) {
    return Number.isFinite(coordenada) && Math.abs(coordenada) <= limite;
  }
  if (!textoValido(datos?.nombre) || !textoValido(datos?.direccion) || !mapa
    || !coordenadaValida(datos.latitud, 90) || !coordenadaValida(datos.longitud, 180)) {
    throw new Error('La API externa devolvió una ubicación con formato inesperado.');
  }
  const demostracion = datos.direccion === 'Avenida Central 123, Chillán'
    && datos.latitud === -36.6066 && datos.longitud === -72.1034;
  return elemento('div', { class: 'external-results appear' },
    elemento('article', { class: 'external-card clinic-card' },
      elemento('div', {}, elemento('h2', {}, datos.nombre),
        demostracion ? elemento('p', { class: 'clinic-demo' }, 'Ubicación de demostración') : null),
      ficha([['Dirección', datos.direccion], ['Coordenadas', datos.latitud + ', ' + datos.longitud]]),
      elemento('a', { class: 'button primary', href: mapa.href, target: '_blank', rel: 'noopener noreferrer' }, 'Abrir mapa')));
}

export function clinica(signal) {
  const resultados = elemento('section', { class: 'surface external-surface', 'aria-label': 'Ubicación de la clínica' });
  async function cargar() {
    resultados.replaceChildren(esqueleto('Consultando la ubicación de la clínica…'));
    try {
      const datos = await consultarRecurso('api/ubicacion-clinica', null, signal);
      if (!signal.aborted) resultados.replaceChildren(fichaClinica(datos));
    } catch (error) {
      if (!signal.aborted) falloConsulta(resultados, error, cargar);
    }
  }
  void cargar();
  return [encabezado('Clínica'), resultados];
}
