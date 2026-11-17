import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { ResultadoBanderas, SolicitudBanderas } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { FichaOpcion, opcionesFila, Seleccion } from '../componentes/Seleccion';
import { SiNo } from '../componentes/SiNo';
import { Texto } from '../componentes/Texto';
import { Veredicto } from '../componentes/Veredicto';
import { type CodigoRegion, codigos, REGIONES } from '../limitaciones/catalogo';
import { useSesion } from '../sesion/sesion';
import { type Theme, useTheme } from '../theme';
import { ConConsentimiento } from './ConConsentimiento';

type ClaveSenal = Exclude<keyof SolicitudBanderas, 'dolorPorZona'>;
type Senales = Partial<Record<ClaveSenal, boolean>>;

/** Redactadas en lenguaje de calle: son las señales que pide el backend, sin tecnicismos. */
const SENALES: readonly { clave: ClaveSenal; texto: string }[] = [
  { clave: 'dolorEnReposo', texto: '¿Te duele aunque estés en reposo, sin moverte?' },
  { clave: 'dolorNocturnoQueDespierta', texto: '¿Te despierta el dolor por la noche?' },
  { clave: 'hormigueoOEntumecimiento', texto: '¿Notas hormigueo o la zona dormida?' },
  { clave: 'perdidaDeFuerza', texto: '¿Has notado que pierdes fuerza en un brazo o una pierna?' },
  {
    clave: 'perdidaControlEsfinteresOAnestesiaSillaDeMontar',
    texto: '¿Has perdido el control de la vejiga o el intestino, o notas dormida la zona genital o anal?',
  },
  { clave: 'fiebreConDolorDeEspalda', texto: '¿Tienes fiebre y, a la vez, dolor de espalda?' },
  { clave: 'traumatismoReciente', texto: '¿Has tenido un golpe o una caída fuerte hace poco?' },
  { clave: 'perdidaDePesoSinExplicacion', texto: '¿Has perdido peso sin explicación, sin dieta ni cambios en tu vida?' },
  { clave: 'dolorToracico', texto: '¿Notas dolor u opresión en el pecho?' },
  { clave: 'mareoODesmayo', texto: '¿Te has mareado o desmayado?' },
];

const NIVELES_DOLOR = Array.from({ length: 11 }, (_, i) => ({ valor: String(i), etiqueta: String(i) }));
const NOMBRES_REGION = codigos(REGIONES);

const completas = (s: Senales): s is Record<ClaveSenal, boolean> => SENALES.every((x) => s[x.clave] !== undefined);

function Bloqueo({ resultado, alVolver, alRepetir }: { resultado: ResultadoBanderas; alVolver: () => void; alRepetir: () => void }) {
  const urgente = resultado.banderas.some((b) => b.nivel === 'urgencias_hoy');
  return (
    <>
      <Veredicto
        tono={urgente ? 'urgente' : 'aviso'}
        titulo={urgente ? 'No entrenes hoy y acude a urgencias' : 'Habla con un profesional en los próximos días'}
        texto={urgente ? undefined : 'Antes de entrenar, consulta lo que has contestado con un profesional sanitario.'}
        motivos={resultado.banderas.map((b) => ({ texto: b.motivo, fuente: b.guia }))}
      />
      <Boton etiqueta="Volver" onPress={alVolver} />
      <Boton variante="secundario" etiqueta="Volver a contestar" onPress={alRepetir} />
    </>
  );
}

/** Las respuestas viven solo en este estado: el backend evalúa y no guarda nada. */
function Cuestionario() {
  const router = useRouter();
  const { api } = useSesion();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [marcadas, setMarcadas] = useState<readonly CodigoRegion[]>([]);
  const [dolor, setDolor] = useState<Partial<Record<CodigoRegion, number>>>({});
  const [senales, setSenales] = useState<Senales>({});
  const envio = useMutation({
    mutationFn: (solicitud: SolicitudBanderas) => api.evaluarBanderas(solicitud),
    onSuccess: (resultado) => {
      if (!resultado.bloquea) router.back();
    },
  });

  const alternar = (region: CodigoRegion) => {
    const quitar = marcadas.includes(region);
    setMarcadas(quitar ? marcadas.filter((r) => r !== region) : [...marcadas, region]);
    if (quitar) setDolor(Object.fromEntries(Object.entries(dolor).filter(([r]) => r !== region)));
  };
  const listo = completas(senales) && marcadas.every((r) => dolor[r] !== undefined);
  const enviar = () => {
    if (!completas(senales)) return;
    envio.mutate({ dolorPorZona: Object.fromEntries(marcadas.map((r) => [r, dolor[r] ?? 0])), ...senales });
  };
  const repetir = () => {
    setMarcadas([]);
    setDolor({});
    setSenales({});
    envio.reset();
  };

  if (envio.data?.bloquea) return <Bloqueo resultado={envio.data} alVolver={() => router.back()} alRepetir={repetir} />;
  return (
    <>
      <Texto tono="secundario">Contesta con sinceridad. Tus respuestas no se guardan: solo sirven para darte el resultado.</Texto>
      <View style={estilos.bloque}>
        <Texto estilo="heading" encabezado>¿Dónde notas dolor ahora?</Texto>
        <Texto tono="secundario">Marca las zonas y pon cuánto duele, de 0 (nada) a 10 (lo máximo). Si no te duele nada, sáltatelo.</Texto>
        <View accessibilityLabel="Zonas con dolor" style={opcionesFila(tema)}>
          {NOMBRES_REGION.map((r) => (
            <FichaOpcion key={r} etiqueta={REGIONES[r]} rol="checkbox" marcada={marcadas.includes(r)} alPulsar={() => alternar(r)} />
          ))}
        </View>
        {marcadas.map((r) => (
          <Seleccion
            key={r}
            etiqueta={`Dolor en ${REGIONES[r].toLowerCase()}, de 0 a 10`}
            opciones={NIVELES_DOLOR}
            valor={dolor[r] === undefined ? undefined : String(dolor[r])}
            alCambiar={(v) => v !== undefined && setDolor({ ...dolor, [r]: Number(v) })}
          />
        ))}
      </View>
      <View style={estilos.bloque}>
        <Texto estilo="heading" encabezado>Otras señales</Texto>
        {SENALES.map((s) => (
          <SiNo key={s.clave} etiqueta={s.texto} valor={senales[s.clave]} alCambiar={(v) => setSenales({ ...senales, [s.clave]: v })} />
        ))}
      </View>
      {envio.isError ? <Aviso tipo="error" mensaje={mensajeDeError(envio.error)} /> : null}
      <Boton etiqueta="Ver resultado" etiquetaCargando="Calculando…" cargando={envio.isPending} deshabilitado={!listo} onPress={enviar} />
    </>
  );
}

export function Dolor() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Cuestionario de dolor" />
      <ConConsentimiento>
        <Cuestionario />
      </ConConsentimiento>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({ bloque: { gap: tema.space.s3 } });
}
