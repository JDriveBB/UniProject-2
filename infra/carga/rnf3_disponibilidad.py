"""RNF3 (disponibilidad): sondea /actuator/health cada segundo, provoca una caída del proceso del núcleo y mide
cuánto tarda Docker en devolverlo (restart: unless-stopped) sin que nadie intervenga.

Uso: python rnf3_disponibilidad.py <carpeta de salida> [--minutos 6] [--caida-en 60]
La caída es un SIGTERM al proceso java (PID 1 del contenedor): termina con código distinto de 0 y Docker lo
reinicia por su política. No cambia datos.
"""

import argparse
import json
import subprocess
import time
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

URL = "http://127.0.0.1:18080/actuator/health"
CONTENEDOR = "uniproject-pruebas-nucleo-1"


def sano() -> bool:
    try:
        with urllib.request.urlopen(URL, timeout=2) as r:
            return r.status == 200 and b'"UP"' in r.read()
    except Exception:  # noqa: BLE001 - cualquier fallo cuenta como caído
        return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("salida")
    ap.add_argument("--minutos", type=float, default=6)
    ap.add_argument("--caida-en", type=float, default=60, help="segundos desde el inicio")
    a = ap.parse_args()
    salida = Path(a.salida)
    salida.mkdir(parents=True, exist_ok=True)

    muestras = []
    inicio = time.monotonic()
    caida_hecha = False
    while (t := time.monotonic() - inicio) < a.minutos * 60:
        if not caida_hecha and t >= a.caida_en:
            subprocess.run(["docker", "exec", CONTENEDOR, "kill", "-TERM", "1"], check=True)
            muestras.append({"t": round(t, 1), "evento": "caida provocada (SIGTERM al proceso java)"})
            caida_hecha = True
        muestras.append({"t": round(time.monotonic() - inicio, 1), "arriba": sano()})
        time.sleep(max(0.0, 1 - ((time.monotonic() - inicio) % 1)))

    sondeos = [m for m in muestras if "arriba" in m]
    abajo = [m for m in sondeos if not m["arriba"]]
    reinicios = subprocess.run(
        ["docker", "inspect", CONTENEDOR, "--format", "{{.RestartCount}}"], capture_output=True, text=True
    ).stdout.strip()
    resultado = {
        "requisito": "RNF3: disponibilidad mínima de 99,5 % en semanas pico",
        "fecha": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "duracion_s": round(sondeos[-1]["t"], 1),
        "sondeos": len(sondeos),
        "sondeos_caidos": len(abajo),
        "disponibilidad_en_la_ventana_pct": round(100 * (len(sondeos) - len(abajo)) / len(sondeos), 2),
        "segundos_sin_servicio": round((abajo[-1]["t"] - abajo[0]["t"] + 1) if abajo else 0, 1),
        "reinicios_docker": reinicios,
        "presupuesto_semanal_99_5_min": round(7 * 24 * 60 * 0.005, 1),
    }
    (salida / "rnf3-muestras.json").write_text(json.dumps(muestras, indent=1), encoding="utf-8")
    (salida / "rnf3-resultado.json").write_text(json.dumps(resultado, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(resultado, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
