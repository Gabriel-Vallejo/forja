import type { SerieLocal } from './almacen';

// Las series solo se añaden (ADR 0008 §3): una corrección es otra serie con `corrigeA` y una anulación, otra con
// `anulada`. Lo que cuenta es la última versión de cada cadena.

function siguientes(series: readonly SerieLocal[]): Map<string, SerieLocal> {
  const mapa = new Map<string, SerieLocal>();
  for (const s of series) if (s.corrigeA) mapa.set(s.corrigeA, s);
  return mapa;
}

function seguir(inicio: SerieLocal, mapa: Map<string, SerieLocal>): SerieLocal {
  let actual = inicio;
  const vistas = new Set([actual.uuid]);
  for (let siguiente = mapa.get(actual.uuid); siguiente && !vistas.has(siguiente.uuid); siguiente = mapa.get(actual.uuid)) {
    vistas.add(siguiente.uuid);
    actual = siguiente;
  }
  return actual;
}

/** El final de la cadena de la serie `uuid` (puede ser una anulación). */
export function ultimaVersion(series: readonly SerieLocal[], uuid: string): SerieLocal | undefined {
  const inicio = series.find((s) => s.uuid === uuid);
  return inicio && seguir(inicio, siguientes(series));
}

/** La última versión de cada serie, sin las anuladas, en el orden en que se registró la original. */
export function ultimaVersionValida(series: readonly SerieLocal[]): SerieLocal[] {
  const uuids = new Set(series.map((s) => s.uuid));
  const mapa = siguientes(series);
  return series
    .filter((s) => !s.corrigeA || !uuids.has(s.corrigeA))
    .map((raiz) => seguir(raiz, mapa))
    .filter((ultima) => !ultima.anulada);
}
