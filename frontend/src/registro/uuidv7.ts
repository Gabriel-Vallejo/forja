import { getRandomBytes } from 'expo-crypto';

/** UUID con versión 7 y variante RFC 9562, en minúsculas. */
export const ES_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

const BYTES_TIEMPO = 6;
const BYTES_AZAR = 10;

interface FuentesUuid {
  /** Milisegundos desde 1970. */
  reloj: () => number;
  azar: (bytes: number) => Uint8Array;
}

/**
 * UUID v7 (RFC 9562 §5.7): 48 bits de milisegundos y 74 de azar. Ordenable por tiempo entre milisegundos; dentro
 * del mismo milisegundo el orden es aleatorio, por eso el almacén ordena por orden de inserción y no por uuid.
 */
export function crearUuidv7({ reloj, azar }: FuentesUuid): () => string {
  return () => {
    const bytes = new Uint8Array(BYTES_TIEMPO + BYTES_AZAR);
    let ms = Math.floor(reloj());
    // Sin operadores de bits: el tiempo pasa de 32 bits.
    for (let i = BYTES_TIEMPO - 1; i >= 0; i--) {
      bytes[i] = ms % 256;
      ms = Math.floor(ms / 256);
    }
    bytes.set(azar(BYTES_AZAR).subarray(0, BYTES_AZAR), BYTES_TIEMPO);
    bytes[6] = 0x70 | (bytes[6]! & 0x0f);
    bytes[8] = 0x80 | (bytes[8]! & 0x3f);
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  };
}

export const uuidv7 = crearUuidv7({ reloj: Date.now, azar: getRandomBytes });
