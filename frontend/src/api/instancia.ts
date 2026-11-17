import { almacenTokens } from '../sesion/almacenTokens';
import { dispositivoActual } from '../sesion/dispositivo';
import { crearCliente } from './cliente';
import { crearApiForja } from './forja';

const urlBase = process.env.EXPO_PUBLIC_API_URL;
if (!urlBase) {
  throw new Error('Falta EXPO_PUBLIC_API_URL: copia frontend/.env.example a frontend/.env.');
}

/** La API real de la app. Los tests construyen la suya con crearCliente y crearApiForja. */
export const apiForja = crearApiForja(
  crearCliente({ urlBase: urlBase.replace(/\/+$/, ''), almacen: almacenTokens }),
  almacenTokens,
  dispositivoActual(),
);
