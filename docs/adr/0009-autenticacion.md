# 0009. Autenticación: token de acceso corto, refresco rotatorio por dispositivo

- **Estado:** propuesta
- **Fecha:** 2026-11-16

## Contexto
Forja guarda datos de salud ([0004](0004-privacidad-fotos-y-datos-de-salud.md)) y tiene tres tipos de cliente: el móvil (Expo), la versión de PC (Tauri, [0006](0006-version-de-pc.md)) y el worker de GPU ([0003](0003-cola-de-trabajos-gpu.md)). El plan solo decía «JWT de acceso corto y refresh rotatorio». La revisión señaló que faltaban el registro, la recuperación de contraseña, la revocación por dispositivo y cómo encaja la webview de Tauri, donde CORS no autentica nada.

## Decisión
1. **Cuenta con correo y contraseña.**
   - Contraseña con **Argon2id** (Spring Security `Argon2PasswordEncoder`), mínimo 12 caracteres y comprobación contra una lista de contraseñas filtradas descargada en local (sin llamar a un servicio externo).
   - Correo verificado con un enlace de un solo uso que caduca en 24 h.
   - En el alta se valida la mayoría de edad con la fecha completa, pero solo se guarda el año y `mayor_de_edad` ([0004](0004-privacidad-fotos-y-datos-de-salud.md), enmienda 6).
2. **Token de acceso:** JWT firmado con **Ed25519**, de 15 minutos, con `sub` (UUID del usuario), `sid` (dispositivo) y `kid` (clave). Las claves rotan; el backend acepta la actual y la anterior. Viaja **siempre** en la cabecera `Authorization: Bearer`, nunca en cookies, también en Tauri.
3. **Token de refresco:** opaco (256 bits aleatorios), de 30 días, **uno por dispositivo**, guardado en la base solo como hash. Cada uso lo cambia por uno nuevo (rotación). Si alguien usa uno ya rotado, se revoca toda la familia de ese dispositivo (detección de robo).
   - Móvil: en `expo-secure-store`.
   - PC: en el llavero del sistema con el plugin comunitario de [0006](0006-version-de-pc.md); si no hay Secret Service, Stronghold.
   - Nunca en `localStorage` ni en `AsyncStorage`.
4. **Dispositivos visibles:** tabla `dispositivo` (nombre, plataforma, última actividad). El usuario los ve en ajustes y puede cerrar la sesión de cualquiera, lo que revoca su refresco al momento. El token de acceso de ese dispositivo caduca solo en 15 minutos como mucho.
5. **Recuperar la contraseña:** enlace de un solo uso por correo, válido 30 minutos. La respuesta es la misma exista o no la cuenta (no revela qué correos están registrados). Cambiar la contraseña revoca todos los dispositivos.
   - El correo sale por un proveedor transaccional, que es **encargado del tratamiento** y va en la lista de la política. **[elegir proveedor con servidores en la UE en la fase 1]**
6. **Límites de intentos (Bucket4j):** por IP y por cuenta en el login, el registro y la recuperación. Tras 10 fallos seguidos en una cuenta, espera creciente. Sin CAPTCHA de terceros.
7. **Orígenes y CSP:**
   - CORS permite solo el dominio de la web y los orígenes de Tauri (`tauri://localhost` y `http://tauri.localhost`). Al ir el token en cabecera y no en cookies, no hace falta protección CSRF.
   - La CSP de Tauri limita `connect-src` al dominio de la API.
8. **Worker de GPU:** no usa cuentas de usuario. Lleva su propio token largo y revocable, guardado como hash, que solo abre `/worker/**` ([0003](0003-cola-de-trabajos-gpu.md)).
9. **Borrar la cuenta:** `DELETE /api/v1/cuenta`, con la contraseña otra vez. Revoca todo y borra en cascada ([0004](0004-privacidad-fotos-y-datos-de-salud.md)).
10. **Tests:** la rotación detecta un refresco reutilizado; cerrar un dispositivo invalida su refresco; la recuperación no revela si existe la cuenta; los límites de intentos saltan; un token con `kid` desconocido se rechaza.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Sesión con cookie del servidor | Clásico, revocación inmediata | En Tauri y en la app nativa las cookies dan guerra; obliga a CSRF |
| Proveedor externo (Auth0, Firebase Auth, Keycloak) | Menos código propio | Otro encargado con datos de los usuarios, o un servicio más que mantener; para una persona, Spring Security basta |
| «Entrar con Google» desde el principio | Menos fricción | Otro tercero en el flujo; se puede añadir después sin cambiar este diseño |
| JWT largo sin refresco | Simple | No se puede revocar un dispositivo robado |
| Enlace mágico sin contraseña | Sin contraseñas que filtrar | Depende del correo en cada inicio de sesión; peor en el gimnasio sin cobertura |

## Consecuencias
- **Gana:** se puede revocar un dispositivo robado, el mismo diseño sirve en móvil, PC y worker, y no hay ningún tercero en el inicio de sesión.
- **Cuesta:** escribir y probar la rotación, la tabla de dispositivos y el flujo de correo, y elegir un proveedor de correo de la UE.
- **Modelo de datos:** `usuario` (correo, hash Argon2id, verificado, año de nacimiento, `mayor_de_edad`), `dispositivo`, `token_refresco` (hash, familia, caducidad, revocado) y `token_un_uso` (verificación y recuperación, con hash y caducidad).
- **API:** `/api/v1/auth/registro`, `/verificar`, `/login`, `/refresco`, `/recuperar`, `/restablecer`, `/cerrar-sesion`, `GET /api/v1/dispositivos` y `DELETE /api/v1/dispositivos/{id}`.
