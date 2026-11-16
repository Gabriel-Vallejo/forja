# AGENTS.md — Forja

Las instrucciones de este proyecto están en [`CLAUDE.md`](CLAUDE.md), en esta misma carpeta. **Léelo entero antes de hacer nada** y síguelo como si estuviera escrito aquí.

- Donde `CLAUDE.md` dice «Claude», entiende «el agente» (tú).
- Si eres Codex trabajando por encargo de Claude (§7), haz solo la tarea acotada que te pasaron, en su rama, y deja el vault a Claude salvo que te lo pida.
- `CLAUDE.md` cita el plugin ECC y agentes de Claude Code. Si tu harness no los tiene, sáltate esas partes.
- No copies `CLAUDE.md` aquí: si cambia una regla, se cambia allí.

Antes de diseñar o implementar, lee también [docs/PRD.md](docs/PRD.md) (qué y para quién), [docs/arquitectura.md](docs/arquitectura.md) (cómo encaja todo), [docs/design-system.md](docs/design-system.md) (para cualquier pantalla) y los ADR de [docs/adr/](docs/adr/README.md) que toque tu tarea.

## Estándares de código

Esta sección sí vive aquí: son las normas de código para cualquier agente.

### Para todo el repo
- **Idioma:** comentarios, mensajes de commit, textos de la app y nombres del dominio en español (`usuario`, `sesion`, `restriccion`). En identificadores de código, **sin tildes ni ñ** (`sesion`, `anio_nacimiento`); en los textos, con todas sus tildes.
- **TDD:** primero el test que falla, después el código. Cobertura mínima del 80 % en el dominio.
- **Commits convencionales en español** (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`), uno por cambio coherente, directos a `main`.
- **Secretos:** solo en `.env`, con su nombre en `.env.example`. Nunca en código, tests, logs ni fixtures.
- **Datos de salud** (ADR 0004): nunca en logs, nunca en fixtures reales; en tests, datos sintéticos.
- **El LLM nunca decide** (ADR 0007): solo extrae a un esquema cerrado o redacta explicaciones. Las reglas y el solver deciden; las banderas rojas van por reglas.
- **Finalidad de fitness:** revisa cualquier texto visible contra las palabras prohibidas de `CLAUDE.md` §4.
- Funciones de menos de 50 líneas, archivos de menos de 800, sin anidar más de 4 niveles y sin valores mágicos (constantes con nombre).

### `backend/` (Java 21, Spring Boot 4)
- **Paquetes por dominio** (`entreno`, `restricciones`, `sesiones`, `dieta`, `suplementos`, `cuenta`, `comun`), cada uno con `api`, `servicio`, `repositorio` y `modelo`. Un dominio no accede a las tablas de otro.
- **DTO como `record`**, validados con Bean Validation. Errores como `ProblemDetail` (RFC 9457), sin trazas.
- **Esquema solo con Flyway:** `src/main/resources/db/migration/V{n}__{descripcion}.sql`. Nada de `ddl-auto`. Cada migración, con su test en Testcontainers (MySQL 8.4 real).
- **Tests:** JUnit 5 y Mockito para reglas; jqwik para propiedades del solver y de la progresión; Testcontainers para repositorios y migraciones; JaCoCo con umbral del 80 %.
- Formato con el que traiga el proyecto (Spotless o equivalente) y sin advertencias del compilador.

### `vision-service/` (Python, FastAPI)
- Python 3.12, Pydantic v2 para todos los contratos y su JSON Schema exportado. Salidas de LLM con `format` de Ollama + validación + reintento (máximo 2).
- `ruff` (lint y formato) y `mypy --strict` sin errores. Tests con `pytest`, cobertura del 80 %.
- Imágenes siempre en memoria: nada de `UploadFile` con volcado a disco (ADR 0003, enmienda 1).
- Cada modelo, en `models.yaml` con revisión fijada y licencia, y con su evaluación en `eval/`.

### `frontend/` (React Native + Expo, TypeScript)
- TypeScript `strict`, ESLint sin avisos y `tsc --noEmit` en el CI.
- Estilos solo con los tokens de `theme.ts` ([docs/design-system.md](docs/design-system.md)); nada de colores ni tamaños sueltos.
- Datos con TanStack Query y respuestas validadas con Zod; cliente de la API generado desde el OpenAPI.
- Todo lo nativo, detrás de una interfaz con `.native.ts` y `.web.ts` (ADR 0006). El registro de series lee de la base local (ADR 0008).
- Tokens en `expo-secure-store` (móvil) o en el llavero (PC), nunca en `AsyncStorage` ni `localStorage` (ADR 0009).
- Accesibilidad: etiqueta en cada control, contraste AA, tamaños dinámicos y zonas táctiles de al menos 44 × 44.
- Tests con Jest y React Native Testing Library; flujos clave con Maestro.

### `eval/`
- Cada dataset con su `DATASET.md` (origen, licencia y cómo se etiquetó), sin datos personales reales salvo consentimiento por escrito y siempre fuera del repo (ADR 0004).
- Las métricas se versionan; el CI falla si empeoran.
