// Dobles de prueba compartidos. Datos sintéticos: nada real (ADR 0004).
import type { ObjetivoEjercicio, PlanActual } from '../api/esquemas';
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

/** Un ejercicio del plan con valores por defecto; `cambios` pisa lo que haga falta. */
export function objetivo(cambios: Partial<ObjetivoEjercicio> = {}): ObjetivoEjercicio {
  return {
    idExterno: 'press_banca',
    nombre: 'Press de banca',
    orden: 1,
    series: 4,
    repeticionesMin: 6,
    repeticionesMax: 10,
    rirObjetivo: 2,
    cargaKg: 80,
    incrementoKg: 2.5,
    estado: 'acumulando',
    motivo: 'Repite 80 kg: te falta completar 10 repeticiones con 2 en reserva o menos en todas las series de trabajo.',
    musculos: [
      { grupo: 'pecho', rol: 'primario' },
      { grupo: 'triceps', rol: 'secundario' },
    ],
    primeraSesion: false,
    ...cambios,
  };
}

const SIN_SESIONES = 'Todavía no hay sesiones de este ejercicio: elige una carga con la que te queden unas 2 repeticiones en reserva.';

/**
 * Plan sintético de dos días (1 TORSO, 2 PIERNA); con `primeraSemana`, ningún ejercicio tiene sesiones aún.
 * `diaDeHoy` es el que toca hoy.
 */
export function planDePrueba(primeraSemana = false, diaDeHoy = 1): PlanActual {
  const motivo = primeraSemana ? { motivo: SIN_SESIONES, primeraSesion: true } : {};
  const dorsal: ObjetivoEjercicio['musculos'] = [
    { grupo: 'dorsal', rol: 'primario' },
    { grupo: 'brazos', rol: 'secundario' },
  ];
  const piernas: ObjetivoEjercicio['musculos'] = [
    { grupo: 'cuadriceps', rol: 'primario' },
    { grupo: 'gluteos', rol: 'secundario' },
  ];
  return {
    uuid: '0193a3c4-7b2e-7c1d-9f00-000000000001',
    creadoEn: '2026-11-16T10:00:00Z',
    estado: 'factible',
    diaDeHoy,
    dias: [
      {
        dia: 1,
        nombre: 'TORSO',
        ejercicios: [
          objetivo(motivo),
          objetivo({ idExterno: 'remo', nombre: 'Remo en máquina', orden: 2, cargaKg: 60, musculos: dorsal, ...motivo }),
          objetivo({ idExterno: 'fondos', nombre: 'Fondos en paralelas', orden: 3, incrementoKg: 0, cargaKg: undefined, ...motivo }),
        ],
      },
      {
        dia: 2,
        nombre: 'PIERNA',
        ejercicios: [objetivo({ idExterno: 'sentadilla', nombre: 'Sentadilla', cargaKg: 100, musculos: piernas, ...motivo })],
      },
    ],
  };
}
