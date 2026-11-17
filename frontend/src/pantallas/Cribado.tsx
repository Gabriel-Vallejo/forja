import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import type { CuestionarioCribado, PreguntaCribado, ResultadoCribado, SolicitudCribado } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { SiNo } from '../componentes/SiNo';
import { Texto } from '../componentes/Texto';
import { Veredicto } from '../componentes/Veredicto';
import { usePreguntasCribado } from '../limitaciones/consultas';
import { useSesion } from '../sesion/sesion';
import { ConConsentimiento } from './ConConsentimiento';

type Mapa = Readonly<Record<string, boolean>>;

/** Las semanas las dice el backend (`RespuestasCribado.SEMANAS_RECIENTE`): aquí no se repite el número. */
const ayudaReciente = (semanas: number) => `Cuenta lo de ${semanas === 1 ? 'la última semana' : `las últimas ${semanas} semanas`}.`;

function Resultado({ resultado, alVolver, alRepetir }: { resultado: ResultadoCribado; alVolver: () => void; alRepetir: () => void }) {
  const motivos = resultado.motivos.map((texto) => ({ texto }));
  return (
    <>
      {resultado.nivel === 'seguir' ? (
        <Veredicto tono="ok" titulo="Puedes seguir" texto="Con lo que has contestado, nada te impide usar Forja." />
      ) : null}
      {resultado.nivel === 'consultar_profesional' ? (
        <Veredicto
          tono="aviso"
          titulo="Habla con un profesional antes de entrenar"
          texto="Esto no te impide usar Forja, pero conviene que lo consultes con un profesional sanitario."
          motivos={motivos}
        />
      ) : null}
      {resultado.nivel === 'parar_urgencias' ? (
        <Veredicto tono="urgente" titulo="No entrenes hoy y acude a urgencias" motivos={motivos} />
      ) : null}
      {resultado.nivel === 'seguir' ? <Boton etiqueta="Listo" onPress={alVolver} /> : null}
      {resultado.nivel === 'consultar_profesional' ? <Boton etiqueta="Entendido" onPress={alVolver} /> : null}
      <Boton variante="secundario" etiqueta="Volver a contestar" onPress={alRepetir} />
    </>
  );
}

/** Las respuestas viven solo en este estado: no se guardan en ningún sitio (datos de salud, ADR 0004). */
function Cuestionario({ cuestionario: { preguntas, semanasReciente } }: { cuestionario: CuestionarioCribado }) {
  const router = useRouter();
  const { api } = useSesion();
  const [respuestas, setRespuestas] = useState<Mapa>({});
  const [recientes, setRecientes] = useState<Mapa>({});
  const envio = useMutation({ mutationFn: (solicitud: SolicitudCribado) => api.cribar(solicitud) });

  const contestar = (p: PreguntaCribado, valor: boolean) => {
    setRespuestas({ ...respuestas, [p.codigo]: valor });
    // Un «no» no puede ser reciente: se olvida la respuesta anterior y se vuelve a preguntar si cambia a sí.
    if (!valor) setRecientes(Object.fromEntries(Object.entries(recientes).filter(([codigo]) => codigo !== p.codigo)));
  };
  const completo = preguntas.every(
    (p) => respuestas[p.codigo] !== undefined && (!p.admiteReciente || !respuestas[p.codigo] || recientes[p.codigo] !== undefined),
  );
  const enviar = () =>
    envio.mutate({
      respuestas,
      recientes: preguntas.filter((p) => p.admiteReciente && respuestas[p.codigo] && recientes[p.codigo]).map((p) => p.codigo),
    });
  const repetir = () => {
    setRespuestas({});
    setRecientes({});
    envio.reset();
  };

  if (envio.data) return <Resultado resultado={envio.data} alVolver={() => router.back()} alRepetir={repetir} />;
  return (
    <>
      <Texto tono="secundario">Contesta las siete. Tus respuestas no se guardan: solo sirven para darte el resultado.</Texto>
      {preguntas.map((p) => (
        <Pregunta
          key={p.codigo}
          pregunta={p}
          semanasReciente={semanasReciente}
          respuesta={respuestas[p.codigo]}
          reciente={recientes[p.codigo]}
          alContestar={(v) => contestar(p, v)}
          alMarcarReciente={(v) => setRecientes({ ...recientes, [p.codigo]: v })}
        />
      ))}
      {envio.isError ? <Aviso tipo="error" mensaje={mensajeDeError(envio.error)} /> : null}
      <Boton
        etiqueta="Ver resultado"
        etiquetaCargando="Calculando…"
        cargando={envio.isPending}
        deshabilitado={!completo}
        onPress={enviar}
      />
    </>
  );
}

interface PropsPregunta {
  pregunta: PreguntaCribado;
  semanasReciente: number;
  respuesta: boolean | undefined;
  reciente: boolean | undefined;
  alContestar: (valor: boolean) => void;
  alMarcarReciente: (valor: boolean) => void;
}

function Pregunta({ pregunta, semanasReciente, respuesta, reciente, alContestar, alMarcarReciente }: PropsPregunta) {
  return (
    <>
      <SiNo etiqueta={pregunta.texto} valor={respuesta} alCambiar={alContestar} />
      {pregunta.admiteReciente && respuesta ? (
        <SiNo etiqueta="¿Fue hace poco?" ayuda={ayudaReciente(semanasReciente)} valor={reciente} alCambiar={alMarcarReciente} />
      ) : null}
    </>
  );
}

export function Cribado() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Cribado previo" />
      <ConConsentimiento>
        <Preguntas />
      </ConConsentimiento>
    </Pantalla>
  );
}

function Preguntas() {
  const preguntas = usePreguntasCribado();
  return <ConConsulta consulta={preguntas} cargando="Cargando las preguntas…">{(cuestionario) => <Cuestionario cuestionario={cuestionario} />}</ConConsulta>;
}
