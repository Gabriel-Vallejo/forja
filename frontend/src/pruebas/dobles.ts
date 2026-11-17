// Dobles de prueba compartidos. Datos sintéticos: nada real (ADR 0004).
import type { ApiForja } from '../api/forja';
import type { AlmacenTokens, SesionGuardada } from '../sesion/almacenTokens';

export const DISPOSITIVO_ID = '0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e5f';

export function datosSesion(n: number) {
  return {
    tokenAcceso: `acceso-${n}`,
    tokenRefresco: `refresco-${n}`,
    expiraEn: '2026-11-16T12:15:00Z',
    dispositivoId: DISPOSITIVO_ID,
  };
}

/** Almacén en memoria con el mismo contrato que el nativo. */
export function almacenEnMemoria(inicial: Partial<SesionGuardada> = {}): AlmacenTokens & { estado: Partial<SesionGuardada> } {
  const estado: Partial<SesionGuardada> = { ...inicial };
  return {
    estado,
    leerAcceso: () => estado.tokenAcceso ?? null,
    leerRefresco: async () => estado.tokenRefresco ?? null,
    leerDispositivo: async () => estado.dispositivoId ?? null,
    async guardar(sesion) {
      Object.assign(estado, sesion);
    },
    async borrar() {
      delete estado.tokenAcceso;
      delete estado.tokenRefresco;
    },
  };
}

/** Respuesta mínima de fetch con cuerpo JSON. */
export function respuesta(status: number, cuerpo?: unknown): Response {
  return {
    status,
    ok: status >= 200 && status < 300,
    json: async () => {
      if (cuerpo === undefined) throw new SyntaxError('sin cuerpo');
      return cuerpo;
    },
  } as Response;
}

export function apiFalsa(cambios: Partial<ApiForja> = {}): jest.Mocked<ApiForja> {
  return {
    haySesionGuardada: jest.fn().mockResolvedValue(true),
    registrar: jest.fn().mockResolvedValue('ok'),
    entrar: jest.fn().mockResolvedValue(undefined),
    cerrarSesion: jest.fn().mockResolvedValue(undefined),
    planActual: jest.fn().mockResolvedValue(null),
    generarPlan: jest.fn().mockResolvedValue(undefined),
    alCaducar: jest.fn().mockReturnValue(() => undefined),
    ...cambios,
  } as jest.Mocked<ApiForja>;
}
