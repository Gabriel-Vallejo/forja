import { z } from 'zod';

import { esquemaPlanActual, type ObjetivoEjercicio, type PlanActual } from '../api/esquemas';
import { type AlmacenRegistro, borrarDatosDeUsuario, type SerieLocal, type SesionLocal } from './almacen';
import { cargaSiguiente } from './reglaSiguiente';
import { uuidv7 } from './uuidv7';
import { ultimaVersion, ultimaVersionValida } from './versiones';

/** Error con un mensaje que la interfaz puede enseñar tal cual. */
export class ErrorRegistro extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'ErrorRegistro';
  }
}

const ERROR_GENERICO = 'No se ha podido usar el registro del móvil. Inténtalo otra vez.';

/** Texto para la interfaz: el de un `ErrorRegistro` tal cual; de otro error nada (podría llevar datos de salud). */
export const mensajeDeRegistro = (e: unknown) => (e instanceof ErrorRegistro ? e.message : ERROR_GENERICO);

const centesimas = (n: number) => Math.round(n * 100) / 100;
const decimas = (n: number) => Math.round(n * 10) / 10;
const entero = (mensaje: string, max: number) => z.number({ error: mensaje }).int(mensaje).min(0, mensaje).max(max, mensaje);

// Los mismos límites que `SerieEntrada` del backend: lo que pase aquí no acabará rechazado por rango.
const esquemaDatosSerie = z.object({
  ejercicio: z.string().trim().min(1, 'Falta el ejercicio.').max(120, 'El ejercicio no es válido.'),
  orden: entero('El orden no es válido.', 1000),
  pesoKg: z.number().min(0, 'El peso va de 0 a 1000 kg.').max(1000, 'El peso va de 0 a 1000 kg.').transform(centesimas),
  repeticiones: entero('Las repeticiones van de 0 a 100.', 100),
  rir: entero('El RIR va de 0 a 10.', 10).optional(),
  rpe: z.number().min(1, 'El RPE va de 1 a 10.').max(10, 'El RPE va de 1 a 10.').transform(decimas).optional(),
  dolor: entero('El dolor va de 0 a 10.', 10).optional(),
  calentamiento: z.boolean().optional(),
});

/** Lo que la pantalla manda al registrar o corregir una serie. */
export type DatosSerie = z.input<typeof esquemaDatosSerie>;

export interface Contadores {
  /** Sin confirmar todavía: pendientes y en envío. */
  pendientes: number;
  rechazadas: number;
}

export interface Outbox {
  empezarSesion(datos?: { diaPlan?: number; planVersion?: string }): Promise<SesionLocal>;
  registrarSerie(sesion: string, datos: DatosSerie): Promise<SerieLocal>;
  /** Serie nueva con `corrigeA` a la última versión de `serie`. */
  corregirSerie(sesion: string, serie: string, datos: DatosSerie): Promise<SerieLocal>;
  /** Serie nueva `anulada` que apunta a la última versión de `serie`. */
  anularSerie(sesion: string, serie: string): Promise<SerieLocal>;
  /** Pasa a `pendiente`, aplica la regla de la siguiente con los `objetivos` del día y lanza el envío. */
  cerrarSesion(sesion: string, objetivos?: readonly ObjetivoEjercicio[]): Promise<void>;
  /** Una sesión en curso y sin series (la que se abandona vacía) o una rechazada que no se quiere corregir. */
  descartarSesion(sesion: string): Promise<void>;
  /** Una rechazada vuelve a `pendiente` desde cero y se lanza el envío. */
  reintentarSesion(sesion: string): Promise<void>;
  sesionEnCurso(): Promise<SesionLocal | null>;
  /** Última versión válida de cada serie de la sesión. */
  seriesValidas(sesion: string): Promise<SerieLocal[]>;
  /** Cerradas sin confirmar (pendientes, en envío y rechazadas), de la más antigua a la más reciente. */
  porRevisar(): Promise<SesionLocal[]>;
  contadores(): Promise<Contadores>;
  /** Carga siguiente calculada sin red por ejercicio (`idExterno` → kg). */
  cargasSiguientes(): Promise<Map<string, number>>;
  /** Borra lo local de una cuenta ya eliminada (sesiones pendientes o rechazadas, series, cargas y plan). Recibe el usuario porque al borrar la cuenta ya no hay sesión de la que leerlo. */
  borrarDatosDe(usuario: string): Promise<void>;
  /** Al retirar el consentimiento de limitaciones: el dolor de las series del usuario actual se borra del móvil. */
  olvidarDolor(): Promise<void>;
  /** Con un plan recién leído del servidor, manda el servidor: borra las cargas locales que ya no valen. */
  conciliarConPlan(plan: PlanActual): Promise<void>;
  /** Guarda el plan recién leído del servidor para entrenar sin red; null (no hay plan) borra el guardado. */
  guardarPlan(plan: PlanActual | null): Promise<void>;
  /** El último plan guardado del usuario actual, o null si no hay (o no cumple el esquema). */
  planGuardado(): Promise<{ plan: PlanActual; guardadoEn: number } | null>;
}

