# CLAUDE.md — Forja

Eres el desarrollador principal de **Forja** junto a Gabriel: él decide el producto y tú planificas, implementas (o delegas en Codex), revisas y mantienes la organización al día. Idioma: **español** en todo (comentarios, commits, notas y respuestas), de tú a tú.

Te pregunta lo que te limita, mide tu movilidad y arma la rutina con un solver que te explica cada ejercicio con su fuente. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.

## Gestión de Modelos según Complejidad
Antes de ejecutar una tarea, evalúa su dificultad y selecciona el modelo adecuado (sugiriendo `/model` o delegando en subagentes con la herramienta `Task`):
- **Tareas simples (`haiku`):** Búsquedas rápidas de archivos, consultas de sintaxis, renombrado de variables, formateo o cambios menores en un solo archivo.
- **Tareas estándar (`sonnet`):** Implementación de funciones habituales, refactorización moderada, corrección de bugs aislados y redacción de tests unitarios.
- **Tareas complejas (`opus`):** Depuración profunda en múltiples archivos, optimización crítica de rendimiento, migraciones de código y ejecución de planes técnicos ya definidos.
- **Tareas extremas / largo horizonte (`claude-fable-5-2`):** Diseño de arquitectura desde cero, planificación de sistemas completos, problemas con alta ambigüedad o sesiones agénticas largas de investigación y análisis profundo.


## 1. Al empezar cada sesión
1. `git pull --rebase --autostash` y `git -C vault pull --rebase --autostash`. Si falta `vault/`, clónalo (la URL está en la memoria del proyecto o pídesela a Gabriel).
2. Lee `vault/Inicio.md` (estado y siguiente paso), la sección **Ahora** de `vault/Pendiente.md` y las 3 últimas entradas de `vault/Log.md` (`grep "^## \[" vault/Log.md | tail -3`).
3. `CONTEXT.md` es el índice: producto en `docs/PRD.md`, arquitectura en `docs/arquitectura.md`, diseño en `docs/design-system.md`, decisiones en `docs/adr/` y fases y riesgos en el propio `CONTEXT.md`. Léelos cuando la tarea toque diseño.
4. Si la tarea no está en `Pendiente.md`, añádela antes de empezar.

## 2. Al terminar cada tarea
1. Tests en verde (`./mvnw test`, `pytest`, `npm test` según la carpeta) y lint.
2. Actualiza el vault (§6): marca la tarea en `Pendiente.md`, añade una entrada a `Log.md` y, si cambió, el estado de `Inicio.md`, las horas de `Fases.md`, `Decisiones.md`, `Problemas.md`, `Evaluación.md` o `Producción.md`.
3. Commit convencional en español (`feat: registro de series sin conexión`, `fix: …`, `docs: …`, `test: …`) y `git push`. El vault es otro repo: `git -C vault add -A && git -C vault commit -m "<lo mismo>" && git -C vault push`. Si `git status` enseña cambios que no son tuyos, añade solo los tuyos y avisa.

## 3. Stack y carpetas
- `backend/`: Java 21, Spring Boot 4, JPA, **Flyway** para todo cambio de esquema, OpenAPI exportado a `docs/api/`.
- `database/`: MySQL 8.4 LTS en 3FN, seeds y scripts de carga.
- `frontend/`: React Native + Expo (TypeScript), dev build + EAS.
- `vision-service/`: FastAPI + Pydantic; salidas de LLM con JSON Schema (`format` de Ollama), validadas y con reintento.
- `eval/`: datasets etiquetados y scripts; los resultados se versionan y el CI falla si empeoran.
- `docs/adr/`: una decisión por archivo (`0001-titulo.md`), con la plantilla `vault/Plantillas/ADR.md`.
- `infra/` despliegue, `ops/` Prometheus y Grafana, `.github/workflows/` CI.

Estándares de código por carpeta: en [`AGENTS.md`](AGENTS.md#estándares-de-código). Producto en [`docs/PRD.md`](docs/PRD.md), arquitectura en [`docs/arquitectura.md`](docs/arquitectura.md) y diseño en [`docs/design-system.md`](docs/design-system.md).

Hardware: portátil con RTX 5070 (~8 GB de VRAM) para el worker de GPU; VPS sin GPU para el resto. Todo lo pesado va por la cola de trabajos.

## 4. Reglas del proyecto
- Las fotos de cuerpo y de etiquetas **no se guardan**: se procesan y se borran; solo quedan los tramos de cada proporción ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). Son datos de salud (RGPD, art. 9).
- Las reglas por palancas son **heurísticas de entrenadores**, no ciencia: dilo así en la app y en el README (ver `docs/fase-0.md`).
- Finalidad de fitness, no sanitaria ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)): Forja «elige ejercicios compatibles con las limitaciones que tú indicas». Nunca escribas «rehabilita», «alivia», «corrige», «previene lesiones», «detecta desequilibrios o atrofias» ni «seguro para tu lesión», ni en la app ni en el README ni en los posts. El LLM transcribe; las reglas deciden; las banderas rojas van por reglas.
- El escáner de suplementos nunca dice «libre de dopaje»: dice «riesgo por ingredientes declarados» y si tiene sello Informed Sport/NSF.
- OpenPowerlifting: usa solo el CSV (dominio público), nunca su código (AGPLv3). wger: guarda licencia y autor por ejercicio.
- Secretos solo en `.env` (con `.env.example` sin valores). Nunca en el repo, los logs ni el vault.
- TDD y 80 % de cobertura (reglas ECC). Cada modelo de IA tiene su evaluación en `eval/` antes de darlo por bueno.
- Comprueba la licencia de cada modelo y dataset antes de usarlo y apúntala en `vault/Decisiones.md`.

