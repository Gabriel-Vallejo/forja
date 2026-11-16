import hashlib
import json
from pathlib import Path

import pytest

import descargar


def preparar(tmp_path: Path, contenido: bytes) -> tuple[Path, Path]:
    fuente = tmp_path / "fuente.json"
    fuente.write_text(
        json.dumps(
            {
                "url": "https://ejemplo.invalido/x.json",
                "sha256": hashlib.sha256(contenido).hexdigest(),
            }
        ),
        encoding="utf-8",
    )
    return fuente, tmp_path / "cache"


def test_descarga_y_guarda_si_el_hash_coincide(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    fuente, cache = preparar(tmp_path, b"[1]")
    monkeypatch.setattr(descargar, "_bajar", lambda url: b"[1]")
    ruta = descargar.asegurar(fuente, cache)
    assert ruta.read_bytes() == b"[1]"


def test_rechaza_una_descarga_con_otro_hash(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    fuente, cache = preparar(tmp_path, b"[1]")
    monkeypatch.setattr(descargar, "_bajar", lambda url: b"[2]")
    with pytest.raises(descargar.HashIncorrecto):
        descargar.asegurar(fuente, cache)
    assert not any(cache.glob("*.json"))


def test_usa_la_cache_sin_tocar_la_red(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    fuente, cache = preparar(tmp_path, b"[1]")
    cache.mkdir()
    (cache / "exercises.json").write_bytes(b"[1]")

    def sin_red(url: str) -> bytes:
        raise AssertionError("no debería descargar")

    monkeypatch.setattr(descargar, "_bajar", sin_red)
    assert descargar.asegurar(fuente, cache).read_bytes() == b"[1]"


def test_cache_corrupta_se_vuelve_a_descargar(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    fuente, cache = preparar(tmp_path, b"[1]")
    cache.mkdir()
    (cache / "exercises.json").write_bytes(b"basura")
    monkeypatch.setattr(descargar, "_bajar", lambda url: b"[1]")
    assert descargar.asegurar(fuente, cache).read_bytes() == b"[1]"


def test_main_imprime_la_ruta(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    fuente, cache = preparar(tmp_path, b"[1]")
    monkeypatch.setattr(descargar, "FUENTE", fuente)
    monkeypatch.setattr(descargar, "CACHE", cache)
    monkeypatch.setattr(descargar, "_bajar", lambda url: b"[1]")
    descargar.main()
    assert "exercises.json" in capsys.readouterr().out