export interface OpcionesOutbox {
  almacen: AlmacenRegistro;
  /** `sub` del token guardado, o null sin sesión iniciada. */
  usuarioActual: () => Promise<string | null>;
  reloj?: () => number;
  nuevoId?: () => string;
  /** Se llama tras cerrar o reintentar una sesión de entreno (para lanzar el envío). */
  alCerrar?: () => void;
}

/** El esquema convierte la `cargaKg` nula del servidor en `undefined`; al guardar se vuelve a null para que relea igual. */
const conCargaNula = (clave: string, valor: unknown) => (clave === 'cargaKg' && valor === undefined ? null : valor);

const SIN_CUENTA = 'Entra en tu cuenta para registrar el entreno.';
const NO_EXISTE = 'No existe esa sesión.';

export function crearOutbox({ almacen, usuarioActual, reloj = Date.now, nuevoId = uuidv7, alCerrar }: OpcionesOutbox): Outbox {
  const ahoraIso = () => new Date(reloj()).toISOString();

  async function exigirUsuario(): Promise<string> {
    const usuario = await usuarioActual();
    if (!usuario) throw new ErrorRegistro(SIN_CUENTA);
    return usuario;
  }

  /** La sesión, solo si es del usuario actual: nunca se toca lo de otra cuenta. */
  async function sesionPropia(uuid: string): Promise<SesionLocal> {
    const [usuario, sesion] = await Promise.all([exigirUsuario(), almacen.leerSesion(uuid)]);
    if (!sesion || sesion.usuario !== usuario) throw new ErrorRegistro(NO_EXISTE);
    return sesion;
  }

  function validar(datos: DatosSerie) {
    const resultado = esquemaDatosSerie.safeParse(datos);
    if (!resultado.success) throw new ErrorRegistro(resultado.error.issues[0]?.message ?? 'La serie no es válida.');
    return resultado.data;
  }

  async function anadir(sesion: SesionLocal, serie: Omit<SerieLocal, 'uuid' | 'sesion' | 'realizadaEn'>): Promise<SerieLocal> {
    // Primero el estado y luego la serie: si la app se cierra entre medias, como mucho se reenvía de más.
    if (sesion.estado !== 'en_curso' && sesion.estado !== 'enviando') {
      await almacen.actualizarSesion(sesion.uuid, { estado: 'pendiente', intentos: 0, proximoIntento: null, ultimoError: null });
    }
    const nueva: SerieLocal = { ...serie, uuid: nuevoId(), sesion: sesion.uuid, realizadaEn: ahoraIso() };
    await almacen.insertarSerie(nueva);
    return nueva;
  }

  function desdeDatos(datos: DatosSerie, corrigeA: string | null): Omit<SerieLocal, 'uuid' | 'sesion' | 'realizadaEn'> {
    const d = validar(datos);
    return {
      ejercicio: d.ejercicio,
      orden: d.orden,
      pesoKg: d.pesoKg,
      repeticiones: d.repeticiones,
      rir: d.rir ?? null,
      rpe: d.rpe ?? null,
      dolor: d.dolor ?? null,
      calentamiento: d.calentamiento ?? false,
      corrigeA,
      anulada: false,
    };
  }

  /** Última versión de la serie dentro de la sesión; no se puede seguir una cadena ya anulada. */
  async function ultimaDe(sesion: string, serie: string): Promise<SerieLocal> {
    const ultima = ultimaVersion(await almacen.listarSeries(sesion), serie);
    if (!ultima) throw new ErrorRegistro('No existe esa serie en la sesión.');
    if (ultima.anulada) throw new ErrorRegistro('Esa serie ya está anulada.');
    return ultima;
  }

  async function delUsuario(estados: SesionLocal['estado'][]): Promise<SesionLocal[]> {
    const usuario = await usuarioActual();
    return usuario ? almacen.listarSesiones({ usuario, estados }) : [];
  }

  return {
    async empezarSesion({ diaPlan, planVersion } = {}) {
      const usuario = await exigirUsuario();
      if ((await almacen.listarSesiones({ usuario, estados: ['en_curso'] })).length > 0) {
        throw new ErrorRegistro('Ya tienes una sesión en curso.');
      }
      const sesion: SesionLocal = {
        uuid: nuevoId(),
        usuario,
        diaPlan: diaPlan ?? null,
        planVersion: planVersion ?? null,
        realizadaEn: ahoraIso(),
        cerradaEn: null,
        estado: 'en_curso',
        intentos: 0,
        proximoIntento: null,
        ultimoError: null,
        confirmadaEn: null,
      };
      await almacen.insertarSesion(sesion);
      return sesion;
    },

    async registrarSerie(uuid, datos) {
      const sesion = await sesionPropia(uuid);
      return anadir(sesion, desdeDatos(datos, null));
    },

    async corregirSerie(uuid, serie, datos) {
      const sesion = await sesionPropia(uuid);
      const ultima = await ultimaDe(uuid, serie);
      return anadir(sesion, desdeDatos(datos, ultima.uuid));
    },

    async anularSerie(uuid, serie) {
      const sesion = await sesionPropia(uuid);
      const ultima = await ultimaDe(uuid, serie);
      // La anulación lleva lo obligatorio de la serie que anula; el dolor no hace falta y no se copia.
      return anadir(sesion, {
        ejercicio: ultima.ejercicio,
        orden: ultima.orden,
        pesoKg: ultima.pesoKg,
        repeticiones: ultima.repeticiones,
        rir: null,
        rpe: null,
        dolor: null,
        calentamiento: ultima.calentamiento,
        corrigeA: ultima.uuid,
        anulada: true,
      });
    },

    async cerrarSesion(uuid, objetivos = []) {
      const sesion = await sesionPropia(uuid);
      if (sesion.estado !== 'en_curso') throw new ErrorRegistro('Esa sesión ya está cerrada.');
      const series = await almacen.listarSeries(uuid);
      if (series.length === 0) throw new ErrorRegistro('No has registrado ninguna serie: descarta la sesión.');
      await almacen.actualizarSesion(uuid, { estado: 'pendiente', cerradaEn: ahoraIso() });
      const { planVersion } = sesion;
      if (planVersion) {
        const validas = ultimaVersionValida(series);
        for (const objetivo of objetivos) {
          const cargaKg = cargaSiguiente(objetivo, validas);
          if (cargaKg !== undefined) {
            await almacen.guardarCarga({ usuario: sesion.usuario, ejercicio: objetivo.idExterno, cargaKg, planVersion, sesion: uuid });
          }
        }
      }
      alCerrar?.();
    },

    async descartarSesion(uuid) {
      const sesion = await sesionPropia(uuid);
      const vacia = sesion.estado === 'en_curso' && (await almacen.listarSeries(uuid)).length === 0;
      if (!vacia && sesion.estado !== 'rechazada') {
        throw new ErrorRegistro('Solo se descarta una sesión en curso y sin series, o una rechazada.');
      }
      await almacen.borrarSesion(uuid);
    },

    async reintentarSesion(uuid) {
      const sesion = await sesionPropia(uuid);
      if (sesion.estado !== 'rechazada') throw new ErrorRegistro('Solo se reintenta una sesión rechazada.');
      await almacen.actualizarSesion(uuid, { estado: 'pendiente', intentos: 0, proximoIntento: null, ultimoError: null });
      alCerrar?.();
    },

    async sesionEnCurso() {
      return (await delUsuario(['en_curso'])).at(-1) ?? null;
    },

    async seriesValidas(uuid) {
      await sesionPropia(uuid);
      return ultimaVersionValida(await almacen.listarSeries(uuid));
    },

    porRevisar: () => delUsuario(['pendiente', 'enviando', 'rechazada']),

    async contadores() {
      const sesiones = await delUsuario(['pendiente', 'enviando', 'rechazada']);
      const rechazadas = sesiones.filter((s) => s.estado === 'rechazada').length;
      return { pendientes: sesiones.length - rechazadas, rechazadas };
    },

    async cargasSiguientes() {
      const usuario = await usuarioActual();
      const cargas = usuario ? await almacen.listarCargas(usuario) : [];
      return new Map(cargas.map((c) => [c.ejercicio, c.cargaKg]));
    },

    async guardarPlan(plan) {
      const usuario = await usuarioActual();
      if (!usuario) return;
      if (!plan) return almacen.borrarPlan(usuario);
      await almacen.guardarPlan({ usuario, planUuid: plan.uuid, json: JSON.stringify(plan, conCargaNula), guardadoEn: reloj() });
    },

    async planGuardado() {
      const usuario = await usuarioActual();
      const guardado = usuario ? await almacen.leerPlan(usuario) : null;
      if (!guardado) return null;
      // Viene del disco: se valida igual que si viniera del servidor.
      let datos: unknown;
      try {
        datos = JSON.parse(guardado.json);
      } catch {
        return null;
      }
      const plan = esquemaPlanActual.safeParse(datos);
      return plan.success ? { plan: plan.data, guardadoEn: guardado.guardadoEn } : null;
    },

    borrarDatosDe: (usuario) => borrarDatosDeUsuario(almacen, usuario),

    async olvidarDolor() {
      const usuario = await usuarioActual();
      if (usuario) await almacen.olvidarDolor(usuario);
    },

    async conciliarConPlan(plan) {
      const usuario = await usuarioActual();
      if (!usuario) return;
      for (const carga of await almacen.listarCargas(usuario)) {
        const sesion = carga.planVersion === plan.uuid ? await almacen.leerSesion(carga.sesion) : null;
        if (!sesion || sesion.estado === 'confirmada') await almacen.borrarCarga(usuario, carga.ejercicio);
      }
    },
  };
}
