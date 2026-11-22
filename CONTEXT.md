# Forja: contexto del proyecto

Índice para agentes y para mí. El plan original del 2026-11-15 se repartió el 2026-11-16 en documentos con un solo dueño cada uno: si algo de aquí contradice a uno de ellos, manda el documento.

| Para saber… | Lee |
|---|---|
| Qué es Forja, para quién, funciones por versión y requisitos | [docs/PRD.md](docs/PRD.md) |
| Cómo encaja todo: componentes, diagramas, modelo de datos y API | [docs/arquitectura.md](docs/arquitectura.md) |
| Colores, tipografía, componentes y voz | [docs/design-system.md](docs/design-system.md) |
| Por qué se decidió cada cosa | [docs/adr/](docs/adr/README.md) (0001-0009) |
| Normas de código por carpeta | [AGENTS.md](AGENTS.md#estándares-de-código) |
| Reglas del proyecto y flujo de trabajo | [CLAUDE.md](CLAUDE.md) |
| Competencia y qué ideas se llevan | [docs/competencia.md](docs/competencia.md) |
| Citas científicas verificadas | [docs/referencias.md](docs/referencias.md) |
| Validación inicial (datos y licencias) | [docs/fase-0.md](docs/fase-0.md) |

**Frase de venta:** *Te pregunta lo que te limita, mide tu movilidad y arma la rutina con un solver que te explica cada ejercicio con su fuente. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.*

## Fases

| Fase | Contenido | Entregable | Horas aprox. **[estimación]** |
|---|---|---|---|
| 0. Validación y diseño ✅ | Competencia probada a mano, datos y licencias, citas verificadas, ADR 0001-0006, base del repo con CI. | Doc de competencia en `docs/` + repo con CI vacío en verde | 15 |
| 1. Entreno MVP ✅ | Catálogo de ejercicios etiquetado (atributos de restricción y escalera de sustitución), perfil manual, entrada de limitaciones con botones y preguntas guiadas, tests de movilidad, CP-SAT con su «por qué», registro sin conexión con dolor 0-10 y progresión doble, cuenta y dispositivos, consentimiento y borrado. Sin fotos ni LLM. | **v0.1** + post 1 («mi app me arma la rutina con un solver») | 60 |
| 2. Cuerpo y estándares | LLM de entrada (extracción del texto libre, con su eval), MediaPipe en tramos, eval con cinta, percentiles de OpenPowerlifting, mapa muscular, LLM que explica. | **v0.2** + post 2 (el mapa y el error medido) | 40 |
| 3. Dieta | Catálogo, programación lineal, precios por foto, avisos, recetas. | **v0.3** + post 3 («180 g de proteína por 3,40 €») | 40 |
| 4. Suplementos | OCR + normalización, lista de sustancias del BOE + sinónimos, ingesta de CELAD y AESAN, ficha de riesgo, push. | **v0.4** + post 4 (el pre-entreno escaneado) | 35 |
| 5. Producción | IaC (Bicep) y red privada en Azure (el despliegue ya está desde la v0.1), observabilidad, ZAP, versión de PC (Tauri), 20 usuarios del gimnasio, Play en prueba cerrada. | **v1.0** + post de cierre con métricas de uso | 20 + 8-10 (PC) |

Cada fase es una versión publicable: si el tiempo se acaba, la v0.2 ya es un proyecto completo. Horas reales en el vault.

## Riesgos y cómo se mitigan

| Riesgo | Mitigación |
|---|---|
| El alcance (tres apps) | Una versión publicable por módulo. |
| Datos de salud (fotos, medidas, limitaciones) | Fotos en memoria, consentimiento explícito por tipo, cifrado por columna y borrado real ([ADR 0004](docs/adr/0004-privacidad-fotos-y-datos-de-salud.md)). |
| Palancas sin base científica | Solo desempatan, en tramos y con su nivel de evidencia visible ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). |
| Que la app se considere producto sanitario (MDR) | Finalidad de fitness: «compatible con lo que declaras»; palabras prohibidas en [CLAUDE.md](CLAUDE.md) §4 ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). |
| Que el LLM pase por alto una bandera roja | Banderas rojas por reglas; el LLM solo extrae con cita literal y confirmación; revisión de un fisio antes de la v0.2 ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). |
| Que el usuario se fíe de «sin riesgo» en un suplemento | Lenguaje de riesgo y aviso fijo de que la app no certifica. |
| mercaapi desaparece | Adaptadores de fuente de precios y precios de usuarios como respaldo ([ADR 0005](docs/adr/0005-fuentes-de-datos.md)). |
| Lesiones por una rutina mal generada | Restricciones duras, máximo de volumen duro y escalera de sustitución ([ADR 0002](docs/adr/0002-solver-cp-sat.md), [ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). |
| Competencia directa en España (Symmetry) | El diferencial es el porqué de cada ejercicio con su fuente, no el mapa muscular ([competencia](docs/competencia.md)). |

## Plan de proyecto (para que luzca)

- **Evaluación de cada modelo:** dataset propio en `eval/` con su `DATASET.md`, métricas versionadas y CI que falla si empeoran; tabla de latencias p50/p95 en GPU frente a CPU en el README.
- **Usuarios reales:** 10-30 (compañeros de la USAL, del gimnasio y del equipo de CTF). Métricas propias en `evento_uso`, **seudonimizada** con HMAC ([ADR 0004](docs/adr/0004-privacidad-fotos-y-datos-de-salud.md)): retención a 7 días y acciones por semana. Sin analítica de terceros. Feedback desde la app a un issue de GitHub vía n8n.
- **README:** GIF de 10-15 s del flujo estrella, una frase de qué resuelve, badges (CI, cobertura, eval, licencia, release), resultados medidos, diagrama, arranque en un comando, enlaces a los ADR, límites conocidos y licencias.
- **LinkedIn** (datos de vendedores, no estudios): carrusel mejor que vídeo, enlace al repo en el primer comentario, la primera hora decide el alcance. 3-4 posts, uno por versión: problema, demo, número medido, decisión técnica.
