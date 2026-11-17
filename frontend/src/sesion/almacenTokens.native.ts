import * as SecureStore from 'expo-secure-store';

import type { AlmacenTokens, SesionGuardada } from './almacenTokens';

const CLAVE_REFRESCO = 'forja.tokenRefresco';
const CLAVE_DISPOSITIVO = 'forja.dispositivoId';

// El acceso dura 15 minutos y nunca toca disco.
let accesoEnMemoria: string | null = null;

export const almacenTokens: AlmacenTokens = {
  leerAcceso: () => accesoEnMemoria,
  leerRefresco: () => SecureStore.getItemAsync(CLAVE_REFRESCO),
  leerDispositivo: () => SecureStore.getItemAsync(CLAVE_DISPOSITIVO),
  async guardar({ tokenAcceso, tokenRefresco, dispositivoId }: SesionGuardada) {
    await SecureStore.setItemAsync(CLAVE_REFRESCO, tokenRefresco);
    await SecureStore.setItemAsync(CLAVE_DISPOSITIVO, dispositivoId);
    accesoEnMemoria = tokenAcceso;
  },
  async borrar() {
    accesoEnMemoria = null;
    await SecureStore.deleteItemAsync(CLAVE_REFRESCO);
  },
};
