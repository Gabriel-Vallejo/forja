import { addNetworkStateListener, getNetworkStateAsync, type NetworkState } from 'expo-network';

/** Con datos a medias (`undefined`) se da por conectada: reenviar de más es seguro, quedarse sin enviar no. */
const conectada = (estado: NetworkState) => estado.isConnected !== false && estado.isInternetReachable !== false;

/** Avisa de la conectividad al empezar y en cada cambio (expo-network). Devuelve cómo dejar de escuchar. */
export function escucharRed(oyente: (conectada: boolean) => void): () => void {
  let activa = true;
  getNetworkStateAsync().then(
    (estado) => activa && oyente(conectada(estado)),
    () => undefined, // Sin estado inicial basta con los cambios.
  );
  const suscripcion = addNetworkStateListener((estado) => oyente(conectada(estado)));
  return () => {
    activa = false;
    suscripcion.remove();
  };
}
