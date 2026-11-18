import type { AlmacenRegistro } from './almacen';

/** En web/PC no hay registro sin conexión (ADR 0006): sin almacén, la app no monta el outbox. */
export async function abrirAlmacenLocal(): Promise<AlmacenRegistro | null> {
  return null;
}
