import zipfile
from pathlib import Path

import calcular
from calcular import Estandar, Grupo, estandares, franja, mejores_marcas, sql

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
    assert "('peso_muerto','hombre',125.0,NULL,1,1.0,300)" in texto
    assert "('peso_muerto','hombre',125.0,NULL,99,99.0,300);" in texto
    assert "sha256 abc" in texto


def test_lee_el_csv_dentro_del_zip(tmp_path: Path) -> None:
    ruta = tmp_path / "opl.zip"
    with zipfile.ZipFile(ruta, "w") as z:
        z.writestr("opl/README.txt", "nada")
        z.writestr("opl/datos.csv", ",".join(CABECERA) + "\nAna,F,Raw,25,63,110,62.5,140\n")

    filas = list(calcular.leer_zip(ruta))

    assert filas == [fila("Ana", sexo="F", peso="63", sentadilla="110", banca="62.5", muerto="140")]
