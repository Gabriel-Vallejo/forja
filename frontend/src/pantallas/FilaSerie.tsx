import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import type { ObjetivoEjercicio } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { aNumero, aTexto, Contador } from '../componentes/Contador';
import { Icono } from '../componentes/Icono';
import { FichaOpcion, opcionesFila } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import { pesoCorporal } from '../plan/plan';
import { type Theme, useTheme } from '../theme';

/** Paso del peso si el plan no trae `incrementoKg` (peso corporal): el disco más pequeño habitual. */
export const PASO_PESO_KG = 2.5;
const MAX_PESO_KG = 1000;
const MAX_REPETICIONES = 100;
const MAX_RIR = 10;
const MAX_DOLOR = 10;
const NIVELES_DOLOR = Array.from({ length: MAX_DOLOR + 1 }, (_, i) => i);
/** RPE de 6 a 10 en medios puntos: por debajo de 6 no es una serie de trabajo. */
const NIVELES_RPE = Array.from({ length: 9 }, (_, i) => 6 + i / 2);
/** Desde aquí la fila avisa y, al guardar, la sesión se para (§5.3, umbral [propuesta]). */
export const DOLOR_ALTO = 7;
export const MENSAJE_DOLOR_ALTO =
  'Dolor alto: para este ejercicio. Si el dolor sigue o es agudo, habla con un profesional sanitario; Forja no puede valorarlo.';

export interface ValoresSerie {
  pesoKg: number;
  repeticiones: number;
  rir: number;
  calentamiento: boolean;
  dolor?: number;
  rpe?: number;
}

/** Lo que se precarga: sin carga conocida (primera semana) el peso va vacío. */
export type ValoresIniciales = Omit<ValoresSerie, 'pesoKg'> & { pesoKg?: number };

interface PropsFilaSerie {
  objetivo: ObjetivoEjercicio;
  /** «Serie 2» o «Serie 2 (corrección)». */
  titulo: string;
  inicial: ValoresIniciales;
  etiquetaGuardar: string;
  alGuardar: (valores: ValoresSerie) => Promise<void>;
  alCancelar?: () => void;
}

