import { selectorMedicamentos } from './recursos.js';
import { boton, campo, elemento, entrada } from './ui.js';

export function medicamentosAtencion(signal) {
  const nombres = new Map();
  const lista = elemento('ul', { class: 'atencion-med-lista', hidden: true, 'aria-label': 'Medicamentos añadidos' });
  const vacio = elemento('p', { class: 'field-note' }, 'No hay medicamentos añadidos. Este registro es opcional.');
  const estado = elemento('p', { class: 'field-note', role: 'status', 'aria-atomic': 'true' });
  const error = elemento('p', { class: 'alert', role: 'alert', hidden: true });
  const catalogo = selectorMedicamentos(signal, agregar);

  const nombre = entrada(null, '', { type: 'text', placeholder: 'Nombre del medicamento' });
  const campoManual = campo('Nombre para agregar manualmente', nombre);
  const manual = elemento('div', { class: 'atencion-med-manual', id: nombre.id + '-manual', hidden: true },
    campoManual, boton('Añadir nombre', agregarManual));

  const abrirCatalogo = boton('Elegir del catálogo', () => mostrarModo(catalogo.nodo.hidden ? 'catalogo' : null),
    'button secondary', { 'aria-controls': catalogo.nodo.id, 'aria-expanded': 'false' });
  const abrirManual = boton('Agregar manualmente', () => {
    mostrarModo(manual.hidden ? 'manual' : null);
    if (!manual.hidden) nombre.focus();
  }, 'button secondary', { 'aria-controls': manual.id, 'aria-expanded': 'false' });

  function mostrarModo(modo) {
    const catalogoAbierto = !catalogo.nodo.hidden;
    const mostrarCatalogo = modo === 'catalogo';
    const mostrarManual = modo === 'manual';
    catalogo.nodo.hidden = !mostrarCatalogo;
    manual.hidden = !mostrarManual;
    abrirCatalogo.setAttribute('aria-expanded', String(mostrarCatalogo));
    abrirManual.setAttribute('aria-expanded', String(mostrarManual));
    limpiarError();
    if (mostrarCatalogo && !catalogoAbierto) void catalogo.cargar();
    if (!mostrarCatalogo && catalogoAbierto) catalogo.limpiar();
  }

  function limpiarError() {
    error.hidden = true;
    error.textContent = '';
  }

  function agregar(valor) {
    const limpio = valor.trim();
    const clave = limpio.normalize('NFC').toLocaleLowerCase('es-CL');
    const mensaje = !limpio ? 'Escribe el nombre de un medicamento.'
      : limpio.length > 120 ? 'Cada medicamento puede tener hasta 120 caracteres.'
        : nombres.has(clave) ? 'Ese medicamento ya está en la lista.' : '';

    estado.textContent = '';
    if (mensaje) {
      error.textContent = mensaje;
      error.hidden = false;
      return false;
    }

    limpiarError();
    nombres.set(clave, limpio);
    const quitar = boton('Quitar', () => {
      const destino = item.nextElementSibling?.querySelector('button')
        ?? item.previousElementSibling?.querySelector('button') ?? abrirManual;
      nombres.delete(clave);
      item.remove();
      lista.hidden = nombres.size === 0;
      vacio.hidden = !lista.hidden;
      limpiarError();
      estado.textContent = 'Se quitó «' + limpio + '».';
      destino.focus();
    }, 'button text-button', { 'aria-label': 'Quitar ' + limpio });
    const item = elemento('li', {}, elemento('span', {}, limpio), quitar);
    lista.append(item);
    lista.hidden = false;
    vacio.hidden = true;
    estado.textContent = 'Se añadió «' + limpio + '».';
    return true;
  }

  function agregarManual() {
    if (agregar(nombre.value)) nombre.value = '';
    nombre.focus();
  }

  nombre.addEventListener('input', limpiarError);
  nombre.addEventListener('keydown', evento => {
    if (evento.key !== 'Enter' || evento.isComposing) return;
    evento.preventDefault();
    agregarManual();
  });

  return {
    nodo: elemento('section', { class: 'atencion-med', 'aria-label': 'Medicamentos de la atención' },
      elemento('h3', {}, 'Medicamentos de la atención'), vacio, lista,
      elemento('div', { class: 'atencion-med-acciones' }, abrirCatalogo, abrirManual), estado, error,
      manual, catalogo.nodo),
    valores() {
      if (nombre.value.trim()) {
        mostrarModo('manual');
        throw new Error('Pulsa «Añadir nombre» para incorporar el medicamento que estás escribiendo, o borra el texto para continuar.');
      }
      return [...nombres.values()];
    },
  };
}
