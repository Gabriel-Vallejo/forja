import { objetivo } from '../pruebas/dobles';
import type { SerieLocal } from './almacen';
import { cargaSiguiente } from './reglaSiguiente';

// Objetivo de prueba: 4 series de 6-10 a RIR 2, 80 kg, +2,5 kg.
function trabajo(repeticiones: number, rir: number | null, cambios: Partial<SerieLocal> = {}): SerieLocal {
  return {
    uuid: `${repeticiones}-${rir}-${Math.random()}`,
    sesion: 's1',
    ejercicio: 'press_banca',
    orden: 1,
    pesoKg: 80,
    repeticiones,
    rir,
    rpe: null,
    dolor: null,
    calentamiento: false,
    corrigeA: null,
    anulada: false,
    realizadaEn: '2026-11-18T10:00:00.000Z',
    ...cambios,
  };
}
const cuatro = (rep: number, rir: number | null) => Array.from({ length: 4 }, () => trabajo(rep, rir));

describe('regla de la siguiente sesión', () => {
  test('rango alto a tu RIR objetivo en todas las series previstas: sube el incremento', () => {
    expect(cargaSiguiente(objetivo(), cuatro(10, 2))).toBe(82.5);
  });

  test('con menos RIR (más esfuerzo) o más repeticiones también cumple', () => {
    expect(cargaSiguiente(objetivo(), [...cuatro(10, 0).slice(0, 2), trabajo(12, 1), trabajo(10, 2)])).toBe(82.5);
  });

  test('si una serie se queda corta de repeticiones o con más RIR, repite la carga', () => {
    expect(cargaSiguiente(objetivo(), [...cuatro(10, 2).slice(0, 3), trabajo(9, 2)])).toBe(80);
    expect(cargaSiguiente(objetivo(), [...cuatro(10, 2).slice(0, 3), trabajo(10, 3)])).toBe(80);
  });

  test('sin RIR anotado no se puede saber si cumple: repite (igual que el servidor)', () => {
    expect(cargaSiguiente(objetivo(), [...cuatro(10, 2).slice(0, 3), trabajo(10, null)])).toBe(80);
  });

  test('si faltan series de las previstas, repite', () => {
    expect(cargaSiguiente(objetivo(), cuatro(10, 2).slice(0, 3))).toBe(80);
  });

  test('el calentamiento y otros ejercicios no cuentan', () => {
    const series = [...cuatro(10, 2), trabajo(3, 5, { calentamiento: true, pesoKg: 40 }), trabajo(1, 9, { ejercicio: 'remo' })];

    expect(cargaSiguiente(objetivo(), series)).toBe(82.5);
  });

  test('parte de la carga más alta que levantaste, como el servidor', () => {
    const series = [...cuatro(10, 2).slice(0, 3), trabajo(10, 2, { pesoKg: 85 })];

    expect(cargaSiguiente(objetivo(), series)).toBe(87.5);
  });

  test('peso corporal (sin carga) o sin series de trabajo: no hay carga que calcular', () => {
    expect(cargaSiguiente(objetivo({ cargaKg: undefined }), cuatro(10, 2))).toBeUndefined();
    expect(cargaSiguiente(objetivo(), [trabajo(10, 2, { calentamiento: true })])).toBeUndefined();
  });
});
