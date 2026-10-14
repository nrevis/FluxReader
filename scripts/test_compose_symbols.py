#!/usr/bin/env python3
from pathlib import Path
from tempfile import TemporaryDirectory
from check_compose_symbols import check

with TemporaryDirectory() as tmp:
    kt = Path(tmp) / "Fixture.kt"
    kt.write_text("package example\nimport androidx.compose.ui.text.style.TextAlign\nfun x() = TextAlign.Center\n")
    assert not check(Path(tmp)), "valid TextAlign import was rejected"
    kt.write_text("package example\nfun x() = TextAlign.Center\n")
    errors = check(Path(tmp))
    assert len(errors) == 1 and "TextAlign" in errors[0], f"missed TextAlign regression: {errors}"
    kt.write_text("package example\nimport androidx.compose.ui.text.font.FontWeight\nfun x() = FontWeight.Bold\n")
    assert not check(Path(tmp)), "valid FontWeight import was rejected"
    kt.write_text("package example\nfun x() = FontWeight.Bold\n")
    assert len(check(Path(tmp))) == 1, "missed FontWeight regression"
    kt.write_text("package example\n// ContentScale.Fit inside comment only\nfun x() = 1\n")
    assert not check(Path(tmp)), "comment mistakenly treated as executable code"
print("✅ Compose symbol checker negative/positive tests passed")
