import type { AlmacenRegistro, CargaSiguiente, PlanGuardado, SerieLocal, SesionLocal } from './almacen';

/** Almacén en memoria con el mismo contrato que el de SQLite. Para los tests. */
export function crearAlmacenMemoria(): AlmacenRegistro {
  // Los Map conservan el orden de inserción, como el rowid de SQLite.
  const sesiones = new Map<string, SesionLocal>();
  const series = new Map<string, SerieLocal>();
  const cargas = new Map<string, CargaSiguiente>();
  const planes = new Map<string, PlanGuardado>();
  const claveCarga = (usuario: string, ejercicio: string) => `${usuario}\u0000${ejercicio}`;

  return {
    async insertarSesion(sesion) {
      if (sesiones.has(sesion.uuid)) throw new Error('La sesión ya existe.');
      sesiones.set(sesion.uuid, { ...sesion });
    },
    async actualizarSesion(uuid, cambios) {
      const actual = sesiones.get(uuid);
      if (actual) sesiones.set(uuid, { ...actual, ...cambios });
    },
    async borrarSesion(uuid) {
      sesiones.delete(uuid);
      for (const [clave, serie] of series) if (serie.sesion === uuid) series.delete(clave);
      for (const [clave, carga] of cargas) if (carga.sesion === uuid) cargas.delete(clave);
    },
    async leerSesion(uuid) {
      const sesion = sesiones.get(uuid);
      return sesion ? { ...sesion } : null;
    },
    async listarSesiones({ usuario, estados } = {}) {
      return [...sesiones.values()]
        .filter((s) => (usuario === undefined || s.usuario === usuario) && (!estados || estados.includes(s.estado)))
        .map((s) => ({ ...s }));
    },
    async insertarSerie(serie) {
      if (series.has(serie.uuid)) throw new Error('La serie ya existe.');
      series.set(serie.uuid, { ...serie });
    },
    async listarSeries(sesion) {
      return [...series.values()].filter((s) => s.sesion === sesion).map((s) => ({ ...s }));
    },
    async guardarCarga(carga) {
      const clave = claveCarga(carga.usuario, carga.ejercicio);
      cargas.delete(clave);
      cargas.set(clave, { ...carga });
    },
    async listarCargas(usuario) {
      return [...cargas.values()]
        .filter((c) => c.usuario === usuario)
        .sort((a, b) => a.ejercicio.localeCompare(b.ejercicio))
        .map((c) => ({ ...c }));
    },
    async borrarCarga(usuario, ejercicio) {
      cargas.delete(claveCarga(usuario, ejercicio));
    },
    async guardarPlan(plan) {
      planes.set(plan.usuario, { ...plan });
    },
    async leerPlan(usuario) {
      const plan = planes.get(usuario);
      return plan ? { ...plan } : null;
    },
    async borrarPlan(usuario) {
      planes.delete(usuario);
    },
    async olvidarDolor(usuario) {
      const suyas = new Set([...sesiones.values()].filter((s) => s.usuario === usuario).map((s) => s.uuid));
      for (const [clave, s] of series) if (suyas.has(s.sesion)) series.set(clave, { ...s, dolor: null });
    },
  };
}
