import zipfile
from pathlib import Path

import calcular
from calcular import Estandar, Grupo, estandares, franja, mejores_lastres, mejores_marcas, metrica, sql

CABECERA = ["Name", "Sex", "Equipment", "Age", "BodyweightKg", "Best3SquatKg", "Best3BenchKg", "Best3DeadliftKg"]


def fila(nombre: str, sexo: str = "M", equipo: str = "Raw", edad: str = "25", peso: str = "82.5",
         sentadilla: str = "180", banca: str = "120", muerto: str = "220") -> dict[str, str]:
    return dict(zip(CABECERA, [nombre, sexo, equipo, edad, peso, sentadilla, banca, muerto], strict=True))


def test_las_franjas_incluyen_el_limite_inferior_y_la_ultima_no_tiene_techo() -> None:
    assert franja("hombre", 59.9) == (0, 60)
    assert franja("hombre", 60.0) == (60, 70)
    assert franja("hombre", 140.0) == (125, None)
    assert franja("mujer", 52.0) == (50, 60)


def test_solo_cuentan_raw_adultos_de_sexo_h_o_m_y_marcas_validas() -> None:
    mejores = mejores_marcas([
        fila("Equipado", equipo="Single-ply"),
        fila("Menor", edad="16"),
        fila("Mixto", sexo="Mx"),
        fila("Sin peso", peso=""),
        fila("Nulo", sentadilla="-200", banca="", muerto="abc"),
        fila("Sin edad", edad=""),
    ])

    assert {nombre for nombre, _, _ in mejores} == {"Sin edad"}
    assert ("Nulo", "hombre", "sentadilla") not in mejores


def test_de_cada_persona_queda_su_mejor_marca_con_el_peso_de_ese_dia() -> None:
    mejores = mejores_marcas([
        fila("Ana", sexo="F", peso="60.5", sentadilla="100"),
        fila("Ana", sexo="F", peso="63.0", sentadilla="110"),
        fila("Ana", sexo="F", peso="58.0", sentadilla="105"),
    ])

    assert mejores[("Ana", "mujer", "sentadilla")] == (110.0, 63.0)


def test_los_grupos_pequenos_no_se_publican_y_los_percentiles_crecen(monkeypatch) -> None:  # type: ignore[no-untyped-def]
    monkeypatch.setattr(calcular, "MUESTRA_MINIMA", 50)
    mejores = {(f"P{i}", "hombre", "banca"): (float(i), 85.0) for i in range(1, 101)}
    mejores[("Sola", "mujer", "banca")] = (60.0, 55.0)

    lista = estandares(mejores)

    assert len(lista) == 1
    e = lista[0]
    assert e.grupo == Grupo("banca", "hombre", 80, 90)
    assert e.muestra == 100
    assert len(e.percentiles) == 99
    assert list(e.percentiles) == sorted(e.percentiles)
    assert e.percentiles[49] == 50.5


def test_el_sql_borra_lo_anterior_y_pone_null_en_la_franja_sin_techo() -> None:
    lista = [Estandar(Grupo("peso_muerto", "hombre", 125, None), 300, tuple(float(i) for i in range(1, 100)))]

    texto = sql(lista, {"url": "u", "last_modified": "ayer", "sha256": "abc"})

    assert texto.count("DELETE FROM estandar_fuerza;") == 1
    assert "('peso_muerto','kg_1rm','hombre',125.0,NULL,1,1.0,300)" in texto
    assert "('peso_muerto','kg_1rm','hombre',125.0,NULL,99,99.0,300);" in texto
    assert "sha256 abc" in texto


def test_lee_el_csv_dentro_del_zip(tmp_path: Path) -> None:
    ruta = tmp_path / "opl.zip"
    with zipfile.ZipFile(ruta, "w") as z:
        z.writestr("opl/README.txt", "nada")
        z.writestr("opl/datos.csv", ",".join(CABECERA) + "\nAna,F,Raw,25,63,110,62.5,140\n")

    filas = list(calcular.leer_zip(ruta))

    assert filas == [fila("Ana", sexo="F", peso="63", sentadilla="110", banca="62.5", muerto="140")]


def osl(nombre: str, sexo: str = "M", estado: str = "competed", peso: str = "75", dominada: str = "60",
        fondo: str = "90", desambiguacion: str = "") -> dict[str, str]:
    return {"FirstName": nombre, "LastName": "X", "Disambiguation": desambiguacion, "Sex": sexo, "Status": estado,
            "BodyweightKg": peso, "BestPullUpKg": dominada, "BestDipsKg": fondo}


def test_openstreetlifting_solo_cuenta_quien_compitio_con_lastre_y_guarda_su_mejor_marca() -> None:
    mejores = mejores_lastres([
        osl("Ana", sexo="F", peso="58", dominada="25", fondo="40"),
        osl("Ana", sexo="F", peso="59", dominada="30", fondo="35"),
        osl("Descalificado", estado="disqualified"),
        osl("Sin peso", peso=""),
        osl("Sin lastre", dominada="0", fondo=""),
        osl("Mixto", sexo="Mx"),
    ])

    assert mejores[("Ana|X|", "mujer", "dominada_lastre")] == (30.0, 59.0)
    assert mejores[("Ana|X|", "mujer", "fondo_lastre")] == (40.0, 58.0)
    assert {a for a, _, _ in mejores} == {"Ana|X|"}


def test_dos_atletas_con_el_mismo_nombre_se_separan_por_la_desambiguacion() -> None:
    mejores = mejores_lastres([osl("Leo", desambiguacion="1"), osl("Leo", desambiguacion="2")])

    assert len([k for k in mejores if k[2] == "dominada_lastre"]) == 2


def test_la_metrica_del_lastre_es_kg_añadidos() -> None:
    assert metrica("dominada_lastre") == "kg_lastre"
    assert metrica("fondo_lastre") == "kg_lastre"
    assert metrica("banca") == "kg_1rm"


def test_lee_los_entries_de_todas_las_competiciones(tmp_path: Path) -> None:
    carpeta = tmp_path / "backend/data/competitions/fed/2026/open"
    carpeta.mkdir(parents=True)
    (carpeta / "entries.csv").write_text("FirstName,Sex\nAna,F\n", encoding="utf-8")

    assert list(calcular.leer_osl(tmp_path)) == [{"FirstName": "Ana", "Sex": "F"}]


def test_lee_el_commit_del_clon_sin_lanzar_git(tmp_path: Path) -> None:
    (tmp_path / ".git/refs/heads").mkdir(parents=True)
    (tmp_path / ".git/HEAD").write_text("ref: refs/heads/main\n", encoding="utf-8")
    (tmp_path / ".git/refs/heads/main").write_text("abc123\n", encoding="utf-8")
    assert calcular.commit_de(tmp_path) == "abc123"

    (tmp_path / ".git/refs/heads/main").unlink()
    (tmp_path / ".git/packed-refs").write_text("# pack\ndef456 refs/heads/main\n", encoding="utf-8")
    assert calcular.commit_de(tmp_path) == "def456"

    (tmp_path / ".git/HEAD").write_text("0123abcd\n", encoding="utf-8")
    assert calcular.commit_de(tmp_path) == "0123abcd"
