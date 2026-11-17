import { e1rmEpley, kgConComa } from './e1rm';
import { nombreRango, nombreRangoMayusculas, topDe } from './rango';

describe('e1RM de Epley', () => {
  test.each([
    [80.5, 8, 102],
    [100, 1, 103],
    [60, 30, 120],
    [20, 10, 27],
  ])('%p kg × %p reps ≈ %p kg', (peso, reps, esperado) => {
    expect(e1rmEpley(peso, reps)).toBe(esperado);
  });

  test('el peso se escribe con un decimal y coma', () => {
    expect(kgConComa(80.5)).toBe('80,5');
    expect(kgConComa(100)).toBe('100,0');
  });
});

describe('nombres de rango', () => {
  test('nivel y grado, forjado y sin estándar', () => {
    expect(nombreRango('acero', 2)).toBe('Acero II');
    expect(nombreRango('forjado', 3)).toBe('Forjado');
    expect(nombreRango('sin', 1)).toBe('Sin estándar');
    expect(nombreRangoMayusculas('sin', 1)).toBe('SIN RANGO');
    expect(topDe('acero', 2)).toBe(26);
    expect(topDe('forjado', 3)).toBe(1);
  });
});