/** Fila de registro de serie en edición (docs/design-system.md §5.3). Lo imprescindible arriba; el dolor, a demanda. */
export function FilaSerie({ objetivo, titulo, inicial, etiquetaGuardar, alGuardar, alCancelar }: PropsFilaSerie) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [peso, setPeso] = useState(inicial.pesoKg === undefined ? '' : aTexto(inicial.pesoKg));
  const [repeticiones, setRepeticiones] = useState(aTexto(inicial.repeticiones));
  const [rir, setRir] = useState(aTexto(inicial.rir));
  const [calentamiento, setCalentamiento] = useState(inicial.calentamiento);
  const [dolor, setDolor] = useState(inicial.dolor);
  const [conDolor, setConDolor] = useState(inicial.dolor !== undefined);
  const [rpe, setRpe] = useState(inicial.rpe);
  const [conRpe, setConRpe] = useState(inicial.rpe !== undefined);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const contexto = `${objetivo.nombre}, ${titulo.toLowerCase()}`;
  const corporal = pesoCorporal(objetivo);

  const guardar = async () => {
    const valores = { pesoKg: aNumero(peso), repeticiones: aNumero(repeticiones), rir: aNumero(rir) };
    if (Number.isNaN(valores.pesoKg) && peso.trim() === '' && !Number.isNaN(valores.repeticiones) && !Number.isNaN(valores.rir)) {
      // Lo normal la primera vez: el peso viene vacío para que lo elijas. Mejor decir justo eso.
      setError(`Falta el ${corporal ? 'lastre' : 'peso'}: escribe con cuántos kg haces esta serie.`);
      return;
    }
    if (Object.values(valores).some(Number.isNaN)) {
      setError('Escribe peso, repeticiones y RIR con números.');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      await alGuardar({ ...valores, calentamiento, dolor: conDolor ? dolor : undefined, rpe: conRpe ? rpe : undefined });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se ha podido guardar.');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <View style={estilos.fila} accessibilityLabel={`${objetivo.nombre}, ${titulo}`}>
      <Texto estilo="etiquetaEstrecha">{titulo}</Texto>
      <Contador
        etiqueta={corporal ? 'Lastre' : 'Peso'}
        unidad="kg"
        contexto={contexto}
        valor={peso}
        alCambiar={setPeso}
        paso={objetivo.incrementoKg || PASO_PESO_KG}
        min={0}
        max={MAX_PESO_KG}
        decimales
        sugerencia="elige"
        pista={inicial.pesoKg === undefined ? 'primera vez: elige la carga' : undefined}
      />
      <View style={estilos.dos}>
        <Contador
          etiqueta="Repeticiones"
          contexto={contexto}
          pista={`objetivo ${objetivo.repeticionesMin}-${objetivo.repeticionesMax}`}
          valor={repeticiones}
          alCambiar={setRepeticiones}
          paso={1}
          min={0}
          max={MAX_REPETICIONES}
        />
        <Contador
          etiqueta="RIR"
          contexto={contexto}
          pista={`objetivo ${objetivo.rirObjetivo}`}
          valor={rir}
          alCambiar={setRir}
          paso={1}
          min={0}
          max={MAX_RIR}
        />
      </View>
      <View style={estilos.opciones}>
        <FichaOpcion etiqueta="Calentamiento" rol="checkbox" marcada={calentamiento} alPulsar={() => setCalentamiento((c) => !c)} />
        <FichaOpcion
          etiqueta="¿Te ha dolido?"
          rol="checkbox"
          marcada={conDolor}
          alPulsar={() => {
            setConDolor((c) => !c);
            setDolor(undefined);
          }}
        />
        <FichaOpcion
          etiqueta="Anotar RPE"
          rol="checkbox"
          marcada={conRpe}
          alPulsar={() => {
            setConRpe((c) => !c);
            setRpe(undefined);
          }}
        />
      </View>
      {calentamiento ? <Texto estilo="nota" tono="secundario">El calentamiento no cuenta para el volumen ni para subir la carga.</Texto> : null}
      {conDolor ? (
        <View accessibilityRole="radiogroup" accessibilityLabel={`Dolor de 0 a 10, ${contexto}`} style={estilos.dolor}>
          <Texto estilo="nota" tono="secundario">Dolor de 0 (nada) a 10 (el peor). Opcional.</Texto>
          <View style={estilos.opciones}>
            {NIVELES_DOLOR.map((n) => (
              <FichaOpcion
                key={n}
                etiqueta={String(n)}
                rol="radio"
                marcada={dolor === n}
                alPulsar={() => setDolor((d) => (d === n ? undefined : n))}
              />
            ))}
          </View>
        </View>
      ) : null}
      {conDolor && dolor !== undefined && dolor >= DOLOR_ALTO ? <Aviso tipo="aviso" mensaje={MENSAJE_DOLOR_ALTO} /> : null}
      {conRpe ? (
        <View accessibilityRole="radiogroup" accessibilityLabel={`RPE de 6 a 10, ${contexto}`} style={estilos.dolor}>
          <Texto estilo="nota" tono="secundario">Esfuerzo percibido: 10 es que no podías hacer ni una más. Opcional.</Texto>
          <View style={estilos.opciones}>
            {NIVELES_RPE.map((n) => (
              <FichaOpcion
                key={n}
                etiqueta={aTexto(n)}
                etiquetaAccesible={`RPE ${aTexto(n)}`}
                rol="radio"
                marcada={rpe === n}
                alPulsar={() => setRpe((r) => (r === n ? undefined : n))}
              />
            ))}
          </View>
        </View>
      ) : null}
      {error ? <Aviso tipo="error" mensaje={error} /> : null}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${guardando ? 'Guardando…' : etiquetaGuardar}, ${contexto}`}
        accessibilityState={{ disabled: guardando, busy: guardando }}
        disabled={guardando}
        onPress={guardar}
        style={({ pressed }) => [estilos.guardar, pressed && estilos.guardarPulsado]}
      >
        <Icono nombre="check" color={tema.color.onPrimary} />
        <Texto estilo="destacado" color={tema.color.onPrimary}>{guardando ? 'Guardando…' : etiquetaGuardar}</Texto>
      </Pressable>
      {alCancelar ? <Boton variante="texto" etiqueta="Cancelar" onPress={alCancelar} /> : null}
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    fila: {
      gap: tema.space.s3,
      padding: tema.space.s35,
      borderRadius: tema.radius.md,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
      backgroundColor: tema.color.surfaceHigh,
    },
    // En móviles de menos de ~400 dp de ancho, repeticiones y RIR no caben juntos: RIR baja a la línea de abajo.
    dos: { flexDirection: 'row', flexWrap: 'wrap', gap: tema.space.s3 },
    opciones: opcionesFila(tema),
    dolor: { gap: tema.space.s2 },
    // Botón de confirmar: 72 dp, ancho completo, relleno primario (§5.3).
    guardar: {
      minHeight: tema.hit.confirm,
      flexDirection: 'row',
      gap: tema.space.s2,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.primary,
    },
    guardarPulsado: { backgroundColor: tema.color.secondary },
  });
}
