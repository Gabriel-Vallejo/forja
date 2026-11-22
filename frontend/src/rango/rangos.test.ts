import type { TablaEstandar } from './estandares';
import { calcularRangos, type Marca } from './rangos';

const cortes = Array.from({ length: 99 }, (_, i) => 50 + i);
const tabla = (levantamiento: TablaEstandar['levantamiento']): TablaEstandar => ({
  levantamiento, sexo: 'hombre', pesoDesdeKg: 80, pesoHastaKg: 90, muestra: 1000, kg: cortes,
});
const TABLAS = [tabla('banca'), tabla('sentadilla'), tabla('peso_muerto')];
const marca = (cambios: Partial<Marca>): Marca => ({
  idExterno: 'x', nombre: 'Ejercicio', grupos: ['pecho'], pesoKg: 80, repeticiones: 8, e1rmKg: 102, levantamiento: null,
  ...cambios,
});

describe('calcularRangos', () => {
  test('una cuenta nueva no tiene rango global ni rango ni ejercicios en ningún músculo', () => {
    const r = calcularRangos([], TABLAS, 'hombre', 85);

    expect(r.global).toBeNull();
    expect(r.faltaPerfil).toBe(false);
    expect(Object.values(r.musculos).every((m) => m.tipo === 'sin' && m.ejercicios.length === 0 && m.top === null)).toBe(true);
  });

  test('la banca da rango al pecho y al global; un aislamiento sale sin estándar', () => {
    const r = calcularRangos(
      [marca({ nombre: 'Press de banca', levantamiento: 'banca', e1rmKg: 102 }), marca({ nombre: 'Aperturas', e1rmKg: 40 })],
      TABLAS, 'hombre', 85,
    );

    // 102 kg → 53 cortes por debajo → top 47 % → Plata III.
    expect(r.global).toEqual({ tipo: 'plata', grado: 3, top: 47 });
    expect(r.musculos.pecho).toMatchObject({ tipo: 'plata', grado: 3, top: 47 });
    expect(r.musculos.pecho.ejercicios.map((e) => [e.nombre, e.serie, e.tipo])).toEqual([
      ['Press de banca', '80,0 kg × 8', 'plata'],
      ['Aperturas', '80,0 kg × 8', 'sin'],
    ]);
    expect(r.musculos.cuadriceps.tipo).toBe('sin');
  });

  test('el global es la media del top de cada levantamiento', () => {
    const r = calcularRangos(
      [marca({ levantamiento: 'banca', e1rmKg: 102 }), marca({ levantamiento: 'sentadilla', grupos: ['cuadriceps'], e1rmKg: 132 })],
      TABLAS, 'hombre', 85,
    );

    // banca top 47, sentadilla 148 cortes… 132 → 83 → top 17: media 32 → Acero I (30 ≥ 32? no) → Oro III (35).
    expect(r.global?.top).toBe(32);
    expect(r.global).toMatchObject({ tipo: 'oro', grado: 3 });
  });

  test('sin sexo o sin peso no hay rango, y lo dice si hay marcas con estándar', () => {
    const marcas = [marca({ levantamiento: 'banca' })];

    expect(calcularRangos(marcas, TABLAS, 'no_indica', 85)).toMatchObject({ global: null, faltaPerfil: true });
    expect(calcularRangos(marcas, TABLAS, 'hombre', null)).toMatchObject({ global: null, faltaPerfil: true });
    expect(calcularRangos([marca({})], TABLAS, undefined, null).faltaPerfil).toBe(false);
  });
});
