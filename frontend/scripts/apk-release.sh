#!/usr/bin/env bash
# APK firmado para GitHub Releases, compilado en local (sin EAS):
#
#   API=https://forja-api.….azurecontainerapps.io scripts/apk-release.sh
#
# La primera vez crea la clave de firma en ~/.config/forja/ (fuera del repo). Guarda una copia: sin ella no se
# puede publicar una actualización que Android acepte encima de esta versión. Si algún día pasas a EAS, se le
# sube esta misma clave (credentials.json) y las actualizaciones siguen valiendo.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${API:?Falta la URL del backend: API=https://… scripts/apk-release.sh}"

CLAVES=${FORJA_CLAVES:-$HOME/.config/forja}
ALMACEN=$CLAVES/forja-release.jks
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Android/Sdk}
# Gradle necesita una JDK 17-21 (con la 27 del sistema falla: «class file major version 71»).
export JAVA_HOME=${JAVA_HOME:-$(ls -d "$HOME"/.local/jdk/jdk-21* 2>/dev/null | tail -1)}
VERSION=$(node -p "require('./app.json').expo.version")

mkdir -p "$CLAVES" && chmod 700 "$CLAVES"
if [ ! -f "$ALMACEN" ]; then
	echo "FORJA_KEYSTORE_PASS=$(openssl rand -hex 24)" > "$CLAVES/firma.env"
	chmod 600 "$CLAVES/firma.env"
	# shellcheck disable=SC1091
	. "$CLAVES/firma.env"
	keytool -genkeypair -keystore "$ALMACEN" -storetype PKCS12 -alias forja -keyalg RSA -keysize 4096 \
		-validity 10000 -dname "CN=Forja, O=Gabriel Vallejo, C=ES" -storepass "$FORJA_KEYSTORE_PASS"
fi
# shellcheck disable=SC1091
. "$CLAVES/firma.env"
export FORJA_KEYSTORE_PASS

# EXPO_PUBLIC_API_URL del entorno manda sobre el de frontend/.env.
EXPO_PUBLIC_API_URL=$API npx expo prebuild --platform android --clean --no-install
(cd android && EXPO_PUBLIC_API_URL=$API ./gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a,x86_64 \
	--no-daemon -q)

HERRAMIENTAS=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
mkdir -p dist
SALIDA=dist/forja-$VERSION.apk
"$HERRAMIENTAS/apksigner" sign --ks "$ALMACEN" --ks-pass env:FORJA_KEYSTORE_PASS --ks-key-alias forja \
	--out "$SALIDA" android/app/build/outputs/apk/release/app-release.apk
"$HERRAMIENTAS/apksigner" verify "$SALIDA"
echo "Listo: frontend/$SALIDA ($(du -h "$SALIDA" | cut -f1))"
