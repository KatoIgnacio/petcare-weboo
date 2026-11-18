import { agenda } from './agenda.js';
import { clinica, medicamentos } from './recursos.js';
import { clientes, encabezado, fichaCliente, veterinarios } from './pacientes.js';
import { cerrarPanel } from './panel.js';
import { boton, elemento, errorVisible, esqueleto } from './ui.js';

const vista = document.querySelector('#vista');
const principal = document.querySelector('#contenido');
const enlaces = document.querySelectorAll('nav a');
let version = 0;
let consultaVista;

document.querySelector('#saltar').addEventListener('click', evento => {
  evento.preventDefault();
  principal.focus();
});

window.addEventListener('hashchange', () => {
  cerrarPanel();
  void presentar(true);
});

function recargar() {
  void presentar(false, true);
}

async function presentar(navegando = false, actualizando = false) {
  const actual = ++version;
  consultaVista?.abort();
  consultaVista = new AbortController();
  const signal = consultaVista.signal;
  const ruta = location.hash || '#/agenda';
  const categoria = ruta.split('/')[1];
  for (const enlace of enlaces) {
    if (enlace.hash === '#/' + categoria) enlace.setAttribute('aria-current', 'page');
    else enlace.removeAttribute('aria-current');
  }
  const cliente = ruta.match(/^#\/clientes\/([1-9]\d*)$/);
  if (cliente || ruta === '#/clientes' || ruta === '#/veterinarios') {
    const cargando = actualizando ? 'Actualizando' : 'Consultando';
    const pantalla = categoria === 'veterinarios' ? 'el equipo veterinario' :
      cliente ? 'la ficha y sus mascotas' : 'los clientes';
    vista.removeAttribute('aria-busy');
    vista.replaceChildren(esqueleto(cargando + ' ' + pantalla + '…', 'pagina'));
  }
  try {
    let nodos;
    if (cliente) nodos = await fichaCliente(cliente[1], recargar);
    else if (ruta === '#/clientes') nodos = await clientes(recargar);
    else if (ruta === '#/veterinarios') nodos = await veterinarios(recargar);
    else if (ruta === '#/agenda') nodos = agenda(recargar, actualizando);
    else if (ruta === '#/medicamentos') nodos = medicamentos(signal);
    else if (ruta === '#/clinica') nodos = clinica(signal);
    else {
      nodos = [encabezado('Página no disponible', 'Abre una sección desde la navegación.'),
        elemento('a', { class: 'button primary', href: '#/agenda' }, 'Ir a la agenda')];
    }
    if (actual !== version) return;
    for (const nodo of nodos) nodo.classList.add('appear');
    vista.replaceChildren(...nodos);
    const titulo = vista.querySelector('h1');
    document.title = titulo.textContent + ' · PetCare';
    if (navegando) window.scrollTo(0, 0);
    if (navegando || (actual > 1 && document.activeElement === document.body)) {
      titulo.tabIndex = -1;
      titulo.focus({ preventScroll: true });
    }
  } catch (error) {
    if (actual !== version) return;
    const alerta = elemento('div', { class: 'alert', role: 'alert' });
    errorVisible(alerta, error);
    vista.replaceChildren(encabezado('No se pudo cargar la sección', 'Puedes volver a intentar la consulta.'),
      alerta, boton('Volver a intentar', recargar));
  } finally {
    if (actual === version) vista.setAttribute('aria-busy', 'false');
  }
}

void presentar();
