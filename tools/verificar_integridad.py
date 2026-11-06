#!/usr/bin/env python3
"""Comprueba que los recursos del proyecto Fabric son los del JAR original de Forge.

Uso:  python3 tools/verificar_integridad.py [ruta/al/DEDsafio4RuletaFanProject-1_1_0.jar]

- Sin JAR: recalcula el SHA-256 de cada recurso de src/main/resources y lo compara con
  docs/INTEGRIDAD_RECURSOS.tsv (detecta cualquier cambio o corrupción desde que se hizo la tabla).
- Con JAR: además compara cada fila con el fichero original dentro del JAR y avisa si algún fichero
  del JAR (que no sea .class ni META-INF) no está en la tabla.
Estados de la tabla: IDENTICO (mismos bytes, misma ruta), MOVIDO (mismos bytes, carpeta renombrada en 1.21),
CONVERTIDO (formato de 1.21.1; la nota dice qué cambió).
"""
import hashlib
import os
import sys
import zipfile

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(RAIZ, 'src', 'main', 'resources')
TABLA = os.path.join(RAIZ, 'docs', 'INTEGRIDAD_RECURSOS.tsv')


def sha(b):
    return hashlib.sha256(b).hexdigest()


def main():
    jar = zipfile.ZipFile(sys.argv[1]) if len(sys.argv) > 1 else None
    filas = [l.rstrip('\n').split('\t') for l in open(TABLA, encoding='utf-8')][1:]
    problemas = 0
    originales = set()
    cuenta = {}
    for f in filas:
        orig, dest, estado, sha_o, sha_d = f[0], f[1], f[2], f[3], f[4]
        originales.add(orig)
        cuenta[estado] = cuenta.get(estado, 0) + 1
        ruta = os.path.join(RES, *dest.split('/'))
        if not os.path.exists(ruta):
            print('FALTA EN EL PROYECTO:', dest)
            problemas += 1
            continue
        if sha(open(ruta, 'rb').read()) != sha_d:
            print('CAMBIADO (no coincide con la tabla):', dest)
            problemas += 1
        if estado in ('IDENTICO', 'MOVIDO') and sha_o != sha_d:
            print('LA TABLA DICE IGUAL PERO EL SHA DIFIERE:', dest)
            problemas += 1
        if jar is not None:
            try:
                if sha(jar.read(orig)) != sha_o:
                    print('EL JAR ORIGINAL NO COINCIDE CON LA TABLA:', orig)
                    problemas += 1
            except KeyError:
                print('NO ESTA EN EL JAR ORIGINAL:', orig)
                problemas += 1
    fuera = []
    if jar is not None:
        for n in jar.namelist():
            if n.endswith('/') or n.endswith('.class') or n.startswith('META-INF/'):
                continue
            if n not in originales:
                fuera.append(n)
        for n in fuera:
            print('FICHERO DEL JAR FUERA DE LA TABLA:', n)
    print(f'{len(filas)} recursos en la tabla ({", ".join(f"{v} {k}" for k, v in sorted(cuenta.items()))}); '
          f'{problemas} problemas' + (f'; {len(fuera)} ficheros del JAR sin tabla' if jar is not None else ''))
    return 1 if problemas or fuera else 0


if __name__ == '__main__':
    sys.exit(main())
