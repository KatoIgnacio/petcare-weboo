export async function solicitar(ruta, opciones = {}) {
  const { metodo = 'GET', datos, signal, externa = false } = opciones;
  const servicio = externa ? 'La API externa' : 'PetCare';
  const plazo = AbortSignal.timeout(12000);
  let respuesta;
  try {
    respuesta = await fetch(ruta, {
      method: metodo,
      headers: datos === undefined ? { Accept: 'application/json' } : {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: datos === undefined ? undefined : JSON.stringify(datos),
      credentials: externa ? 'omit' : 'same-origin',
      cache: 'no-store',
      signal: signal ? AbortSignal.any([signal, plazo]) : plazo,
    });
  } catch (error) {
    if (signal?.aborted) throw error;
    let mensaje = 'No se pudo comprobar el resultado. Revisa los datos guardados antes de repetir la operación.';
    if (metodo === 'GET') {
      const destino = externa ? 'la API externa' : 'PetCare';
      mensaje = plazo.aborted
        ? servicio + ' tardó demasiado en responder. Vuelve a intentar.'
        : 'No se pudo conectar con ' + destino + '. Comprueba la conexión y vuelve a intentar.';
    }
    throw new Error(mensaje, { cause: error });
  }

  if (respuesta.status === 204) return;
  const esJson = respuesta.headers.get('content-type')?.includes('json');
  const cuerpo = esJson ? await respuesta.json().catch(() => null) : null;
  if (!respuesta.ok) {
    const mensaje = typeof cuerpo?.mensaje === 'string' ? cuerpo.mensaje : null;
    const error = new Error(mensaje || `${servicio} respondió con un error (${respuesta.status}).`);
    error.status = respuesta.status;
    error.codigo = cuerpo?.codigo;
    error.detalles = Array.isArray(cuerpo?.detalles) ? cuerpo.detalles : [];
    throw error;
  }
  if (cuerpo === null) throw new Error(servicio + ' devolvió una respuesta que no se pudo leer.');
  return cuerpo;
}
