"""Descarga el volcado de OpenPowerlifting (dominio público) a .cache/ y apunta de dónde salió.

Solo los datos: el código de OpenPowerlifting es AGPLv3 y no se usa (ADR 0005).
"""

from __future__ import annotations

import hashlib
import json
import urllib.request
from datetime import UTC, datetime
from pathlib import Path

URL = "https://openpowerlifting.gitlab.io/opl-csv/files/openpowerlifting-latest.zip"
CACHE = Path(__file__).parent / ".cache"
ZIP = CACHE / "openpowerlifting-latest.zip"
PROCEDENCIA = CACHE / "procedencia.json"


def sha256(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def descargar() -> dict[str, str]:
    CACHE.mkdir(exist_ok=True)
    peticion = urllib.request.Request(URL, headers={"User-Agent": "forja-estandares"})
    with urllib.request.urlopen(peticion, timeout=600) as respuesta, ZIP.open("wb") as destino:  # noqa: S310
        modificado = respuesta.headers.get("Last-Modified", "")
        while bloque := respuesta.read(1 << 20):
            destino.write(bloque)
    procedencia = {
        "url": URL,
        "descargado": datetime.now(UTC).isoformat(timespec="seconds"),
        "last_modified": modificado,
        "sha256": sha256(ZIP),
    }
    PROCEDENCIA.write_text(json.dumps(procedencia, indent=2), encoding="utf-8")
    return procedencia


if __name__ == "__main__":  # pragma: no cover
    print(json.dumps(descargar(), indent=2))
