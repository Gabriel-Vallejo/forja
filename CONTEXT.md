# Forja: contexto del proyecto

Contexto para agentes y para mí. Sale del plan de desarrollo del 2026-11-15.

## 1. Forja: entrena, come y suplementa según tu cuerpo

**Frase de venta:** *Fotos de tu cuerpo y tus marcas → una rutina que elige los ejercicios según tus palancas y tus puntos débiles. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.*

### 1.1 Por qué llega a 24

| Criterio | Cómo se llega a 5 |
|---|---|
| Reclut. | Motor de reglas y optimización en **Java** (lo más pedido en España) + IA evaluada + despliegue en Azure + usuarios reales del gimnasio. |
| Encaje | Las cuatro carpetas son imprescindibles. El `vision-service` saca proporciones del cuerpo (pose) y lee etiquetas (OCR). El `backend` es el cerebro (CP-SAT y programación lineal). La `database` tiene un dominio rico. El `frontend` se usa en cada sesión para apuntar series. |
| Hueco | Tres pruebas que se suman. Global DRO no cubre España ni complementos. Proteína por euro está probado en EE. UU. y vacío en España. Rutina por palancas: ya lo prometen apps de escaneo de iPhone y pago, sin método ni error publicados (fase 0). |
| Viab. | Pose (MediaPipe) y OCR (Florence-2) corren en CPU. Lo pesado son algoritmos, no GPU. |
| Uso | Entrenas fuerza e hipertrofia, comes alto en proteína y bajo en carbohidratos y tomas suplementos: es tu app diaria. |

**El riesgo es el alcance:** son tres apps. Por eso se construye por módulos y **cada módulo es un release publicable**. Si solo terminas el de entreno, ya tienes un proyecto completo.

### 1.2 Módulos

**A. Entreno (el núcleo, tipo Symmetric Strength + rutina)**
1. **Perfil:**
   - Sexo, edad, altura, peso, experiencia, días por semana, material (gimnasio completo, casa…), lesiones y objetivo (hipertrofia, fuerza o recomposición).
   - Músculos prioritarios.
   - Circunferencias con cinta (cuello, cintura, cadera) para estimar el % de grasa con la fórmula de la US Navy (pública).
2. **Proporciones por foto:**
   - Dos fotos estáticas en casa (de frente y de perfil, de cuerpo entero, ropa ajustada). Nada de grabarte en el gimnasio.
   - MediaPipe Pose saca los puntos articulares. Con tu altura como escala se calculan segmentos y ratios: fémur/torso, brazo/torso, anchura de hombros/cadera, envergadura aproximada.
   - Las fotos **no se guardan**: se procesan y se borran; solo quedan los ratios. Eso va escrito en la app y en el README (RGPD, datos de salud).
   - Si no quieres fotos, medidas manuales con cinta.
3. **Estándares de fuerza y puntos débiles:**
   - Registras tus marcas, o salen solas del registro de series (e1RM con Epley o Brzycki).
   - Percentiles de sentadilla, banca y peso muerto por sexo y peso corporal, calculados con el dataset de **OpenPowerlifting** (CSV en dominio público, código AGPLv3; comprobado en la fase 0). Ojo: son levantadores de competición, así que el percentil está sesgado hacia arriba. Se dice en la app.
   - Para los accesorios (press militar, remo, dominadas…), ratios frente a los básicos con heurísticas propias **[inferencia, documentarlas como tales]**.
   - Resultado: **mapa muscular coloreado** (fuerte, en la media, débil), como Symmetric Strength.
4. **Generador de rutina:** en el backend, ver 1.5.
5. **Registro de sesiones:**
   - Series, repes, peso y RIR, con temporizador de descanso, sin conexión.
   - **Progresión doble automática**: si completas el rango alto de repes a tu RIR objetivo, sube la carga.
   - Deload cuando el e1RM se estanca 2-3 semanas.
6. **Explicación con LLM:**
   - El LLM **solo explica** («te he puesto sentadilla hack en vez de barra baja porque tu ratio fémur/torso es alto»).
   - Traduce peticiones en lenguaje natural a una intención estructurada («me duele el hombro en el press» → `{restriccion: hombro, patron: empuje_horizontal}`).
   - Las reglas vuelven a resolver. El LLM nunca escribe series.

