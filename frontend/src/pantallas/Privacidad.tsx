import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { ErrorApi, mensajeDeError } from '../api/errores';
import type { Consentimiento, DispositivoCuenta } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { CONSENTIMIENTOS, type InfoConsentimiento } from '../privacidad/catalogo';
import {
  useCerrarDispositivo,
  useConsentimientos,
  useDescargarDatos,
  useDispositivos,
  useRetirarConsentimiento,
} from '../privacidad/consultas';
import { type Theme, useTheme } from '../theme';

const fecha = (iso: string) => new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });

function TarjetaConsentimiento({ info, dado }: { info: InfoConsentimiento; dado: Consentimiento | undefined }) {
  const router = useRouter();
  const estilos = crearEstilos(useTheme());
  const [confirmando, setConfirmando] = useState(false);
  const retirar = useRetirarConsentimiento();
  const nombre = info.nombre.toLowerCase();

  const estado = dado ? `Dado el ${fecha(dado.dadoEn)}` : info.rutaParaDar ? 'No lo has dado' : 'Sin usar todavía';
  return (
    <View accessibilityLabel={`Consentimiento: ${info.nombre}`} style={estilos.tarjeta}>
      <Texto peso="700">{info.nombre}</Texto>
      <Texto estilo="nota" tono="secundario">{info.cubre}</Texto>
      <Texto estilo="nota">{estado}</Texto>
      {dado && !confirmando ? (
        <Boton variante="texto" etiqueta="Retirar" etiquetaAccesible={`Retirar consentimiento de ${nombre}`} onPress={() => setConfirmando(true)} />
      ) : null}
      {!dado && info.rutaParaDar ? (
        <Boton
          variante="secundario"
          etiqueta="Dar consentimiento"
          etiquetaAccesible={`Dar consentimiento de ${nombre}`}
          onPress={() => router.push(info.rutaParaDar!)}
        />
      ) : null}
      {dado && confirmando ? (
        <Confirmacion
          mensaje={info.alRetirar}
          etiquetaConfirmar={info.etiquetaRetirar}
          etiquetaCargando="Retirando…"
          cargando={retirar.isPending}
          error={retirar.isError ? mensajeDeError(retirar.error) : undefined}
          alConfirmar={() => retirar.mutate(info.tipo, { onSuccess: () => setConfirmando(false) })}
          alCancelar={() => {
            retirar.reset();
            setConfirmando(false);
          }}
        />
      ) : null}
    </View>
  );
}

function Consentimientos() {
  const consulta = useConsentimientos();
  const estilos = crearEstilos(useTheme());
  return (
    <>
      <Texto estilo="heading" encabezado>Consentimientos</Texto>
      <Texto tono="secundario">Tú decides qué guarda Forja. Retirar uno borra lo que dependía de él y la app sigue funcionando sin él.</Texto>
      <ConConsulta consulta={consulta} cargando="Cargando tus consentimientos…">
        {(dados) => (
          <View style={estilos.lista}>
            {CONSENTIMIENTOS.map((info) => (
              <TarjetaConsentimiento key={info.tipo} info={info} dado={dados.find((c) => c.tipo === info.tipo)} />
            ))}
          </View>
        )}
      </ConConsulta>
    </>
  );
}

