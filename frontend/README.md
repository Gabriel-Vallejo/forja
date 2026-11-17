# frontend

App de Forja: React Native + Expo (TypeScript `strict`) con Expo Router, TanStack Query y Zod. La misma base sirve el móvil (Android con EAS) y, más adelante, la versión de PC con Tauri ([ADR 0006](../docs/adr/0006-version-de-pc.md)).

## Arrancar

```bash
cd frontend
cp .env.example .env   # y ajusta la URL del backend
npm ci
npx expo start         # «a» abre Android; «w», la web
```

El backend tiene que estar levantado (`backend/README.md`). Desde un móvil físico, `localhost` es el propio móvil: pon en `EXPO_PUBLIC_API_URL` la IP de tu equipo en la red local.

La versión web todavía no guarda la sesión: el llavero del sistema de Tauri llega en otro bloque y, mientras tanto, el inicio de sesión dice que no está disponible. Nunca se usa `localStorage` ni `AsyncStorage` para los tokens ([ADR 0009](../docs/adr/0009-autenticacion.md)).

## Variables

| Variable | Para qué |
|---|---|
| `EXPO_PUBLIC_API_URL` | URL base del backend, sin barra final (p. ej. `http://localhost:8080`). Va dentro del bundle: nunca pongas un secreto en una variable `EXPO_PUBLIC_*`. |

## Comprobaciones

```bash
npm run lint        # ESLint sin avisos
npx tsc --noEmit    # tipos
npm test            # Jest + React Native Testing Library
```

El CI (job `frontend` de `.github/workflows/ci.yml`) corre lo mismo en Node 22.

## Cliente de la API

Los tipos salen de la especificación OpenAPI del backend, `docs/api/openapi.json`, que escribe un test del backend (`EspecificacionOpenApiTest`) durante `./mvnw verify`. Para regenerarlos:

```bash
cd backend && ./mvnw -B verify          # o solo: ./mvnw test -Dtest=EspecificacionOpenApiTest
cd ../frontend && npm run api           # escribe src/api/esquema.d.ts
```

Los dos archivos se versionan y el CI falla si no coinciden con lo generado. Las respuestas se validan con los esquemas Zod de `src/api/esquemas.ts`, atados a esos tipos con `satisfies`: si el backend cambia un campo, `tsc` avisa.

## Estructura

```
app/                      rutas de Expo Router (finas: leen y validan parámetros y eligen pantalla)
app/(dentro)/             todo lo que pide sesión; sin ella, a /entrar
app/(dentro)/(tabs)/      Hoy, Entreno, Forja IA, Progreso y Perfil, con la barra propia
app/(dentro)/*.tsx        apiladas: ejercicio, escala, rango, calculadora, racha, sesión y «próximamente»
src/theme.ts              tokens de Crimson Ink y del boceto aprobado; único sitio con colores y tamaños
src/componentes/          Cuerpo (SVG, giro con gesture-handler y Reanimated), Emblema, TabBar, TarjetaSesion,
                          FilaEjercicio, FilaRango, Regla y los básicos (Boton, Texto, Aviso, Pantalla…)
src/pantallas/            una por ruta; ConPlan resuelve cargando, error y «sin plan»
src/plan/  src/rango/     ayudas del plan (prescripción, primera semana) y del rango (e1RM de Epley)
src/datos/ejemplo.ts      datos de ejemplo de rangos, racha y perfil hasta la v0.2 (cada pantalla lo avisa)
src/api/                  cliente HTTP (renovación de sesión única), esquemas Zod y tipos generados
src/sesion/               almacén de tokens (.native.ts / .web.ts), sesión y validación del alta
```

Los trazados del cuerpo (`src/componentes/cuerpoTrazados.ts`) son los del boceto aprobado, copiados tal cual: no se retocan a mano.

## Licencias de terceros

| Recurso | Licencia |
|---|---|
| Inter, JetBrains Mono y Noto Serif JP (vía `@expo-google-fonts/*`) | SIL Open Font License 1.1 (las fuentes); MIT (los paquetes) |
| react-native-svg, react-native-gesture-handler, react-native-reanimated, react-native-worklets | MIT |
