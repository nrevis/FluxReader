#!/usr/bin/env python3
"""Flag missing explicit imports of ReaderSettings extension properties.

Conservative source check, not a substitute for the Android Kotlin compiler.
"""
from pathlib import Path
import argparse
import re

EXTENSION = re.compile(r"(?m)^\s*val\s+ReaderSettings\.(\w+)\s*:")
PACKAGE = re.compile(r"(?m)^\s*package\s+([\w.]+)")


def problems(project: Path, overrides: dict[Path, str] | None = None) -> list[str]:
    root = project / "app/src/main/java"
    models = root / "io/fluxreader/app/data/Models.kt"
    if not models.is_file():
        return [f"Missing model declaration file: {models}"]
    declarations = set(EXTENSION.findall(models.read_text(encoding="utf-8")))
    if not declarations:
        return [f"No ReaderSettings extension declarations found: {models}"]
    issues = []
    overrides = overrides or {}
    for path in sorted(root.rglob("*.kt")):
        source = overrides.get(path, None)
        if source is None:
            source = path.read_text(encoding="utf-8")
        package_match = PACKAGE.search(source)
        package = package_match.group(1) if package_match else ""
        if package == "io.fluxreader.app.data":
            continue
        imports = set(re.findall(r"(?m)^\s*import\s+([\w.*]+)\s*$", source))
        body = re.sub(r"(?m)^\s*import\s+[^\n]+$", "", source)
        body = re.sub(r"/\*[\s\S]*?\*/", "", body)
        body = re.sub(r"(?m)//[^\n]*$", "", body)
        body = re.sub(r'"""[\s\S]*?"""', '""', body)
        body = re.sub(r'"(?:\\.|[^"\\])*"', '""', body)
        for name in declarations:
            # Cross-package receiver-property use of a data extension.
            if not re.search(r"\b[a-zA-Z_]\w*\." + re.escape(name) + r"\b", body):
                continue
            fq = f"io.fluxreader.app.data.{name}"
            if fq in imports or "io.fluxreader.app.data.*" in imports:
                continue
            issues.append(f"{path.relative_to(project)}: usage .{name} requires import {fq}")
    return issues


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    errors = problems(project)
    if errors:
        print("\n".join("FAIL " + err for err in errors))
        raise SystemExit(1)
    if args.self_test:
        target = project / 'app/src/main/java/io/fluxreader/app/ui/ExperimentalColors.kt'
        correct = target.read_text(encoding='utf-8')
        broken = correct.replace('import io.fluxreader.app.data.activeUiTheme\n','')
        assert broken != correct, 'Expected explicit import in ExperimentalColors.kt'
        assert any('ExperimentalColors.kt' in e and 'activeUiTheme' in e
                   for e in problems(project, {target:broken}))
        print('PASS regression: missing activeUiTheme import is detected')
    print('PASS ReaderSettings extension import scan')

if __name__ == '__main__':
    main()
