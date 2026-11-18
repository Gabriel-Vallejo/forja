import type { Medidas } from '../api/esquemas';
import { ERROR_CINTURA_CUELLO, validarMedidas, type ValoresMedidas, valoresDe } from './medidas';
import { faltaParaGrasa, resumenPerfil, textoGrasa, valorExperiencia, valorObjetivo } from './textos';

const VACIOS: ValoresMedidas = { alturaCm: '', pesoKg: '', cuelloCm: '', cinturaCm: '', caderaCm: '' };

describe('validarMedidas', () => {
  test('un campo vacío es una medida sin dar', () => {
    expect(validarMedidas(VACIOS)).toEqual({ cuerpo: { alturaCm: null, pesoKg: null, cuelloCm: null, cinturaCm: null, caderaCm: null } });
  });

  test('acepta la coma decimal y los espacios', () => {
    expect(validarMedidas({ ...VACIOS, alturaCm: ' 180 ', pesoKg: '78,5' })).toEqual({
      cuerpo: { alturaCm: 180, pesoKg: 78.5, cuelloCm: null, cinturaCm: null, caderaCm: null },
    });
  });

  test('rechaza lo que no es un número o cae fuera de rango, con el rango en el mensaje', () => {
    const resultado = validarMedidas({ ...VACIOS, alturaCm: '99', pesoKg: 'mucho', cuelloCm: '71', cinturaCm: '39', caderaCm: '201' });

    expect(resultado).toEqual({
      errores: {
        alturaCm: 'Altura: un número entre 100 y 250 cm.',
        pesoKg: 'Peso: un número entre 30 y 300 kg.',
        cuelloCm: 'Cuello: un número entre 20 y 70 cm.',
        cinturaCm: 'Cintura: un número entre 40 y 200 cm.',
        caderaCm: 'Cadera: un número entre 50 y 200 cm.',
      },
    });
  });

  test('los límites del rango valen', () => {
    expect(validarMedidas({ ...VACIOS, alturaCm: '100', pesoKg: '300' })).toHaveProperty('cuerpo');
    expect(validarMedidas({ ...VACIOS, alturaCm: '250', pesoKg: '30' })).toHaveProperty('cuerpo');
  });

  test('la cintura tiene que ser mayor que el cuello', () => {
    expect(validarMedidas({ ...VACIOS, cuelloCm: '40', cinturaCm: '40' })).toEqual({ errores: { cinturaCm: ERROR_CINTURA_CUELLO } });
    expect(validarMedidas({ ...VACIOS, cuelloCm: '40', cinturaCm: '40,5' })).toHaveProperty('cuerpo');
  });
});

describe('valoresDe', () => {
  test('pasa lo guardado a texto de campo con coma decimal y deja vacío lo que falta', () => {
    const medidas: Medidas = { alturaCm: 180, pesoKg: 78.5 };

    expect(valoresDe(medidas)).toEqual({ ...VACIOS, alturaCm: '180', pesoKg: '78,5' });
  });
});

describe('textos del perfil', () => {
  test('la estimación de grasa lleva su margen', () => {
    expect(textoGrasa({ porcentaje: 16.1, margen: 4 })).toBe('≈ 16,1 % (± 4 puntos)');
  });

  test('explica qué falta según el sexo guardado', () => {
    expect(faltaParaGrasa('no_indica')).toMatch(/indica arriba/);
    expect(faltaParaGrasa('mujer')).toMatch(/cadera/);
    expect(faltaParaGrasa('hombre')).not.toMatch(/cadera/);
  });

  test('resumen y valores del perfil, con «Sin definir» y una invitación a completarlo', () => {
    expect(resumenPerfil({ objetivo: 'fuerza_hipertrofia', experiencia: 'intermedio' })).toBe('Fuerza e hipertrofia · Intermedio');
    expect(resumenPerfil({ objetivo: 'fuerza' })).toBe('Fuerza');
    expect(resumenPerfil({})).toBe('Completa tu objetivo y tu experiencia');
    expect(valorObjetivo({})).toBe('Sin definir');
    expect(valorObjetivo({ objetivo: 'hipertrofia' })).toBe('Hipertrofia');
    expect(valorExperiencia({})).toBe('Sin definir');
    expect(valorExperiencia({ experiencia: 'avanzado' })).toBe('Avanzado');
  });
});
