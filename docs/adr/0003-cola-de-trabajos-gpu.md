# 0003. Cola de trabajos en MySQL y worker de GPU en el portátil

- **Estado:** aceptada (2026-11-15)
- **Fecha:** 2026-11-15

## Contexto
El VPS no tiene GPU. Los modelos que sí la quieren (el LLM que normaliza ingredientes a JSON, el que explica la rutina y las recetas) corren en el portátil (RTX 5070, ~8 GB), que no está encendido siempre y está detrás de un NAT doméstico. Hay que mandar trabajo de un lado a otro sin exponer el portátil a internet ni montar infraestructura que una persona no pueda mantener.

Además, las fotos de cuerpo y de etiquetas **no se guardan** ([CLAUDE.md](../../CLAUDE.md) §4). Una cola que guarde imágenes contradice esa regla.

## Decisión
1. **La cola es una tabla de MySQL**, `trabajo`, y se reclama con `SELECT … FOR UPDATE SKIP LOCKED` (MySQL 8.0.1 o superior; usamos 8.4, ver [0001](0001-stack.md)). No hay Redis, RabbitMQ ni Kafka.
2. **El worker tira de la cola; el backend no empuja.** El worker de GPU llama por HTTPS (Caddy) a `POST /api/v1/worker/trabajos/reclamar` con un token de worker. El portátil no abre puertos y **no tiene credenciales de la base de datos**: solo habla con la API.
3. **Por la cola solo pasa texto y JSON, nunca imágenes:**
   - **Fotos de cuerpo:** MediaPipe Pose en CPU, **síncrono** dentro de la petición y en memoria. Devuelve los ratios y la foto no se escribe en ningún sitio. Esto cambia la API del plan: `POST /perfil/fotos` pasa de `202 + trabajo` a `200 + ratios`.
   - **Etiquetas de suplementos y precios:** el OCR (Florence-2 en CPU) también es síncrono y en memoria. Lo que se encola es el **texto** del OCR para que el LLM lo normalice a JSON (`202 + trabajo`).
4. **Modelo de la tabla:**
   - Campos: `id`, `tipo`, `estado` (`pendiente`, `en_curso`, `hecho`, `fallido`), `prioridad`, `carga` (JSON), `resultado` (JSON), `intentos`, `max_intentos`, `disponible_en`, `lease_hasta`, `lease_token`, `worker_id`, `modelo_usado`, `clave_idempotencia` (única) y las fechas.
   - Índice `(estado, tipo, disponible_en, prioridad)` para el reclamo.
5. **Arrendamiento (lease):** al reclamar un trabajo, se marca `en_curso` con `lease_hasta = ahora + 2 min` y un `lease_token` nuevo. El worker renueva el arrendamiento mientras trabaja y entrega el resultado con su `lease_token`; si no coincide, el resultado se rechaza. Un job programado devuelve a `pendiente` los trabajos con el arrendamiento caducado y pasa a `fallido` los que agotan `max_intentos`, con backoff en `disponible_en`.
6. **Si el portátil está apagado:** si nadie reclama un trabajo de GPU en 10 min **[ajustable]**, lo toma un worker de CPU en el VPS con un modelo más pequeño. `modelo_usado` queda guardado para que `eval/` compare la calidad de cada uno.
7. **El cliente consulta** `GET /api/v1/trabajos/{id}` con sondeo cada pocos segundos. El push queda para la fase 4.
8. **Seguridad del worker:**
   - un token por worker, guardado como hash y revocable;
   - el token solo abre las rutas `/worker/**`;
   - rate limiting;
   - la carga nunca sale en los logs.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Redis + RQ/Celery | Estándar en Python, rápido | Otro servicio con estado; Celery mete Python en el lado del backend; el portátil necesitaría llegar a Redis (exponerlo o un túnel) |
| RabbitMQ | Colas de verdad, ACK y DLQ | Infraestructura extra para decenas de trabajos al día |
| Kafka | Escala | Desproporcionado; es un log, no una cola de tareas |
| Azure Queue Storage / Service Bus | Gestionado | Ata la cola a la nube, y el VPS no está en Azure; coste y credenciales en el portátil |
| HTTP síncrono al portátil (túnel) | Sin cola | Si el portátil duerme, la petición falla; expone el portátil |
| Encolar las imágenes | Un solo camino para todo | Guarda fotos (aunque sea un rato) y choca con la regla de privacidad |

## Consecuencias
- **Gana:**
  - cero servicios nuevos;
  - la cola es transaccional con los datos (el trabajo se crea en la misma transacción que la petición);
  - se ve con un `SELECT`, y Grafana muestra la longitud de la cola;
  - el patrón «la GPU de casa tira de la nube» se explica bien en una entrevista.
- **Cuesta:**
  - sondear la tabla genera carga en MySQL; es despreciable a esta escala con el índice y el worker esperando 2-5 s entre sondeos vacíos;
  - el arrendamiento y la limpieza hay que escribirlos y probarlos (Testcontainers con dos workers que compiten).
- **Límite:** si llegaran miles de trabajos por minuto, migrar a una cola dedicada. La interfaz `ColaTrabajos` del backend lo deja preparado sin tocar a quien la usa.
