import { solicitar } from './api.js';
import { consultarPanel, formulario, mostrarPanel } from './panel.js';
import { boton, campo, elemento, entrada, fechaHoraLegible, fechaLegible, fechaLocal,
  ficha, mensajeVacio, tabla } from './ui.js';

export function encabezado(titulo, descripcion, accion) {
  return elemento('header', { class: 'page-header' },
    elemento('div', {}, elemento('h1', {}, titulo), descripcion ? elemento('p', {}, descripcion) : null), accion);
}

function listado(titulo, columnas, filas, vacio) {
  return elemento('section', { class: 'surface' },
    elemento('div', { class: 'surface-header' }, elemento('h2', {}, titulo)),
    filas.length ? tabla(columnas, filas) : vacio);
}

function accionFila(texto, nombre, accion) {
  return boton(texto, accion, 'button row-button', { 'aria-label': texto + ' de ' + nombre });
}

export async function clientes(recargar) {
  const personas = await solicitar('/api/clientes');
  return [
    encabezado('Clientes y mascotas', 'Gestiona las fichas de los clientes y sus pacientes.',
      boton('Nuevo cliente', () => editarCliente(recargar), 'button primary add')),
    listado('Clientes activos', ['Nombre', 'Correo', 'Teléfono', 'Acciones'],
      personas.map(cliente => [elemento('strong', {}, cliente.nombre), cliente.email, cliente.telefono,
        elemento('a', { href: '#/clientes/' + cliente.id, class: 'button row-button',
          'aria-label': 'Ver ficha de ' + cliente.nombre }, 'Ver ficha')]),
      mensajeVacio('Aún no hay clientes activos', 'Registra un cliente para agregar sus mascotas.')),
  ];
}

export async function fichaCliente(id, recargar) {
  const ruta = '/api/clientes/' + id;
  const [cliente, mascotas] = await Promise.all([solicitar(ruta), solicitar(ruta + '/mascotas')]);
  return [
    elemento('a', { href: '#/clientes', class: 'back-link' }, '← Volver a clientes'),
    encabezado(cliente.nombre, 'Ficha de cliente',
      boton('Agregar mascota', () => editarMascota(cliente, recargar), 'button primary add')),
    elemento('section', { class: 'surface contact' },
      elemento('div', { class: 'surface-header' }, elemento('h2', {}, 'Datos del cliente')),
      ficha([['Correo', cliente.email], ['Teléfono', cliente.telefono]]),
      elemento('div', { class: 'section-actions' },
        boton('Editar cliente', () => editarCliente(recargar, cliente)),
        boton('Dar de baja cliente', () => bajaCliente(cliente), 'button danger'))),
    listado('Mascotas activas', ['Nombre', 'Especie', 'Raza', 'Acciones'],
      mascotas.map(mascota => [elemento('strong', {}, mascota.nombre), mascota.especie, mascota.raza,
        elemento('div', { class: 'row-actions' },
          accionFila('Ver mascota', mascota.nombre, () => detalleMascota(cliente, mascota.id, recargar)),
          accionFila('Historial', mascota.nombre, () => mostrarHistorial(mascota.id, mascota.nombre)))]),
      mensajeVacio('Sin mascotas activas', 'Agrega una mascota desde esta ficha.')),
  ];
}

function editarCliente(recargar, cliente = null) {
  formulario({
    nombre: cliente ? 'Editar cliente' : 'Nuevo cliente',
    campos: datosContacto(cliente),
    guardar: cliente ? 'Guardar cambios' : 'Crear cliente',
    mensajeEnvio: cliente ? 'Actualizando los datos del cliente…' : 'Registrando al cliente…',
    enviar: datos => solicitar('/api/clientes' + (cliente ? '/' + cliente.id : ''),
      { metodo: cliente ? 'PUT' : 'POST', datos }),
    terminado: guardado => {
      const destino = '#/clientes/' + guardado.id;
      if (location.hash === destino) recargar();
      else location.hash = destino;
    },
    aviso: cliente ? 'Cliente actualizado.' : 'Cliente registrado.',
  });
}

function bajaCliente(cliente) {
  formulario({
    nombre: 'Dar de baja cliente', guardar: 'Confirmar baja', peligro: true,
    mensajeEnvio: 'Registrando la baja del cliente…',
    campos: [elemento('p', { class: 'confirmation-copy' },
      'Se dará de baja a ' + cliente.nombre + ' y se desactivarán sus mascotas. El historial clínico se conserva.')],
    enviar: () => solicitar('/api/clientes/' + cliente.id, { metodo: 'DELETE' }),
    terminado: () => { location.hash = '#/clientes'; },
    aviso: 'Cliente dado de baja.',
  });
}

function detalleMascota(cliente, id, recargar) {
  consultarPanel('Ficha de mascota', () => solicitar('/api/mascotas/' + id), mascota => {
    mostrarPanel('Ficha de mascota', elemento('h3', {}, mascota.nombre),
      ficha([['Especie', mascota.especie], ['Raza', mascota.raza],
        ['Fecha de nacimiento', fechaLegible(mascota.fechaNacimiento)], ['Peso', mascota.peso + ' kg']]),
      elemento('div', { class: 'dialog-actions' },
        boton('Ver historial', () => mostrarHistorial(mascota.id, mascota.nombre)),
        boton('Dar de baja mascota', () => bajaMascota(mascota, recargar), 'button danger'),
        boton('Editar mascota', () => editarMascota(cliente, recargar, mascota), 'button primary')));
  }, 'Consultando la ficha de la mascota…');
}

