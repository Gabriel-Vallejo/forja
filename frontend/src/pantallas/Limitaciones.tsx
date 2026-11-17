import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { Restriccion } from '../api/esquemas';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { FilaEnlace } from '../componentes/FilaEnlace';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { ESTADOS, LADOS, ORIGENES, PATRONES, REGIONES, TIPOS } from '../limitaciones/catalogo';
import {
  CLAVE_BLOQUEO,
  CLAVE_CONSENTIMIENTO,
  CLAVE_EFECTO,
  CLAVE_RESTRICCIONES,
  useBloqueoSalud,
  useRestricciones,
} from '../limitaciones/consultas';
import { MAXIMO_POR_USUARIO } from '../limitaciones/restriccion';
import { FINALIDAD, NOTA_PLAN } from '../limitaciones/textos';
import { useSesion } from '../sesion/sesion';
import { type Theme, useTheme } from '../theme';
import { ConConsentimiento } from './ConConsentimiento';
import { AvisoBloqueo } from '../componentes/AvisoBloqueo';
import { RegenerarPlan } from '../plan/RegenerarPlan';
import { ConsentimientoLimitaciones } from './ConsentimientoLimitaciones';

/** «Rodilla», «Sentadilla» o «Rodilla · Sentadilla» si nombra las dos. */
export function nombreLimitacion(r: Restriccion): string {
  return [r.region ? REGIONES[r.region] : undefined, r.patron ? PATRONES[r.patron] : undefined].filter(Boolean).join(' · ');
}

const detalleLimitacion = (r: Restriccion) =>
  [TIPOS[r.tipo], LADOS[r.lado], ESTADOS[r.estado], r.origen === 'profesional' ? ORIGENES.profesional : undefined, r.parametro]
    .filter(Boolean)
    .join(' · ');

