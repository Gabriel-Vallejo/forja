import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Texto } from '../componentes/Texto';
import { CLAVE_CONSENTIMIENTO, CLAVE_RESTRICCIONES } from '../limitaciones/consultas';
import { CLAVE_CONSENTIMIENTOS } from '../privacidad/claves';
import { VERSIONES_VIGENTES } from '../privacidad/versiones';
import { FINALIDAD } from '../limitaciones/textos';
import { useSesion } from '../sesion/sesion';
import { type Theme, useTheme } from '../theme';

/**
 * Versión del texto de abajo. El backend solo acepta la vigente (`TipoConsentimiento.versionVigente`): si el
 * texto cambia, se sube allí y aquí a la vez.
 */
export const VERSION_TEXTO = VERSIONES_VIGENTES.limitaciones;

// v2 (2026-11-21): revisión preliminar (docs/legal/revision-consentimiento.md); una asesoría la dio por buena el
// mismo día (RF-45, ADR 0004).
export const PUNTOS_LIMITACIONES = [
  {
    titulo: 'Qué se guarda',
    texto:
      'La limitación funcional, por ejemplo «sin carga axial» en la rodilla izquierda; el dolor de 0 a 10 que anotes en tus series; y, si el cuestionario de dolor da una señal de aviso, solo su nivel (no tus respuestas). No guardamos la patología ni nada que te hayan dicho en consulta.',
  },
  {
    titulo: 'Cómo se guarda',
    texto:
      'Cifrado y ligado a tu cuenta, hasta que lo retires o borres la cuenta. No sale en los registros técnicos (logs) ni se comparte con nadie.',
  },
  {
    titulo: 'Si no lo das',
    texto: 'La app sigue funcionando sin que lo des: tu plan se arma sin tener en cuenta ninguna limitación.',
  },
  {
    titulo: 'Si te arrepientes',
    texto: 'Retirarlo es igual de fácil: un toque en esta misma pantalla, y eso borra todo lo guardado. Lo hecho antes de retirarlo sigue siendo válido.',
  },
] as const;

/** Pantalla de consentimiento aparte (RF-45): se da con «Acepto» y no se da por defecto. */
export function ConsentimientoLimitaciones() {
  const router = useRouter();
  const { api } = useSesion();
  const consultas = useQueryClient();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const dar = useMutation({
    mutationFn: () => api.darConsentimientoLimitaciones(VERSION_TEXTO),
    onSuccess: () => {
      consultas.setQueryData(CLAVE_CONSENTIMIENTO, true);
      void consultas.invalidateQueries({ queryKey: CLAVE_CONSENTIMIENTOS });
      return consultas.invalidateQueries({ queryKey: CLAVE_RESTRICCIONES });
    },
  });

  return (
    <>
      <Texto estilo="heading" encabezado>Tus limitaciones, solo si tú quieres</Texto>
      <Texto>{FINALIDAD}</Texto>
      {PUNTOS_LIMITACIONES.map((p) => (
        <View key={p.titulo} style={estilos.punto}>
          <Texto peso="700">{p.titulo}</Texto>
          <Texto tono="secundario">{p.texto}</Texto>
        </View>
      ))}
      {dar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(dar.error)} /> : null}
      <Boton etiqueta="Acepto" etiquetaCargando="Guardando…" cargando={dar.isPending} onPress={() => dar.mutate()} />
      <Boton variante="secundario" etiqueta="Ahora no" deshabilitado={dar.isPending} onPress={() => router.back()} />
    </>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({ punto: { gap: tema.space.s1 } });
}
