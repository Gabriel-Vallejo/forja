import json
from typing import Any

import pytest

import descargar


@pytest.fixture(scope="session")
def registros() -> list[dict[str, Any]]:
    """El JSON real de free-exercise-db (verificado por hash; se descarga si no está en caché)."""
    datos: list[dict[str, Any]] = json.loads(descargar.asegurar().read_text(encoding="utf-8"))
    return datos
