// Abre el almacén del registro según la plataforma: SQLite en el móvil (almacenLocal.native.ts) y nada en web/PC,
// donde no hay registro sin conexión (ADR 0006): ahí devuelve null y la app sigue sin outbox.
import type { AlmacenRegistro } from './almacen';

export declare function abrirAlmacenLocal(): Promise<AlmacenRegistro | null>;
