import * as SecureStore from 'expo-secure-store';

import type { AlmacenTokens, SesionGuardada } from './almacenTokens';
import { usuarioDeToken } from './usuarioToken';

const CLAVE_REFRESCO = 'forja.tokenRefresco';
const CLAVE_DISPOSITIVO = 'forja.dispositivoId';
const CLAVE_USUARIO = 'forja.usuario';

// El acceso dura 15 minutos y nunca toca disco.
let accesoEnMemoria: string | null = null;

export const almacenTokens: AlmacenTokens = {
  leerAcceso: () => accesoEnMemoria,
  leerRefresco: () => SecureStore.getItemAsync(CLAVE_REFRESCO),
  leerDispositivo: () => SecureStore.getItemAsync(CLAVE_DISPOSITIVO),
  leerUsuario: () => SecureStore.getItemAsync(CLAVE_USUARIO),
  async guardar({ tokenAcceso, tokenRefresco, dispositivoId }: SesionGuardada) {
    await SecureStore.setItemAsync(CLAVE_REFRESCO, tokenRefresco);
    await SecureStore.setItemAsync(CLAVE_DISPOSITIVO, dispositivoId);
    // El usuario se guarda aparte porque el acceso solo vive en memoria: sin red y recién abierta la app, el outbox
    // tiene que saber de quién son las sesiones sin poder renovar el token.
    const usuario = usuarioDeToken(tokenAcceso);
    if (usuario) await SecureStore.setItemAsync(CLAVE_USUARIO, usuario);
    accesoEnMemoria = tokenAcceso;
  },
  async borrar() {
    accesoEnMemoria = null;
    await SecureStore.deleteItemAsync(CLAVE_REFRESCO);
    await SecureStore.deleteItemAsync(CLAVE_USUARIO);
  },
};
