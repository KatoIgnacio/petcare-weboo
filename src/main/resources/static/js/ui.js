export function elemento(etiqueta, atributos = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [nombre, valor] of Object.entries(atributos)) {
    if (valor == null || valor === false) continue;
    nodo.setAttribute(nombre, valor === true ? '' : valor);
  }
  for (const hijo of hijos.flat()) {
    if (hijo != null) nodo.append(hijo);
  }
  return nodo;
}

export function boton(texto, accion, clase = 'button secondary', atributos = {}) {
  const nodo = elemento('button', { type: 'button', class: clase, ...atributos }, texto);
  nodo.addEventListener('click', accion);
  return nodo;
}

export function opcion(valor, texto) {
  return elemento('option', { value: valor }, texto);
}

let camposCreados = 0;

export function campo(etiqueta, control) {
  control.id = 'campo-' + ++camposCreados;
  return elemento('div', { class: 'field' }, elemento('label', { for: control.id }, etiqueta), control);
}

export function entrada(nombre, valor = '', opciones = {}) {
  return elemento('input', { name: nombre, value: valor, ...opciones });
}

export function errorVisible(destino, error) {
  destino.hidden = false;
  destino.replaceChildren(elemento('p', {}, error.message));
  if (error.detalles?.length) {
    destino.append(elemento('ul', {}, error.detalles.map(detalle => elemento('li', {}, detalle))));
  }
}

export function mensajeVacio(titulo, texto) {
  return elemento('div', { class: 'empty' },
    elemento('strong', {}, titulo),
    elemento('p', {}, texto));
}

export function indicador(mensaje) {
  return elemento('p', { class: 'progress-note', role: 'status', 'aria-atomic': 'true' },
    elemento('span', { class: 'progress-mark', 'aria-hidden': 'true' }), mensaje);
}

export function esqueleto(mensaje, variante = 'listado') {
  const formas = elemento('div', { class: 'skeleton-lines', 'aria-hidden': 'true' });
  for (let i = 0; i < 3; i++) {
    formas.append(elemento('div', { class: 'skeleton-row' },
      elemento('span'), elemento('span'), elemento('span')));
  }
  return elemento('div', { class: 'skeleton skeleton-' + variante }, indicador(mensaje), formas);
}

export function ficha(datos) {
  return elemento('dl', { class: 'details' }, datos.map(([nombre, valor]) =>
    elemento('div', {}, elemento('dt', {}, nombre), elemento('dd', {}, valor ?? '—'))));
}

export function fechaLocal(fecha = new Date()) {
  const dos = valor => String(valor).padStart(2, '0');
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
}

const fechaFormato = new Intl.DateTimeFormat('es-CL', {
  weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC',
});

export function fechaLegible(valor) {
  return fechaFormato.format(new Date(`${valor.slice(0, 10)}T12:00:00Z`));
}

export function horaLegible(valor) {
  return valor.slice(11, 16) + (valor.length > 16 && valor.slice(17, 19) !== '00' ? valor.slice(16, 19) : '');
}

export function fechaHoraLegible(valor) {
  return `${fechaLegible(valor)} · ${horaLegible(valor)}`;
}

const nombresEstado = {
  PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada',
};

export function estadoReserva(estado) {
  return elemento('span', { class: `status status-${estado.toLowerCase()}` },
    nombresEstado[estado] ?? estado);
}

export function tabla(columnas, filas) {
  const encabezado = elemento('thead', {}, elemento('tr', {}, columnas.map(columna =>
    elemento('th', { scope: 'col' }, columna))));
  const cuerpo = elemento('tbody', {}, filas.map(fila => elemento('tr', {}, fila.map((valor, i) =>
    elemento('td', { 'data-label': columnas[i] }, valor)))));
  return elemento('table', { class: 'data-table' }, encabezado, cuerpo);
}