function FilaDispositivo({ dispositivo }: { dispositivo: DispositivoCuenta }) {
  const estilos = crearEstilos(useTheme());
  const [confirmando, setConfirmando] = useState(false);
  const cerrar = useCerrarDispositivo();
  const plataforma = dispositivo.plataforma === 'pc' ? 'PC' : 'Móvil';
  const uso = dispositivo.ultimaActividad ? `último uso el ${fecha(dispositivo.ultimaActividad)}` : 'sin actividad registrada';
  return (
    <View accessibilityLabel={`Dispositivo: ${dispositivo.nombre}`} style={estilos.tarjeta}>
      <Texto peso="700">{dispositivo.nombre}</Texto>
      <Texto estilo="nota" tono="secundario">{`${plataforma} · ${uso}`}</Texto>
      {dispositivo.actual ? (
        <>
          <Texto estilo="nota" peso="700">Este dispositivo</Texto>
          <Texto estilo="nota" tono="secundario">Para salir de aquí usa «Cerrar sesión» en tu perfil.</Texto>
        </>
      ) : confirmando ? (
        <Confirmacion
          mensaje={`¿Cerrar la sesión en ${dispositivo.nombre}? Ese dispositivo tendrá que volver a entrar con tu contraseña.`}
          etiquetaConfirmar="Sí, cerrar sesión"
          etiquetaCargando="Cerrando…"
          cargando={cerrar.isPending}
          error={cerrar.isError ? mensajeDeError(cerrar.error) : undefined}
          alConfirmar={() => cerrar.mutate(dispositivo.id)}
          alCancelar={() => {
            cerrar.reset();
            setConfirmando(false);
          }}
        />
      ) : (
        <Boton variante="texto" etiqueta="Cerrar sesión" etiquetaAccesible={`Cerrar sesión en ${dispositivo.nombre}`} onPress={() => setConfirmando(true)} />
      )}
    </View>
  );
}

function Dispositivos() {
  const consulta = useDispositivos();
  const estilos = crearEstilos(useTheme());
  return (
    <>
      <Texto estilo="heading" encabezado>Dispositivos</Texto>
      <Texto tono="secundario">Donde tienes la sesión abierta. Cierra la de cualquiera que no reconozcas.</Texto>
      <ConConsulta consulta={consulta} cargando="Cargando tus dispositivos…">
        {(lista) => (
          <View style={estilos.lista}>
            {lista.map((d) => (
              <FilaDispositivo key={d.id} dispositivo={d} />
            ))}
          </View>
        )}
      </ConConsulta>
    </>
  );
}

const DEMASIADAS_PETICIONES = 429;
const LIMITE_DESCARGAS = 'Ya has descargado tus datos varias veces en la última hora. Vuelve a probar dentro de un rato.';

function errorDeDescarga(error: unknown): string {
  return error instanceof ErrorApi && error.estado === DEMASIADAS_PETICIONES ? LIMITE_DESCARGAS : mensajeDeError(error);
}

function TusDatos() {
  const estilos = crearEstilos(useTheme());
  const descargar = useDescargarDatos();
  return (
    <>
      <Texto estilo="heading" encabezado>Tus datos</Texto>
      <Texto tono="secundario">
        Un archivo JSON con todo lo que Forja guarda de ti: cuenta, perfil, plan, sesiones y, si los diste, tus datos de salud. Guárdalo en un sitio seguro. Hasta 5 descargas por hora.
      </Texto>
      <View style={estilos.lista}>
        <Boton
          variante="secundario"
          etiqueta="Descargar mis datos"
          etiquetaCargando="Preparando tus datos…"
          cargando={descargar.isPending}
          onPress={() => descargar.mutate()}
        />
        {descargar.isError ? <Aviso tipo="error" mensaje={errorDeDescarga(descargar.error)} /> : null}
        {descargar.isSuccess ? <Aviso tipo="exito" mensaje={`Archivo ${descargar.data} preparado.`} /> : null}
      </View>
    </>
  );
}

/** @param irADatos baja hasta «Tus datos», la última sección, en cuanto carga lo de encima. */
export function Privacidad({ irADatos = false }: { irADatos?: boolean }) {
  return (
    <Pantalla hueco="s4" seguirFinal={irADatos}>
      <EncabezadoAtras titulo="Privacidad" />
      <Consentimientos />
      <Dispositivos />
      <TusDatos />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    lista: { gap: tema.space.s2 },
    tarjeta: { gap: tema.space.s1, padding: tema.space.s35, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
  });
}
