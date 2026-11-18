import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { Texto } from '../componentes/Texto';
import { useDarConsentimientoMedidas, useRetirarConsentimientoMedidas } from '../movilidad/consultas';
import { type Theme, useTheme } from '../theme';

/**
 * Versión del texto del consentimiento de medidas de salud. El backend solo acepta la vigente
 * (`TipoConsentimiento.versionVigente`): si el texto cambia, se sube allí y aquí a la vez.
 */
export const VERSION_TEXTO_MEDIDAS = 'v1';

// PROVISIONAL: texto pendiente de revisión por asesoría legal antes de abrir el repo (RF-45, ADR 0004).
const PUNTOS = [
  {
    titulo: 'Qué se guarda',
    texto:
      'El tramo de cada test de movilidad (limitado, medio o bueno) y su fecha. Y, solo si las rellenas, tu altura, tu peso y las circunferencias de cuello, cintura y cadera. Ninguna foto.',
  },
  {
    titulo: 'Para qué',
    texto:
      'Para elegir variantes de ejercicios al generar tu plan, por ejemplo la sentadilla goblet en vez de con barra, y para estimar tu % de grasa con la fórmula de la US Navy.',
  },
  { titulo: 'Si no lo das', texto: 'La app sigue funcionando: tu plan se arma sin tener en cuenta la movilidad ni las medidas.' },
  {
    titulo: 'Si te arrepientes',
    texto:
      'Retirarlo es igual de fácil, en las pantallas donde lo diste, y borra los tests y las medidas guardados. Es el consentimiento de medidas de salud.',
  },
] as const;

interface PropsPide {
  titulo: string;
}

/** Qué se guarda y para qué, antes de dar el consentimiento de medidas de salud. */
export function PideConsentimientoMedidas({ titulo }: PropsPide) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const dar = useDarConsentimientoMedidas(VERSION_TEXTO_MEDIDAS);
  return (
    <>
      <Texto estilo="heading" encabezado>{titulo}</Texto>
      {PUNTOS.map((p) => (
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

/** Retirar el consentimiento de medidas de salud: pide confirmar porque borra tests y medidas. */
export function RetirarConsentimientoMedidas() {
  const [confirmando, setConfirmando] = useState(false);
  const retirar = useRetirarConsentimientoMedidas();
  if (!confirmando) {
    return <Boton variante="texto" etiqueta="Retirar mi consentimiento" onPress={() => setConfirmando(true)} />;
  }
  return (
    <Confirmacion
      mensaje="Se borrarán tus tests de movilidad guardados. También tus medidas: altura, peso y circunferencias. Forja seguirá funcionando sin ellos. No se puede deshacer."
      etiquetaConfirmar="Sí, retirar y borrar"
      etiquetaCargando="Borrando…"
      cargando={retirar.isPending}
      error={retirar.isError ? mensajeDeError(retirar.error) : undefined}
      alConfirmar={() => retirar.mutate()}
      alCancelar={() => setConfirmando(false)}
    />
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({ punto: { gap: tema.space.s1 } });
}