function editarMascota(cliente, recargar, mascota = null) {
  formulario({
    nombre: mascota ? 'Editar mascota' : 'Agregar mascota',
    campos: [
      campo('Nombre', entrada('nombre', mascota?.nombre, { required: true, maxlength: 100 })),
      campo('Especie', entrada('especie', mascota?.especie, { required: true, maxlength: 80 })),
      campo('Raza', entrada('raza', mascota?.raza, { required: true, maxlength: 100 })),
      campo('Fecha de nacimiento', entrada('fechaNacimiento', mascota?.fechaNacimiento,
        { type: 'date', required: true, max: fechaLocal() })),
      campo('Peso (kg)', entrada('peso', mascota?.peso, { type: 'number', min: '0.1', step: '0.01', required: true })),
    ],
    guardar: mascota ? 'Guardar cambios' : 'Registrar mascota',
    mensajeEnvio: mascota ? 'Actualizando la ficha de la mascota…' : 'Registrando la mascota…',
    enviar: datos => solicitar(mascota ? '/api/mascotas/' + mascota.id : '/api/clientes/' + cliente.id + '/mascotas',
      { metodo: mascota ? 'PUT' : 'POST', datos: { ...datos, peso: Number(datos.peso) } }),
    terminado: () => recargar(),
    aviso: mascota ? 'Mascota actualizada.' : 'Mascota registrada.',
  });
}

function bajaMascota(mascota, recargar) {
  formulario({
    nombre: 'Dar de baja mascota', guardar: 'Confirmar baja', peligro: true,
    mensajeEnvio: 'Registrando la baja de la mascota…',
    campos: [elemento('p', { class: 'confirmation-copy' },
      mascota.nombre + ' dejará de aparecer entre las mascotas activas. Su historial clínico se conserva.')],
    enviar: () => solicitar('/api/mascotas/' + mascota.id, { metodo: 'DELETE' }),
    terminado: () => recargar(),
    aviso: 'Mascota dada de baja.',
  });
}

export function mostrarHistorial(id, nombre) {
  consultarPanel('Historial clínico', () => solicitar('/api/mascotas/' + id + '/historial'), atenciones => {
    mostrarPanel('Historial clínico',
      elemento('h3', {}, nombre),
      atenciones.length ? tratamientos(atenciones) :
        mensajeVacio('Sin atenciones registradas', 'Las atenciones de esta mascota aparecerán aquí.'));
  }, 'Buscando atenciones en el historial…');
}

export function tratamientos(atenciones) {
  return elemento('div', { class: 'treatments' }, atenciones.map(atencion => {
    const datos = [
      ['Diagnóstico', atencion.diagnostico],
      ['Tratamiento', atencion.tratamiento],
      ['Medicamentos', atencion.medicamentos.join(', ') || 'Sin medicamentos registrados'],
    ];
    if (atencion.observaciones) datos.push(['Observaciones', atencion.observaciones]);
    return elemento('article', { class: 'treatment' },
      elemento('h4', {}, fechaHoraLegible(atencion.fecha)), ficha(datos));
  }));
}

function datosContacto(persona) {
  return [
    campo('Nombre', entrada('nombre', persona?.nombre, { required: true, maxlength: 120 })),
    campo('Correo electrónico', entrada('email', persona?.email, { type: 'email', required: true, maxlength: 160 })),
    campo('Teléfono', entrada('telefono', persona?.telefono, { type: 'tel', required: true, maxlength: 30 })),
  ];
}

export async function veterinarios(recargar) {
  const profesionales = await solicitar('/api/veterinarios');
  return [
    encabezado('Veterinarios', 'Consulta el equipo disponible para atender reservas.',
      boton('Nuevo veterinario', () => registrarVeterinario(recargar), 'button primary add')),
    listado('Veterinarios activos', ['Nombre', 'Especialidad', 'Correo'],
      profesionales.map(persona => [elemento('strong', {}, persona.nombre), persona.especialidad, persona.email]),
      mensajeVacio('Aún no hay veterinarios activos', 'Registra un veterinario para empezar a agendar.')),
  ];
}

function registrarVeterinario(recargar) {
  formulario({
    nombre: 'Nuevo veterinario', guardar: 'Registrar veterinario',
    mensajeEnvio: 'Incorporando al profesional al listado…',
    campos: [
      campo('Nombre', entrada('nombre', '', { required: true, maxlength: 120 })),
      campo('Especialidad', entrada('especialidad', '', { required: true, maxlength: 120 })),
      campo('Correo electrónico', entrada('email', '', { type: 'email', required: true, maxlength: 160 })),
    ],
    enviar: datos => solicitar('/api/veterinarios', { metodo: 'POST', datos }),
    terminado: () => recargar(),
    aviso: 'Veterinario registrado.',
  });
}
