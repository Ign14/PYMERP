#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
import sys


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verifica el coverage de Flutter contra un umbral mínimo.")
    parser.add_argument("lcov", type=Path, help="Ruta al archivo lcov.info generado por flutter test --coverage")
    parser.add_argument("threshold", type=float, help="Porcentaje mínimo de cobertura esperado")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.lcov.exists():
        print(f"ERROR: El archivo de cobertura no existe: {args.lcov}", file=sys.stderr)
        return 1

    total_lines = 0
    covered_lines = 0
    with args.lcov.open() as stream:
        for raw in stream:
            line = raw.strip()
            if line.startswith("LF:"):
                total_lines += int(line.split(":", 1)[1])
            elif line.startswith("LH:"):
                covered_lines += int(line.split(":", 1)[1])

    coverage = 0.0 if total_lines == 0 else covered_lines / total_lines * 100
    print(f"Flutter coverage total: {coverage:.2f}% (umbral {args.threshold:.2f}%)")

    if coverage + 1e-9 < args.threshold:
        print(f"ERROR: Cobertura insuficiente ({coverage:.2f}% < {args.threshold:.2f}%)", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
