#!/usr/bin/env python3
"""Lanzador en Docker de la prueba de carga RNF2 (k6) contra la sesión de pruebas."""

import argparse
import os
from pathlib import Path
import subprocess
import sys
import tempfile

IMAGEN_K6 = "grafana/k6:2.3.0"
BASE_POR_DEFECTO = "http://host.docker.internal:18080"


def ruta_credenciales() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "UniProject" / "sesion-pruebas" / "credenciales.txt"
    return Path.home() / "AppData" / "Local" / "UniProject" / "sesion-pruebas" / "credenciales.txt"


def leer_contrasena_demo(archivo: Path) -> str:
    if not archivo.is_file():
        raise RuntimeError(
            f"No se encontró el archivo de credenciales en {archivo}. "
            "Levanta primero la sesión de pruebas con: "
            ".\\infra\\pruebas\\sesion-pruebas.ps1 -Accion Levantar"
        )
    for linea in archivo.read_text(encoding="utf-8-sig").splitlines():
        if linea.startswith("DEMO_CONTRASENA="):
            valor = linea[len("DEMO_CONTRASENA="):].strip()
            if valor:
                return valor
    raise RuntimeError(
        f"Falta DEMO_CONTRASENA en {archivo}. "
        "Reinicia la sesión de pruebas con: "
        ".\\infra\\pruebas\\sesion-pruebas.ps1 -Accion Reiniciar"
    )


def parsear_argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Ejecuta la prueba de carga RNF2 (k6 en Docker) contra la sesión de pruebas."
    )
    parser.add_argument(
        "--corta",
        action="store_true",
        help="Corrida corta de verificación (10 usuarios virtuales, 30 s en total).",
    )
    parser.add_argument(
        "--salida",
        type=Path,
        default=None,
        help="Carpeta donde se guardarán resumen.json y resumen.txt.",
    )
    return parser.parse_args()


def main() -> int:
    args = parsear_argumentos()

    carpeta_carga = Path(os.path.abspath(__file__)).parent
    guion = carpeta_carga / "rnf2-evaluacion.js"
    if not guion.is_file():
        print(f"Error: no se encontró el guion de k6 en {guion}", file=sys.stderr)
        return 1

    try:
        contrasena_demo = leer_contrasena_demo(ruta_credenciales())
    except RuntimeError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1

    if args.salida is not None:
        carpeta_salida = Path(os.path.abspath(args.salida))
        carpeta_salida.mkdir(parents=True, exist_ok=True)
    else:
        carpeta_salida = Path(tempfile.mkdtemp(prefix="uniproject-carga-"))

    base = os.environ.get("BASE", BASE_POR_DEFECTO)
    if args.corta:
        vus_max = "10"
        duracion_subida = "5s"
        duracion_sostenida = "20s"
        duracion_bajada = "5s"
        modo = "corta (10 VUs, 30 s)"
    else:
        vus_max = os.environ.get("VUS_MAX", "500")
        duracion_subida = os.environ.get("DURACION_SUBIDA", "1m")
        duracion_sostenida = os.environ.get("DURACION_SOSTENIDA", "3m")
        duracion_bajada = os.environ.get("DURACION_BAJADA", "30s")
        modo = f"completa RNF2 ({vus_max} VUs, {duracion_subida} -> {duracion_sostenida} -> {duracion_bajada})"

    # La contraseña se hereda desde env del proceso con `-e DEMO_CONTRASENA` (nunca en argv).
    entorno = os.environ.copy()
    entorno["DEMO_CONTRASENA"] = contrasena_demo

    comando = [
        "docker",
        "run",
        "--rm",
        "-e",
        "DEMO_CONTRASENA",
        "-e",
        f"BASE={base}",
        "-e",
        "SALIDA=/salida",
        "-e",
        f"VUS_MAX={vus_max}",
        "-e",
        f"DURACION_SUBIDA={duracion_subida}",
        "-e",
        f"DURACION_SOSTENIDA={duracion_sostenida}",
        "-e",
        f"DURACION_BAJADA={duracion_bajada}",
        "-v",
        f"{carpeta_carga}:/guion:ro",
        "-v",
        f"{carpeta_salida}:/salida",
        IMAGEN_K6,
        "run",
        "/guion/rnf2-evaluacion.js",
    ]

    print(f"Iniciando prueba de carga RNF2 con {IMAGEN_K6}")
    print(f"Modo            : {modo}")
    print(f"Objetivo        : {base}")
    print(f"Carpeta salida  : {carpeta_salida}")

    try:
        resultado = subprocess.run(comando, env=entorno, check=False)
    except FileNotFoundError:
        print("Error: no se encontró el ejecutable 'docker' en el PATH.", file=sys.stderr)
        return 1

    print(f"Resumen guardado en: {carpeta_salida}")
    print(f"- {carpeta_salida / 'resumen.txt'}")
    print(f"- {carpeta_salida / 'resumen.json'}")
    if resultado.returncode == 0:
        print("Resultado: todos los umbrales de RNF2 se cumplieron (código 0).")
    elif resultado.returncode == 99:
        print("Resultado: uno o más umbrales fallaron (código 99).")
    else:
        print(f"Resultado: k6 finalizó con código {resultado.returncode}.")

    return resultado.returncode


if __name__ == "__main__":
    sys.exit(main())
