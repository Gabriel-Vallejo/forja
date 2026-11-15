# 0006. Versión de PC: Expo web empaquetada con Tauri 2

- **Estado:** aceptada (2026-11-15)
- **Fecha:** 2026-11-15

## Contexto
Al publicar, Forja sale en GitHub Releases como APK para Android y como **versión de PC** ([CLAUDE.md](../../CLAUDE.md) §8). El frontend es React Native + Expo (TypeScript, [0001](0001-stack.md)). Hace falta decidir cómo se convierte en una app de escritorio sin mantener otra base de código.

En PC se usa sobre todo para planificar: ver y ajustar la rutina, el mapa muscular, la cesta semanal y la ficha de un suplemento. Las series se apuntan en el móvil, en el gimnasio. Pesan además estas restricciones:
- **Plataformas:** Windows 11 (la de casi todo el mundo) y Linux (Gabriel usa CachyOS).
- **Tamaño:** que el instalador sea pequeño, porque se descarga desde Releases.
- **Mantenimiento:** que lo pueda llevar una sola persona.

## Decisión
**Exportación web estática de Expo (`npx expo export -p web`) dentro de un shell de Tauri 2.**

1. **Una sola base de código:** el mismo `frontend/` compila a Android (EAS) y a web (react-native-web). Tauri sirve el `dist/` web dentro de la webview del sistema: WebView2 en Windows y WebKitGTK en Linux.
2. **Carpeta del shell:** `frontend/src-tauri/` solo tiene el arranque mínimo de Tauri y su configuración: ventana, permisos (capabilities) y CSP. **Nada de lógica en Rust:** si algo hace falta en el escritorio, va por un plugin oficial de Tauri.
3. **Diferencias por plataforma, con archivos `.native.ts` y `.web.ts`** detrás de una interfaz común:

   | Pieza | Móvil | PC (web + Tauri) |
   |---|---|---|
   | Fotos de cuerpo y etiquetas | Cámara guiada | **Subir un archivo** (no cámara en vivo); el servidor procesa igual ([0003](0003-cola-de-trabajos-gpu.md), [0004](0004-privacidad-fotos-y-datos-de-salud.md)) |
   | Token de refresco | `expo-secure-store` (Keychain/Keystore) | Llavero del sistema mediante un plugin de Tauri **[elegir el plugin en la fase 1]**. Nunca en `localStorage` |
   | Caché sin conexión | TanStack Query + almacenamiento nativo | TanStack Query con persistencia en IndexedDB; el registro de series sin conexión es solo del móvil |
   | Avisos | Push (fase 4) | Notificaciones locales del plugin de Tauri, si hacen falta |

4. **Regla para el frontend:** ninguna librería solo nativa entra sin su alternativa web o sin quedar tras la interfaz de plataforma. Cuando exista `frontend/package.json`, el CI añade `npx expo export -p web` para que la versión web no se rompa en silencio.
5. **Publicación:** un `release.yml` que, al crear una etiqueta `v*`, compila en Windows (NSIS `.exe`) y en Linux (`.AppImage` y `.deb`) y adjunta los instaladores a la release junto al APK. **Sin firma de código al principio** (SmartScreen avisará en Windows): se dice en el README.
6. **macOS fuera de alcance:** firmar y notarizar exige cuenta de desarrollador de pago. Tauri lo soportaría si algún día hace falta.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| **Electron** | Chromium igual en todas partes (la cámara web funciona igual en Windows y Linux); ecosistema enorme | Instaladores de unos 100-150 MB y más memoria; para lo que hace Forja en PC sobra |
| **Solo PWA** (la web instalable desde el navegador) | Cero empaquetado | No hay «versión de PC» descargable en Releases, como pide §8; depende del navegador |
| React Native Windows/macOS | Nativo de verdad | Sin Linux; otro árbol nativo que mantener; poco ecosistema Expo |
| Una web aparte (Vite + React) | Libertad total en el escritorio | Dos frontends; duplica pantallas y lógica |

## Consecuencias
- **Gana:** una base de código para móvil y PC, instaladores de pocos MB (la mayor parte del tamaño es el bundle JS), y una arquitectura que se puede contar («la misma app en Android y en escritorio»).
- **Cuesta:**
  - Rust y las dependencias de sistema de Tauri en el CI de release (WebKitGTK en Linux).
  - Disciplina de plataforma en el frontend (archivos `.web.ts` y librerías con soporte web).
  - La webview varía entre sistemas: hay que probar en Windows y en Linux antes de cada release.
- **Riesgos conocidos en Linux:**
  - **Cámara:** en WebKitGTK falla con frecuencia (permisos, PipeWire, Wayland). Por eso en PC las fotos se suben como archivo.
  - **NVIDIA:** con algunos drivers, WebKitGTK necesita desactivar el renderizador DMABUF (la guía de Tauri lo documenta). Es el caso del portátil de desarrollo (RTX 5070 + Wayland/niri): comprobarlo en la fase 1.
- **Sin medir:** el tamaño real del instalador y el consumo de memoria. Las cifras de Tauri frente a Electron salen de comparativas de terceros (pocos MB frente a ~100 MB). Se miden con el primer build y van al README.

## Fuentes
- [Tauri 2: problemas gráficos en Linux (NVIDIA/DMABUF)](https://v2.tauri.app/develop/debug/linux-graphics/)
- [WebRTC y getUserMedia en WebKitGTK (discusión de Tauri)](https://github.com/tauri-apps/tauri/discussions/8426)
- [Expo + Tauri (Netguru)](https://netguru.com/blog/react-native-expo-tauri) · [caso Doodraw (Expo)](https://expo.dev/blog/doodraw-rust-gpu-engine-expo-config-plugin-100k-installs)
- [Tauri vs Electron (Better Stack)](https://betterstack.com/community/guides/scaling-nodejs/tauri-vs-electron-vs-deno-vs-electrobun/)
- [expo-sqlite: soporte web en alpha](https://docs.expo.dev/versions/latest/sdk/sqlite/)
