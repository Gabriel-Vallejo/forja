// Interfaz del almacén de tokens (ADR 0009). La implementación sale de almacenTokens.native.ts (móvil) o
// almacenTokens.web.ts (PC): Metro y Jest eligen el archivo por plataforma.
// El token de acceso vive solo en memoria; el de refresco y el id del dispositivo, en almacenamiento seguro.
// Nunca AsyncStorage ni localStorage.

export interface SesionGuardada {
  tokenAcceso: string;
  tokenRefresco: string;
  dispositivoId: string;
}

export interface AlmacenTokens {
  leerAcceso(): string | null;
  leerRefresco(): Promise<string | null>;
  /** El `sid` del último inicio de sesión, para que el backend reutilice el mismo dispositivo. */
  leerDispositivo(): Promise<string | null>;
  /** El usuario (`sub` del token de acceso) de la sesión guardada; sobrevive al reinicio, se olvida al borrar. */
  leerUsuario(): Promise<string | null>;
  guardar(sesion: SesionGuardada): Promise<void>;
  /** Cierra la sesión local: borra acceso, refresco y usuario, pero conserva el id del dispositivo. */
  borrar(): Promise<void>;
}

export declare const almacenTokens: AlmacenTokens;
