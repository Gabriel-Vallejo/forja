// Consultas y acciones de la pantalla de privacidad. Nada va a almacenamiento local: la caché de TanStack Query
// vive en memoria y `ProveedorSesion` la vacía al salir.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { TipoConsentimiento } from '../api/esquemas';
import { olvidarLimitaciones } from '../limitaciones/consultas';
import { olvidarMovilidad } from '../movilidad/consultas';
import { useSincronizacion } from '../registro/sincronizacion';
import { useSesion } from '../sesion/sesion';
import { esTipoConocido } from './catalogo';
import { CLAVE_CONSENTIMIENTOS, CLAVE_DISPOSITIVOS } from './claves';

export function useConsentimientos() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_CONSENTIMIENTOS, queryFn: () => api.consentimientos() });
}

export function useDispositivos() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_DISPOSITIVOS, queryFn: () => api.dispositivos() });
}

/** Retira un consentimiento; con `limitaciones` deja la caché sin lo que el backend borra (RF-46). */
export function useRetirarConsentimiento() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (tipo: TipoConsentimiento) => api.retirarConsentimiento(tipo),
    onSuccess: (_, tipo) =>
      tipo === 'limitaciones'
        ? olvidarLimitaciones(consultas)
        : tipo === 'medidas_salud'
          ? olvidarMovilidad(consultas)
          : consultas.invalidateQueries({ queryKey: CLAVE_CONSENTIMIENTOS }),
  });
}

export function useCerrarDispositivo() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.cerrarDispositivo(id),
    onSuccess: () => consultas.invalidateQueries({ queryKey: CLAVE_DISPOSITIVOS }),
  });
}

/** Borra la cuenta y, con ella, lo que el móvil guardaba de esa cuenta (RF-47). La contraseña no se guarda. */
export function useBorrarCuenta() {
  const { borrarCuenta } = useSesion();
  const { outbox } = useSincronizacion();
  return useMutation({
    mutationFn: (contrasena: string) =>
      borrarCuenta(contrasena, async (usuario) => {
        if (outbox && usuario) await outbox.borrarDatosDe(usuario);
      }),
  });
}

/** Lo que dice la fila de privacidad del perfil: «N de 5 · M dispositivos». */
export function useResumenPrivacidad(): string {
  const consentimientos = useConsentimientos();
  const dispositivos = useDispositivos();
  if (consentimientos.isError || dispositivos.isError) return 'No disponible';
  if (consentimientos.isPending || dispositivos.isPending) return '…';
  const dados = consentimientos.data.filter((c) => esTipoConocido(c.tipo)).length;
  const n = dispositivos.data.length;
  return `${dados} de 5 · ${n} ${n === 1 ? 'dispositivo' : 'dispositivos'}`;
}
