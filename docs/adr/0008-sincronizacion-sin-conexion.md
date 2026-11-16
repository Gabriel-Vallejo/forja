# 0008. Registro de series sin conexión: outbox en el móvil y series que solo se añaden

- **Estado:** aceptada (2026-11-16)
- **Fecha:** 2026-11-16

## Contexto
El registro de series es el uso diario de Forja y ocurre en el gimnasio, donde la cobertura falla. El plan solo decía «`POST /sesiones` idempotente», y la revisión de arquitectura lo señaló como el mayor hueco: una caché de TanStack Query no es una cola de envío, no hay resolución de conflictos y la progresión doble vive en el backend, así que sin conexión la app no sabría el siguiente objetivo de carga. Además, el solver puede regenerar el plan ([0002](0002-solver-cp-sat.md), [0007](0007-motor-de-seleccion-de-ejercicios.md)) mientras hay series registradas sin subir.

En PC no hay registro sin conexión ([0006](0006-version-de-pc.md)): esto es solo para el móvil.

## Decisión
1. **Identidad en el cliente.** Cada sesión y cada serie nace en el móvil con un UUID v7 (ordenable por tiempo). Ese UUID es su clave en el servidor y su clave de idempotencia: reenviar lo mismo no duplica nada.
2. **Upsert por sesión:** `PUT /api/v1/sesiones/{uuid}` con la sesión entera (cabecera y series). Responde 200 tanto la primera vez como en un reenvío idéntico. Sustituye al `POST /sesiones` del plan.
3. **Las series solo se añaden.** Una serie registrada no se edita ni se borra en el servidor: una corrección es una serie nueva con `corrige_a = <uuid>` y una anulación es una serie con `anulada = true` que apunta a la original. Así no hay conflictos de escritura entre dispositivos, y el historial y la progresión se calculan siempre sobre la última versión válida de cada serie.
4. **Outbox persistente en el móvil.** Una tabla local en `expo-sqlite` (en el móvil es estable; su versión web en alpha no se usa porque en PC no hay registro sin conexión) guarda cada sesión pendiente con su estado (`pendiente`, `enviando`, `confirmada`, `rechazada`) y sus intentos. La interfaz se lee de la base local, no de la red.
   - Se envía al recuperar la red, al abrir la app y al cerrar la sesión de entreno, con backoff exponencial.
   - Una respuesta 4xx de validación marca la sesión como `rechazada` y la app lo enseña («revisa esta sesión»); nunca se borra sola. Un 401 espera a renovar el token ([0009](0009-autenticacion.md)). Si la cuenta ya no existe (410), se avisa y se descarta el outbox.
5. **El plan viaja con lo necesario para entrenar sin red.** Cada `ejercicio_plan` lleva el objetivo de la sesión (series, rango de repeticiones, RIR y carga) y la **regla de la siguiente**: «si completas el rango alto a tu RIR objetivo en todas las series, la próxima vez +X kg; si no, repite». La app aplica esa regla sola entre sesiones. Cuando sincroniza, el servidor recalcula la progresión con la máquina de estados completa (deload incluido) y su resultado manda.
6. **Plan versionado.** Cada plan generado es una versión inmutable (`plan_version`). Cada sesión guarda la versión con la que se hizo. Si el solver regenera el plan, las sesiones ya registradas siguen apuntando a su versión, y la nueva solo vale para las siguientes. Si llega una sesión de una versión antigua, se acepta y alimenta la progresión igual.
7. **Dos relojes.** `realizada_en` (reloj del móvil, lo que ve el usuario) y `recibida_en` (reloj del servidor, para auditoría). Si el reloj del móvil está desfasado más de 24 h respecto a la recepción, se guarda igual y se marca para revisión, sin rechazarla.
8. **Tests:** sin red se registran sesiones, se corrige una serie, se recupera la red y el servidor queda con el mismo estado; reenviar dos veces no duplica; una sesión de una versión de plan antigua se acepta; un 401 a mitad de envío no pierde nada.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Solo caché de TanStack Query con reintentos | Cero código propio | Se pierde al cerrar la app; no es una cola; sin resolución de conflictos |
| Sincronización con un CRDT o un motor tipo PowerSync/ElectricSQL | Resuelve conflictos genéricos | Otra pieza de infraestructura; con series que solo se añaden no hay conflictos que resolver |
| `POST /sesiones` con clave de idempotencia en cabecera | Estándar | La sesión crece serie a serie; un PUT idempotente de la sesión entera es más simple de reintentar |
| Calcular toda la progresión en el móvil | Funciona sin red siempre | Duplica la lógica en dos lenguajes; el deload necesita el historial completo |

## Consecuencias
- **Gana:** el gimnasio sin cobertura deja de ser un problema, los reenvíos son seguros y el historial es auditable.
- **Cuesta:** una base local y su migración en el móvil, la pantalla de sesiones rechazadas y que la «última versión válida» de una serie se calcule en las consultas (una vista en MySQL).
- **Modelo de datos:** `sesion` y `serie_realizada` con UUID como clave, `corrige_a`, `anulada`, `plan_version_id`, `realizada_en` y `recibida_en`; tabla `plan_version`; la regla de la siguiente en `ejercicio_plan`.
- **API:** `PUT /api/v1/sesiones/{uuid}` sustituye a `POST /api/v1/sesiones`.
