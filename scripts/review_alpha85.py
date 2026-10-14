#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parent.parent
ui=root/'app/src/main/java/io/fluxreader/app/ui'
s=(ui/'SpeedReaderScreen.kt').read_text()
r=(ui/'ReaderPreferences.kt').read_text()
checks={
 'single shared 56dp toolbar':s.count('Shared 56dp layout for all palettes')==1,
 'standard/experimental same speed container': 'containerColor = background,' in s,
 'no heavyweight standalone standard top bar': 'TopAppBar(' not in s,
 'fullscreen Speed Reader shares viewport': s.count('SpeedViewport(')>=3,
 'settings headings use accessible foreground': all(('Text("'+name+'", style = MaterialTheme.typography.' in r) for name in ('Режим','Палитра','Экранные эффекты')),
 'no 3-wide theme palette': 'ReaderThemeMode.entries.chunked(3)' not in r,
 'two-column responsive palette': 'ReaderThemeMode.entries.chunked(2)' in r,
 'appearance names cannot wrap': 'Text(appearanceLabel(appearance), maxLines = 1, softWrap = false' in r,
 'device frame and CRT explicitly gated': 'settings.useWastelandDesign' in (ui/'FluxReaderApp.kt').read_text(),
 'performance capture available': (root/'scripts/profile-ui-shells.command').exists(),
}
for key, ok in checks.items(): print(('PASS ' if ok else 'FAIL ')+key)
if not all(checks.values()): raise SystemExit(1)
