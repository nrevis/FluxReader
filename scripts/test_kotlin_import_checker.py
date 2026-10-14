#!/usr/bin/env python3
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys

checker = Path(__file__).with_name("check_kotlin_imports.py")


def scenario(name, lines, expect_ok):
    with TemporaryDirectory() as td:
        target = Path(td) / "Example.kt"
        target.write_text("package test\n" + "\n".join(lines) + "\n", encoding="utf8")
        completed = subprocess.run([sys.executable, str(checker), td], capture_output=True, text=True)
        ok = completed.returncode == 0
        assert ok == expect_ok, f"{name}: returncode={completed.returncode}\n{completed.stdout}\n{completed.stderr}"
        print(f"OK: {name}")

scenario("unique imports", ["import example.Foo", "import example.Bar"], True)
scenario("exact duplicate", ["import example.Foo", "import example.Foo"], False)
scenario("conflicting short identifier", ["import example.Foo", "import other.Foo"], False)
scenario("legal overloaded functions", ["import example.items", "import other.items"], True)
scenario("alias disambiguation", ["import example.Foo as EFoo", "import other.Foo"], True)
scenario("BoxScope-only import", ["import androidx.compose.foundation.layout.matchParentSize"], False)
scenario("RowScope-only import", ["import androidx.compose.foundation.layout.weight"], False)
