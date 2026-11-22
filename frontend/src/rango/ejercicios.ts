import type { Grupo } from '../componentes/cuerpoTrazados';
import type { Levantamiento } from './estandares';

/**
 * Los ejercicios de la calculadora: solo los que tienen tabla de OpenPowerlifting. Dominadas, press militar o curl
 * vuelven cuando haya datos con fuente. `desplazamiento` encuadra la miniatura en el músculo.
 */
export const EJERCICIOS_CON_ESTANDAR: readonly {
  nombre: string;
  musculo: string;
  levantamiento: Levantamiento;
  grupo: Grupo;
  desplazamiento: number;
  desplazamientoGrande: number;
}[] = [
  { nombre: 'Press de banca con barra', musculo: 'Pecho · tríceps · hombro', levantamiento: 'banca', grupo: 'pecho', desplazamiento: -14, desplazamientoGrande: -22 },
  { nombre: 'Sentadilla con barra', musculo: 'Cuádriceps · glúteos', levantamiento: 'sentadilla', grupo: 'cuadriceps', desplazamiento: -60, desplazamientoGrande: -96 },
  { nombre: 'Peso muerto convencional', musculo: 'Espalda baja · glúteos · isquios', levantamiento: 'peso_muerto', grupo: 'cuadriceps', desplazamiento: -40, desplazamientoGrande: -64 },
];

export const SERIE_INICIAL = { peso: 80.5, repeticiones: 8 } as const;
