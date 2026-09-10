#!/usr/bin/env python3
"""Firma un APK release usando una clave privada conservada fuera del repositorio."""
import argparse
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("apk", type=Path, help="APK release sin firma")
parser.add_argument("firma", type=Path, help="Carpeta privada con detallitos-release.p12 y clave.txt")
parser.add_argument("salida", type=Path, help="APK firmado de salida")
parser.add_argument("--build-tools", type=Path, required=True, help="Carpeta Android SDK build-tools/35.0.0 o posterior")
args = parser.parse_args()
apk, firma, salida = args.apk.resolve(), args.firma.resolve(), args.salida.resolve()
if not apk.is_file() or not (firma / "detallitos-release.p12").is_file() or not (firma / "clave.txt").is_file():
    parser.error("Falta el APK o el respaldo privado de firma.")
if salida.exists() or salida == apk:
    parser.error("La salida debe ser un archivo nuevo.")
salida.parent.mkdir(parents=True, exist_ok=True)
zipalign = args.build_tools.resolve() / "zipalign"
apksigner = args.build_tools.resolve() / "apksigner"
import tempfile
with tempfile.TemporaryDirectory(prefix="detallitos-firma-") as temp:
    alineado = Path(temp) / "alineado.apk"
    subprocess.run([str(zipalign), "-P", "16", "-f", "4", str(apk), str(alineado)], check=True)
    subprocess.run([str(apksigner), "sign", "--ks", str(firma / "detallitos-release.p12"),
        "--ks-type", "PKCS12", "--ks-key-alias", "detallitos",
        "--ks-pass", "file:" + str(firma / "clave.txt"),
        "--out", str(salida), str(alineado)], check=True)
    subprocess.run([str(apksigner), "verify", "--verbose", "--print-certs", str(salida)], check=True)
    subprocess.run([str(zipalign), "-c", "-P", "16", "4", str(salida)], check=True)
print("APK firmado y verificado:", salida)
