# Forja

Entrena, come y suplementa según tu cuerpo.

> 🚧 **En desarrollo** · repo privado hasta la v1.0 · fase actual: **0**

Te pregunta lo que te limita, mide tu movilidad y arma la rutina con un solver que te explica cada ejercicio con su fuente. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.

## ◆ Qué hace
- **Entreno:** entrada guiada de limitaciones (lesiones, dolor, discapacidad) con IA que solo transcribe, tests de movilidad, estándares de fuerza con OpenPowerlifting y rutina generada con CP-SAT que explica cada ejercicio con su fuente y su nivel de evidencia. Las proporciones por foto solo desempatan ([ADR 0007](docs/adr/0007-motor-de-seleccion-de-ejercicios.md)). Forja no evalúa, diagnostica, trata ni previene lesiones.
- **Dieta (ProteEuro):** cesta semanal que cumple tu proteína y tu tope de carbohidratos al menor coste (programación lineal con precios de Mercadona).
- **Suplementos:** OCR de la etiqueta y riesgo por ingredientes frente a la lista de sustancias prohibidas del BOE y las alertas de la CELAD/AESAN.
- **Registro de series** sin conexión, con progresión doble y deload automáticos.
- **IA explicable:** un LLM local solo explica las decisiones de las reglas; nunca escribe series.

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
Con la v1.0: APK para Android y versión de PC en [Releases](https://github.com/Gabriel-Vallejo/forja/releases).

## ◆ Arrancar
La app llega con la v0.1. Lo que ya se puede levantar en local:
```bash
cp .env.example .env   # rellena los valores
docker compose --profile dev up -d   # dev: MySQL 8.4
docker compose --profile obs up -d   # obs: Prometheus (9090) y Grafana (3000)
docker compose --profile gpu up -d   # gpu: Ollama en contenedor (puerto 11435), solo si no tienes Ollama nativo; necesita NVIDIA Container Toolkit
```
- `down` también necesita los perfiles (`docker compose --profile dev --profile obs down`).
- Compose pide todas las variables del `.env` aunque solo arranques un perfil.
- La contraseña de Grafana solo se aplica en el primer arranque del volumen.

## Licencia
[MIT](LICENSE)
