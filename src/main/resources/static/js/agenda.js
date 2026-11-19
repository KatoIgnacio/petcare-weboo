import { solicitar } from './api.js';
import { seguimientoAviso } from './avisos.js';
import { medicamentosAtencion } from './medicamentos-atencion.js';
import { alSalirDelPanel, cerrarPanel, consultarPanel, formulario, mostrarPanel } from './panel.js';
import { boton, campo, elemento, entrada, errorVisible, esqueleto, estadoReserva, fechaHoraLegible,
  fechaLegible, fechaLocal, ficha, horaLegible, indicador, mensajeVacio, opcion, tabla } from './ui.js';
import { mostrarHistorial, tratamientos } from './pacientes.js';

const filtros = { fecha: '', veterinarioId: '' };
const calendarioClinica = new Intl.DateTimeFormat('en', {
  timeZone: 'America/Santiago', year: 'numeric', month: '2-digit', day: '2-digit',
});

function fechaClinica() {
  const partes = Object.fromEntries(calendarioClinica.formatToParts(new Date()).map(({ type, value }) => [type, value]));
  return partes.year + '-' + partes.month + '-' + partes.day;
}

export function agenda(recargar, actualizando) {
  const nuevo = boton('Nueva reserva', () => editarReserva(recargar), 'button primary add');
  const hero = elemento('header', { class: 'agenda-hero' },
    elemento('div', { class: 'hero-copy' },
      elemento('p', { class: 'hero-date' }, fechaLegible(fechaLocal())),
      elemento('h1', {}, 'Agenda clínica'),
      nuevo));

  const conteos = elemento('div', { class: 'metrics', 'aria-label': 'Resumen de reservas' });
  const tablaAgenda = elemento('div', { class: 'agenda-results' });
  const seccion = elemento('section', { class: 'surface', 'aria-labelledby': 'titulo-agenda' });
  const dia = entrada('fecha', filtros.fecha, { type: 'date' });
  const vet = elemento('select', { name: 'veterinarioId' }, opcion('', 'Todos'));
  const errorOpciones = elemento('div', { class: 'alert', role: 'alert', hidden: true });
  const estadoFiltro = elemento('div', { class: 'filter-progress' });
  const form = elemento('form', { class: 'filters' },
    campo('Fecha', dia), campo('Veterinario', vet),
    elemento('button', { type: 'submit', class: 'button secondary filter-submit' }, 'Aplicar filtros'),
    boton('Limpiar', () => {
      dia.value = '';
      vet.value = '';
      form.requestSubmit();
    }, 'button text-button'));
  seccion.append(elemento('div', { class: 'surface-header' },
    elemento('h2', { id: 'titulo-agenda' }, 'Reservas')), form, estadoFiltro, errorOpciones, tablaAgenda);
  form.addEventListener('submit', evento => {
    evento.preventDefault();
    filtros.fecha = dia.value;
    filtros.veterinarioId = vet.value;
    void cargar('Buscando reservas con los filtros elegidos…');
  });
  let version = 0;
  async function cargar(mensaje) {
    const solicitud = ++version;
    const parametros = new URLSearchParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor) parametros.set(clave, valor);
    }
    conteos.classList.add('metrics-refreshing');
    tablaAgenda.replaceChildren(esqueleto(mensaje));
    try {
      const reservas = await solicitar('/api/reservas' + (parametros.size ? '?' + parametros : ''));
      if (solicitud !== version) return;
      conteos.replaceChildren(
        metrica('Reservas', reservas.length, 'total'),
        metrica('Pendientes', reservas.filter(reserva => reserva.estado === 'PENDIENTE').length, 'pending'),
        metrica('Confirmadas', reservas.filter(reserva => reserva.estado === 'CONFIRMADA').length, 'confirmed'));
      const listado = reservas.length ? tabla(
        ['Fecha y hora', 'Paciente', 'Veterinario', 'Estado', 'Acciones'],
        reservas.map(reserva => [
          elemento('div', { class: 'stack' }, elemento('strong', {}, horaLegible(reserva.fechaHora)),
            elemento('small', {}, fechaLegible(reserva.fechaHora))),
          elemento('div', { class: 'stack' }, elemento('strong', {}, reserva.mascotaNombre),
            elemento('small', {}, reserva.clienteNombre)),
          reserva.veterinarioNombre, estadoReserva(reserva.estado),
          boton('Ver reserva', () => detalleReserva(reserva.id, recargar), 'button row-button',
            { 'aria-label': 'Ver reserva de ' + reserva.mascotaNombre + ', ' + fechaHoraLegible(reserva.fechaHora) }),
        ])) : mensajeVacio('Sin reservas para esta consulta', 'Cambia los filtros o registra una nueva reserva.');
      listado.classList.add('appear');
      tablaAgenda.replaceChildren(listado);
    } catch (error) {
      if (solicitud !== version) return;
      conteos.replaceChildren();
      const alerta = elemento('div', { class: 'alert', role: 'alert' });
      errorVisible(alerta, error);
      tablaAgenda.replaceChildren(elemento('div', { class: 'section-feedback' }, alerta,
        boton('Volver a intentar', () => cargar('Consultando la agenda de nuevo…'))));
    } finally {
      if (solicitud === version) conteos.classList.remove('metrics-refreshing');
    }
  }
  async function cargarVeterinarios() {
    errorOpciones.hidden = true;
    vet.disabled = true;
    estadoFiltro.replaceChildren(indicador('Cargando veterinarios para el filtro…'));
    try {
      const activos = await solicitar('/api/veterinarios');
      vet.replaceChildren(opcion('', 'Todos'),
        ...activos.map(persona => opcion(persona.id, persona.nombre)));
      vet.value = filtros.veterinarioId;
    } catch (error) {
      errorVisible(errorOpciones, error);
      errorOpciones.append(boton('Recargar veterinarios', cargarVeterinarios));
    } finally {
      vet.disabled = false;
      estadoFiltro.replaceChildren();
    }
  }
  void cargarVeterinarios();
  void cargar(actualizando ? 'Actualizando la agenda…' : 'Consultando las reservas de la clínica…');
  return [hero, conteos, seccion];
}

