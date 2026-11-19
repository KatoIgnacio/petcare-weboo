import { boton, elemento, errorVisible, esqueleto, indicador } from './ui.js';

const dialogo = document.querySelector('#panel');
const titulo = document.querySelector('#titulo-panel');
const contenido = document.querySelector('#contenido-panel');
const cerrar = document.querySelector('#cerrar-panel');
const confirmacion = document.querySelector('#confirmacion');
let version = 0;
let ocupado = false;
let temporizador;
let limpieza;

cerrar.addEventListener('click', cerrarPanel);
dialogo.addEventListener('cancel', evento => {
  if (ocupado) evento.preventDefault();
  else salirDeVista();
});

function salirDeVista() {
  version++;
  limpieza?.();
  limpieza = null;
}

export function alSalirDelPanel(accion) {
  limpieza = accion;
}

export function cerrarPanel() {
  if (!dialogo.open || ocupado) return;
  salirDeVista();
  dialogo.close();
}

export function avisar(mensaje) {
  clearTimeout(temporizador);
  confirmacion.textContent = mensaje;
  temporizador = setTimeout(() => { confirmacion.textContent = ''; }, 6500);
}

export function mostrarPanel(nombre, ...nodos) {
  salirDeVista();
  titulo.textContent = nombre;
  titulo.tabIndex = -1;
  contenido.replaceChildren(...nodos);
  contenido.classList.remove('appear');
  requestAnimationFrame(() => contenido.classList.add('appear'));
  if (!dialogo.open) dialogo.showModal();
  titulo.focus();
}

export async function consultarPanel(nombre, obtener, mostrar, mensaje = 'Consultando los datos…') {
  mostrarPanel(nombre, esqueleto(mensaje, 'panel'));
  const solicitud = version;
  try {
    const datos = await obtener();
    if (solicitud === version && dialogo.open) mostrar(datos);
  } catch (error) {
    if (solicitud !== version || !dialogo.open) return;
    const alerta = elemento('div', { class: 'alert', role: 'alert' });
    errorVisible(alerta, error);
    mostrarPanel(nombre, alerta, boton('Volver a intentar', () => consultarPanel(nombre, obtener, mostrar, mensaje)));
  }
}

export function formulario({ nombre, campos, guardar, enviar, terminado, aviso, peligro = false, mensajeEnvio = 'Guardando los cambios…' }) {
  const alerta = elemento('div', { class: 'alert', role: 'alert', hidden: true, tabindex: '-1' });
  const submit = elemento('button', { type: 'submit', class: peligro ? 'button danger' : 'button primary' }, guardar);
  const progreso = elemento('div', { class: 'form-progress' });
  const acciones = elemento('div', { class: 'dialog-actions' },
    boton('Volver', cerrarPanel, 'button secondary'), submit);
  const inputs = elemento('fieldset', { class: 'form-fields' }, ...campos, acciones);
  const form = elemento('form', {}, alerta, inputs, progreso);
  form.addEventListener('submit', async evento => {
    evento.preventDefault();
    if (ocupado) return;
    const datos = new FormData(form);
    const payload = Object.fromEntries([...datos].map(([clave, valor]) => [clave, valor.trim()]));
    ocupado = true;
    cerrar.disabled = true;
    inputs.disabled = true;
    inputs.setAttribute('aria-busy', 'true');
    alerta.hidden = true;
    submit.textContent = 'Procesando…';
    progreso.replaceChildren(indicador(mensajeEnvio));
    let resultado;
    let guardado = false;
    try {
      resultado = await enviar(payload);
      guardado = true;
    } catch (error) {
      errorVisible(alerta, error);
      alerta.focus();
    } finally {
      ocupado = false;
      cerrar.disabled = false;
      inputs.disabled = false;
      inputs.removeAttribute('aria-busy');
      submit.textContent = guardar;
      progreso.replaceChildren();
    }
    if (guardado) {
      cerrarPanel();
      avisar(typeof aviso === 'function' ? aviso(resultado) : aviso);
      terminado(resultado);
    }
  });
  mostrarPanel(nombre, form);
  return submit;
}
