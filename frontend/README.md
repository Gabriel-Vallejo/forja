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
app/                rutas de Expo Router (finas: solo eligen pantalla)
src/theme.ts        tokens de Crimson Ink; único sitio con colores y tamaños
src/componentes/    Boton, CampoTexto, Pantalla, Aviso y Texto
src/pantallas/      registro, «revisa tu correo», entrar y portada
src/api/            cliente HTTP (renovación de sesión única), esquemas Zod y tipos generados
src/sesion/         almacén de tokens (.native.ts / .web.ts), sesión y validación del alta
```
