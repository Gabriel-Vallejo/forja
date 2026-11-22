import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { CONSENTIMIENTOS } from '../privacidad/catalogo';
import { CLAVE_CONSENTIMIENTOS } from '../privacidad/claves';
import { useRetirarConsentimiento } from '../privacidad/consultas';
import { type TipoConTexto, VERSIONES_VIGENTES } from '../privacidad/versiones';
import { useSesion } from '../sesion/sesion';
import { type Theme, useTheme } from '../theme';
import { PUNTOS_LIMITACIONES } from './ConsentimientoLimitaciones';
import { PUNTOS_MEDIDAS } from './ConsentimientoMedidas';

const PUNTOS: Record<TipoConTexto, readonly { titulo: string; texto: string }[]> = {
  limitaciones: PUNTOS_LIMITACIONES,
  medidas_salud: PUNTOS_MEDIDAS,
};

interface PropsRenovar {
  tipo: TipoConTexto;
}

/**
 * El texto de un consentimiento cambió desde que se dio: se enseña el nuevo y no se sigue sin aceptarlo o retirarlo.
 * No hay «Ahora no»: seguir usando los datos con un texto que no se ha leído es justo lo que se quiere evitar.
 */
export function RenovarConsentimiento({ tipo }: PropsRenovar) {
  const { api } = useSesion();
  const consultas = useQueryClient();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [retirando, setRetirando] = useState(false);
  const info = CONSENTIMIENTOS.find((c) => c.tipo === tipo);
  const version = VERSIONES_VIGENTES[tipo];
  const dar = useMutation({
    mutationFn: () =>
      tipo === 'limitaciones' ? api.darConsentimientoLimitaciones(version) : api.darConsentimientoMedidas(version),
    onSuccess: () => consultas.invalidateQueries({ queryKey: CLAVE_CONSENTIMIENTOS }),
  });
  const retirar = useRetirarConsentimiento();

  return (
    <Pantalla titulo="Hemos cambiado un texto">
      <Texto>
        Hemos cambiado el texto del consentimiento de «{info?.nombre ?? tipo}». Léelo otra vez: para seguir usando lo que
        guardaste, acepta el nuevo. Si no, retíralo y se borra. Lo que ya guardaste no cambia hasta que elijas.
      </Texto>
      {PUNTOS[tipo].map((p) => (
        <View key={p.titulo} style={estilos.punto}>
          <Texto peso="700">{p.titulo}</Texto>
          <Texto tono="secundario">{p.texto}</Texto>
        </View>
      ))}
      {dar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(dar.error)} /> : null}
      {retirando ? (
        <Confirmacion
          mensaje={info?.alRetirar ?? 'Se borrará lo que dependa de este consentimiento. No se puede deshacer.'}
          etiquetaConfirmar={info?.etiquetaRetirar ?? 'Sí, retirar y borrar'}
          etiquetaCargando="Borrando…"
          cargando={retirar.isPending}
          error={retirar.isError ? mensajeDeError(retirar.error) : undefined}
          alConfirmar={() => retirar.mutate(tipo)}
          alCancelar={() => setRetirando(false)}
        />
      ) : (
        <>
          <Boton etiqueta="Acepto el texto nuevo" etiquetaCargando="Guardando…" cargando={dar.isPending} onPress={() => dar.mutate()} />
          <Boton variante="secundario" etiqueta="No acepto: retirar y borrar" deshabilitado={dar.isPending} onPress={() => setRetirando(true)} />
        </>
      )}
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({ punto: { gap: tema.space.s1 } });
}
