import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { Material, PerfilEntreno } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { FichaOpcion, opcionesFila, Seleccion } from '../componentes/Seleccion';
import { Texto } from '../componentes/Texto';
import { DIAS_POSIBLES, MINUTOS_POSIBLES, useGuardarAjustes, useMateriales, usePerfilEntreno } from '../plan/ajustes';
import { type Theme, useTheme } from '../theme';

export const NOTA_AJUSTES = 'Los ajustes cuentan al generar o regenerar tu plan: el plan que tienes ahora no cambia solo.';
export const ERROR_SIN_MATERIAL = 'Elige al menos un material.';

const OPCIONES_DIAS = DIAS_POSIBLES.map((d) => ({ valor: String(d), etiqueta: String(d) }));
const OPCIONES_MINUTOS = MINUTOS_POSIBLES.map((m) => ({ valor: String(m), etiqueta: `${m} min` }));

function Formulario({ perfil, materiales }: { perfil: PerfilEntreno; materiales: readonly Material[] }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const guardar = useGuardarAjustes();
  const [dias, setDias] = useState(String(perfil.diasPorSemana));
  const [minutos, setMinutos] = useState(String(perfil.minutosPorSesion));
  // Solo material que el backend ofrece: si guardó uno que ya no se ofrece, no se manda de vuelta.
  const [material, setMaterial] = useState(() => perfil.material.filter((m) => materiales.some((o) => o.codigo === m)));
  const [error, setError] = useState<string>();

  const cambiado = () => {
    guardar.reset();
    setError(undefined);
  };
  const alternar = (codigo: string) => {
    cambiado();
    setMaterial((actual) => (actual.includes(codigo) ? actual.filter((m) => m !== codigo) : [...actual, codigo]));
  };
  const enviar = () => {
    if (material.length === 0) {
      setError(ERROR_SIN_MATERIAL);
      return;
    }
    guardar.mutate({ diasPorSemana: Number(dias), minutosPorSesion: Number(minutos), material });
  };

  return (
    <>
      <Seleccion
        etiqueta="Días por semana"
        opciones={OPCIONES_DIAS}
        valor={dias}
        alCambiar={(valor) => {
          cambiado();
          if (valor) setDias(valor);
        }}
      />
      <Seleccion
        etiqueta="Minutos por sesión"
        opciones={OPCIONES_MINUTOS}
        valor={minutos}
        alCambiar={(valor) => {
          cambiado();
          if (valor) setMinutos(valor);
        }}
      />
      <View accessibilityRole="group" accessibilityLabel="Material" style={estilos.grupo}>
        <Texto peso="700">Material</Texto>
        {error ? null : <Texto estilo="nota" tono="secundario">Marca todo lo que tienes a mano. Mínimo uno.</Texto>}
        <View style={estilos.fichas}>
          {materiales.map((m) => (
            <FichaOpcion key={m.codigo} etiqueta={m.nombre} rol="checkbox" marcada={material.includes(m.codigo)} alPulsar={() => alternar(m.codigo)} />
          ))}
        </View>
        {error ? (
          <View accessibilityRole="alert" accessibilityLiveRegion="polite">
            <Texto estilo="texto" color={tema.color.error}>{`Error: ${error}`}</Texto>
          </View>
        ) : null}
      </View>
      <Texto estilo="nota" tono="secundario">{NOTA_AJUSTES}</Texto>
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      {guardar.isSuccess ? <Aviso tipo="exito" mensaje="Guardado. Se aplica la próxima vez que generes o regeneres tu plan." /> : null}
      <Boton etiqueta="Guardar" etiquetaCargando="Guardando…" cargando={guardar.isPending} onPress={enviar} />
    </>
  );
}

function Contenido() {
  const perfil = usePerfilEntreno();
  const materiales = useMateriales();
  return (
    <ConConsulta consulta={perfil} cargando="Cargando tus ajustes…">
      {(datos) => (
        <ConConsulta consulta={materiales} cargando="Cargando el material…">
          {(lista) => <Formulario perfil={datos} materiales={lista} />}
        </ConConsulta>
      )}
    </ConConsulta>
  );
}

/** Días por semana, minutos por sesión y material con los que se genera el plan. */
export function AjustesPlan() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Ajustes del plan" />
      <Contenido />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    grupo: { gap: tema.space.s2 },
    fichas: opcionesFila(tema),
  });
}
