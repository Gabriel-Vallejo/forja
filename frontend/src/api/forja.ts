import type { AlmacenTokens } from '../sesion/almacenTokens';
import type { ClienteApi } from './cliente';
import { ErrorApi } from './errores';
import {
  esquemaDatosSesion,
  esquemaMensaje,
  esquemaPlanActual,
  esquemaPlanGenerado,
  type PlanActual,
  type Plataforma,
  type SolicitudRegistro,
  type SolicitudRutina,
} from './esquemas';

const NO_ENCONTRADO = 404;

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
    alCaducar: (oyente) => cliente.alCaducar(oyente),
  };
}
