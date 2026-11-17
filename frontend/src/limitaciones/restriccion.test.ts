import { MAXIMO_PARAMETRO, TIPOS_CON_PARAMETRO, validarRestriccion, type BorradorRestriccion } from './restriccion';

const BASE: BorradorRestriccion = {
  region: 'rodilla',
  lado: 'izquierdo',
  tipo: 'sin_impacto',
  parametro: '',
  estado: 'actual',
  profesional: false,
};

describe('validarRestriccion', () => {
  test('con región, lado, tipo y estado es válida y no manda campos vacíos', () => {
    const { errores, datos } = validarRestriccion(BASE);

    expect(errores).toEqual({});
    expect(datos).toEqual({ region: 'rodilla', lado: 'izquierdo', tipo: 'sin_impacto', estado: 'actual', origen: 'usuario' });
  });

  test('«me la indicó un profesional» pasa a origen profesional', () => {
    expect(validarRestriccion({ ...BASE, profesional: true }).datos?.origen).toBe('profesional');
  });

  test('sin región ni patrón pide uno de los dos', () => {
    const { errores, datos } = validarRestriccion({ ...BASE, region: undefined });

    expect(errores.ubicacion).toBe('Elige una región o un patrón de movimiento.');
    expect(datos).toBeUndefined();
  });

  test('solo con patrón también vale', () => {
    const { errores, datos } = validarRestriccion({ ...BASE, region: undefined, patron: 'sentadilla' });

    expect(errores).toEqual({});
    expect(datos).toMatchObject({ patron: 'sentadilla' });
    expect(datos?.region).toBeUndefined();
  });

  test('sin lado ni tipo ni estado los pide', () => {
    const { errores } = validarRestriccion({ ...BASE, lado: undefined, tipo: undefined, estado: undefined });

    expect(errores).toEqual({ lado: 'Elige el lado.', tipo: 'Elige qué te limita.', estado: 'Elige cómo está ahora.' });
  });

  test.each(TIPOS_CON_PARAMETRO)('el parámetro solo viaja con %s y se recorta', (tipo) => {
    const { datos } = validarRestriccion({ ...BASE, tipo, parametro: '  RPE 6  ' });

    expect(datos?.parametro).toBe('RPE 6');
  });

  test('con un tipo que no lo usa, el parámetro se descarta', () => {
    expect(validarRestriccion({ ...BASE, parametro: 'RPE 6' }).datos?.parametro).toBeUndefined();
  });

  test('parámetro de más de 40 caracteres, no', () => {
    const { errores } = validarRestriccion({ ...BASE, tipo: 'limitar_carga', parametro: 'a'.repeat(MAXIMO_PARAMETRO + 1) });

    expect(errores.parametro).toBe('Máximo 40 caracteres.');
  });

  test.each(['20 kg', '90°', '45 grados', '10 kilos'])('parámetro con unidades («%s»), no', (parametro) => {
    const { errores } = validarRestriccion({ ...BASE, tipo: 'limitar_carga', parametro });

    expect(errores.parametro).toBe('Escribe una categoría o un RPE, sin kilos ni grados.');
  });

  test('al editar conserva el texto original que ya tenía', () => {
    expect(validarRestriccion({ ...BASE, textoOriginal: 'sin sentadilla profunda' }).datos?.textoOriginal).toBe('sin sentadilla profunda');
  });
});
