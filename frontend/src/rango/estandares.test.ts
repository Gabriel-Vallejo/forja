import { estimar, percentilDe, rangoDeTop, type TablaEstandar, tablaPara } from './estandares';

const cortes = Array.from({ length: 99 }, (_, i) => 50 + i);
const tabla = (cambios: Partial<TablaEstandar> = {}): TablaEstandar => ({
  levantamiento: 'banca',
  metrica: 'kg_1rm',
  sexo: 'hombre',
  pesoDesdeKg: 80,
  pesoHastaKg: 90,
  muestra: 1000,
  kg: cortes,
  ...cambios,
});

describe('tablaPara', () => {
  const tablas = [tabla(), tabla({ pesoDesdeKg: 90, pesoHastaKg: null }), tabla({ sexo: 'mujer' })];

  test('elige por levantamiento, sexo y franja, con el límite inferior dentro', () => {
    expect(tablaPara(tablas, 'banca', 'hombre', 80)).toBe(tablas[0]);
    expect(tablaPara(tablas, 'banca', 'hombre', 89.9)).toBe(tablas[0]);
    expect(tablaPara(tablas, 'banca', 'hombre', 90)).toBe(tablas[1]);
  });

  test('la última franja no tiene techo y lo que no existe da undefined', () => {
    expect(tablaPara(tablas, 'banca', 'hombre', 160)).toBe(tablas[1]);
    expect(tablaPara(tablas, 'sentadilla', 'hombre', 85)).toBeUndefined();
    expect(tablaPara(tablas, 'banca', 'hombre', 70)).toBeUndefined();
  });
});

describe('percentilDe y estimar', () => {
  test('cuenta los cortes alcanzados y el top nunca baja de 1', () => {
    expect(percentilDe(49, cortes)).toBe(0);
    expect(percentilDe(99.5, cortes)).toBe(50);
    expect(percentilDe(500, cortes)).toBe(99);
    expect(estimar(tabla(), 500).top).toBe(1);
    expect(estimar(tabla(), 49).top).toBe(100);
  });
});

describe('rangoDeTop', () => {
  test('el escalón más alto cuyo umbral se alcanza', () => {
    expect(rangoDeTop(26)).toEqual({ tipo: 'acero', grado: 2 });
    expect(rangoDeTop(27)).toEqual({ tipo: 'acero', grado: 1 });
    expect(rangoDeTop(3)).toEqual({ tipo: 'diamante', grado: 3 });
    expect(rangoDeTop(1)).toEqual({ tipo: 'forjado', grado: 1 });
    expect(rangoDeTop(100)).toEqual({ tipo: 'hierro', grado: 1 });
    expect(rangoDeTop(95)).toEqual({ tipo: 'hierro', grado: 1 });
  });
});
