import type { AlmacenTokens } from '../sesion/almacenTokens';
import type { ClienteApi } from './cliente';
import { ErrorApi, RESPUESTA_INESPERADA } from './errores';
import {
  type Consentimiento,
  type DatosRestriccion,
  type DispositivoCuenta,
  type EfectoEjercicio,
  type EstadoBloqueo,
  esquemaConsentimientos,
  esquemaDatosSesion,
  esquemaDispositivos,
  esquemaEfecto,
  esquemaEstadoBloqueo,
  esquemaMensaje,
  esquemaPlanActual,
  esquemaPorQue,
  esquemaPlanGenerado,
  esquemaCuestionarioCribado,
  esquemaResultadoBanderas,
  esquemaResultadoCribado,
  esquemaRestriccion,
  esquemaRestricciones,
  type PlanActual,
  type Razon,
  type Plataforma,
  type CuestionarioCribado,
  type ResultadoBanderas,
  type ResultadoCribado,
  type Restriccion,
  type SolicitudBanderas,
  type SolicitudCribado,
  type SolicitudRegistro,
  type AjustesEntreno,
  type Material,
  type PerfilEntreno,
  esquemaMateriales,
  esquemaPerfilEntreno,
  esquemaSesionGuardada,
  type SesionEntrada,
  type TipoConsentimiento,
  esquemaMovilidad,
  type Movilidad,
  esquemaDatosPerfil,
  type DatosPerfil,
  type CuerpoDatosPerfil,
  esquemaMedidas,
  type Medidas,
  type CuerpoMedidas,
  type TramosMovilidad,
} from './esquemas';

const NO_ENCONTRADO = 404;
const TIPO_LIMITACIONES = 'limitaciones';
const TIPO_MEDIDAS = 'medidas_salud';

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
  /** «Por qué este ejercicio» en ese día del plan actual; no se guarda en el móvil (puede citar limitaciones). */
  porQue(dia: number, idExterno: string): Promise<Razon[]>;
  /** Sin ajustes, usa los guardados del perfil (o los de por defecto del backend). */
  generarPlan(ajustes?: AjustesEntreno): Promise<void>;
  /** Días, minutos y material del plan; sin guardar, los de por defecto con `porDefecto: true`. */
  perfilEntreno(): Promise<PerfilEntreno>;
  guardarPerfilEntreno(ajustes: AjustesEntreno): Promise<PerfilEntreno>;
  materiales(): Promise<Material[]>;
  // Limitaciones (ADR 0007). Datos de salud: ninguna de estas respuestas se guarda fuera de la memoria.
  /** Los consentimientos vivos (los retirados no salen). */
  consentimientos(): Promise<Consentimiento[]>;
  /** Borra también lo que dependía de él (RF-46); hoy, solo `limitaciones` guarda algo. */
  retirarConsentimiento(tipo: TipoConsentimiento): Promise<void>;
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
  /** Dispositivos con sesión viva; `actual` marca el que hace la petición. */
  dispositivos(): Promise<DispositivoCuenta[]>;
  /** Cierra la sesión de otro dispositivo de la cuenta (RF-43). */
  cerrarDispositivo(id: string): Promise<void>;
  /** Pide la contraseña, borra la cuenta en el backend (en cascada) y la sesión local (RF-47). */
  borrarCuenta(contrasena: string): Promise<void>;
  // Tests de movilidad (RF-08), con el consentimiento de medidas de salud (ADR 0004). Igual: solo en memoria.
  tieneConsentimientoMedidas(): Promise<boolean>;
  darConsentimientoMedidas(versionTexto: string): Promise<void>;
  /** Borra también los tests de movilidad guardados (lo hace el backend). */
  retirarConsentimientoMedidas(): Promise<void>;
  movilidad(): Promise<Movilidad>;
  guardarMovilidad(tramos: TramosMovilidad): Promise<Movilidad>;
  /** Sexo, experiencia y objetivo: preferencias, sin consentimiento. */
  perfilDatos(): Promise<DatosPerfil>;
  guardarPerfilDatos(datos: CuerpoDatosPerfil): Promise<DatosPerfil>;
  /** Altura, peso y circunferencias con el % de grasa estimado: piden el consentimiento de medidas (403 sin él). */
  medidas(): Promise<Medidas>;
  guardarMedidas(medidas: CuerpoMedidas): Promise<Medidas>;
  /** Upsert idempotente de una sesión del outbox (ADR 0008 §2): reenviar lo mismo no duplica nada. */
  guardarSesion(uuid: string, sesion: SesionEntrada): Promise<void>;
  /** `sub` del token guardado: de quién son las sesiones que se envían. Null sin sesión iniciada. */
  usuarioActual(): Promise<string | null>;
  alCaducar(oyente: () => void): () => void;
}