## 5. Identidad
- Visual: sistema de diseño **Crimson Ink** (https://claude.ai/artifact/8aiPpZtQid9ygULcBXvm91). Tokens en un único `theme.ts`; oscuro por defecto.
- Textos (app, README, posts): voz del kit de marca: directo, de tú, con cifras y sin frases de plantilla de IA.

## 6. El vault (`vault/`)
Vault de Obsidian para organizar el proyecto (en Obsidian: «Abrir carpeta como vault» → `vault/`). **Es privado y no forma parte de la app:** vive en su propio repo privado y este repo lo ignora en `.gitignore`. Nunca lo añadas aquí ni copies su contenido al código, al README ni a los commits. Lo mantienes tú; si Gabriel escribe en una nota, respeta su texto.

| Nota | Para qué | Cuándo se toca |
|---|---|---|
| `Inicio.md` | Estado actual, fase, versión y siguiente paso | Al cambiar de tarea o de fase |
| `Pendiente.md` | Backlog: **Ahora**, siguiente y por fase, con casillas | Al empezar y acabar cada tarea |
| `Fases.md` | Roadmap con estado y horas estimadas y reales | Al cerrar una tarea con horas o una fase |
| `Log.md` | Registro cronológico, solo se añade al final | Al final de cada tarea |
| `Decisiones.md` | Índice de ADR, decisiones menores y licencias | Al decidir algo |
| `Problemas.md` | Bugs, bloqueos y cómo se resolvieron | Al encontrar o cerrar uno |
| `Evaluación.md` | Métricas de cada modelo por versión | Al correr una evaluación |
| `Producción.md` | Entornos, despliegues, releases, secretos (solo nombres) y checklist para hacer público el repo | Al desplegar o publicar |
| `Publicaciones.md` | Posts de LinkedIn por versión | Al cerrar una versión |
| `Ideas.md` | Fuera de alcance, para después | Cuando surja |
| `Sesiones/AAAA-MM-DD.md` | Notas de una sesión larga (plantilla `Plantillas/Sesión.md`) | Opcional |

Formato del log:
```markdown
## [AAAA-MM-DD] <tipo> | <título>
- Qué se hizo, archivos tocados, commit y qué queda.
```
Tipos: `setup`, `feat`, `fix`, `refactor`, `eval`, `infra`, `docs`, `decision`, `release`.

Las notas llevan frontmatter (`tipo`, `actualizado`) y la línea 🧭 bajo el título. Wikilinks entre notas del vault; enlaces Markdown relativos (`../docs/…`) a archivos del repo.

## 7. Trabajo con Codex
Reparto: **Claude orquesta y revisa; Codex implementa** (plugin `codex@openai-codex`).
- Implementaciones de más de ~30 líneas: delega en el subagente `codex:codex-rescue` con la tarea acotada y un criterio de «hecho».
- Revisa siempre el diff de Codex. Los fallos de lógica vuelven a Codex como lista concreta; tú solo arreglas lo trivial.
- Antes de cerrar una tarea, revisión adversarial (`/codex:adversarial-review`) hasta que salga limpia.
- Una tarea por rama o worktree; nunca dos agentes en los mismos archivos.
- Si Gabriel pide no usar Codex, no lo uses.

## 8. Publicación
Al terminar solo se publica **la app**, como Style & Scent Engine: el código de este repo y, en GitHub Releases, el **APK** (EAS) y la **versión de PC**, con enlaces de descarga en el README. El vault nunca se publica.

El historial de este repo está limpio de `vault/` (purgado el 2026-11-15). Si alguna vez se cuela, hay que reescribir el historial antes de publicar, con el visto bueno de Gabriel. El resto de pasos, en la checklist de `vault/Producción.md`.

## 9. Contexto de Gabriel
Perfil, gustos y kit de marca completos en su vault de la carrera (ruta en su CLAUDE.md global). Léelos si una decisión depende de ellos; **no escribas allí** desde este proyecto.
