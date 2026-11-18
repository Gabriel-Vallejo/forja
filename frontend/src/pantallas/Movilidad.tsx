import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import { type Movilidad as DatosMovilidad, TESTS_MOVILIDAD, type Tramo, type TramosMovilidad, TRAMOS } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Seleccion } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import {
  useConsentimientoMedidas,
  useDarConsentimientoMedidas,
  useGuardarMovilidad,
  useMovilidad,
  useRetirarConsentimientoMedidas,
} from '../movilidad/consultas';
import { type DefinicionTest, HEURISTICA, INTRO, NOTA_MOVILIDAD, TESTS } from '../movilidad/tests';
import { type Theme, useTheme } from '../theme';

/**
 * Versión del texto del consentimiento de medidas de salud. El backend solo acepta la vigente
 * (`TipoConsentimiento.versionVigente`): si el texto cambia, se sube allí y aquí a la vez.
 */
export const VERSION_TEXTO_MEDIDAS = 'v1';

// PROVISIONAL: texto pendiente de revisión por asesoría legal antes de abrir el repo (RF-45, ADR 0004).
const PUNTOS = [
  { titulo: 'Qué se guarda', texto: 'Solo el tramo de cada test (limitado, medio o bueno) y la fecha. Ni medidas exactas ni fotos.' },
  { titulo: 'Para qué', texto: 'Para elegir variantes de ejercicios al generar tu plan, por ejemplo la sentadilla goblet en vez de con barra.' },
  { titulo: 'Si no lo das', texto: 'La app sigue funcionando: tu plan se arma sin tener en cuenta la movilidad.' },
  {
    titulo: 'Si te arrepientes',
    texto: 'Retirarlo es igual de fácil, en esta misma pantalla, y borra los tests guardados. Es el consentimiento de medidas de salud.',
  },
] as const;

const fecha = (iso: string) => new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });

function textoResultado(resultado: DatosMovilidad[keyof DatosMovilidad]): string {
  return resultado ? `Resultado: ${resultado.tramo} · ${fecha(resultado.hechoEn)}` : 'Resultado: sin hacer';
}

function PideConsentimiento() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const dar = useDarConsentimientoMedidas(VERSION_TEXTO_MEDIDAS);
  return (
    <>
      <Texto estilo="heading" encabezado>Tus tests de movilidad, solo si tú quieres</Texto>
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

interface PropsTarjetaTest {
  definicion: DefinicionTest;
  tramo: Tramo | undefined;
  resultado: string;
  alCambiar: (tramo: Tramo | undefined) => void;
}

function TarjetaTest({ definicion, tramo, resultado, alCambiar }: PropsTarjetaTest) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const opciones = TRAMOS.map((t) => ({ valor: t, etiqueta: definicion.opciones[t] }));
  return (
    <View style={estilos.tarjeta}>
      <Texto estilo="heading" encabezado>{definicion.titulo}</Texto>
      <Texto tono="secundario">{definicion.mide}</Texto>
      <View accessibilityRole="list" style={estilos.pasos}>
        {definicion.pasos.map((paso, i) => (
          <Texto key={paso}>{`${i + 1}. ${paso}`}</Texto>
        ))}
      </View>
      <Seleccion etiqueta={definicion.pregunta} opciones={opciones} valor={tramo} alCambiar={alCambiar} opcional />
      <Texto estilo="nota" peso="700">{resultado}</Texto>
    </View>
  );
}

function RetirarConsentimiento() {
  const [confirmando, setConfirmando] = useState(false);
  const retirar = useRetirarConsentimientoMedidas();
  if (!confirmando) {
    return <Boton variante="texto" etiqueta="Retirar mi consentimiento" onPress={() => setConfirmando(true)} />;
  }
  return (
    <Confirmacion
      mensaje="Se borrarán tus tests de movilidad guardados. Forja seguirá funcionando sin ellos. No se puede deshacer."
      etiquetaConfirmar="Sí, retirar y borrar"
      etiquetaCargando="Borrando…"
      cargando={retirar.isPending}
      error={retirar.isError ? mensajeDeError(retirar.error) : undefined}
      alConfirmar={() => retirar.mutate()}
      alCancelar={() => setConfirmando(false)}
    />
  );
}

function Formulario({ datos }: { datos: DatosMovilidad }) {
  const guardar = useGuardarMovilidad();
  const [tramos, setTramos] = useState<Partial<Record<keyof DatosMovilidad, Tramo>>>(() =>
    Object.fromEntries(TESTS_MOVILIDAD.map((t) => [t, datos[t]?.tramo])),
  );
  const enviar = () => {
    const cuerpo = Object.fromEntries(TESTS_MOVILIDAD.map((t) => [t, tramos[t] ?? null])) as TramosMovilidad;
    guardar.mutate(cuerpo);
  };
  return (
    <>
      {TESTS.map((d) => (
        <TarjetaTest
          key={d.test}
          definicion={d}
          tramo={tramos[d.test]}
          resultado={textoResultado(datos[d.test])}
          alCambiar={(tramo) => {
            guardar.reset();
            setTramos((actual) => ({ ...actual, [d.test]: tramo }));
          }}
        />
      ))}
      <Texto estilo="nota" tono="secundario">{NOTA_MOVILIDAD}</Texto>
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      {guardar.isSuccess ? <Aviso tipo="exito" mensaje="Guardado. Cuenta la próxima vez que generes o regeneres tu plan." /> : null}
      <Boton etiqueta="Guardar" etiquetaCargando="Guardando…" cargando={guardar.isPending} onPress={enviar} />
      <RetirarConsentimiento />
    </>
  );
}

function ConTests() {
  const movilidad = useMovilidad();
  return (
    <>
      <Texto>{INTRO}</Texto>
      <Texto estilo="nota" tono="secundario">{HEURISTICA}</Texto>
      <ConConsulta consulta={movilidad} cargando="Cargando tus tests…">
        {(datos) => <Formulario datos={datos} />}
      </ConConsulta>
    </>
  );
}

function Contenido() {
  const consentimiento = useConsentimientoMedidas();
  return <ConConsulta consulta={consentimiento}>{(dado) => (dado ? <ConTests /> : <PideConsentimiento />)}</ConConsulta>;
}

/** Los tres tests caseros de movilidad (RF-08): instrucciones, una respuesta cerrada por tramo y el resultado. */
export function Movilidad() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Tests de movilidad" />
      <Contenido />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    punto: { gap: tema.space.s1 },
    tarjeta: { gap: tema.space.s3, padding: tema.space.s4, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    pasos: { gap: tema.space.s2 },
  });
}
