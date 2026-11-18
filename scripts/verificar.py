"""Puerta de calidad de UniProject: lo mismo para el jefe, el ejecutor y la integración.

    python scripts/verificar.py

1. Web (web/): `npm ci`, chequeo de tipos + build de Vite y pruebas de Vitest.
2. Java (raíz): `mvnw verify` en todos los módulos (las pruebas de integración usan Testcontainers:
   Docker Desktop debe estar encendido).
Se detiene en el primer paso que falle y devuelve su código de salida.
"""

import os
import shutil
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
WEB = RAIZ / "web"


def mvnw() -> list[str]:
    if os.name == "nt":
        return [str(RAIZ / "mvnw.cmd")]
    return ["sh", str(RAIZ / "mvnw")]


def npm() -> str:
    return shutil.which("npm") or "npm"


PASOS = [
    ("Web: dependencias", [npm(), "ci", "--no-audit", "--no-fund"], WEB),
    ("Web: tipos y build", [npm(), "run", "build"], WEB),
    ("Web: pruebas", [npm(), "test"], WEB),
    ("Java: compilación y pruebas", [*mvnw(), "-B", "-ntp", "verify"], RAIZ),
]


def correr(pasos) -> int:
    for nombre, orden, carpeta in pasos:
        print(f"\n=== {nombre} ===", flush=True)
        codigo = subprocess.run(orden, cwd=carpeta).returncode
        if codigo:
            print(f"\n*** FALLÓ: {nombre} (código {codigo}) ***", flush=True)
            return codigo
    print("\n=== Puerta de calidad en verde ===", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(correr(PASOS))
