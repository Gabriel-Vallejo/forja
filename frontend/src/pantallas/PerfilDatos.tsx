import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import {
  CAMPOS_MEDIDAS,
  type DatosPerfil,
  EXPERIENCIAS,
  type Experiencia,
  type Medidas,
  OBJETIVOS,
  type ObjetivoPerfil,
  SEXOS,
  type Sexo,
} from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { ConConsulta } from '../componentes/ConConsulta';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Seleccion } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import { useConsentimientoMedidas } from '../movilidad/consultas';
import { useGuardarMedidas, useGuardarPerfilDatos, useMedidas, usePerfilDatos } from '../perfil/consultas';
import { type ErroresMedidas, MEDIDAS, validarMedidas, type ValoresMedidas, valoresDe } from '../perfil/medidas';
import {
  AYUDA_SEXO,
  DESCRIPCION_EXPERIENCIA,
  ETIQUETA_EXPERIENCIA,
  ETIQUETA_OBJETIVO,
  ETIQUETA_SEXO,
  faltaParaGrasa,
  NOTA_GRASA,
  NOTA_OBJETIVO,
  textoGrasa,
} from '../perfil/textos';
import { type Theme, useTheme } from '../theme';
import { PideConsentimientoMedidas, RetirarConsentimientoMedidas } from './ConsentimientoMedidas';

const OPCIONES_SEXO = SEXOS.map((valor) => ({ valor, etiqueta: ETIQUETA_SEXO[valor] }));
const OPCIONES_EXPERIENCIA = EXPERIENCIAS.map((valor) => ({ valor, etiqueta: ETIQUETA_EXPERIENCIA[valor] }));
const OPCIONES_OBJETIVO = OBJETIVOS.map((valor) => ({ valor, etiqueta: ETIQUETA_OBJETIVO[valor] }));

function FormularioDatos({ datos }: { datos: DatosPerfil }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const guardar = useGuardarPerfilDatos();
  const [sexo, setSexo] = useState<Sexo>(datos.sexo);
  const [experiencia, setExperiencia] = useState<Experiencia | undefined>(datos.experiencia);
  const [objetivo, setObjetivo] = useState<ObjetivoPerfil | undefined>(datos.objetivo);

  const cambiar = <T,>(poner: (v: T) => void) => (valor: T) => {
    guardar.reset();
    poner(valor);
  };

  return (
    <View style={estilos.seccion}>
      <Texto estilo="heading" encabezado>Sobre ti</Texto>
      <Seleccion etiqueta="Sexo" ayuda={AYUDA_SEXO} opciones={OPCIONES_SEXO} valor={sexo} alCambiar={(v) => v && cambiar(setSexo)(v)} />
      <Seleccion
        etiqueta="Experiencia"
        opciones={OPCIONES_EXPERIENCIA}
        valor={experiencia}
        alCambiar={cambiar(setExperiencia)}
        opcional
      />
      <View style={estilos.niveles}>
        {EXPERIENCIAS.map((e) => (
          <Texto key={e} estilo="nota" tono="secundario">{`${ETIQUETA_EXPERIENCIA[e]}: ${DESCRIPCION_EXPERIENCIA[e]}`}</Texto>
        ))}
      </View>
      <Seleccion etiqueta="Objetivo" ayuda={NOTA_OBJETIVO} opciones={OPCIONES_OBJETIVO} valor={objetivo} alCambiar={cambiar(setObjetivo)} opcional />
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      {guardar.isSuccess ? <Aviso tipo="exito" mensaje="Guardado." /> : null}
      <Boton
        etiqueta="Guardar"
        etiquetaCargando="Guardando…"
        cargando={guardar.isPending}
        onPress={() => guardar.mutate({ sexo, experiencia: experiencia ?? null, objetivo: objetivo ?? null })}
      />
    </View>
  );
}

const TECLADO_DECIMAL = 'decimal-pad';

function FormularioMedidas({ medidas, sexo }: { medidas: Medidas; sexo: Sexo }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const guardar = useGuardarMedidas();
  const [valores, setValores] = useState<ValoresMedidas>(() => valoresDe(medidas));
  const [errores, setErrores] = useState<ErroresMedidas>({});

  const enviar = () => {
    const resultado = validarMedidas(valores);
    if ('errores' in resultado) {
      setErrores(resultado.errores);
      return;
    }
    setErrores({});
    guardar.mutate(resultado.cuerpo);
  };

  return (
    <>
      {CAMPOS_MEDIDAS.map((campo) => (
        <CampoTexto
          key={campo}
          etiqueta={`${MEDIDAS[campo].etiqueta} (${MEDIDAS[campo].unidad})`}
          valor={valores[campo]}
          keyboardType={TECLADO_DECIMAL}
          error={errores[campo]}
          alCambiar={(v) => {
            guardar.reset();
            setValores((actual) => ({ ...actual, [campo]: v }));
          }}
        />
      ))}
      <Texto estilo="nota" tono="secundario">Todas son opcionales. Mide el cuello bajo la nuez, la cintura a la altura del ombligo y la cadera por su parte más ancha.</Texto>
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      {guardar.isSuccess ? <Aviso tipo="exito" mensaje="Guardado." /> : null}
      <Boton etiqueta="Guardar medidas" etiquetaCargando="Guardando…" cargando={guardar.isPending} onPress={enviar} />
      <View style={estilos.grasa} accessibilityRole="summary">
        <Texto peso="700">% de grasa estimado</Texto>
        {medidas.grasaEstimada ? (
          <>
            <Texto estilo="heading">{textoGrasa(medidas.grasaEstimada)}</Texto>
            <Texto estilo="nota" tono="secundario">{NOTA_GRASA}</Texto>
          </>
        ) : (
          <Texto tono="secundario">{faltaParaGrasa(sexo)}</Texto>
        )}
      </View>
      <RetirarConsentimientoMedidas />
    </>
  );
}

function ConMedidas({ sexo }: { sexo: Sexo }) {
  const medidas = useMedidas();
  return (
    <ConConsulta consulta={medidas} cargando="Cargando tus medidas…">
      {(datos) => <FormularioMedidas medidas={datos} sexo={sexo} />}
    </ConConsulta>
  );
}

function SeccionMedidas({ sexo }: { sexo: Sexo }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const consentimiento = useConsentimientoMedidas();
  return (
    <View style={estilos.seccion}>
      <ConConsulta consulta={consentimiento}>
        {(dado) =>
          dado ? (
            <>
              <Texto estilo="heading" encabezado>Medidas</Texto>
              <ConMedidas sexo={sexo} />
            </>
          ) : (
            <PideConsentimientoMedidas titulo="Tus medidas, solo si tú quieres" />
          )
        }
      </ConConsulta>
    </View>
  );
}

function Contenido() {
  const datos = usePerfilDatos();
  return (
    <ConConsulta consulta={datos} cargando="Cargando tu perfil…">
      {(d) => (
        <>
          <FormularioDatos datos={d} />
          <SeccionMedidas sexo={d.sexo} />
        </>
      )}
    </ConConsulta>
  );
}

/** Perfil manual: sexo, experiencia, objetivo y medidas (con consentimiento), con el % de grasa estimado. */
export function PerfilDatos() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Tus datos" />
      <Contenido />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    seccion: { gap: tema.space.s4 },
    niveles: { gap: tema.space.s1 },
    grasa: { gap: tema.space.s2, padding: tema.space.s4, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
  });
}
