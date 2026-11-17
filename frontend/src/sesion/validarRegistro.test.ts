import { edadEn, validarRegistro } from './validarRegistro';

// 16 de noviembre de 2026, hora local.
const HOY = new Date(2026, 10, 16);
const VALIDOS = { correo: 'ana@forja.test', contrasena: 'doce-caracteres', fechaNacimiento: '2000-01-31' };

describe('validarRegistro', () => {
  test('acepta datos válidos', () => {
    expect(validarRegistro(VALIDOS, HOY)).toEqual({});
  });

  test('rechaza una contraseña de menos de 12 caracteres', () => {
    const errores = validarRegistro({ ...VALIDOS, contrasena: 'once-carac.' }, HOY);

    expect(errores.contrasena).toBe('La contraseña necesita al menos 12 caracteres.');
  });

  test.each([
    ['cumple 18 mañana', '2008-11-17', false],
    ['cumple 18 hoy', '2008-11-16', true],
    ['tiene 40', '1986-03-02', true],
    ['nace en el futuro', '2030-01-01', false],
  ])('edad: %s', (_caso, fechaNacimiento, valida) => {
    const errores = validarRegistro({ ...VALIDOS, fechaNacimiento }, HOY);

    expect(errores.fechaNacimiento === undefined).toBe(valida);
    if (!valida) expect(errores.fechaNacimiento).toBe('Forja es para mayores de 18 años.');
  });

  test.each(['16/11/2000', '2001-02-29', '2000-13-01', ''])('rechaza la fecha mal escrita «%s»', (fecha) => {
    expect(validarRegistro({ ...VALIDOS, fechaNacimiento: fecha }, HOY).fechaNacimiento).toMatch(/AAAA-MM-DD/);
  });

  test('rechaza un correo sin arroba', () => {
    expect(validarRegistro({ ...VALIDOS, correo: 'ana.forja.test' }, HOY).correo).toBe('Escribe un correo válido.');
  });

  test('edadEn cuenta los años cumplidos', () => {
    expect(edadEn({ anio: 2000, mes: 11, dia: 16 }, HOY)).toBe(26);
    expect(edadEn({ anio: 2000, mes: 11, dia: 17 }, HOY)).toBe(25);
  });
});
