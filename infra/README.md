# infra

Despliegue del backend en Azure (Azure for Students).

## ◆ Qué se crea
| Recurso | Para qué | Tamaño |
|---|---|---|
| Azure Database for MySQL Flexible | La base de datos (MySQL 8.4) | B1ms, 20 GB, copias 7 días |
| Container Registry | La imagen del backend (se compila en local con Docker: Azure for Students no permite `az acr build`) | Basic |
| Container Apps | El backend con HTTPS | 0,5 vCPU, 1 GiB, 1 réplica siempre encendida |
| Communication Services Email | Correos de verificación y recuperación | Dominio gestionado por Azure, datos en Europa |

Todo en el grupo `forja`, región `spaincentral` (cámbiala con `REGION=…`).

## ◆ Desplegar
```bash
az login                      # una vez (Docker necesita el plugin buildx)
infra/azure/desplegar.sh      # crea lo que falte, sube la versión del commit actual e imprime la URL
```
- Se puede repetir: las siguientes veces solo compila y sube la imagen nueva (~3 min).
- Los secretos (contraseña de MySQL, clave JWT, clave de cifrado de columnas) se generan la primera vez en `infra/azure/.env.azure`, ignorado por git. **Guarda una copia fuera del portátil**: sin la clave de cifrado, las limitaciones guardadas no se pueden leer.
- Una réplica siempre encendida (~10 $/mes del crédito). Con `REPLICAS_MINIMAS=0 infra/azure/desplegar.sh` se duerme sin uso y sale casi gratis, pero tarda ~32 s en despertar y la primera petición da 503.
- Logs: `az containerapp logs show -g forja -n forja-api --tail 100`.

## ◆ Límites conocidos
- MySQL acepta conexiones de cualquier servicio de Azure (regla `0.0.0.0`), con TLS y contraseña larga. La red privada (VNet) llega con la IaC en la fase 5.
- El dominio de correo gestionado por Azure envía como mucho 10 correos por hora. Con un dominio propio verificado se puede pedir más.
