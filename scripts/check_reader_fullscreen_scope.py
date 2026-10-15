#!/usr/bin/env python3
"""Reject the exact invalid BoxScope `weight` regression in the immersive reader.

This is a targeted source guard, not a replacement for real Gradle compilation.
"""
from pathlib import Path
import argparse
import re
import sys


def check(source: str) -> list[str]:
    errors = []
    calls = list(re.finditer(r'^\s+ReaderContent\(', source, re.M))
    if len(calls) != 1:
        return ['ReaderContent must have one composition slot across fullscreen changes']
    start = calls[0].start()
    end = source.find('ReaderCutoutStatus(', start)
    if end == -1:
        return ['Immersive ReaderCutoutStatus is missing']
    immersive = source[start:end]
    if 'modifier = pageAnimationModifier,' not in immersive:
        errors.append('Immersive ReaderContent must use pageAnimationModifier without Row/Column weight')
    if '.weight(' in immersive:
        errors.append('Modifier.weight is illegal in the fullscreen BoxScope')
    if 'isFullscreen = !chromeVisible,' not in immersive:
        errors.append('The shared reader must switch fullscreen by parameter')
    if 'Box(Modifier.fillMaxSize().then(fullscreenTransitionModifier))' not in source[:start][-600:]:
        errors.append('Fullscreen reader must have a full-size Box parent')
    if 'modifier\n            .fillMaxSize()' not in source:
        errors.append('ReaderContent itself must fill its measured viewport')
    if 'import androidx.compose.foundation.layout.weight' in source:
        errors.append('RowScope/ColumnScope weight must never be imported as a top-level symbol')
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    path = project/'app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt'
    source = path.read_text(encoding='utf-8')
    errors = check(source)
    if errors:
        print('FAIL: ' + '; '.join(errors))
        return 1
    if args.self_test:
        broken = source.replace('modifier = pageAnimationModifier,', 'modifier = pageAnimationModifier.weight(1f),', 1)
        assert any('weight' in e for e in check(broken)), 'checker failed to detect original bug'
        broken_import = source.replace('package io.fluxreader.app.ui', 'package io.fluxreader.app.ui\nimport androidx.compose.foundation.layout.weight')
        assert any('imported' in e or 'import' in e for e in check(broken_import)), 'checker failed illegal-import regression'
        split_slots = source.replace('ReaderContent(', 'ReaderContent(\n ReaderContent(', 1)
        assert any('composition slot' in e for e in check(split_slots)), 'checker failed split-slot regression'
        print('PASS: self-test detects old BoxScope.weight and illegal import')
    print('PASS: Immersive ReaderContent fills Box without BoxScope.weight')
    return 0


if __name__ == '__main__':
    sys.exit(main())
