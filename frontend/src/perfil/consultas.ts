// Consultas del perfil manual. Sexo, experiencia y objetivo son preferencias; las medidas son dato de salud (ADR 0004):
// solo viven en la memoria de la caché, que `ProveedorSesion` vacía al cerrar sesión, y se olvidan al retirar el
// consentimiento (`olvidarMovilidad`).
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { CuerpoDatosPerfil, CuerpoMedidas } from '../api/esquemas';
import { CLAVE_MEDIDAS, useConsentimientoMedidas } from '../movilidad/consultas';
import { useSesion } from '../sesion/sesion';
import { textoGrasa } from './textos';

const CLAVE_DATOS = ['perfil', 'datos'] as const;

export function usePerfilDatos() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_DATOS, queryFn: () => api.perfilDatos() });
}

/** Guarda y deja en caché lo que devuelve el backend; el sexo cambia la estimación de grasa, así que se relee. */
export function useGuardarPerfilDatos() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (datos: CuerpoDatosPerfil) => api.guardarPerfilDatos(datos),
    onSuccess: async (guardados) => {
      consultas.setQueryData(CLAVE_DATOS, guardados);
      await consultas.invalidateQueries({ queryKey: CLAVE_MEDIDAS });
    },
  });
}

/** Sin consentimiento el backend contesta 403: `habilitada` evita pedirlas. */
export function useMedidas(habilitada = true) {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_MEDIDAS, queryFn: () => api.medidas(), enabled: habilitada });
}

export function useGuardarMedidas() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (medidas: CuerpoMedidas) => api.guardarMedidas(medidas),
    onSuccess: (guardadas) => consultas.setQueryData(CLAVE_MEDIDAS, guardadas),
  });
}

export interface ResumenMedidas {
  valor: string;
  /** Falta algo por rellenar: se pinta con el color de aviso. */
  pendiente: boolean;
}

/** Lo que dice la fila «Medidas» del perfil: «Sin dar», «Pendientes», «Guardadas» o la estimación de grasa. */
export function useResumenMedidas(): ResumenMedidas {
  const consentimiento = useConsentimientoMedidas();
  const medidas = useMedidas(consentimiento.data === true);
  if (consentimiento.isPending) return { valor: '…', pendiente: false };
  if (consentimiento.isError) return { valor: 'No disponible', pendiente: false };
  if (!consentimiento.data) return { valor: 'Pendientes', pendiente: true };
  if (medidas.isPending) return { valor: '…', pendiente: false };
  if (medidas.isError) return { valor: 'No disponible', pendiente: false };
  if (medidas.data.grasaEstimada) return { valor: textoGrasa(medidas.data.grasaEstimada), pendiente: false };
  const algunaMedida = [medidas.data.alturaCm, medidas.data.pesoKg].some((v) => v !== undefined);
  return algunaMedida ? { valor: 'Guardadas', pendiente: false } : { valor: 'Pendientes', pendiente: true };
}
