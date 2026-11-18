import { getRandomBytes } from 'expo-crypto';

import { crearUuidv7, uuidv7, ES_UUID } from './uuidv7';

const ceros = (n: number) => new Uint8Array(n);
const unos = (n: number) => new Uint8Array(n).fill(0xff);

describe('UUID v7', () => {
  test('pone la marca de tiempo en los 48 primeros bits, versión 7 y variante 10', () => {
    const generar = crearUuidv7({ reloj: () => 0x0193a3c47b2e, azar: ceros });

    expect(generar()).toBe('0193a3c4-7b2e-7000-8000-000000000000');
  });

  test('la versión y la variante no dependen del azar', () => {
    const id = crearUuidv7({ reloj: () => 1, azar: unos })();

    expect(id).toBe('00000000-0001-7fff-bfff-ffffffffffff');
    expect(id).toMatch(ES_UUID);
  });

  test('es ordenable por tiempo: uno generado más tarde es mayor como texto', () => {
    let ahora = Date.UTC(2026, 10, 18);
    const generar = crearUuidv7({ reloj: () => ahora, azar: getRandomBytes });
    const ids = Array.from({ length: 50 }, () => {
      ahora += 1;
      return generar();
    });

    expect([...ids].sort()).toEqual(ids);
    expect(new Set(ids).size).toBe(ids.length);
  });

  test('la versión por defecto usa la aleatoriedad de expo-crypto', () => {
    expect(uuidv7()).toMatch(ES_UUID);
  });
});
