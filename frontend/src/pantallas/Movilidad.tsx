import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import { type Movilidad as DatosMovilidad, TESTS_MOVILIDAD, type Tramo, type TramosMovilidad, TRAMOS } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Seleccion } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import { useConsentimientoMedidas, useGuardarMovilidad, useMovilidad } from '../movilidad/consultas';
import { type DefinicionTest, HEURISTICA, INTRO, NOTA_MOVILIDAD, TESTS } from '../movilidad/tests';
import { type Theme, useTheme } from '../theme';
import { PideConsentimientoMedidas, RetirarConsentimientoMedidas } from './ConsentimientoMedidas';

export { VERSION_TEXTO_MEDIDAS } from './ConsentimientoMedidas';

const fecha = (iso: string) => new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });

function textoResultado(resultado: DatosMovilidad[keyof DatosMovilidad]): string {
  return resultado ? `Resultado: ${resultado.tramo} · ${fecha(resultado.hechoEn)}` : 'Resultado: sin hacer';
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
      <RetirarConsentimientoMedidas />
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
  return <ConConsulta consulta={consentimiento}>{(dado) => (dado ? <ConTests /> : <PideConsentimientoMedidas titulo="Tus tests de movilidad, solo si tú quieres" />)}</ConConsulta>;
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
    tarjeta: { gap: tema.space.s3, padding: tema.space.s4, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    pasos: { gap: tema.space.s2 },
  });
}
