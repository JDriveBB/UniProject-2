"""Formatea (o revisa el formato de) los archivos cambiados respecto de la rama main.

    python scripts/formatear.py            # formatea los archivos cambiados
    python scripts/formatear.py --revisar  # solo revisa (falla si alguno no está formateado)

- Java (`*.java`): Spotless con palantir-java-format (`-DspotlessFiles=...`).
- Web (`web/**/*.{ts,tsx,css}`): Prettier a versión fija con `npm exec`.
El resto del código no se toca.
"""

import re
import subprocess
import sys

sys.dont_write_bytecode = True

from verificar import RAIZ, WEB, correr, mvnw, npm

PRETTIER = "prettier@3.10.1"


def archivos_cambiados() -> list[str]:
    ordenes = [
        ["git", "diff", "--name-only", "--diff-filter=ACMR", "main"],
        ["git", "ls-files", "--others", "--exclude-standard"],
    ]
    archivos: set[str] = set()
    for orden in ordenes:
        salida = subprocess.run(
            orden, cwd=RAIZ, check=True, capture_output=True, text=True, encoding="utf-8"
        ).stdout
        for linea in salida.splitlines():
            ruta = linea.strip().replace("\\", "/")
            if ruta:
                archivos.add(ruta)
    return sorted(archivos)


def patron_spotless(archivos: list[str]) -> str:
    return ",".join(
        ".*" + r"[\\/]".join(re.escape(parte) for parte in ruta.split("/"))
        for ruta in archivos
    )


def main(argumentos: list[str]) -> int:
    if argumentos not in ([], ["--revisar"]):
        sys.exit(__doc__)
    revisar = argumentos == ["--revisar"]

    cambiados = archivos_cambiados()
    java = [a for a in cambiados if a.endswith(".java")]
    web = [
        a.removeprefix("web/")
        for a in cambiados
        if a.startswith("web/") and a.endswith((".ts", ".tsx", ".css"))
    ]

    if not java and not web:
        print("No hay archivos Java ni web cambiados que formatear.", flush=True)
        return 0

    pasos = []
    if java:
        meta = "spotless:check" if revisar else "spotless:apply"
        pasos.append((
            f"Java: {meta} ({', '.join(java)})",
            [*mvnw(), "-B", "-ntp", meta, f"-DspotlessFiles={patron_spotless(java)}"],
            RAIZ,
        ))
    if web:
        modo = "--check" if revisar else "--write"
        pasos.append((
            f"Web: Prettier {modo} ({', '.join(web)})",
            [npm(), "exec", "--yes", "--", PRETTIER, modo, *web],
            WEB,
        ))
    return correr(pasos)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
