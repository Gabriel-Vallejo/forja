// Páginas del enlace del correo. El token viene en el fragmento (#…): el navegador no lo manda al servidor,
// así que no queda en ningún log. Solo se envía al pulsar el botón, no al abrir la página (los antivirus de
// correo abren los enlaces solos y gastarían el token).
'use strict';

const token = decodeURIComponent(location.hash.slice(1));
history.replaceState(null, '', location.pathname);
const formulario = document.querySelector('form');
const mensaje = document.getElementById('mensaje');
const hecho = document.getElementById('hecho');

function error(texto) {
  mensaje.textContent = texto;
  mensaje.className = 'error';
}

async function detalle(respuesta) {
  try {
    const problema = await respuesta.json();
    return (problema.errores && problema.errores[0] && problema.errores[0].mensaje) || problema.detail;
  } catch {
    return null;
  }
}

if (!token) {
  formulario.hidden = true;
  error('El enlace está incompleto. Ábrelo otra vez desde el correo.');
}

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const cuerpo = { token };
  const nueva = formulario.elements.namedItem('contrasena');
  if (nueva) {
    if (nueva.value !== formulario.elements.namedItem('repetida').value) {
      error('Las dos contraseñas no coinciden.');
      return;
    }
    cuerpo.contrasenaNueva = nueva.value;
  }
  const boton = formulario.querySelector('button');
  boton.disabled = true;
  try {
    const respuesta = await fetch(formulario.dataset.ruta, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cuerpo),
    });
    if (respuesta.ok) {
      formulario.hidden = true;
      mensaje.textContent = '';
      hecho.hidden = false;
      return;
    }
    error((await detalle(respuesta)) || 'No ha funcionado. Pide un enlace nuevo desde la app.');
  } catch {
    error('No hay conexión con Forja. Prueba otra vez en un rato.');
  }
  boton.disabled = false;
});