function metrica(etiqueta, total, tipo) {
  return elemento('div', { class: 'metric ' + tipo },
    elemento('span', { class: 'metric-mark', 'aria-hidden': 'true' }),
    elemento('div', {}, elemento('span', { class: 'metric-label' }, etiqueta), elemento('strong', {}, total)));
}

function editarReserva(recargar, reserva = null) {
  const nombre = reserva ? 'Reprogramar reserva' : 'Nueva reserva';
  consultarPanel(nombre, async () => {
    const [veterinarios, clientes] = await Promise.all([
      solicitar('/api/veterinarios'), reserva ? [] : solicitar('/api/clientes'),
    ]);
    return { veterinarios, clientes };
  }, ({ veterinarios, clientes }) => {
    if (!veterinarios.length || (!reserva && !clientes.length)) {
      const destino = veterinarios.length ? 'clientes' : 'veterinarios';
      mostrarPanel(nombre, mensajeVacio('Faltan datos para la reserva',
        veterinarios.length ? 'Registra un cliente con una mascota activa.' : 'Registra un veterinario activo.'),
      boton('Ir a ' + destino, () => {
        cerrarPanel();
        location.hash = '#/' + destino;
      }));
      return;
    }
    const veterinario = elemento('select', { name: 'veterinarioId', required: true },
      opcion('', 'Selecciona un veterinario'), veterinarios.map(persona => opcion(persona.id, persona.nombre)));
    if (reserva) veterinario.value = String(reserva.veterinarioId);
    const cliente = elemento('select', { name: 'clienteId', required: true },
      opcion('', 'Selecciona un cliente'), clientes.map(persona => opcion(persona.id, persona.nombre)));
    const mascota = elemento('select', { name: 'mascotaId', required: true, disabled: true },
      opcion('', 'Selecciona primero un cliente'));
    const avisoMascotas = elemento('p', { class: 'field-note', role: 'status' });
    let consulta = 0;
    async function cargarMascotas() {
      const actual = ++consulta;
      mascota.disabled = true;
      mascota.replaceChildren(opcion('', cliente.value ? 'Cargando mascotas…' : 'Selecciona primero un cliente'));
      avisoMascotas.replaceChildren();
      if (!cliente.value) return;
      avisoMascotas.textContent = 'Buscando las mascotas activas del cliente…';
      try {
        const mascotas = await solicitar('/api/clientes/' + cliente.value + '/mascotas');
        if (actual !== consulta) return;
        mascota.replaceChildren(opcion('', mascotas.length ? 'Selecciona una mascota' : 'Sin mascotas activas'),
          ...mascotas.map(paciente => opcion(paciente.id, paciente.nombre)));
        mascota.disabled = mascotas.length === 0;
        avisoMascotas.textContent = mascotas.length ? '' : 'El cliente todavía no tiene mascotas activas.';
      } catch (error) {
        if (actual !== consulta) return;
        mascota.replaceChildren(opcion('', 'Mascotas no disponibles'));
        avisoMascotas.replaceChildren(error.message + ' ', boton('Reintentar', cargarMascotas, 'button text-button'));
      }
    }
    cliente.addEventListener('change', cargarMascotas);
    const fecha = entrada('fecha', reserva?.fechaHora.slice(0, 10) ?? '', {
      type: 'date', min: fechaClinica(), required: true,
    });
    const hora = elemento('select', { name: 'hora', required: true, disabled: true },
      opcion('', 'Selecciona un veterinario y una fecha'));
    const avisoHoras = elemento('div', { class: 'field-note', role: 'status' });
    let solicitudHoras;
    async function cargarHoras(preferida = '') {
      solicitudHoras?.abort();
      const actual = new AbortController();
      solicitudHoras = actual;
      hora.disabled = true;
      guardar.disabled = true;
      avisoHoras.replaceChildren();
      if (!veterinario.value || !fecha.value) {
        hora.replaceChildren(opcion('', 'Selecciona un veterinario y una fecha'));
        return;
      }
      hora.replaceChildren(opcion('', 'Cargando horas disponibles…'));
      avisoHoras.replaceChildren(indicador('Cargando horas disponibles…'));
      const params = new URLSearchParams({ fecha: fecha.value });
      if (reserva) params.set('reservaId', reserva.id);
      try {
        const libres = await solicitar('/api/veterinarios/' + veterinario.value + '/disponibilidad?' + params,
          { signal: actual.signal });
        if (actual.signal.aborted) return;
        hora.replaceChildren(opcion('', libres.length ? 'Selecciona una hora' : 'Sin horas disponibles'),
          ...libres.map(bloque => opcion(bloque, bloque)));
        if (libres.includes(preferida)) hora.value = preferida;
        hora.disabled = guardar.disabled = libres.length === 0;
        avisoHoras.textContent = libres.length ? ''
          : 'No hay horas disponibles para este veterinario en la fecha elegida.';
      } catch (error) {
        if (actual.signal.aborted) return;
        hora.replaceChildren(opcion('', 'Horas no disponibles'));
        avisoHoras.replaceChildren('No se pudieron cargar las horas. ' + error.message + ' ',
          boton('Reintentar', () => cargarHoras(preferida), 'button text-button'));
      }
    }
    veterinario.addEventListener('change', () => cargarHoras());
    fecha.addEventListener('change', () => cargarHoras());
    const campos = reserva ? [] : [campo('Cliente', cliente), campo('Mascota', mascota), avisoMascotas];
    if (reserva) campos.push(elemento('p', { class: 'field-note' },
      'Hora actual: ' + fechaHoraLegible(reserva.fechaHora)));
    campos.push(campo('Veterinario', veterinario), campo('Fecha', fecha), campo('Hora disponible', hora), avisoHoras);
    const guardar = formulario({
      nombre, campos, guardar: reserva ? 'Guardar cambios' : 'Crear reserva',
      mensajeEnvio: reserva ? 'Guardando el nuevo horario…' : 'Guardando la reserva…',
      enviar: async ({ mascotaId, veterinarioId, fecha, hora }) => {
        if (!reserva && !mascotaId) throw new Error('Selecciona una mascota activa para continuar.');
        if (!fecha || !hora) throw new Error('Selecciona una fecha y una hora disponible.');
        const datos = { veterinarioId: Number(veterinarioId), fechaHora: fecha + 'T' + hora };
        if (!reserva) datos.mascotaId = Number(mascotaId);
        try {
          return await solicitar(reserva ? '/api/reservas/' + reserva.id : '/api/reservas',
            { metodo: reserva ? 'PUT' : 'POST', datos });
        } catch (error) {
          if (error.status === 409) void cargarHoras(hora);
          throw error;
        }
      },
      terminado: resultado => {
        recargar();
        if (resultado.resultadoNotificacion?.startsWith('PENDIENTE:')) mostrarDetalleReserva(resultado, recargar);
      },
      aviso: reserva ? 'Reserva reprogramada.' : 'Reserva creada.',
    });
    alSalirDelPanel(() => {
      solicitudHoras?.abort();
      consulta++;
    });
    const actual = reserva?.fechaHora.split('T')[1] ?? '';
    void cargarHoras(/^\d{2}:\d{2}(?::00(?:\.0+)?)?$/.test(actual) ? actual.slice(0, 5) : '');
  }, reserva ? 'Consultando los veterinarios disponibles…' : 'Cargando clientes y veterinarios…');
}

