import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import type { Restriccion } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { ConConsulta } from '../componentes/ConConsulta';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Seleccion } from '../componentes/Seleccion';
import { SiNo } from '../componentes/SiNo';
import { Texto } from '../componentes/Texto';
import { ESTADOS, LADOS, opcionesDe, PATRONES, REGIONES, TIPOS } from '../limitaciones/catalogo';
import { CLAVE_EFECTO, CLAVE_RESTRICCIONES, useRestricciones } from '../limitaciones/consultas';
import {
  type BorradorRestriccion,
  borradorDe,
  type ErroresRestriccion,
  MAXIMO_PARAMETRO,
  TIPOS_CON_PARAMETRO,
  validarRestriccion,
} from '../limitaciones/restriccion';
import { useSesion } from '../sesion/sesion';
import { ConConsentimiento } from './ConConsentimiento';

const NUEVA: BorradorRestriccion = { parametro: '', estado: 'actual', profesional: false };

function Formulario({ existente }: { existente?: Restriccion }) {
  const router = useRouter();
  const { api } = useSesion();
  const consultas = useQueryClient();
  const [borrador, setBorrador] = useState<BorradorRestriccion>(existente ? borradorDe(existente) : NUEVA);
  const [errores, setErrores] = useState<ErroresRestriccion>({});
  const [borrando, setBorrando] = useState(false);

  const alTerminar = async () => {
    await Promise.all([
      consultas.invalidateQueries({ queryKey: CLAVE_RESTRICCIONES }),
      consultas.invalidateQueries({ queryKey: CLAVE_EFECTO }),
    ]);
    router.back();
  };
  const guardar = useMutation({
    mutationFn: (datos: Parameters<typeof api.crearRestriccion>[0]) =>
      existente ? api.actualizarRestriccion(existente.uuid, datos) : api.crearRestriccion(datos),
    onSuccess: alTerminar,
  });
  const borrar = useMutation({
    mutationFn: (uuid: string) => api.borrarRestriccion(uuid),
    onSuccess: alTerminar,
  });

  const cambiar = (cambios: Partial<BorradorRestriccion>) => setBorrador({ ...borrador, ...cambios });
  const enviar = () => {
    const { errores: encontrados, datos } = validarRestriccion(borrador);
    setErrores(encontrados);
    if (datos) guardar.mutate(datos);
  };
  const pideParametro = borrador.tipo !== undefined && TIPOS_CON_PARAMETRO.includes(borrador.tipo);

  return (
    <>
      <Seleccion
        etiqueta="Región"
        opciones={opcionesDe(REGIONES)}
        valor={borrador.region}
        alCambiar={(region) => cambiar({ region })}
        opcional
        error={errores.ubicacion}
      />
      <Seleccion
        etiqueta="Patrón de movimiento"
        ayuda="Opcional si ya has elegido región."
        opciones={opcionesDe(PATRONES)}
        valor={borrador.patron}
        alCambiar={(patron) => cambiar({ patron })}
        opcional
      />
      <Seleccion etiqueta="Lado" opciones={opcionesDe(LADOS)} valor={borrador.lado} alCambiar={(lado) => cambiar({ lado })} error={errores.lado} />
      <Seleccion etiqueta="Qué te limita" opciones={opcionesDe(TIPOS)} valor={borrador.tipo} alCambiar={(tipo) => cambiar({ tipo })} error={errores.tipo} />
      {pideParametro ? (
        <CampoTexto
          etiqueta="Parámetro"
          valor={borrador.parametro}
          alCambiar={(parametro) => cambiar({ parametro })}
          error={errores.parametro}
          ayuda={`Una categoría o un RPE, por ejemplo «ligera» o «RPE 6». Sin kilos ni grados. Máximo ${MAXIMO_PARAMETRO} caracteres.`}
        />
      ) : null}
      <Seleccion etiqueta="Cómo está ahora" opciones={opcionesDe(ESTADOS)} valor={borrador.estado} alCambiar={(estado) => cambiar({ estado })} error={errores.estado} />
      <SiNo etiqueta="¿Te la indicó un profesional?" valor={borrador.profesional} alCambiar={(profesional) => cambiar({ profesional })} />
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      <Boton etiqueta="Guardar" etiquetaCargando="Guardando…" cargando={guardar.isPending} onPress={enviar} />
      {existente && !borrando ? <Boton variante="secundario" etiqueta="Borrar esta limitación" onPress={() => setBorrando(true)} /> : null}
      {existente && borrando ? (
        <Confirmacion
          mensaje="¿Borrar esta limitación? No se puede deshacer."
          etiquetaConfirmar="Sí, borrar"
          etiquetaCargando="Borrando…"
          cargando={borrar.isPending}
          error={borrar.isError ? mensajeDeError(borrar.error) : undefined}
          alConfirmar={() => borrar.mutate(existente.uuid)}
          alCancelar={() => setBorrando(false)}
        />
      ) : null}
    </>
  );
}

function Contenido({ id }: { id?: string }) {
  const restricciones = useRestricciones();
  return (
    <ConConsulta consulta={restricciones} cargando="Cargando…">
      {(lista) => {
        const existente = id ? lista.find((r) => r.uuid === id) : undefined;
        if (id && !existente) return <Texto>Esta limitación ya no existe.</Texto>;
        return <Formulario existente={existente} />;
      }}
    </ConConsulta>
  );
}

/** Crea una limitación o, con `id`, edita la que ya existe: el mismo formulario para las dos cosas. */
export function FormularioLimitacion({ id }: { id?: string }) {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo={id ? 'Editar limitación' : 'Nueva limitación'} />
      <ConConsentimiento>
        <Contenido id={id} />
      </ConConsentimiento>
    </Pantalla>
  );
}
