import Constants from 'expo-constants';
import { Platform } from 'react-native';

import type { Dispositivo } from '../api/forja';

const LARGO_MAXIMO_NOMBRE = 80;

/** Nombre y plataforma que el backend enseña en la lista de dispositivos (ADR 0009 §4). */
export function dispositivoActual(): Dispositivo {
  const esPc = Platform.OS === 'web';
  const nombre = Constants.deviceName?.trim() || (esPc ? 'PC' : `Móvil ${Platform.OS}`);
  return { nombre: nombre.slice(0, LARGO_MAXIMO_NOMBRE), plataforma: esPc ? 'pc' : 'movil' };
}
