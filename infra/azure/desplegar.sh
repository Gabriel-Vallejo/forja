#!/usr/bin/env bash
# Despliega el backend de Forja en Azure: MySQL Flexible (B1ms), Container Apps (escala a cero) y el correo con
# Azure Communication Services (dominio gestionado por Azure, datos en Europa). Se puede repetir: la primera vez
# crea todo y las siguientes solo suben la versión nueva del backend.
#
#   az login                      # una vez, con tu cuenta de Azure for Students
#   infra/azure/desplegar.sh      # al acabar imprime la URL pública del backend
#
# Los secretos se generan la primera vez y se guardan en infra/azure/.env.azure (ignorado por git). Guarda una
# copia fuera del portátil: sin FORJA_CIFRADO_CLAVE_ACTUAL los datos cifrados de la base no se pueden leer.
#
# shortcut: MySQL con acceso público limitado a servicios de Azure (regla 0.0.0.0) y contraseña larga con TLS;
# la red privada (VNet) y la IaC con Bicep van en la fase 5.
set -euo pipefail
cd "$(dirname "$0")"

GRUPO=${GRUPO:-forja}
REGION=${REGION:-spaincentral}
# Con 0 el backend se duerme sin uso y tarda ~32 s en despertar (0,5 vCPU): el primer clic en el enlace del correo
# o la primera petición de la app dan 503. Con 1 siempre responde; cuesta ~10 $/mes del crédito de estudiante.
REPLICAS_MINIMAS=${REPLICAS_MINIMAS:-1}
SECRETOS=.env.azure
RAIZ=$(git rev-parse --show-toplevel)

az config set extension.use_dynamic_install=yes_without_prompt core.only_show_errors=true >/dev/null
az account show --query name -o tsv >/dev/null || { echo "Primero: az login"; exit 1; }

# --- Secretos: solo se generan si faltan ------------------------------------------------------------------
touch "$SECRETOS" && chmod 600 "$SECRETOS"
falta() { grep -q "^$1=" "$SECRETOS" || echo "$1=$2" >> "$SECRETOS"; }
falta SUFIJO "$(openssl rand -hex 3)"
falta MYSQL_PASSWORD "$(openssl rand -base64 30 | tr -d '/+=' | head -c 32)Aa1"
falta FORJA_JWT_KID_ACTUAL "az-$(date +%Y%m%d)"
falta FORJA_JWT_CLAVE_PRIVADA_ACTUAL "$(openssl genpkey -algorithm ed25519 -outform DER | base64 -w0)"
falta FORJA_CIFRADO_KID_ACTUAL "az-$(date +%Y%m%d)"
falta FORJA_CIFRADO_CLAVE_ACTUAL "$(openssl rand -base64 32)"
set -a; . "./$SECRETOS"; set +a

MYSQL=forja-mysql-$SUFIJO
REGISTRO=forja$SUFIJO
CORREO=forja-correo-$SUFIJO
EMAIL=forja-email-$SUFIJO
ENTORNO=forja-entorno
APP=forja-api
VERSION=$(git -C "$RAIZ" rev-parse --short HEAD)
existe() { "$@" >/dev/null 2>&1; }

echo "→ Proveedores y grupo $GRUPO en $REGION"
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.ContainerRegistry Microsoft.DBforMySQL \
	Microsoft.Communication; do
	az provider register -n "$p" >/dev/null
done
az group create -n "$GRUPO" -l "$REGION" -o none

echo "→ MySQL $MYSQL (B1ms, 20 GB; tarda ~10 min la primera vez)"
if ! existe az mysql flexible-server show -g "$GRUPO" -n "$MYSQL"; then
	az mysql flexible-server create -g "$GRUPO" -n "$MYSQL" -l "$REGION" --tier Burstable --sku-name Standard_B1ms \
		--storage-size 20 --version 8.4 --admin-user forja --admin-password "$MYSQL_PASSWORD" \
		--public-access 0.0.0.0 --backup-retention 7 --yes -o none
fi
existe az mysql flexible-server db show -g "$GRUPO" -s "$MYSQL" -d forja ||
	az mysql flexible-server db create -g "$GRUPO" -s "$MYSQL" -d forja -o none

echo "→ Registro de imágenes $REGISTRO e imagen $VERSION (se compila aquí con Docker)"
existe az acr show -n "$REGISTRO" || az acr create -g "$GRUPO" -n "$REGISTRO" -l "$REGION" --sku Basic --admin-enabled true -o none
REGISTRO_CLAVE=$(az acr credential show -n "$REGISTRO" --query 'passwords[0].value' -o tsv)
# Azure for Students no permite ACR Tasks (az acr build): la imagen se compila en local y se sube.
# Con BuildKit y capas OCI: el constructor clásico de Docker mezcla capas OCI y Docker en el manifiesto y
# Container Apps lo rechaza («unsupported MIME type for compression»).
echo "$REGISTRO_CLAVE" | docker login "$REGISTRO.azurecr.io" -u "$REGISTRO" --password-stdin >/dev/null
docker buildx build -q --provenance=false --sbom=false -t "$REGISTRO.azurecr.io/forja-backend:$VERSION" \
	--output type=image,oci-mediatypes=true,push=true "$RAIZ/backend" >/dev/null