export function detalleReserva(id, recargar) {
  consultarPanel('Detalle de reserva', () => solicitar('/api/reservas/' + id),
    reserva => mostrarDetalleReserva(reserva, recargar), 'Buscando los datos de la reserva y el último aviso…');
}

function mostrarDetalleReserva(inicial, recargar) {
  const datos = elemento('div', { class: 'reservation-summary' });
  const accionesEstado = elemento('div', { class: 'row-actions' });
  let reserva = inicial;
  const seguimiento = seguimientoAviso(inicial, (actual, manual) => {
    const cambioEstado = actual.estado !== reserva.estado;
    reserva = actual;
    if (manual || cambioEstado) pintarDetalle();
  });
  const historial = boton('Ver historial', () => mostrarHistorial(reserva.mascotaId, reserva.mascotaNombre));

  function pintarDetalle() {
    const acciones = [];
    const editable = ['PENDIENTE', 'CONFIRMADA'].includes(reserva.estado);
    if (editable) {
      acciones.push(boton('Reprogramar', () => editarReserva(recargar, reserva)));
      acciones.push(boton('Cancelar reserva', () => cambiarEstado(reserva, 'CANCELADA', recargar), 'button danger'));
    }
    if (reserva.estado === 'PENDIENTE') {
      acciones.push(boton('Confirmar reserva', () => cambiarEstado(reserva, 'CONFIRMADA', recargar), 'button primary'));
    }
    if (reserva.estado === 'CONFIRMADA') {
      acciones.push(boton('Registrar atención', () => registrarAtencion(reserva, recargar), 'button primary'));
    }
    datos.replaceChildren(
      elemento('div', { class: 'detail-title' }, elemento('h3', {}, reserva.mascotaNombre), estadoReserva(reserva.estado)),
      ficha([['Veterinario', reserva.veterinarioNombre], ['Fecha y hora', fechaHoraLegible(reserva.fechaHora)]]));
    if (reserva.atencion) datos.append(tratamientos([reserva.atencion]));
    accionesEstado.replaceChildren(...acciones);
  }

  pintarDetalle();
  mostrarPanel('Detalle de reserva', datos, seguimiento.vista,
    elemento('div', { class: 'dialog-actions' }, seguimiento.actualizar, historial, accionesEstado));
  alSalirDelPanel(seguimiento.detener);
  seguimiento.iniciar();
}

