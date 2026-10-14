#!/usr/bin/env python3
"""Detect missing imports of explicitly referenced Compose typography symbols.

An intentionally conservative source check: not a replacement for Kotlin compilation.
"""
import argparse
import re
from pathlib import Path

SYMBOLS = {
    "TextAlign": "androidx.compose.ui.text.style.TextAlign",
    "TextOverflow": "androidx.compose.ui.text.style.TextOverflow",
    "TextDecoration": "androidx.compose.ui.text.style.TextDecoration",
    "FontWeight": "androidx.compose.ui.text.font.FontWeight",
    "FontStyle": "androidx.compose.ui.text.font.FontStyle",
    "KeyboardType": "androidx.compose.ui.text.input.KeyboardType",
    "KeyboardCapitalization": "androidx.compose.ui.text.input.KeyboardCapitalization",
    "ImeAction": "androidx.compose.ui.text.input.ImeAction",
    "ContentScale": "androidx.compose.ui.layout.ContentScale",
}

def check(root: Path) -> list[str]:
    problems = []
    for path in sorted(root.rglob("*.kt")):
        source = path.read_text(encoding="utf-8")
        imports = set(re.findall(r"(?m)^\s*import\s+([\w.]+)(?:\s+as\s+\w+)?\s*$", source))
        body = re.sub(r"(?m)^\s*import\s+[^\n]+$", "", source)
        # Strip comments before token matching; comments are not symbol usages.
        body = re.sub(r"(?m)//[^\n]*$", "", body)
        body = re.sub(r"/\*[\s\S]*?\*/", "", body)
        for symbol, fqname in SYMBOLS.items():
            if not re.search(r"(?<![\w.])" + symbol + r"\s*\.", body):
                continue
            if fqname in imports or fqname in body:
                continue
            # Do not treat a locally-declared type as a missing import.
            if re.search(r"\b(?:class|object|enum\s+class|typealias)\s+" + symbol + r"\b", body):
                continue
            # Wildcard imports are legal in Kotlin; prefer not to flag them.
            if fqname.rsplit(".", 1)[0] + ".*" in imports:
                continue
            loc = next(i for i, line in enumerate(source.splitlines(),1)
                       if re.search(r"(?<![\w.])" + symbol + r"\s*\.", line)
                       and not line.lstrip().startswith("import "))
            problems.append(f"{path}:{loc}: {symbol} requires 'import {fqname}'")
    return problems

if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument('root', nargs='?', default='app/src/main/java')
    args=parser.parse_args()
    failures=check(Path(args.root))
    if failures:
        print("❌ Missing Compose symbol imports:\n" + "\n".join(failures))
        raise SystemExit(1)
    print('✅ Explicit Compose symbol imports OK')
