import { solicitar } from './api.js';
import { encabezado } from './pacientes.js';
import { boton, campo, elemento, entrada, errorVisible, esqueleto, ficha, mensajeVacio, opcion } from './ui.js';

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

function falloConsulta(destino, error, intentar, etiqueta = 'Volver a intentar') {
  const alerta = elemento('div', { class: 'alert', role: 'alert' });
  errorVisible(alerta, error);
  destino.replaceChildren(elemento('div', { class: 'section-feedback' }, alerta,
    boton(etiqueta, intentar)));
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

function sinCoincidencias() {
  const vacio = mensajeVacio('Sin coincidencias', 'Prueba con otro nombre.');
  vacio.setAttribute('role', 'status');
  return vacio;
}

async function consultarMedicamentos(ruta, parametros, signal) {
  const registros = await consultarRecurso(ruta, parametros, signal);
  if (!Array.isArray(registros) || registros.some(medicamento =>
    !textoValido(medicamento?.nombre) || camposMedicamento.some(([clave]) => !textoValido(medicamento[clave])))) {
    throw new Error('La API externa devolvió un catálogo con formato inesperado.');
  }
  return registros;
}

function datosMedicamento(medicamento) {
  return ficha(camposMedicamento.map(([clave, etiqueta]) => [etiqueta, medicamento[clave]]));
}

function fichaMedicamento(medicamento, encabezado = 'h3') {
  return elemento('article', { class: 'external-card' }, elemento(encabezado, {}, medicamento.nombre),
    datosMedicamento(medicamento));
}

function resumenMedicamento(medicamento, indice) {
  const datos = datosMedicamento(medicamento);
  datos.id = 'detalles-medicamento-' + indice;
  datos.hidden = true;
  const detalles = boton('Detalles', () => {
    datos.hidden = !datos.hidden;
    detalles.setAttribute('aria-expanded', String(!datos.hidden));
  }, 'button row-button', {
    'aria-label': 'Detalles de ' + medicamento.nombre,
    'aria-controls': datos.id,
    'aria-expanded': 'false',
  });
  return elemento('article', { class: 'external-card' },
    elemento('div', { class: 'medicamento-resumen' }, elemento('h3', {}, medicamento.nombre), detalles), datos);
}

export function medicamentos(vista) {
  const nombre = entrada(null, '', { type: 'search', maxlength: 120, placeholder: 'Ej. Amoxicilina' });
  const alerta = elemento('div', { class: 'alert', role: 'alert', hidden: true });
  const resultados = elemento('div', { class: 'external-list' });
  const controles = elemento('form', { class: 'external-search' }, campo('Nombre del medicamento', nombre),
    elemento('button', { type: 'submit', class: 'button primary' }, 'Buscar'),
    boton('Mostrar todos', () => {
      nombre.value = '';
      void consultar();
    }));
  let consulta;
  let terminoConsultado = '';

  async function consultar(termino = '') {
    consulta?.abort();
    consulta = new AbortController();
    terminoConsultado = termino;
    alerta.hidden = true;
    if (termino.length > 120) {
      errorVisible(alerta, new Error('El nombre del medicamento puede tener hasta 120 caracteres.'));
      resultados.replaceChildren();
      nombre.focus();
      return;
    }

    const signal = AbortSignal.any([vista, consulta.signal]);
    const ruta = termino ? 'api/medicamentos' : 'api/medicamentos/catalogo';
    const parametros = termino ? { nombre: termino } : null;
    resultados.replaceChildren(esqueleto(termino ? 'Buscando medicamentos…' : 'Cargando catálogo de medicamentos…'));
    try {
      const registros = await consultarMedicamentos(ruta, parametros, signal);
      if (signal.aborted) return;
      if (registros.length) {
        resultados.replaceChildren(elemento('div', { class: 'external-results appear' },
          registros.map(resumenMedicamento)));
      } else {
        const vacio = termino ? sinCoincidencias() : mensajeVacio('El catálogo está vacío',
          'Por ahora no hay medicamentos disponibles para consultar.');
        vacio.setAttribute('role', 'status');
        resultados.replaceChildren(vacio);
      }
    } catch (error) {
      if (signal.aborted) return;
      if (termino && error.status === 404 && error.codigo === 'SIN_RESULTADOS') {
        resultados.replaceChildren(sinCoincidencias());
        return;
      }
      falloConsulta(resultados, error, () => {
        nombre.focus();
        void consultar(termino);
      }, 'Reintentar');
    }
  }

  nombre.addEventListener('input', () => {
    if (!nombre.value.trim() && terminoConsultado) void consultar();
  });
  controles.addEventListener('submit', evento => {
    evento.preventDefault();
    void consultar(nombre.value.trim());
  });
  void consultar();
  return [
    encabezado('Medicamentos', 'Explora el catálogo o busca por nombre completo o parcial.'),
    elemento('section', { class: 'surface external-surface', 'aria-label': 'Catálogo de medicamentos' },
      controles, alerta, resultados),
  ];
}

export function selectorMedicamentos(vista, alAnadir) {
  const selector = elemento('select', { disabled: true }, opcion('', 'Selecciona un medicamento'));
  const campoSelector = campo('Medicamento del catálogo', selector);
  const resultado = elemento('div');
  const detalle = elemento('div');
  const anadir = boton('Añadir a la atención', () => {
    const medicamento = seleccionado();
    if (medicamento && alAnadir(medicamento.nombre)) {
      selector.value = '';
      mostrarSeleccion();
      selector.focus();
    }
  }, 'button secondary', { disabled: true });
  const controles = elemento('div', { class: 'atencion-med-selector', hidden: true }, campoSelector, anadir);
  const nodo = elemento('div', { class: 'atencion-med-catalogo', id: selector.id + '-catalogo', hidden: true },
    resultado, controles, detalle);
  let consulta;
  let registros = [];

  function seleccionado() {
    return selector.value === '' ? null : registros[Number(selector.value)];
  }

  function mostrarSeleccion() {
    const medicamento = seleccionado();
    anadir.disabled = !medicamento;
    detalle.replaceChildren(...(medicamento ? [fichaMedicamento(medicamento, 'h4')] : []));
  }

  function limpiar() {
    consulta?.abort();
    registros = [];
    controles.hidden = true;
    selector.disabled = true;
    selector.replaceChildren(opcion('', 'Selecciona un medicamento'));
    resultado.replaceChildren();
    mostrarSeleccion();
  }

  async function cargar() {
    limpiar();
    consulta = new AbortController();
    const signal = AbortSignal.any([vista, consulta.signal]);
    resultado.replaceChildren(esqueleto('Cargando catálogo de medicamentos…'));
    try {
      const catalogo = await consultarMedicamentos('api/medicamentos/catalogo', null, signal);
      if (signal.aborted) return;
      if (!catalogo.length) {
        const vacio = mensajeVacio('El catálogo está vacío', 'Puedes agregar un medicamento manualmente.');
        vacio.setAttribute('role', 'status');
        resultado.replaceChildren(vacio);
        return;
      }
      registros = catalogo;
      selector.append(...catalogo.map((registro, indice) => opcion(indice, registro.nombre)));
      selector.disabled = false;
      controles.hidden = false;
      resultado.replaceChildren(elemento('p', { class: 'field-note', role: 'status' },
        catalogo.length === 1 ? 'Un medicamento disponible.' : catalogo.length + ' medicamentos disponibles.'));
    } catch (error) {
      if (!signal.aborted) falloConsulta(resultado, error, cargar, 'Reintentar');
    }
  }

  selector.addEventListener('change', mostrarSeleccion);
  return { nodo, cargar, limpiar };
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
