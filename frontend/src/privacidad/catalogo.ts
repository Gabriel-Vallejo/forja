// Qué cubre cada consentimiento y qué se borra al retirarlo (RF-45, RF-46). Lo de «se borra» sale del backend de
// hoy: `limitaciones` (restricciones, bloqueo por bandera y dolor de las series) y `medidas_salud` (tests de movilidad).
// Si otro tipo empieza a guardar datos, su texto de aquí y su listener del backend cambian a la vez.
import { TIPOS_CONSENTIMIENTO, type TipoConsentimiento } from '../api/esquemas';

export interface InfoConsentimiento {
  tipo: TipoConsentimiento;
  nombre: string;
  /** Para qué sirve, en una línea. */
  cubre: string;
  /** Pantalla donde se da; sin ella, el tipo aún no tiene función en la app y no se ofrece darlo. */
  rutaParaDar?: string;
  /** Qué se borra al retirarlo, tal como se lo decimos antes de confirmar. */
  alRetirar: string;
  etiquetaRetirar: string;
}

const SIN_DATOS = 'Hoy Forja no guarda nada que dependa de este permiso, así que retirarlo no borra ningún dato. La app sigue funcionando igual.';

const INFO: Record<TipoConsentimiento, Omit<InfoConsentimiento, 'tipo'>> = {
  fotos_cuerpo: {
    nombre: 'Fotos de cuerpo',
    cubre: 'Procesar una foto tuya para sacar tus proporciones. La foto no se guarda: solo quedan los tramos.',
    alRetirar: SIN_DATOS,
    etiquetaRetirar: 'Sí, retirar',
  },
  medidas_salud: {
    nombre: 'Medidas y peso',
    cubre: 'Guardar tus tests de movilidad (y, más adelante, tu peso y tus medidas) para ajustar el plan.',
    rutaParaDar: '/movilidad',
    alRetirar: 'Se borrarán tus tests de movilidad. Forja sigue funcionando sin ellos: tu plan se arma sin tener en cuenta tu movilidad.',
    etiquetaRetirar: 'Sí, retirar y borrar',
  },
  lesiones: {
    nombre: 'Lesiones',
    cubre: 'Guardar lesiones en tu perfil. Hoy Forja usa «Limitaciones» para esto y no lo necesita.',
    alRetirar: SIN_DATOS,
    etiquetaRetirar: 'Sí, retirar',
  },
  limitaciones: {
    nombre: 'Limitaciones',
    cubre: 'Guardar tus limitaciones funcionales (por ejemplo, «sin carga axial» en la rodilla izquierda) para elegir ejercicios compatibles.',
    rutaParaDar: '/limitaciones',
    alRetirar:
      'Se borrarán tus limitaciones guardadas, el aviso de bandera roja que recuerda Forja y el dolor que hayas anotado en tus series. Forja sigue funcionando sin ellas: tu plan se arma sin tener en cuenta ninguna limitación. No se puede deshacer.',
    etiquetaRetirar: 'Sí, retirar y borrar',
  },
  suplementos: {
    nombre: 'Suplementos',
    cubre: 'Guardar los suplementos que tomas para el escáner de riesgo por ingredientes declarados.',
    alRetirar: SIN_DATOS,
    etiquetaRetirar: 'Sí, retirar',
  },
};

export const CONSENTIMIENTOS: readonly InfoConsentimiento[] = TIPOS_CONSENTIMIENTO.map((tipo) => ({ tipo, ...INFO[tipo] }));

export const esTipoConocido = (tipo: string): tipo is TipoConsentimiento => (TIPOS_CONSENTIMIENTO as readonly string[]).includes(tipo);
