"""Descarga `dist/exercises.json` de free-exercise-db fijado a un commit y comprueba su SHA-256.

La URL, el commit y el hash esperado viven en `fuente.json`. El archivo va a `.cache/`, que no se
sube al repo (ADR 0005: los volcados no se versionan, se apunta cómo obtenerlos).
"""

import hashlib
import json
import urllib.request
from pathlib import Path

AQUI = Path(__file__).parent
FUENTE = AQUI / "fuente.json"
CACHE = AQUI / ".cache"
NOMBRE = "exercises.json"
TIEMPO_MAXIMO_S = 60


class HashIncorrecto(RuntimeError):
    """El contenido no coincide con el SHA-256 fijado en fuente.json."""


def _bajar(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=TIEMPO_MAXIMO_S) as respuesta:  # noqa: S310
        datos: bytes = respuesta.read()
    return datos


def _sha256(datos: bytes) -> str:
    return hashlib.sha256(datos).hexdigest()


def asegurar(fuente: Path = FUENTE, cache: Path = CACHE) -> Path:
    """Devuelve la ruta del JSON verificado, descargándolo si no está en caché o está corrupto."""
    config = json.loads(fuente.read_text(encoding="utf-8"))
    destino = cache / NOMBRE
    if destino.exists() and _sha256(destino.read_bytes()) == config["sha256"]:
        return destino
    datos = _bajar(config["url"])
    obtenido = _sha256(datos)
    if obtenido != config["sha256"]:
        raise HashIncorrecto(f"esperado {config['sha256']}, obtenido {obtenido}")
    cache.mkdir(parents=True, exist_ok=True)
    destino.write_bytes(datos)
    return destino


def main() -> None:
    print(asegurar(FUENTE, CACHE))


if __name__ == "__main__":
    main()