export function crearApiForja(cliente: ClienteApi, almacen: AlmacenTokens, dispositivo: Dispositivo): ApiForja {
  const leerPerfilEntreno = () => cliente.pedir({ ruta: '/api/v1/perfil/entreno', esquema: esquemaPerfilEntreno });
  const tieneConsentimiento = async (tipo: string) => {
    const consentimientos = await cliente.pedir({ ruta: '/api/v1/consentimientos', esquema: esquemaConsentimientos });
    return consentimientos.some((c) => c.tipo === tipo);
  };
  const darConsentimiento = async (tipo: string, versionTexto: string) => {
    await cliente.pedir({ metodo: 'POST', ruta: '/api/v1/consentimientos', cuerpo: { tipo, versionTexto } });
  };
  const retirarConsentimiento = async (tipo: TipoConsentimiento) => {
    await cliente.pedir({ metodo: 'DELETE', ruta: `/api/v1/consentimientos/${tipo}` });
  };
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
    async porQue(dia, idExterno) {
      const ruta = `/api/v1/planes/actual/ejercicios/${dia}/${encodeURIComponent(idExterno)}/por-que`;
      return (await cliente.pedir({ ruta, esquema: esquemaPorQue })).porQue;
    },
    async generarPlan(ajustes) {
      const { diasPorSemana, minutosPorSesion, material } = ajustes ?? (await leerPerfilEntreno());
      await cliente.pedir({
        metodo: 'POST',
        ruta: '/api/v1/rutinas/generar',
        cuerpo: { diasPorSemana, minutosPorSesion, material, exclusiones: [], prioritarios: [] },
        esquema: esquemaPlanGenerado,
      });
    },
    perfilEntreno: leerPerfilEntreno,
    guardarPerfilEntreno: (ajustes) =>
      cliente.pedir({ metodo: 'PUT', ruta: '/api/v1/perfil/entreno', cuerpo: ajustes, esquema: esquemaPerfilEntreno }),
    materiales: () => cliente.pedir({ ruta: '/api/v1/catalogo/materiales', esquema: esquemaMateriales }),
    consentimientos: () => cliente.pedir({ ruta: '/api/v1/consentimientos', esquema: esquemaConsentimientos }),
    retirarConsentimiento,
    tieneConsentimientoLimitaciones: () => tieneConsentimiento(TIPO_LIMITACIONES),
    darConsentimientoLimitaciones: (versionTexto) => darConsentimiento(TIPO_LIMITACIONES, versionTexto),
    retirarConsentimientoLimitaciones: () => retirarConsentimiento(TIPO_LIMITACIONES),
    tieneConsentimientoMedidas: () => tieneConsentimiento(TIPO_MEDIDAS),
    darConsentimientoMedidas: (versionTexto) => darConsentimiento(TIPO_MEDIDAS, versionTexto),
    retirarConsentimientoMedidas: () => retirarConsentimiento(TIPO_MEDIDAS),
    movilidad: () => cliente.pedir({ ruta: '/api/v1/movilidad', esquema: esquemaMovilidad }),
    guardarMovilidad: (tramos) =>
      cliente.pedir({ metodo: 'PUT', ruta: '/api/v1/movilidad', cuerpo: tramos, esquema: esquemaMovilidad }),
    perfilDatos: () => cliente.pedir({ ruta: '/api/v1/perfil/datos', esquema: esquemaDatosPerfil }),
    guardarPerfilDatos: (datos) =>
      cliente.pedir({ metodo: 'PUT', ruta: '/api/v1/perfil/datos', cuerpo: datos, esquema: esquemaDatosPerfil }),
    medidas: () => cliente.pedir({ ruta: '/api/v1/perfil/medidas', esquema: esquemaMedidas }),
    guardarMedidas: (medidas) =>
      cliente.pedir({ metodo: 'PUT', ruta: '/api/v1/perfil/medidas', cuerpo: medidas, esquema: esquemaMedidas }),
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
    async guardarSesion(uuid, sesion) {
      const guardada = await cliente.pedir({
        metodo: 'PUT',
        ruta: `/api/v1/sesiones/${encodeURIComponent(uuid)}`,
        cuerpo: sesion,
        esquema: esquemaSesionGuardada,
      });
      if (guardada.uuid !== uuid) {
        throw new ErrorApi(200, RESPUESTA_INESPERADA);
      }
    },
    dispositivos: () => cliente.pedir({ ruta: '/api/v1/dispositivos', esquema: esquemaDispositivos }),
    async cerrarDispositivo(id) {
      await cliente.pedir({ metodo: 'DELETE', ruta: `/api/v1/dispositivos/${encodeURIComponent(id)}` });
    },
    async borrarCuenta(contrasena) {
      await cliente.pedir({ metodo: 'DELETE', ruta: '/api/v1/cuenta', cuerpo: { contrasena } });
      // La cuenta ya no existe: no hay a quién cerrarle la sesión, solo olvidarla aquí.
      await almacen.borrar();
    },
    usuarioActual: () => almacen.leerUsuario(),
    alCaducar: (oyente) => cliente.alCaducar(oyente),
  };
}
