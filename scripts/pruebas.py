"""Corre solo algunas pruebas de UniProject, según los archivos que se le pasen.

    python scripts/pruebas.py <archivo> [<archivo> …]

- `web/src/**/x.test.ts` → Vitest con esos archivos (instala dependencias si faltan).
- `<módulo>/src/test/java/**/XTest.java` → Maven, solo esas clases en su módulo (y lo que necesita).
Rutas relativas a la raíz del código (separadores / o \\).
"""

import re
import sys
from collections import defaultdict
from pathlib import Path

from verificar import RAIZ, WEB, correr, mvnw, npm

PRUEBA_JAVA = re.compile(r"^(?P<modulo>[\w-]+)/src/test/java/(?:.+/)?(?P<clase>\w+)\.java$")
PRUEBA_WEB = re.compile(r"^web/(?P<ruta>src/.+\.test\.ts)$")


def main(archivos: list[str]) -> int:
    if not archivos:
        sys.exit(__doc__)
    web, java, otros = [], defaultdict(list), []
    for a in archivos:
        a = a.replace("\\", "/").removeprefix("./")
        if m := PRUEBA_WEB.match(a):
            web.append(m["ruta"])
        elif m := PRUEBA_JAVA.match(a):
            java[m["modulo"]].append(m["clase"])
        else:
            otros.append(a)
    if otros:
        sys.exit("No son archivos de prueba reconocidos: " + ", ".join(otros))

    pasos = []
    if web:
        if not (WEB / "node_modules").is_dir():
            pasos.append(("Web: dependencias", [npm(), "ci", "--no-audit", "--no-fund"], WEB))
        pasos.append(("Web: pruebas elegidas", [npm(), "exec", "--", "vitest", "run", *web], WEB))
    for modulo, clases in java.items():
        pasos.append((
            f"Java: pruebas elegidas de {modulo}",
            [*mvnw(), "-B", "-ntp", "-pl", modulo, "-am", "test",
             "-Dtest=" + ",".join(clases), "-Dsurefire.failIfNoSpecifiedTests=false"],
            RAIZ,
        ))
    return correr(pasos)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
