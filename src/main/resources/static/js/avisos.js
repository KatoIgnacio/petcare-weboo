import { solicitar } from './api.js';
import { boton, elemento } from './ui.js';

const etiquetas = {
  NO_APLICA: 'Sin aviso registrado',
  PENDIENTE: 'Pendiente de envío',
  EN_PROCESO: 'En proceso',
  PENDIENTE_REINTENTO: 'Pendiente de reintento',
  ACEPTADA: 'Aceptado por el servidor SMTP',
  FALLIDA: 'Envío fallido',
  INCIERTA: 'Resultado incierto. Requiere revisión',
};
const enCurso = new Set(['PENDIENTE', 'EN_PROCESO', 'PENDIENTE_REINTENTO']);
const intervalo = 2500;
const duracion = 60000;

export function seguimientoAviso(inicial, alConsultar) {
  const estadoVisible = elemento('p', { class: 'status aviso-estado', role: 'status' });
  const mensaje = elemento('p', { class: 'aviso-mensaje', role: 'status' });
  const vista = elemento('section', { class: 'aviso-pane', 'aria-label': 'Notificación de la reserva' },
    elemento('h4', {}, 'Notificación'), estadoVisible, mensaje);
  const actualizar = boton('Actualizar detalle', () => {
    if (solicitud || cerrado) return;
    limite = performance.now() + duracion;
    void consultar(true);
  }, 'button text-button');
  let estado;
  let limite;
  let temporizador;
  let solicitud;
  let cerrado = false;

  function presentar(valor) {
    const siguiente = typeof valor === 'string' ? valor.split(':', 1)[0] : '';
    if (siguiente === estado) return;
    estado = siguiente;
    estadoVisible.dataset.estado = estado;
    estadoVisible.textContent = Object.hasOwn(etiquetas, estado) ? etiquetas[estado] : 'Estado del aviso no disponible';
  }

  function informar(texto) {
    if (mensaje.textContent !== texto) mensaje.textContent = texto;
  }

  function programar() {
    if (cerrado) return;
    if (!enCurso.has(estado)) {
      informar('');
      return;
    }
    if (performance.now() + intervalo > limite) {
      informar('El aviso sigue pendiente. Usa «Actualizar detalle» para volver a consultar.');
      return;
    }
    informar('Se actualiza automáticamente mientras ves esta reserva.');
    temporizador = setTimeout(() => void consultar(false), intervalo);
  }

  async function consultar(manual) {
    if (cerrado || solicitud) return;
    clearTimeout(temporizador);
    if (!manual && performance.now() >= limite) {
      programar();
      return;
    }
    solicitud = new AbortController();
    const finSeguimiento = manual ? null : AbortSignal.timeout(Math.max(1, Math.ceil(limite - performance.now())));
    const signal = finSeguimiento ? AbortSignal.any([solicitud.signal, finSeguimiento]) : solicitud.signal;
    actualizar.setAttribute('aria-disabled', 'true');
    if (manual) informar('Consultando el estado de la reserva y su aviso…');
    let consultado = false;
    try {
      const reserva = await solicitar('/api/reservas/' + inicial.id, { signal });
      if (cerrado) return;
      if (reserva.id !== inicial.id || typeof reserva.resultadoNotificacion !== 'string') {
        throw new Error('Respuesta de reserva inválida');
      }
      alConsultar(reserva, manual);
      presentar(reserva.resultadoNotificacion);
      consultado = true;
    } catch {
      if (finSeguimiento?.aborted) programar();
      else if (!cerrado) informar('No se pudo consultar el aviso. Se muestra el último estado conocido. Usa «Actualizar detalle» para reintentar.');
    } finally {
      solicitud = null;
      actualizar.removeAttribute('aria-disabled');
      if (consultado) programar();
    }
  }

  presentar(inicial.resultadoNotificacion);
  return {
    vista,
    actualizar,
    iniciar() {
      limite = performance.now() + duracion;
      programar();
    },
    detener() {
      cerrado = true;
      clearTimeout(temporizador);
      solicitud?.abort();
    },
  };
}
