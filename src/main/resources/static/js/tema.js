(() => {
  const clave = 'petcare.tema';
  const sistema = matchMedia('(prefers-color-scheme: dark)');
  const raiz = document.documentElement;
  let preferencia;
  let boton;

  function leerPreferencia() {
    try {
      const valor = localStorage.getItem(clave);
      return valor === 'claro' || valor === 'oscuro' ? valor : null;
    } catch {
      return null;
    }
  }

  function aplicar() {
    const modo = preferencia ?? (sistema.matches ? 'oscuro' : 'claro');
    raiz.dataset.tema = modo;
    document.querySelector('meta[name="theme-color"]').content = modo === 'oscuro' ? '#121b17' : '#f6f7f3';
    if (boton) {
      const oscuro = modo === 'oscuro';
      boton.setAttribute('aria-pressed', String(oscuro));
      boton.title = oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
    }
  }

  preferencia = leerPreferencia();
  aplicar();
  document.addEventListener('DOMContentLoaded', () => {
    boton = document.querySelector('#tema');
    boton.addEventListener('click', () => {
      preferencia = raiz.dataset.tema === 'oscuro' ? 'claro' : 'oscuro';
      try {
        localStorage.setItem(clave, preferencia);
      } catch {
        // La preferencia sigue disponible durante la visita.
      }
      aplicar();
    });
    aplicar();
    boton.hidden = false;
  });
  sistema.addEventListener('change', aplicar);
  addEventListener('storage', evento => {
    if (evento.key === clave || evento.key === null) {
      preferencia = leerPreferencia();
      aplicar();
    }
  });
})();
