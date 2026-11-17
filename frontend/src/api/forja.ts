import type { AlmacenTokens } from '../sesion/almacenTokens';
import type { ClienteApi } from './cliente';
import { ErrorApi } from './errores';
import {
  type DatosRestriccion,
  type EfectoEjercicio,
  type EstadoBloqueo,
  esquemaConsentimientos,
  esquemaDatosSesion,
  esquemaEfecto,
  esquemaEstadoBloqueo,
  esquemaMensaje,
  esquemaPlanActual,
  esquemaPlanGenerado,
  esquemaCuestionarioCribado,
  esquemaResultadoBanderas,
  esquemaResultadoCribado,
  esquemaRestriccion,
  esquemaRestricciones,
  type PlanActual,
  type Plataforma,
  type CuestionarioCribado,
  type ResultadoBanderas,
  type ResultadoCribado,
  type Restriccion,
  type SolicitudBanderas,
  type SolicitudCribado,
  type SolicitudRegistro,
  type SolicitudRutina,
} from './esquemas';

const NO_ENCONTRADO = 404;
const TIPO_LIMITACIONES = 'limitaciones';

/** Lo que se genera con «Generar mi plan» mientras no haya pantalla para elegirlo. */
export const RUTINA_POR_DEFECTO: SolicitudRutina = {
  diasPorSemana: 3,
  minutosPorSesion: 60,
  material: ['barra', 'barra_ez', 'mancuerna', 'maquina', 'polea', 'kettlebell', 'banda', 'peso_corporal'],
  exclusiones: [],
  prioritarios: [],
};

export interface Dispositivo {
  nombre: string;
  plataforma: Plataforma;
}

export interface Credenciales {
  correo: string;
  contrasena: string;
}

export interface ApiForja {
  haySesionGuardada(): Promise<boolean>;
  registrar(datos: SolicitudRegistro): Promise<string>;
  entrar(credenciales: Credenciales): Promise<void>;
  cerrarSesion(): Promise<void>;
  planActual(): Promise<PlanActual | null>;
  generarPlan(): Promise<void>;
  // Limitaciones (ADR 0007). Datos de salud: ninguna de estas respuestas se guarda fuera de la memoria.
  tieneConsentimientoLimitaciones(): Promise<boolean>;
  darConsentimientoLimitaciones(versionTexto: string): Promise<void>;
  /** Borra también todas las limitaciones guardadas (lo hace el backend). */
  retirarConsentimientoLimitaciones(): Promise<void>;
  preguntasCribado(): Promise<CuestionarioCribado>;
  cribar(solicitud: SolicitudCribado): Promise<ResultadoCribado>;
  evaluarBanderas(solicitud: SolicitudBanderas): Promise<ResultadoBanderas>;
  /** Si el backend recuerda una bandera roja o un cribado urgente (solo el nivel, nunca las respuestas). */
  estadoBloqueo(): Promise<EstadoBloqueo>;
  restricciones(): Promise<Restriccion[]>;
  crearRestriccion(datos: DatosRestriccion): Promise<Restriccion>;
  actualizarRestriccion(uuid: string, datos: DatosRestriccion): Promise<Restriccion>;
  borrarRestriccion(uuid: string): Promise<void>;
  efectoRestricciones(): Promise<EfectoEjercicio[]>;
  alCaducar(oyente: () => void): () => void;
}

export function crearApiForja(cliente: ClienteApi, almacen: AlmacenTokens, dispositivo: Dispositivo): ApiForja {
  return {
    async haySesionGuardada() {
      return (await almacen.leerRefresco()) !== null;
    },
    async registrar(datos) {
      const { mensaje } = await cliente.pedir({
        metodo: 'POST',
        ruta: '/api/v1/auth/registro',
        cuerpo: datos,
        esquema: esquemaMensaje,
        autenticada: false,
      });
      return mensaje;
    },
    async entrar({ correo, contrasena }) {
      // Se reutiliza el `sid` del último inicio de sesión para no crear un dispositivo nuevo cada vez.
      const dispositivoId = await almacen.leerDispositivo();
      const sesion = await cliente.pedir({
        metodo: 'POST',
        ruta: '/api/v1/auth/login',
        cuerpo: {
          correo,
          contrasena,
          dispositivoId,
          nombreDispositivo: dispositivo.nombre,
          plataforma: dispositivo.plataforma,
        },
        esquema: esquemaDatosSesion,
        autenticada: false,
      });
      await almacen.guardar(sesion);
    },
    async cerrarSesion() {
      try {
        await cliente.pedir({ metodo: 'POST', ruta: '/api/v1/auth/cerrar-sesion' });
      } finally {
        // Aunque falle la red, la sesión local se cierra.
        await almacen.borrar();
      }
    },
    async planActual() {
      try {
        return await cliente.pedir({ ruta: '/api/v1/planes/actual', esquema: esquemaPlanActual });
      } catch (e) {
        if (e instanceof ErrorApi && e.estado === NO_ENCONTRADO) {
          return null;
        }
        throw e;
      }
    },
    async generarPlan() {
      await cliente.pedir({
        metodo: 'POST',
        ruta: '/api/v1/rutinas/generar',
        cuerpo: RUTINA_POR_DEFECTO,
        esquema: esquemaPlanGenerado,
      });
    },
    async tieneConsentimientoLimitaciones() {
      const consentimientos = await cliente.pedir({ ruta: '/api/v1/consentimientos', esquema: esquemaConsentimientos });
      return consentimientos.some((c) => c.tipo === TIPO_LIMITACIONES);
    },
    async darConsentimientoLimitaciones(versionTexto) {
      await cliente.pedir({
        metodo: 'POST',
        ruta: '/api/v1/consentimientos',
        cuerpo: { tipo: TIPO_LIMITACIONES, versionTexto },
      });
    },
    async retirarConsentimientoLimitaciones() {
      await cliente.pedir({ metodo: 'DELETE', ruta: `/api/v1/consentimientos/${TIPO_LIMITACIONES}` });
    },
    preguntasCribado: () => cliente.pedir({ ruta: '/api/v1/cribado/preguntas', esquema: esquemaCuestionarioCribado }),
    cribar: (solicitud) =>
      cliente.pedir({ metodo: 'POST', ruta: '/api/v1/cribado', cuerpo: solicitud, esquema: esquemaResultadoCribado }),
    evaluarBanderas: (solicitud) =>
      cliente.pedir({ metodo: 'POST', ruta: '/api/v1/banderas', cuerpo: solicitud, esquema: esquemaResultadoBanderas }),
    estadoBloqueo: () => cliente.pedir({ ruta: '/api/v1/banderas/estado', esquema: esquemaEstadoBloqueo }),
    restricciones: () => cliente.pedir({ ruta: '/api/v1/restricciones', esquema: esquemaRestricciones }),
    crearRestriccion: (datos) =>
      cliente.pedir({ metodo: 'POST', ruta: '/api/v1/restricciones', cuerpo: datos, esquema: esquemaRestriccion }),
    actualizarRestriccion: (uuid, datos) =>
      cliente.pedir({ metodo: 'PUT', ruta: `/api/v1/restricciones/${uuid}`, cuerpo: datos, esquema: esquemaRestriccion }),
    async borrarRestriccion(uuid) {
      await cliente.pedir({ metodo: 'DELETE', ruta: `/api/v1/restricciones/${uuid}` });
    },
    efectoRestricciones: () => cliente.pedir({ ruta: '/api/v1/restricciones/efecto', esquema: esquemaEfecto }),
    alCaducar: (oyente) => cliente.alCaducar(oyente),
  };
}
