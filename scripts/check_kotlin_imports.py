#!/usr/bin/env python3
"""Fail fast on duplicate/conflicting explicit Kotlin imports.

This catches failures such as alpha.74's duplicate material.icons.Icons lines before
Gradle's compileDebugKotlin and compileReleaseKotlin stages.
"""
import argparse
from pathlib import Path
import re
import sys

IMPORT = re.compile(r"^\s*import\s+([a-zA-Z_][\w.]*(?:\*)?)\s*(?:as\s+(\w+))?\s*$")
FORBIDDEN_SCOPE_ONLY = {
    "androidx.compose.foundation.layout.matchParentSize",
    "androidx.compose.foundation.layout.weight",
}


def check_tree(source_root: Path) -> list[str]:
    problems: list[str] = []
    for source in sorted(source_root.rglob("*.kt")):
        explicit: dict[str, tuple[str, int]] = {}
        all_imports: dict[str, int] = {}
        for line_no, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
            if not line.lstrip().startswith("import "):
                continue
            matched = IMPORT.fullmatch(line)
            if not matched:
                problems.append(f"{source}:{line_no}: malformed import: {line.strip()}")
                continue
            full_path, alias = matched.groups()
            if full_path in FORBIDDEN_SCOPE_ONLY:
                problems.append(f"{source}:{line_no}: Box/Row scope-only function cannot be imported: {full_path}")
            # Exact duplicate lines are never intentional, including aliased imports.
            signature = full_path + (" as " + alias if alias else "")
            if signature in all_imports:
                problems.append(f"{source}:{line_no}: duplicate import (previous line {all_imports[signature]}): {signature}")
            else:
                all_imports[signature] = line_no
            if full_path.endswith(".*"):
                continue  # wildcard namespace cannot be checked without symbol resolution
            identifier = alias if alias else full_path.rsplit(".", 1)[-1]
            previous = explicit.get(identifier)
            # Kotlin permits importing overloads with the same lowercase function name
            # from multiple packages, e.g. LazyColumn/LazyGrid `items`.
            if previous and previous[0] != full_path and identifier[:1].isupper():
                problems.append(f"{source}:{line_no}: conflicting import name '{identifier}' with line {previous[1]}: {previous[0]} vs {full_path}")
            else:
                explicit[identifier] = (full_path, line_no)
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", default="app/src/main/java")
    args = parser.parse_args()
    root = Path(args.root)
    if not root.is_dir():
        print(f"Missing Kotlin source tree: {root}", file=sys.stderr)
        return 2
    errors = check_tree(root)
    if errors:
        for err in errors: print("❌ " + err, file=sys.stderr)
        return 1
    count = sum(1 for _ in root.rglob("*.kt"))
    print(f"✅ Kotlin explicit imports OK ({count} files; no duplicate or conflicting imports)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
