// Dobles de prueba compartidos. Datos sintéticos: nada real (ADR 0004).
import type {
  Consentimiento,
  CuestionarioCribado,
  DatosExportados,
  DispositivoCuenta,
  EfectoEjercicio,
  Material,
  ObjetivoEjercicio,
  PerfilEntreno,
  PlanActual,
  PreguntaCribado,
  PreferenciasGuardadas,
  PreferenciasPlan,
  VistaPrevia,
  Restriccion,
} from '../api/esquemas';
import type { ApiForja } from '../api/forja';
import type { AlmacenTokens, SesionGuardada } from '../sesion/almacenTokens';
import { usuarioDeToken } from '../sesion/usuarioToken';

export const DISPOSITIVO_ID = '0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e5f';
export const USUARIO_ID = '0193a3c4-7b2e-7c1d-9f00-00000000abcd';

/** Token de acceso con forma de JWT (sin firma válida: el móvil solo lee `sub`). */
export function tokenDe(usuario: string): string {
  const b64url = (texto: string) => btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url('{"alg":"EdDSA"}')}.${b64url(JSON.stringify({ sub: usuario }))}.firma`;
}

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
    leerUsuario: async () => (estado.tokenAcceso ? usuarioDeToken(estado.tokenAcceso) : null),
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
    porQue: jest.fn().mockResolvedValue([]),
    generarPlan: jest.fn().mockResolvedValue(undefined),
    perfilEntreno: jest.fn().mockResolvedValue(perfilDePrueba()),
    guardarPerfilEntreno: jest.fn().mockImplementation(async (a) => ({ ...a, porDefecto: false })),
    materiales: jest.fn().mockResolvedValue(materialesDePrueba()),
    vistaPrevia: jest.fn().mockResolvedValue(vistaPreviaDePrueba()),
    aplicarAjuste: jest.fn().mockResolvedValue(undefined),
    preferencias: jest.fn().mockResolvedValue(preferenciasDePrueba()),
    guardarPreferencias: jest.fn().mockImplementation(async (p: PreferenciasPlan) => guardadasDe(p)),
    consentimientos: jest.fn().mockResolvedValue([]),
    retirarConsentimiento: jest.fn().mockResolvedValue(undefined),
    dispositivos: jest.fn().mockResolvedValue([dispositivoCuenta()]),
    cerrarDispositivo: jest.fn().mockResolvedValue(undefined),
    borrarCuenta: jest.fn().mockResolvedValue(undefined),
    exportarDatos: jest.fn().mockResolvedValue(datosExportados()),
    tieneConsentimientoLimitaciones: jest.fn().mockResolvedValue(false),
    darConsentimientoLimitaciones: jest.fn().mockResolvedValue(undefined),
    retirarConsentimientoLimitaciones: jest.fn().mockResolvedValue(undefined),
    preguntasCribado: jest.fn().mockResolvedValue({ semanasReciente: 4, preguntas: [] }),
    cribar: jest.fn().mockResolvedValue({ nivel: 'seguir', motivos: [] }),
    evaluarBanderas: jest.fn().mockResolvedValue({ bloquea: false, banderas: [] }),
    estadoBloqueo: jest.fn().mockResolvedValue({ bloquea: false, nivel: null }),
    restricciones: jest.fn().mockResolvedValue([]),
    crearRestriccion: jest.fn().mockResolvedValue(restriccion()),
    actualizarRestriccion: jest.fn().mockResolvedValue(restriccion()),
    borrarRestriccion: jest.fn().mockResolvedValue(undefined),
    efectoRestricciones: jest.fn().mockResolvedValue([]),
    tieneConsentimientoMedidas: jest.fn().mockResolvedValue(false),
    darConsentimientoMedidas: jest.fn().mockResolvedValue(undefined),
    retirarConsentimientoMedidas: jest.fn().mockResolvedValue(undefined),
    movilidad: jest.fn().mockResolvedValue({}),
    guardarMovilidad: jest.fn().mockImplementation(async (tramos: Record<string, string | null>) =>
      Object.fromEntries(
        Object.entries(tramos)
          .filter(([, tramo]) => tramo !== null)
          .map(([test, tramo]) => [test, { tramo, hechoEn: '2026-11-18T10:00:00Z' }]),
      ),
    ),
    perfilDatos: jest.fn().mockResolvedValue({ sexo: 'no_indica' }),
    guardarPerfilDatos: jest.fn().mockImplementation(async (d) => ({
      sexo: d.sexo,
      ...(d.experiencia ? { experiencia: d.experiencia } : {}),
      ...(d.objetivo ? { objetivo: d.objetivo } : {}),
    })),
    medidas: jest.fn().mockResolvedValue({}),
    guardarMedidas: jest.fn().mockImplementation(async (m: Record<string, number | null>) =>
      Object.fromEntries(Object.entries(m).filter(([, v]) => v !== null)),
    ),
    guardarSesion: jest.fn().mockResolvedValue(undefined),
    usuarioActual: jest.fn().mockResolvedValue(USUARIO_ID),
    alCaducar: jest.fn().mockReturnValue(() => undefined),
    alBorrarseLaCuenta: jest.fn().mockReturnValue(() => undefined),
    ...cambios,
  } as jest.Mocked<ApiForja>;
}

/** Un consentimiento vivo, dado a mediodía UTC para que la fecha no cambie con la zona horaria. */
export function consentimiento(tipo: string, cambios: Partial<Consentimiento> = {}): Consentimiento {
  return { tipo, versionTexto: 'v1', dadoEn: '2026-11-17T12:00:00Z', ...cambios };
}

/** Un dispositivo de la cuenta: por defecto, el actual. */
/** La exportación de «Descargar mis datos» con todas las secciones vacías salvo la cuenta. */
export function datosExportados(cambios: Partial<DatosExportados> = {}): DatosExportados {
  return {
    formato: 1,
    generadoEn: '2026-11-20T09:30:00Z',
    cuenta: { correo: 'ana@forja.test', verificado: true, anioNacimiento: 1998, creadoEn: '2026-11-01T10:00:00Z' },
    dispositivos: [],
    consentimientos: [],
    perfilDatos: { sexo: 'no_indica' },
    medidas: null,
    perfilEntreno: { diasPorSemana: 3, minutosPorSesion: 60, material: [], porDefecto: true },
    movilidad: null,
    limitaciones: [],
    bloqueos: [],
    preferenciasPlan: { exclusiones: [], prioritarios: [], series: [], variedad: false },
    planes: [],
    sesiones: [],
    ...cambios,
  };
}

export function dispositivoCuenta(cambios: Partial<DispositivoCuenta> = {}): DispositivoCuenta {
  return { id: DISPOSITIVO_ID, nombre: 'Pixel 8', plataforma: 'movil', ultimaActividad: '2026-11-18T12:00:00Z', actual: true, ...cambios };
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
    motivo: 'Repite 80 kg: te falta completar 10 repeticiones con 2 en reserva o menos en las 4 series de trabajo.',
    musculos: [
      { grupo: 'pecho', rol: 'primario' },
      { grupo: 'triceps', rol: 'secundario' },
    ],
    primeraSesion: false,
    ...cambios,
  };
}

const SIN_SESIONES = 'Elige una carga con la que te queden unas 2 repeticiones en reserva.';

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

/** Una limitación sintética; `cambios` pisa lo que haga falta. */
export function restriccion(cambios: Partial<Restriccion> = {}): Restriccion {
  return {
    uuid: '0193a3c4-7b2e-7c1d-9f00-0000000000a1',
    region: 'rodilla',
    lado: 'izquierdo',
    tipo: 'sin_impacto',
    estado: 'actual',
    origen: 'usuario',
    ...cambios,
  };
}

/** Las siete preguntas del cribado con textos de prueba; solo las dos primeras admiten «reciente». */
export function preguntasDePrueba(): PreguntaCribado[] {
  const nombres = ['sintomas_en_esfuerzo', 'desmayo_o_mareo', 'enfermedad_conocida', 'medicacion', 'problema_musculoesqueletico', 'indicacion_supervisada', 'embarazo'];
  return nombres.map((codigo, i) => ({ codigo, texto: `Pregunta ${i + 1}`, admiteReciente: i < 2 }));
}

export function cuestionarioDePrueba(): CuestionarioCribado {
  return { semanasReciente: 4, preguntas: preguntasDePrueba() };
}

export function efectoDePrueba(): EfectoEjercicio[] {
  return [
    { idExterno: 'sentadilla_barra', nombre: 'Sentadilla con barra', decision: 'excluido', motivo: 'Sin carga axial en la rodilla.', anotaciones: [] },
    {
      idExterno: 'zancada_barra',
      nombre: 'Zancada con barra',
      decision: 'sustituido',
      motivo: 'Sin impacto en la rodilla.',
      sustituto: { idExterno: 'prensa', nombre: 'Prensa de piernas' },
      anotaciones: [],
    },
    { idExterno: 'extension', nombre: 'Extensión de cuádriceps', decision: 'con_anotaciones', anotaciones: ['Limitar carga: ligera'] },
  ];
}

/** Ajustes por defecto del backend: 3 días, 60 minutos y los 8 materiales de un gimnasio completo. */
export function perfilDePrueba(cambios: Partial<PerfilEntreno> = {}): PerfilEntreno {
  return {
    diasPorSemana: 3,
    minutosPorSesion: 60,
    material: ['barra', 'mancuerna', 'maquina', 'polea', 'peso_corporal', 'kettlebell', 'banda', 'barra_ez'],
    porDefecto: true,
    ...cambios,
  };
}

/** Lo que ofrece `GET /catalogo/materiales`: todo menos «otro». */
export function materialesDePrueba(): Material[] {
  return [
    ['barra', 'Barra'],
    ['mancuerna', 'Mancuerna'],
    ['maquina', 'Máquina'],
    ['polea', 'Polea'],
    ['peso_corporal', 'Peso corporal'],
    ['kettlebell', 'Kettlebell'],
    ['banda', 'Banda elástica'],
    ['barra_ez', 'Barra EZ'],
    ['balon_medicinal', 'Balón medicinal'],
    ['fitball', 'Fitball'],
    ['rodillo', 'Rodillo'],
  ].map(([codigo, nombre]) => ({ codigo: codigo as string, nombre: nombre as string }));
}

/** Vista previa de quitar el press de banca: entra el press inclinado y el pecho baja algo. */
export function vistaPreviaDePrueba(cambios: Partial<VistaPrevia> = {}): VistaPrevia {
  return {
    version: '0193a3c4-7b2e-7c1d-9f00-00000000f1a1',
    plan: {
      resumen: 'Es el mejor plan que cabe en lo que nos has dado.',
      notas: [],
      dias: [{ dia: 1, ejercicios: [{ idExterno: 'press_inclinado', nombre: 'Press inclinado', series: 4 }] }],
    },
    ejercicios: [
      { idExterno: 'press_banca', nombre: 'Press de banca', antes: 4, despues: 0 },
      { idExterno: 'press_inclinado', nombre: 'Press inclinado', antes: 0, despues: 4 },
      { idExterno: 'remo', nombre: 'Remo con barra', antes: 3, despues: 4 },
    ],
    musculos: [{ musculo: 'pecho', nombre: 'Pecho', antes: 12, despues: 10.5 }],
    preferencias: { exclusiones: ['press_banca'], prioritarios: [], series: {}, variedad: false },
    ...cambios,
  };
}

/** Lo que devolvería el backend al guardar `p`, con los nombres de `preferenciasDePrueba` (o el código si no está). */
function guardadasDe(p: PreferenciasPlan): PreferenciasGuardadas {
  const base = preferenciasDePrueba();
  const nombre = (codigo: string) =>
    [...base.exclusiones, ...base.prioritarios].find((e) => e.codigo === codigo)?.nombre ??
    base.series.find((s) => s.idExterno === codigo)?.nombre ??
    codigo;
  return {
    exclusiones: p.exclusiones.map((codigo) => ({ codigo, nombre: nombre(codigo) })),
    prioritarios: p.prioritarios.map((codigo) => ({ codigo, nombre: nombre(codigo) })),
    series: Object.entries(p.series).map(([idExterno, series]) => ({ idExterno, nombre: nombre(idExterno), series })),
    variedad: p.variedad,
  };
}

/** Preferencias guardadas: un ejercicio quitado, un músculo enfocado, unas series fijadas y más variedad. */
export function preferenciasDePrueba(cambios: Partial<PreferenciasGuardadas> = {}): PreferenciasGuardadas {
  return {
    exclusiones: [{ codigo: 'press_banca', nombre: 'Press de banca' }],
    prioritarios: [{ codigo: 'biceps', nombre: 'Bíceps' }],
    series: [{ idExterno: 'remo', nombre: 'Remo con barra', series: 5 }],
    variedad: true,
    ...cambios,
  };
}
