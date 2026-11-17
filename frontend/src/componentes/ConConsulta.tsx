import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { mensajeDeError } from '../api/errores';
import { Aviso } from './Aviso';
import { Boton } from './Boton';
import { Texto } from './Texto';

interface PropsConConsulta<T> {
  consulta: UseQueryResult<T>;
  cargando?: string;
  children: (datos: T) => ReactNode;
}

/** Resuelve cargando y error con reintento de una consulta; con datos, pinta `children`. */
export function ConConsulta<T>({ consulta, cargando = 'Cargando…', children }: PropsConConsulta<T>) {
  if (consulta.isPending) return <Texto tono="secundario">{cargando}</Texto>;
  if (consulta.isError) {
    return (
      <>
        <Aviso tipo="error" mensaje={mensajeDeError(consulta.error)} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={() => consulta.refetch()} />
      </>
    );
  }
  return <>{children(consulta.data)}</>;
}
