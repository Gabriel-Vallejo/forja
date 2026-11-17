import { ErrorApi, mensajeDeError, mensajeDeProblema } from './errores';

describe('mensajeDeProblema', () => {
  test('usa el detail del ProblemDetail', () => {
    expect(mensajeDeProblema(403, { status: 403, detail: 'Verifica tu correo antes de entrar.' })).toBe(
      'Verifica tu correo antes de entrar.',
    );
  });

  test('junta los mensajes de los errores de campo', () => {
    const cuerpo = {
      detail: 'Hay datos que no son válidos.',
      errores: [
        { campo: 'correo', mensaje: 'El correo no es válido.' },
        { campo: 'fechaNacimiento', mensaje: 'La fecha de nacimiento debe ser pasada.' },
      ],
    };
    expect(mensajeDeProblema(400, cuerpo)).toBe('El correo no es válido. La fecha de nacimiento debe ser pasada.');
  });

  test.each([
    [429, undefined, 'Demasiados intentos seguidos. Espera un poco y vuelve a probar.'],
    [502, '<html>proxy</html>', 'Algo ha fallado en el servidor. Inténtalo más tarde.'],
    [418, {}, 'No se ha podido completar la petición.'],
  ])('sin detail, %i da un mensaje genérico', (estado, cuerpo, mensaje) => {
    expect(mensajeDeProblema(estado, cuerpo)).toBe(mensaje);
  });

  test('mensajeDeError usa el mensaje de un Error y uno genérico para lo demás', () => {
    expect(mensajeDeError(new ErrorApi(400, 'Mal.'))).toBe('Mal.');
    expect(mensajeDeError('cadena')).toBe('No se ha podido completar la petición.');
  });
});
