# 0001. Stack y forma de la arquitectura

- **Estado:** propuesta
- **Fecha:** 2026-11-15

## Contexto
Forja son tres módulos (entreno, dieta y suplementos) con un núcleo de algoritmos (CP-SAT, programación lineal y reglas de riesgo) y una parte de IA (pose, OCR y LLM local). Lo desarrolla una persona en unas 210 horas repartidas en 6 fases. Pesan cuatro restricciones:

- **Empleabilidad:** Java y Spring son lo más pedido en España, y el proyecto tiene que lucir en una entrevista.
- **Ecosistema de IA:** MediaPipe, Florence-2, Ollama y Pydantic viven en Python.
- **Uso diario en el gimnasio:** el registro de series tiene que funcionar sin conexión en Android.
- **Hardware:** un VPS sin GPU para todo lo que está siempre encendido y el portátil con una RTX 5070 (~8 GB) para lo pesado.

## Decisión
**Un monolito modular en Java y un servicio de IA en Python**, más la app móvil:

| Pieza | Tecnología | Por qué |
|---|---|---|
| `backend/` | Java 21 (LTS) · Spring Boot 4 · JPA · Flyway · springdoc | Lógica de negocio y solvers (OR-Tools tiene API Java). Spring Boot 4 salió el 2025-11-20 y recomienda Java 21 o superior. |
| `database/` | **MySQL 8.4 LTS** · 3FN · Flyway | Fuente de verdad única. **8.4 y no 8.0:** MySQL 8.0 dejó de tener soporte en abril de 2026; 8.4 LTS lo tiene hasta 2032. |
| `vision-service/` | Python · FastAPI · Pydantic · Ollama | Pose, OCR y LLM. Sin estado y sin lógica de negocio. |
| `frontend/` | React Native · Expo (TypeScript) · dev build · TanStack Query · Zod | Android primero, con modo sin conexión. Cliente generado desde el OpenAPI. |

- **Organización del backend:** por dominio (`entreno/`, `dieta/`, `suplementos/`, `comun/`), y cada dominio con `api`, `servicio`, `repositorio` y `modelo`. Un dominio no toca las tablas de otro; se hablan por servicios.
- **Contratos:** OpenAPI exportado a `docs/api/` y JSON Schema de Pydantic exportado desde el `vision-service`. El CI comprueba que no se desincronizan.
- **Sin búsqueda vectorial:** la plantilla común propone Qdrant, pero Forja no la necesita. El cruce de ingredientes usa normalización y Jaro-Winkler (ver [CONTEXT.md](../../CONTEXT.md) §1.5). Si algún día hace falta, va en otro ADR.
- **La versión de PC** tiene su propio ADR pendiente (Tauri o Electron).

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Todo en Python (FastAPI + OR-Tools Python) | Un solo lenguaje, OR-Tools tiene la misma API | Pierde el gancho de Java para el CV; FastAPI no trae Flyway, seguridad ni capas tan maduras |
| Kotlin + Spring | Más conciso, mismo ecosistema | Menos pedido en ofertas junior que Java |
| Microservicios por módulo | Despliegue independiente | Tres servicios, red y trazas distribuidas para una sola persona: coste sin beneficio a esta escala |
| PostgreSQL | pgvector, tipos más ricos | El resto de proyectos de Gabriel y la carrera usan MySQL; Forja no necesita lo que aporta Postgres |
| MySQL 8.0 (lo que decía el plan) | Igual que en clase | Sin parches de seguridad desde abril de 2026 |
| Java 25 (LTS) | La LTS más nueva, con soporte de primera en Spring Boot 4 | Java 21 está más extendido en ofertas y librerías; el cambio a 25 es trivial más adelante |
| Flutter | Buen rendimiento | Dart no está en el stack de Gabriel; Expo ya está probado en Style & Scent Engine |

## Consecuencias
- **Gana:** cada lenguaje hace lo que mejor se le da, el backend se enseña en una entrevista de Java y el `vision-service` puede caerse sin tumbar la app (entrada manual como plan B, con Resilience4j).
- **Cuesta:** dos runtimes, dos suites de tests y dos imágenes Docker, y hay que mantener los contratos entre ellos.
- **A cambiar:** el plan y el README dicen «MySQL 8»: pasan a «MySQL 8.4 LTS» y la imagen Docker se fija a `mysql:8.4`.

## Fuentes
- [Spring Boot 4.0.0 available now](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/) (2025-11-20)
- [Versiones de MySQL Server (Oracle)](https://docs.oracle.com/en-us/iaas/mysql-database/doc/mysql-server-versions.html): fin de soporte de 8.0 en abril de 2026
