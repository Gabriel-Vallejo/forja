# Forja

Entrena, come y suplementa según tu cuerpo.

> 🚧 **En desarrollo** · repo privado hasta la v1.0 · **v0.1** publicada (el entreno) · siguiente: fase **2**

Te pregunta lo que te limita, mide tu movilidad y arma la rutina con un solver que te explica cada ejercicio con su fuente. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.

## ◆ Qué hace
- **Entreno:** entrada guiada de limitaciones (lesiones, dolor, discapacidad) con IA que solo transcribe, tests de movilidad, estándares de fuerza con OpenPowerlifting y rutina generada con CP-SAT que explica cada ejercicio con su fuente y su nivel de evidencia. Las proporciones por foto solo desempatan ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). Forja elige ejercicios compatibles con las limitaciones que tú indicas; no es una herramienta sanitaria.
- **Dieta (ProteEuro):** cesta semanal que cumple tu proteína y tu tope de carbohidratos al menor coste (programación lineal con precios de Mercadona).
- **Suplementos:** OCR de la etiqueta y riesgo por ingredientes frente a la lista de sustancias prohibidas del BOE y las alertas de la CELAD/AESAN.
- **Registro de series** sin conexión, con progresión doble y deload automáticos.
- **IA explicable:** un LLM local solo explica las decisiones de las reglas; nunca escribe series.

## ◆ Lo que ya funciona (v0.1)
- Cuenta con correo y contraseña (Argon2id, lista local de 46 000 contraseñas filtradas), verificación y recuperación por correo (Azure Communication Services, datos en Europa), dispositivos, «Descargar mis datos» y borrado total; si borras la cuenta desde otro dispositivo, este borra lo que guarda de ella.
- Limitaciones con consentimiento aparte y cifradas, cribado de 7 preguntas y banderas rojas por reglas que bloquean el plan cuando toca.
- Tres tests de movilidad que cambian variantes concretas del plan.
- Rutina con CP-SAT: duración, topes por patrón y día, volumen por músculo, como mucho 2 días por ejercicio si hay alternativa, y «por qué este ejercicio» con su fuente y su nivel.
- Ajustes con vista previa (una serie más o menos, reemplazar, no recomendar, variedad, enfocar) y material de tu gimnasio.
- Registro de series sin conexión: RPE, dolor (con aviso desde 7), deshacer, progresión doble con deload y aviso si un ejercicio te duele en 2 de sus 3 últimas sesiones.
- Racha semanal y cifras del perfil con tus sesiones; el rango por percentil aún es de ejemplo (fase 2).

## ◆ Stack
- **Backend:** Java 21 · Spring Boot 4 · JPA · Flyway · MySQL 8.4 LTS
- **App:** React Native · Expo (TypeScript) · EAS
- **IA y visión:** FastAPI · OpenCV · modelos locales (Ollama) · worker de GPU con cola en MySQL
- **Infra:** Docker Compose (perfiles `dev`, `gpu`, `obs`) · GitHub Actions · Prometheus + Grafana

## ◆ Estructura
| Carpeta | Qué hay |
|---|---|
| `backend/` | API Spring Boot |
| `database/` | Init de MySQL, seeds y carga de datos |
| `frontend/` | App Expo |
| `vision-service/` | FastAPI con los modelos |
| `eval/` | Datasets etiquetados y evaluación versionada |
| `docs/` | ADR, arquitectura y OpenAPI |
| `infra/`, `ops/` | Despliegue y observabilidad |

## ◆ Evaluación
Lo que se mide (los resultados estarán en `eval/`):
- Acierto del tramo de cada proporción (corto, medio, largo) frente a cinta métrica
- Precisión y recall de ingredientes extraídos por OCR
- Exactitud de la intención en 50 frases de prueba
- Coste de la cesta frente a una cesta manual

## ◆ Descargar
- **v0.1:** [APK para Android](https://github.com/Gabriel-Vallejo/forja/releases/latest/download/forja-0.1.0.apk) (habla con el backend en Azure). Mientras el repo sea privado, solo se descarga con acceso.
- Con la v1.0, también la versión de PC.

## ◆ Arrancar
Base de datos y servicios:
```bash
cp .env.example .env   # rellena los valores
docker compose --profile dev up -d   # dev: MySQL 8.4
docker compose --profile obs up -d   # obs: Prometheus (9090) y Grafana (3000)
docker compose --profile gpu up -d   # gpu: Ollama en contenedor (puerto 11435), solo si no tienes Ollama nativo; necesita NVIDIA Container Toolkit
```
- `down` también necesita los perfiles (`docker compose --profile dev --profile obs down`).
- Compose pide todas las variables del `.env` aunque solo arranques un perfil.
- La contraseña de Grafana solo se aplica en el primer arranque del volumen.

Backend y app (Java 21 y Node 22):
```bash
cd backend && ./mvnw spring-boot:run     # API en :8080 (necesita el .env y MySQL)
cd frontend && npm ci && npx expo start  # «a» abre Android con la build de desarrollo
```
Tests: `scripts/verificar.sh` pasa en local lo mismo que el CI (backend, app, catálogo, compose y workflows; con `--e2e`, también Maestro). Los caminos críticos de Maestro corren en un emulador en el CI al etiquetar una versión (`.github/workflows/e2e.yml`); en local, ver [frontend/README.md](frontend/README.md).

## ◆ Desplegar
El backend corre en Azure (Container Apps, MySQL Flexible y Communication Services para el correo):
```bash
az login
infra/azure/desplegar.sh   # crea lo que falte y sube la versión del commit actual
```
Detalles, costes y límites en [infra/README.md](infra/README.md). El APK firmado sale de `frontend/scripts/apk-release.sh`.

## Licencia
[MIT](LICENSE)
