# Forja

Entrena, come y suplementa según tu cuerpo.

> 🚧 **En desarrollo** · repo privado hasta la v1.0 · fase actual: **0**

Fotos de tu cuerpo y tus marcas → una rutina que elige los ejercicios según tus palancas y tus puntos débiles, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.

## ◆ Qué hace
- **Entreno:** proporciones corporales por pose (MediaPipe), estándares de fuerza con OpenPowerlifting, mapa muscular y rutina generada con CP-SAT, con el error de medición publicado.
- **Dieta (ProteEuro):** cesta semanal que cumple tu proteína y tu tope de carbohidratos al menor coste (programación lineal con precios de Mercadona).
- **Suplementos:** OCR de la etiqueta y riesgo por ingredientes frente a la lista de sustancias prohibidas del BOE y las alertas de la CELAD/AESAN.
- **Registro de series** sin conexión, con progresión doble y deload automáticos.
- **IA explicable:** un LLM local solo explica las decisiones de las reglas; nunca escribe series.

## ◆ Stack
- **Backend:** Java 21 · Spring Boot 4 · JPA · Flyway · MySQL 8
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
- MAE de los ratios corporales frente a cinta métrica (mm y %)
- Precisión y recall de ingredientes extraídos por OCR
- Exactitud de la intención en 50 frases de prueba
- Coste de la cesta frente a una cesta manual

## ◆ Descargar
Con la v1.0: APK para Android y versión de PC en [Releases](https://github.com/Gabriel-Vallejo/forja/releases).

## ◆ Arrancar
Llega con la v0.1.

## Licencia
[MIT](LICENSE)
