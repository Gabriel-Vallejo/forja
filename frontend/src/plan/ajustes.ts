// Ajustes del plan (días, minutos y material). Son preferencias de entreno, no datos de salud: se guardan en el
// backend y la caché de TanStack Query solo los tiene en memoria.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { AjustesEntreno, PerfilEntreno } from '../api/esquemas';
import { useSesion } from '../sesion/sesion';

export const CLAVE_AJUSTES = ['ajustes-plan'] as const;
const CLAVE_PERFIL_ENTRENO = [...CLAVE_AJUSTES, 'perfil'] as const;
const CLAVE_MATERIALES = [...CLAVE_AJUSTES, 'materiales'] as const;

/** Días por semana que admite el backend (`EntradaRutina`). */
export const DIAS_POSIBLES = [2, 3, 4, 5, 6] as const;
/** De 30 a 120 minutos de 15 en 15. */
export const MINUTOS_POSIBLES = [30, 45, 60, 75, 90, 105, 120] as const;

/** Los 7 materiales de «gimnasio completo» (el defecto del backend): los que tienen ejercicios curados. */
const MATERIAL_COMPLETO = ['barra', 'barra_ez', 'mancuerna', 'maquina', 'polea', 'kettlebell', 'peso_corporal'];

export function usePerfilEntreno() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_PERFIL_ENTRENO, queryFn: () => api.perfilEntreno() });
}

/** El catálogo solo cambia con una migración del backend. */
export function useMateriales() {
  const { api } = useSesion();
  return useQuery({ queryKey: CLAVE_MATERIALES, queryFn: () => api.materiales(), staleTime: Infinity });
}

/** Guarda los ajustes y vuelve a leerlos. El plan actual no cambia: cuenta al generar o regenerar. */
export function useGuardarAjustes() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  return useMutation({
    mutationFn: (ajustes: AjustesEntreno) => api.guardarPerfilEntreno(ajustes),
    onSuccess: (guardado) => consultas.setQueryData(CLAVE_PERFIL_ENTRENO, guardado),
  });
}

/** Tiene los 7; un perfil antiguo que además guarda la banda (sin ejercicios) sigue siendo completo. */
export function esMaterialCompleto(material: readonly string[]): boolean {
  return MATERIAL_COMPLETO.every((m) => material.includes(m));
}

/** «3 × 60 min». */
export function resumenDiasYMinutos(perfil: PerfilEntreno): string {
  return `${perfil.diasPorSemana} × ${perfil.minutosPorSesion} min`;
}

/** «Completo» o «N tipos». */
export function resumenMaterial(perfil: PerfilEntreno): string {
  return esMaterialCompleto(perfil.material) ? 'Completo' : `${perfil.material.length} tipos`;
}

/** Los ajustes en una frase, para los textos de generar y regenerar. */
export function frasePerfil(perfil: PerfilEntreno): string {
  const material = esMaterialCompleto(perfil.material)
    ? 'el material de un gimnasio completo'
    : `${perfil.material.length} ${perfil.material.length === 1 ? 'tipo' : 'tipos'} de material`;
  return `${perfil.diasPorSemana} días por semana, ${perfil.minutosPorSesion} minutos por sesión y ${material}`;
}