function FilaLimitacion({ restriccion, alBorrar }: { restriccion: Restriccion; alBorrar: () => void }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const nombre = nombreLimitacion(restriccion);
  return (
    <View style={estilos.tarjeta}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Editar limitación de ${nombre}`}
        accessibilityHint={detalleLimitacion(restriccion)}
        onPress={() => router.push({ pathname: '/limitacion', params: { id: restriccion.uuid } })}
        style={({ pressed }) => [estilos.editar, pressed && estilos.pulsada]}
      >
        <Texto peso="700">{nombre}</Texto>
        <Texto estilo="nota" tono="secundario">{detalleLimitacion(restriccion)}</Texto>
      </Pressable>
      <Boton variante="texto" etiqueta={`Borrar limitación de ${nombre}`} onPress={alBorrar} />
    </View>
  );
}

function ListaLimitaciones({ restricciones }: { restricciones: Restriccion[] }) {
  const router = useRouter();
  const { api } = useSesion();
  const consultas = useQueryClient();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [porBorrar, setPorBorrar] = useState<string | null>(null);
  const borrar = useMutation({
    mutationFn: (uuid: string) => api.borrarRestriccion(uuid),
    onSuccess: async () => {
      setPorBorrar(null);
      await Promise.all([
        consultas.invalidateQueries({ queryKey: CLAVE_RESTRICCIONES }),
        consultas.invalidateQueries({ queryKey: CLAVE_EFECTO }),
      ]);
    },
  });
  const llena = restricciones.length >= MAXIMO_POR_USUARIO;

  return (
    <>
      {restricciones.length === 0 ? <Texto tono="secundario">Todavía no has añadido ninguna limitación.</Texto> : null}
      <View accessibilityRole="list" style={estilos.lista}>
        {restricciones.map((r) => (
          <View key={r.uuid} style={estilos.lista}>
            <FilaLimitacion
              restriccion={r}
              alBorrar={() => {
                borrar.reset();
                setPorBorrar(r.uuid);
              }}
            />
            {porBorrar === r.uuid ? (
              <Confirmacion
                mensaje="¿Borrar esta limitación? No se puede deshacer."
                etiquetaConfirmar="Sí, borrar"
                etiquetaCargando="Borrando…"
                cargando={borrar.isPending}
                error={borrar.isError ? mensajeDeError(borrar.error) : undefined}
                alConfirmar={() => borrar.mutate(r.uuid)}
                alCancelar={() => setPorBorrar(null)}
              />
            ) : null}
          </View>
        ))}
      </View>
      {llena ? (
        <Texto tono="secundario">{`Has llegado al máximo de ${MAXIMO_POR_USUARIO} limitaciones.`}</Texto>
      ) : (
        <Boton etiqueta="Añadir limitación" icono="mas" onPress={() => router.push('/limitacion')} />
      )}
    </>
  );
}

function RetirarConsentimiento({ cuantas }: { cuantas: number }) {
  const { api } = useSesion();
  const consultas = useQueryClient();
  const [confirmando, setConfirmando] = useState(false);
  const retirar = useMutation({
    mutationFn: () => api.retirarConsentimientoLimitaciones(),
    onSuccess: () => {
      // El backend borra las limitaciones al retirarlo: fuera de la memoria también.
      consultas.setQueryData(CLAVE_CONSENTIMIENTO, false);
      consultas.removeQueries({ queryKey: CLAVE_RESTRICCIONES });
      consultas.removeQueries({ queryKey: CLAVE_EFECTO });
      // Retirar el consentimiento borra también el bloqueo (RF-46).
      consultas.setQueryData(CLAVE_BLOQUEO, { bloquea: false, nivel: null });
    },
  });

  if (!confirmando) {
    return <Boton variante="texto" etiqueta="Retirar mi consentimiento" onPress={() => setConfirmando(true)} />;
  }
  const guardadas = cuantas === 1 ? 'tu limitación guardada' : `tus ${cuantas} limitaciones guardadas`;
  return (
    <Confirmacion
      mensaje={`Se borrarán ${guardadas}. Forja seguirá funcionando sin ellas. No se puede deshacer.`}
      etiquetaConfirmar="Sí, retirar y borrar"
      etiquetaCargando="Borrando…"
      cargando={retirar.isPending}
      error={retirar.isError ? mensajeDeError(retirar.error) : undefined}
      alConfirmar={() => retirar.mutate()}
      alCancelar={() => setConfirmando(false)}
    />
  );
}

function Contenido() {
  const router = useRouter();
  const restricciones = useRestricciones();
  const bloqueo = useBloqueoSalud();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <>
      {bloqueo.data?.bloquea ? <AvisoBloqueo nivel={bloqueo.data.nivel} conEnlace={false} /> : null}
      <Texto>{FINALIDAD}</Texto>
      <Texto estilo="nota" tono="secundario">{NOTA_PLAN}</Texto>
      <ConConsulta consulta={restricciones} cargando="Cargando tus limitaciones…">
        {(lista) => (
          <>
            <ListaLimitaciones restricciones={lista} />
            <View style={estilos.enlaces}>
              <FilaEnlace etiqueta="Qué cambia en mis ejercicios" detalle="Qué ejercicios se excluyen, se sustituyen o llevan aviso" alPulsar={() => router.push('/efecto')} />
              <FilaEnlace etiqueta="Cribado previo al ejercicio" detalle="Siete preguntas de sí o no" alPulsar={() => router.push('/cribado')} />
              <FilaEnlace etiqueta="Cuestionario de dolor" detalle="Dolor por zona y señales de aviso" alPulsar={() => router.push('/dolor')} />
            </View>
            <RegenerarPlan etiqueta="Regenerar mi plan con mis limitaciones" avisoBloqueo={false} />
            <RetirarConsentimiento cuantas={lista.length} />
          </>
        )}
      </ConConsulta>
    </>
  );
}

export function Limitaciones() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Limitaciones" />
      <ConConsentimiento sin={<ConsentimientoLimitaciones />}>
        <Contenido />
      </ConConsentimiento>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    lista: { gap: tema.space.s2 },
    tarjeta: { gap: tema.space.s05, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    editar: { minHeight: tema.hit.min, justifyContent: 'center', gap: tema.space.s05, padding: tema.space.s35, borderRadius: tema.radius.md },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    enlaces: { gap: tema.space.s2 },
  });
}