echo "→ Correo $CORREO (dominio gestionado por Azure, datos en Europa)"
existe az communication email show -g "$GRUPO" -n "$EMAIL" ||
	az communication email create -g "$GRUPO" -n "$EMAIL" -l global --data-location Europe -o none
existe az communication email domain show -g "$GRUPO" --email-service-name "$EMAIL" -n AzureManagedDomain ||
	az communication email domain create -g "$GRUPO" --email-service-name "$EMAIL" -n AzureManagedDomain \
		-l global --domain-management AzureManaged -o none
DOMINIO_ID=$(az communication email domain show -g "$GRUPO" --email-service-name "$EMAIL" -n AzureManagedDomain --query id -o tsv)
REMITENTE="DoNotReply@$(az communication email domain show -g "$GRUPO" --email-service-name "$EMAIL" \
	-n AzureManagedDomain --query mailFromSenderDomain -o tsv)"
existe az communication show -g "$GRUPO" -n "$CORREO" ||
	az communication create -g "$GRUPO" -n "$CORREO" -l global --data-location Europe -o none
az communication update -g "$GRUPO" -n "$CORREO" --linked-domains "$DOMINIO_ID" -o none
CADENA_CORREO=$(az communication list-key -g "$GRUPO" -n "$CORREO" --query primaryConnectionString -o tsv)

echo "→ Container Apps ($ENTORNO)"
existe az containerapp env show -g "$GRUPO" -n "$ENTORNO" ||
	az containerapp env create -g "$GRUPO" -n "$ENTORNO" -l "$REGION" -o none
URL="https://$APP.$(az containerapp env show -g "$GRUPO" -n "$ENTORNO" --query properties.defaultDomain -o tsv)"

SECRETOS_APP=(
	"mysql-password=$MYSQL_PASSWORD"
	"jwt-clave=$FORJA_JWT_CLAVE_PRIVADA_ACTUAL"
	"cifrado-clave=$FORJA_CIFRADO_CLAVE_ACTUAL"
	"correo-cadena=$CADENA_CORREO"
)
VARIABLES=(
	"MYSQL_HOST=$MYSQL.mysql.database.azure.com" MYSQL_PUERTO=3306 MYSQL_DATABASE=forja MYSQL_USER=forja
	MYSQL_PASSWORD=secretref:mysql-password
	"FORJA_JWT_KID_ACTUAL=$FORJA_JWT_KID_ACTUAL" FORJA_JWT_CLAVE_PRIVADA_ACTUAL=secretref:jwt-clave
	"FORJA_CIFRADO_KID_ACTUAL=$FORJA_CIFRADO_KID_ACTUAL" FORJA_CIFRADO_CLAVE_ACTUAL=secretref:cifrado-clave
	FORJA_CORREO_CADENA_CONEXION=secretref:correo-cadena "FORJA_CORREO_REMITENTE=$REMITENTE"
	"FORJA_CORREO_URL_ENLACES=$URL"
	# Detrás del ingress de Azure: la IP del cliente sale de X-Forwarded-For (el límite de intentos va por IP).
	SERVER_FORWARD_HEADERS_STRATEGY=native
)
IMAGEN="$REGISTRO.azurecr.io/forja-backend:$VERSION"

echo "→ App $APP con la imagen $VERSION"
if existe az containerapp show -g "$GRUPO" -n "$APP"; then
	az containerapp secret set -g "$GRUPO" -n "$APP" --secrets "${SECRETOS_APP[@]}" -o none
	az containerapp update -g "$GRUPO" -n "$APP" --image "$IMAGEN" --set-env-vars "${VARIABLES[@]}" \
		--min-replicas "$REPLICAS_MINIMAS" -o none
else
	az containerapp create -g "$GRUPO" -n "$APP" --environment "$ENTORNO" --image "$IMAGEN" \
		--registry-server "$REGISTRO.azurecr.io" --registry-username "$REGISTRO" --registry-password "$REGISTRO_CLAVE" \
		--target-port 8080 --ingress external --transport auto \
		--cpu 0.5 --memory 1.0Gi --min-replicas "$REPLICAS_MINIMAS" --max-replicas 1 \
		--secrets "${SECRETOS_APP[@]}" --env-vars "${VARIABLES[@]}" -o none
fi

echo "→ Esperando a que arranque (la primera petición despierta el contenedor)"
for _ in $(seq 1 60); do
	curl -sf "$URL/actuator/health" >/dev/null && { echo "Listo: $URL"; exit 0; }
	sleep 5
done
echo "No responde en 5 min: az containerapp logs show -g $GRUPO -n $APP --tail 100" && exit 1
