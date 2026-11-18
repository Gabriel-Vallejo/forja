// Contrato del almacén local del registro de series (ADR 0008 §4). Dos implementaciones: SQLite en el móvil
// (almacenSqlite.ts) y en memoria para los tests (almacenMemoria.ts). La lógica vive en outbox.ts y
// sincronizador.ts; el almacén solo guarda y lee.
// «dolor» es un dato de salud (RGPD art. 9): solo vive aquí y en la petición al backend. Nunca a un log.

/**
 * `en_curso`: entrenando. `pendiente`: cerrada (o con cambios) y por enviar. `enviando`: hay un PUT en vuelo.
 * `confirmada`: el servidor la tiene tal cual. `rechazada`: el servidor dijo que no (4xx); se enseña y no se borra.
 */
export type EstadoSesionLocal = 'en_curso' | 'pendiente' | 'enviando' | 'confirmada' | 'rechazada';

export interface SesionLocal {
  uuid: string;
  /** `sub` del token de quien la empezó. Solo se envía con un token de ese mismo usuario. */
  usuario: string;
  diaPlan: number | null;
  /** `uuid` del plan con el que se hizo (`planVersion` en el backend). */
  planVersion: string | null;
  /** Reloj del móvil, ISO 8601. */
  realizadaEn: string;
  cerradaEn: string | null;
  estado: EstadoSesionLocal;
  intentos: number;
  /** Milisegundos: antes de esto no se reintenta salvo que se fuerce (la red acaba de volver). */
  proximoIntento: number | null;
  /** Mensaje del último fallo, para enseñarlo («revisa esta sesión»). Nunca lleva datos de las series. */
  ultimoError: string | null;
  /** Milisegundos. Para purgar las confirmadas pasados unos días. */
  confirmadaEn: number | null;
}

export interface SerieLocal {
  uuid: string;
  sesion: string;
  /** `idExterno` del ejercicio. */
  ejercicio: string;
  orden: number;
  pesoKg: number;
  repeticiones: number;
  rir: number | null;
  rpe: number | null;
  dolor: number | null;
  calentamiento: boolean;
  corrigeA: string | null;
  anulada: boolean;
  realizadaEn: string;
}

/** Carga calculada sin red con la regla de la siguiente sesión, hasta que el servidor mande (ADR 0008 §5). */
export interface CargaSiguiente {
  usuario: string;
  ejercicio: string;
  cargaKg: number;
  /** Plan sobre el que se calculó: con un plan nuevo del servidor deja de valer. */
  planVersion: string;
  /** Sesión que la produjo: cuando se confirma, el servidor ya la tiene en cuenta. */
  sesion: string;
}

/** Última respuesta buena de `GET /planes/actual` de un usuario, para entrenar sin red. `json` tal cual llegó. */
export interface PlanGuardado {
  usuario: string;
  planUuid: string;
  json: string;
  /** Milisegundos. */
  guardadoEn: number;
}

export type CambiosSesion = Partial<Omit<SesionLocal, 'uuid' | 'usuario'>>;

export interface FiltroSesiones {
  usuario?: string;
  estados?: readonly EstadoSesionLocal[];
}

export interface AlmacenRegistro {
  insertarSesion(sesion: SesionLocal): Promise<void>;
  actualizarSesion(uuid: string, cambios: CambiosSesion): Promise<void>;
  /** Borra la sesión con sus series y sus cargas siguientes. */
  borrarSesion(uuid: string): Promise<void>;
  leerSesion(uuid: string): Promise<SesionLocal | null>;
  /** En orden de creación. */
  listarSesiones(filtro?: FiltroSesiones): Promise<SesionLocal[]>;
  insertarSerie(serie: SerieLocal): Promise<void>;
  /** En orden de inserción: una corrección siempre va detrás de lo que corrige (el backend lo exige). */
  listarSeries(sesion: string): Promise<SerieLocal[]>;
  /** Una por usuario y ejercicio: la nueva sustituye a la anterior. */
  guardarCarga(carga: CargaSiguiente): Promise<void>;
  listarCargas(usuario: string): Promise<CargaSiguiente[]>;
  borrarCarga(usuario: string, ejercicio: string): Promise<void>;
  /** Uno por usuario: el nuevo sustituye al anterior. */
  guardarPlan(plan: PlanGuardado): Promise<void>;
  leerPlan(usuario: string): Promise<PlanGuardado | null>;
  borrarPlan(usuario: string): Promise<void>;
}

/** Borra todo lo local de un usuario: sus sesiones (con series y cargas) y su plan guardado. Lo de otros no se toca. */
export async function borrarDatosDeUsuario(almacen: AlmacenRegistro, usuario: string): Promise<void> {
  for (const s of await almacen.listarSesiones({ usuario })) await almacen.borrarSesion(s.uuid);
  await almacen.borrarPlan(usuario);
}
