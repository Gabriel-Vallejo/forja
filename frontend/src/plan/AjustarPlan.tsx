import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { Boton } from '../componentes/Boton';
import { FichaOpcion, opcionesFila } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import { type Theme, useTheme } from '../theme';
import { MAXIMO_ENFOQUE, MUSCULOS_ENFOCABLES, usePreferencias } from './preferencias';

/** Elige hasta 2 músculos; parte de los que ya tiene enfocados. */
function ElegirMusculos({ alCancelar }: { alCancelar: () => void }) {
  const router = useRouter();
  const estilos = crearEstilos(useTheme());
  const guardadas = usePreferencias();
  const [elegidos, setElegidos] = useState<string[] | undefined>(undefined);
  const actuales = elegidos ?? guardadas.data?.prioritarios.map((m) => m.codigo) ?? [];
  const alternar = (codigo: string) => {
    if (actuales.includes(codigo)) setElegidos(actuales.filter((m) => m !== codigo));
    else if (actuales.length < MAXIMO_ENFOQUE) setElegidos([...actuales, codigo]);
  };
  return (
    <View accessibilityRole="group" accessibilityLabel="Músculos a enfocar" style={estilos.grupo}>
      <Texto estilo="nota" tono="secundario">{`Hasta ${MAXIMO_ENFOQUE}. Sin ninguno, quitas el enfoque.`}</Texto>
      <View style={estilos.fichas}>
        {MUSCULOS_ENFOCABLES.map((m) => (
          <FichaOpcion key={m.codigo} etiqueta={m.nombre} rol="checkbox" marcada={actuales.includes(m.codigo)} alPulsar={() => alternar(m.codigo)} />
        ))}
      </View>
      <Boton
        etiqueta="Ver cómo queda"
        onPress={() => router.push({ pathname: '/vista-previa', params: { tipo: 'enfocar', musculos: actuales.join(',') } })}
      />
      <Boton variante="secundario" etiqueta="Cancelar" onPress={alCancelar} />
    </View>
  );
}

/** Ajustes de todo el plan (PRD v0.1): más variedad y enfocar músculos, cada uno con su vista previa. */
export function AjustarPlan() {
  const router = useRouter();
  const estilos = crearEstilos(useTheme());
  const [enfocando, setEnfocando] = useState(false);
  return (
    <View style={estilos.grupo}>
      <Texto estilo="etiqueta" tono="secundario">Ajustar el plan</Texto>
      {enfocando ? (
        <ElegirMusculos alCancelar={() => setEnfocando(false)} />
      ) : (
        <>
          <Boton variante="secundario" etiqueta="Enfocar músculos" onPress={() => setEnfocando(true)} />
          <Boton
            variante="secundario"
            etiqueta="Más variedad"
            onPress={() => router.push({ pathname: '/vista-previa', params: { tipo: 'variedad' } })}
          />
        </>
      )}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    grupo: { gap: tema.space.s2 },
    fichas: opcionesFila(tema),
  });
}