**B. Dieta (ProteEuro)**
1. **Objetivos:**
   - Gasto energético con Mifflin-St Jeor por factor de actividad, ajustado con tu peso real semana a semana. Si la tendencia no cuadra, se recalcula.
   - Proteína por kg: el metaanálisis de Morton 2018 sitúa la meseta en 1,62 g/kg/día de ingesta total (IC del 95 %: 1,03-2,20) y propone ~2,2 para quien quiera maximizar. Verificado: [referencias](docs/referencias.md#proteína).
   - Tope de carbohidratos netos para tu low-carb.
2. **Catálogo:**
   - Mercadona vía mercaapi o mercapy (no oficial; en línea el 2026-11-15, MIT, y se puede alojar con su `docker-compose`) + Open Food Facts.
   - **Precios aportados por usuarios** con la foto de la etiqueta del lineal, para cadenas sin API y sin hacer scraping.
3. **Optimizador:**
   - Programación lineal: minimiza el coste semanal sujeto a proteína ≥ objetivo, carbohidratos ≤ tope, kcal en rango, máximo de raciones por producto (variedad) y alimentos excluidos.
   - Resultado: «con 3,40 €/día llegas a 180 g de proteína con estos 7 productos».
4. **Avisos:** cuando baja de precio un producto de tu cesta (job programado o n8n).
5. **Recetas:** el LLM propone recetas **solo con lo que hay en la cesta**, en JSON validado (y las macros las recalcula el backend, no el LLM).

**C. Suplementos (SuplementoLimpio)**
1. Foto de la etiqueta → OCR → lista de ingredientes normalizada en JSON.
2. **Cruce con la lista de sustancias prohibidas** (la de la AMA, en su publicación del BOE: BOE-A-2025-25042 para 2026; ver [ADR 0005](docs/adr/0005-fuentes-de-datos.md)), con sinónimos (por ejemplo, la DMAA tiene muchos nombres), y con las **alertas de CELAD y AESAN**, que se ingieren de sus PDF.
3. **Ficha de riesgo con motivos.** Nunca dice «apto»: dice «sin señales en lo que se puede leer», porque ni Global DRO certifica complementos.
4. **Stack del usuario:** lo que tomas. Push si sale una alerta que afecta a algo de tu stack.
5. **(Opcional)** Dosis frente a la evidencia, por ejemplo creatina 3-5 g/día (o carga de ~0,3 g/kg/día durante 5-7 días) según la posición de la ISSN (Kreider 2017). Verificado: [referencias](docs/referencias.md#creatina).

### 1.3 Qué hace cada carpeta

| Carpeta | Qué hace |
|---|---|
| `backend` | Motor de rutina (CP-SAT), progresión y deload, estándares y percentiles, optimizador de dieta (LP), motor de riesgo de suplementos, ingesta programada (CELAD/AESAN, precios), avisos push, cola de trabajos para el vision-service. |
| `database` | Ejercicios con sus músculos y patrones (N:M), perfiles y medidas, sesiones y series, estándares, productos, precios históricos, nutrición, ingredientes, sinónimos, sustancias, alertas. |
| `frontend` | Registro de series sin conexión (uso diario), cámara guiada para las fotos de cuerpo y las etiquetas, mapa muscular, cesta semanal, ficha de riesgo, push. |
| `vision-service` | Pose → ratios corporales; OCR de etiquetas (suplementos, nutrición y precio de lineal) → JSON validado; extracción de tablas de los PDF de alertas; LLM local para explicar y para interpretar peticiones. |

### 1.4 Modelo de datos (resumen)

**Entreno**
- `usuario`, `perfil` (1:1), `medida_corporal` (histórico: fecha, tipo, valor), `ratio_corporal` (fecha, fémur/torso…, origen foto o manual)
- `musculo`, `patron_movimiento`, `material`
- `ejercicio`, `ejercicio_musculo` (N:M con rol primario/secundario y peso), `ejercicio_material`, `ejercicio_contraindicacion`
- `estandar_fuerza` (ejercicio, sexo, rango de peso, percentiles), `marca_usuario`
- `mesociclo`, `semana_plan`, `dia_plan`, `ejercicio_plan` (series, rango de repes, RIR objetivo)
- `sesion`, `serie_realizada` (peso, repes, RIR, e1RM calculado)

**Dieta**
- `cadena`, `producto`, `producto_cadena`, `precio` (histórico, fuente API o usuario), `nutricion_100g`
- `objetivo_macros`, `cesta`, `cesta_linea`, `receta`, `receta_ingrediente`

**Suplementos**
- `suplemento`, `marca`, `ingrediente`, `suplemento_ingrediente` (N:M con cantidad), `sinonimo`
- `sustancia_prohibida`, `version_lista` (por año), `alerta` (CELAD/AESAN, PDF origen), `alerta_producto`, `stack_usuario`

**Transversal:** `trabajo` (cola), `evento_uso`, `consentimiento`

Los ejercicios iniciales salen de un catálogo abierto: free-exercise-db (Unlicense, comprobado) o los ejercicios de wger (CC-BY-SA por entrada, comprobado). Encima añades tú los campos biomecánicos (`exige_movilidad_tobillo`, `sensible_ratio_femur`, `estres_hombro`…). Ese etiquetado es trabajo tuyo, y es lo que nadie más tiene.

### 1.5 Algoritmos del backend (lo que enseñas en la entrevista)

**Generador de rutina con OR-Tools CP-SAT (Java, Apache 2.0):**
- **Variables:**
  - `x[d][e]`: el ejercicio e va el día d (booleana).
  - `s[d][e]`: series del ejercicio e el día d (entera).
- **Restricciones duras** (y metas con holgura, ver [ADR 0002](docs/adr/0002-solver-cp-sat.md)):
  - Series semanales por músculo dentro de un rango según el objetivo: el **máximo es duro** y el mínimo es meta con holgura. Las series de los músculos secundarios cuentan a medias (método «fraccional», el de más respaldo en Pelland 2026).
  - Frecuencia ≥ 2 por músculo y semana. **Es una heurística práctica, no evidencia:** con el volumen igualado, la frecuencia no cambia la hipertrofia (Schoenfeld 2019). Ver [referencias](docs/referencias.md#frecuencia-de-entreno).
  - Duración máxima por sesión.
  - Material disponible y contraindicaciones por lesión.
  - Un máximo de ejercicios por patrón y día.
  - No repetir un básico pesado en días consecutivos.
- **Objetivo (maximizar):**
  - Cobertura de los músculos débiles y prioritarios (pesa más).
  - Encaje biomecánico, por ejemplo una penalización a la sentadilla trasera barra baja si el ratio fémur/torso es alto.
  - Preferencias del usuario.
  - Menos fatiga sistémica (cada ejercicio tiene un coste de fatiga).
- **Salida:** plan + **traza de por qué**: qué restricción o término del objetivo decidió cada elección. Eso es lo que el LLM convierte en texto.
- **Tests:** propiedades del tipo «nunca supera la duración», «nunca supera el máximo de series por músculo» y «con lesión de hombro no hay press por encima de la cabeza». Se pueden hacer con jqwik (property-based testing en Java).

Los rangos de volumen y frecuencia salen de la literatura: Schoenfeld 2017 y Pelland 2026 para volumen, y Schoenfeld 2016 y 2019 y Pelland 2026 para frecuencia. Las cifras MEV/MAV/MRV de Renaissance Periodization son de un libro de entrenadores, no revisado por pares, y se tratan como heurística. Citas verificadas en [docs/referencias.md](docs/referencias.md); van al ADR con sus referencias. Las heurísticas de palancas son **tuyas** y se presentan como heurísticas, no como ciencia.

**Progresión:** la doble progresión y el deload son una máquina de estados por ejercicio (`acumulando`, `subir_carga`, `estancado`, `deload`), con tests de cada transición.

**Optimizador de dieta:** programación lineal con OR-Tools (solver GLOP o CP-SAT si hay raciones enteras) en Java. Va en el backend y no en Python, porque es lógica de negocio y deja el `vision-service` solo para IA.

**Motor de riesgo de suplementos:** reglas puntuadas, por ejemplo coincidencia exacta con sustancia prohibida = alto, coincidencia por sinónimo = alto, aparece en alerta = alto y sin certificado = medio. Emparejamiento difuso de ingredientes con normalización y Jaro-Winkler o Levenshtein; si una coincidencia es dudosa, se marca para revisión.

### 1.6 IA del vision-service

| Tarea | Modelo | Licencia | Dónde corre | Métrica en `eval/` |
|---|---|---|---|---|
| Puntos del cuerpo | MediaPipe Pose Landmarker | Apache 2.0 | CPU (y en el móvil si quieres) | Error de los ratios frente a cinta métrica en 15-20 personas (compañeros del gimnasio) |
| OCR de etiquetas | Florence-2 base/large (`<OCR_WITH_REGION>`) | MIT | CPU | CER del OCR en 100 etiquetas propias |
| Normalizar a JSON | Qwen3-VL-2B o Gemma 4 E2B vía Ollama con `format` | Apache 2.0 | GPU del portátil o CPU lento | Precisión y recall de ingredientes extraídos frente a tu etiquetado |
| Explicar y entender peticiones | LLM pequeño de Ollama (familia Qwen3 o Gemma 4) | Apache 2.0 | GPU o CPU | Exactitud de la intención en 50 frases de prueba |

**Por qué la IA no es un wrapper:** extrae y mide, pero **las reglas deciden**. Cada modelo tiene su número publicado en el README.

### 1.7 API (principales)

```
POST /api/v1/perfil/fotos            → 200 + ratios (síncrono, en memoria; la foto no se guarda)
GET  /api/v1/perfil/ratios
GET  /api/v1/estandares              → percentiles y mapa muscular
POST /api/v1/rutinas/generar         → plan + traza (síncrono, del solver) + trabajo para la explicación del LLM
POST /api/v1/rutinas/{id}/ajustar    → texto libre → 202 + trabajo (intención con LLM) → re-solve
POST /api/v1/sesiones                → registro (idempotente, sincronización sin conexión)
GET  /api/v1/progresion/{ejercicio}
POST /api/v1/dieta/cesta/optimizar
POST /api/v1/precios                 → foto de lineal (crowdsourcing)
POST /api/v1/suplementos/escanear    → OCR síncrono + 202 + trabajo (normalización) → ficha de riesgo
GET  /api/v1/alertas
GET  /api/v1/trabajos/{id}           → estado y resultado (solo del dueño)
```

### 1.8 Fases

| Fase | Contenido | Entregable | Horas aprox. **[estimación]** |
|---|---|---|---|
| 0. Validación y diseño | Probar a fondo Fitbod, Alpha Progression, Dr. Muscle, Gravl, JuggernautAI, RP Hypertrophy y Hevy: ¿alguna usa proporciones corporales? Estado de mercaapi y licencia de la lista de la AMA. ADR 0001-0005. Plantilla `repo-base`. | Doc de competencia en `docs/` + repo con CI vacío en verde | 15 |
| 1. Entreno MVP | Catálogo de ejercicios etiquetado, perfil manual, CP-SAT, registro de sesiones, progresión doble. Sin fotos todavía. | **v0.1** + post 1 («mi app me arma la rutina con un solver») | 60 |
| 2. Cuerpo y estándares | MediaPipe, ratios, eval con cinta, percentiles OpenPowerlifting, mapa muscular, LLM explicador. | **v0.2** + post 2 (el mapa muscular y el error medido) | 40 |
| 3. Dieta | Catálogo, LP, precios por foto, avisos, recetas. | **v0.3** + post 3 («180 g de proteína por 3,40 €») | 40 |
| 4. Suplementos | OCR + normalización, AMA, ingesta de CELAD, ficha de riesgo, push. | **v0.4** + post 4 (el pre-entreno escaneado) | 35 |
| 5. Producción | Azure + IaC, observabilidad, ZAP, 20 usuarios del gimnasio, Play en prueba cerrada. | **v1.0** + post de cierre con métricas de uso | 20 |

### 1.9 Riesgos y cómo se mitigan

| Riesgo | Mitigación |
|---|---|
| El alcance (tres apps) | Releases por módulo. Si te quedas sin tiempo, v0.2 ya es un proyecto completo. |
| Datos de salud (fotos, medidas) | Las fotos no se guardan, consentimiento explícito, borrado de cuenta real y cifrado en reposo de las medidas. ADR de privacidad. |
| Heurísticas de palancas sin base fuerte | Presentarlas como heurísticas, con la regla visible y desactivable. Nunca como diagnóstico. |
| Que el usuario se fíe de «sin riesgo» en un suplemento | Lenguaje de riesgo y aviso fijo de que la app no certifica. |
| mercaapi desaparece | Capa de adaptadores de fuente de precios; Open Food Facts y precios de usuarios como respaldo. |
| Lesiones por una rutina mal generada | Restricciones de lesión duras, límites de volumen conservadores y aviso de consultar a un profesional. |

---

---

## 0. Base común: lo que lleva cada repo para el 5 en reclutadores

Docker y CI ya no diferencian: Docker lo usa el 73,8 % según Stack Overflow 2025. Diferencian el despliegue, los usuarios reales, el modelo evaluado con números, la observabilidad y la seguridad. Todo esto va en los cuatro proyectos y se hace **una vez**: plantilla `repo-base`, que se clona para cada uno.

### 0.1 Estructura del repo

Igual que Style & Scent Engine, con cuatro añadidos (`docs/`, `eval/`, `infra/`, `ops/`):

```
<proyecto>/
├── backend/            Java 21 + Spring Boot 4 + JPA + Flyway
├── database/           init de MySQL 8.4 LTS, seeds, scripts de carga de datos
├── frontend/           React Native + Expo (TypeScript), dev build + EAS
├── vision-service/     FastAPI + modelos (o ai-service si no es visión)
├── eval/               datasets etiquetados, scripts de evaluación, resultados versionados
├── docs/
│   ├── adr/            decisiones de arquitectura numeradas (0001-…)
│   ├── arquitectura.md diagrama Mermaid C4 + flujos
│   └── api/            OpenAPI exportado
├── infra/              despliegue (compose de producción, Caddy, scripts)
├── ops/                Prometheus, Grafana (dashboards en JSON), alertas
├── .github/workflows/  ci.yml, eval.yml, security.yml, release.yml
├── docker-compose.yml  perfiles: dev, gpu, obs
├── CONTEXT.md          contexto para agentes (como en Style & Scent)
├── README.md
├── SECURITY.md
├── CHANGELOG.md
└── LICENSE
```

### 0.2 Backend (Spring Boot)
- **Arquitectura por capas por dominio** (`entreno/`, `dieta/`…), no por tipo. Cada dominio tiene `api`, `servicio`, `repositorio` y `modelo`.
- **Flyway** para todas las migraciones. Nada de `ddl-auto=update`: un reclutador de banca lo mira.
- **OpenAPI** con springdoc. El cliente TypeScript del frontend se genera con `openapi-typescript`, así el contrato no se desincroniza.
- **Seguridad:**
  - Spring Security con JWT de acceso corto y refresh rotatorio. Contraseñas con Argon2 o BCrypt.
  - Validación con Bean Validation en cada DTO.
  - Rate limiting con Bucket4j.
  - CORS cerrado.
  - Errores sin trazas (`ProblemDetail`, RFC 9457).
- **Comunicación con el vision-service:** HTTP con timeout, reintentos con backoff y *circuit breaker* (Resilience4j). Si el modelo cae, la app degrada (entrada manual) en vez de romperse.
- **Trabajos pesados:** tabla `trabajo` con `SELECT … FOR UPDATE SKIP LOCKED` (MySQL 8.0.1+ lo soporta; usamos 8.4 LTS) como cola. No hace falta Redis ni Kafka. Lo explica el [ADR 0003](docs/adr/0003-cola-de-trabajos-gpu.md).
- **Tests:**
  - JUnit 5 + Mockito para las reglas.
  - **Testcontainers** con MySQL real para repositorios y migraciones.
  - Tests de contrato contra el OpenAPI.
  - Objetivo ≥ 80 % de cobertura de líneas en el dominio (JaCoCo), con informe en el CI.

### 0.3 Base de datos
- MySQL 8.4 LTS en Docker (imagen `mysql:8.4`; la 8.0 no tiene soporte desde abril de 2026, ver [ADR 0001](docs/adr/0001-stack.md)), 3FN y claves foráneas con `ON DELETE` pensado. Índices justificados en un ADR con `EXPLAIN`.
- **Búsqueda vectorial:** MySQL Community **no** calcula distancias vectoriales (`DISTANCE()` solo existe en HeatWave y MySQL AI). Decisión común: los datos y el vector «oficial» se guardan en MySQL y el índice vive en **Qdrant** (Apache 2.0, un contenedor más). Un job reconstruye Qdrant desde MySQL, así MySQL sigue siendo la fuente de verdad. Esa decisión, con sus alternativas (MariaDB Vector, pgvector, FAISS), es un ADR que luce en la entrevista. **Forja no la usa:** el cruce de ingredientes va con Jaro-Winkler ([ADR 0001](docs/adr/0001-stack.md)).

### 0.4 Frontend (Expo)
- **Dev build**, no Expo Go, porque hace falta cámara avanzada y módulos nativos. APK por EAS publicada en **GitHub Releases** y, si puedes, en **Google Play en prueba cerrada**: «está en Play Store» pesa más que «hay un APK».
- **Datos:** TanStack Query para la caché y el modo sin conexión, y Zod para validar respuestas.
- **Seguridad:** tokens en `expo-secure-store`, nunca en AsyncStorage.
- **Accesibilidad:** etiquetas, contraste AA y tamaños dinámicos. Puntúa y casi ningún junior lo hace.
- **Tests:** Jest + React Native Testing Library en componentes clave y Maestro para dos o tres flujos E2E.

### 0.5 Vision-service (FastAPI)
- **Contratos Pydantic** de entrada y salida; el JSON Schema se exporta al repo.
- **Registro de modelos** (`models.yaml`): nombre, `revision` fijada de Hugging Face, licencia, tamaño y tarea. El CI falla si un modelo no tiene licencia anotada.
- Perfil `gpu` y perfil `cpu` en compose. El VPS usa el de CPU y los trabajos de GPU van a un worker en tu portátil (ver 0.7).
- **Salida estructurada:** Ollama con `format` = JSON Schema, después validación Pydantic y, si falla, reintento con el error en el prompt (máximo 2) y fallback.
- **Tests:** pytest con imágenes de muestra, y tests de regresión que comparan con salidas de referencia.

### 0.6 Evaluación del modelo (lo que más te separa del «wrapper»)
- Carpeta `eval/` con un **dataset etiquetado por ti** (100-300 ejemplos según el proyecto), su ficha (`DATASET.md`: origen, licencia, cómo se etiquetó) y un script que saca métricas.
- **Workflow `eval.yml`:** corre la evaluación en cada PR que toque el `vision-service` y **falla si la métrica cae** por debajo del umbral. Publica la tabla en el resumen del job.
- **Tabla de latencias** p50/p95 en GPU frente a CPU, y antes/después de cuantizar (ONNX o int8). Va al README.

### 0.7 Despliegue
- **VPS Hostinger:** `docker compose -f infra/compose.prod.yml` con Caddy (HTTPS automático) delante.
- **Nube para el CV:** la habilidad más pedida en España es cloud computing (más de 7.000 ofertas en el Mapa del Empleo de Fundación Telefónica). Despliega al menos un proyecto (Forja) en **Azure con Azure for Students** (crédito sin tarjeta **[verificar importe actual]**): Azure Container Apps para backend y vision-service CPU, y Azure Database for MySQL. Infraestructura como código con Bicep o Terraform en `infra/`.
- **Worker de GPU:** un proceso en tu portátil que consulta la cola del backend por HTTPS con un token de worker. Así el VPS no necesita GPU y el modelo grande sigue en tu 5070. Ese patrón es en sí una decisión de arquitectura contable.

### 0.8 Observabilidad
- Micrometer + Prometheus + Grafana (perfil `obs`), con dashboards de latencia por endpoint, errores, longitud de la cola y latencia del modelo.
- Logs JSON con `traceId`. OpenTelemetry entre el backend y el vision-service para ver la traza completa de una petición.
- Una captura del dashboard en el README.

### 0.9 Seguridad en el CI (`security.yml`)
- **CodeQL** (gratis en repos públicos), **Dependabot**, **gitleaks** (secretos), **Trivy** (imágenes Docker) y **OWASP ZAP baseline** contra el backend levantado en el CI.
- `SECURITY.md` con cómo reportar fallos. Tus reglas: nada de credenciales de competiciones en el repo.

### 0.10 Usuarios reales y métricas de producto
- **Usuarios:** 10-30 reales (compañeros de la USAL, del gimnasio, del equipo de CTF). Tabla `evento_uso` propia y anónima: retención a 7 días y acciones por semana. Sin terceros de analítica.
- **Feedback:** formulario dentro de la app → issue de GitHub vía n8n (tu terreno).

### 0.11 README (lo primero que se mira)
1. GIF de 10-15 s del flujo estrella arriba del todo.
2. Una frase de qué resuelve y para quién.
3. Badges: CI, cobertura, eval, licencia, última release.
4. **Resultados medidos**: tabla de métricas del modelo y latencias.
5. Diagrama de arquitectura (Mermaid).
6. Cómo arrancarlo en un comando (`docker compose --profile dev up`).
7. Decisiones (enlaces a los ADR).
8. Límites conocidos y licencias de los modelos.

### 0.12 LinkedIn
Datos de vendedores, no estudios:
- Los carruseles o documentos rinden más que el vídeo.
- Los enlaces externos en el cuerpo recortan el alcance en torno a un 60 %: el enlace al repo va en el primer comentario.
- La primera hora decide buena parte del alcance.

**Plan por proyecto:** 3-4 posts, uno por hito, en formato carrusel: problema, demo, número medido, decisión técnica, enlace en comentario. Pinea el repo en GitHub y añádelo en «Proyectos» del perfil.

---