function cambiarEstado(reserva, estado, recargar) {
  const cancelar = estado === 'CANCELADA';
  const accion = cancelar ? 'Cancelar reserva' : 'Confirmar reserva';
  formulario({
    nombre: accion, campos: [elemento('p', { class: 'confirmation-copy' },
      (cancelar ? 'Se cancelará' : 'Se confirmará') + ' la reserva de ' + reserva.mascotaNombre +
      ' del ' + fechaHoraLegible(reserva.fechaHora) + '.')],
    guardar: accion, peligro: cancelar,
    mensajeEnvio: cancelar ? 'Registrando la cancelación…' : 'Confirmando la reserva…',
    enviar: () => solicitar('/api/reservas/' + reserva.id + '/estado', { metodo: 'PATCH', datos: { estado } }),
    terminado: resultado => {
      recargar();
      if (resultado.resultadoNotificacion?.startsWith('PENDIENTE:')) mostrarDetalleReserva(resultado, recargar);
      else detalleReserva(reserva.id, recargar);
    },
    aviso: cancelar ? 'Reserva cancelada.' : 'Reserva confirmada.',
  });
}

function registrarAtencion(reserva, recargar) {
  const consulta = new AbortController();
  const medicamentos = medicamentosAtencion(consulta.signal);
  const area = (nombre, maximo, obligatorio = false) =>
    elemento('textarea', { name: nombre, maxlength: maximo, required: obligatorio, rows: 3 });
  formulario({
    nombre: 'Registrar atención', guardar: 'Guardar atención', mensajeEnvio: 'Guardando la atención en el historial…',
    campos: [
      campo('Diagnóstico', area('diagnostico', 1000, true)),
      campo('Tratamiento', area('tratamiento', 1000, true)),
      medicamentos.nodo,
      campo('Observaciones', area('observaciones', 1500)),
    ],
    enviar: ({ diagnostico, tratamiento, observaciones }) => {
      return solicitar('/api/reservas/' + reserva.id + '/atencion', { metodo: 'POST',
        datos: { diagnostico, tratamiento, medicamentos: medicamentos.valores(), observaciones: observaciones || null } });
    },
    terminado: () => {
      recargar();
      detalleReserva(reserva.id, recargar);
    },
    aviso: 'Atención registrada.',
  });
  alSalirDelPanel(() => consulta.abort());
}
