#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PYVERIFY'
from pathlib import Path
import json, re, sys
root=Path('.')
try:
    settings=dict(line.strip().split('=',1) for line in (root/'VERSION.txt').read_text().splitlines() if '=' in line)
    name=settings['versionName']
    code=int(settings['versionCode'])
    assert re.fullmatch(r'0\.2\.0-alpha\.[0-9]+',name),f'Invalid versionName {name!r}'
    gradle=(root/'app/build.gradle.kts').read_text()
    gradle_name=re.search(r'^\s*versionName\s*=\s*"([^"]+)"',gradle,re.M)
    gradle_code=re.search(r'^\s*versionCode\s*=\s*(\d+)',gradle,re.M)
    assert gradle_name and gradle_name.group(1)==name, 'Gradle versionName disagrees with VERSION.txt'
    assert gradle_code and int(gradle_code.group(1))==code, 'Gradle versionCode disagrees with VERSION.txt'
    info=json.loads((root/'app/src/main/assets/build-info.json').read_text())
    assert info['versionName']==name, 'build-info versionName disagrees with VERSION.txt'
    assert info['versionCode']==code, 'build-info versionCode disagrees with VERSION.txt'
    for script in ('build-release','build-debug','install-release','install-debug','package-release'):
        content=(root/f'scripts/{script}.command').read_text()
        assert 'VERSION="$(sed -n \'s/^versionName=//p\' VERSION.txt)"' in content, f'{script}: must use VERSION.txt'
        assert 'bash scripts/verify-version.command' in content, f'{script}: missing preflight'
    installer=root/f'FluxReader-{name}-build-install.command'
    assert installer.is_file(),f'Expected {installer}'
    install_text=installer.read_text()
    assert 'APK="$PWD/dist/FluxReader-$VERSION-release.apk"' in install_text, 'Installer must use VERSION.txt for APK name'
    assert 'bash scripts/verify-version.command' in install_text, 'Installer missing preflight'
    print(f'✅ Version consistency: {name}, code={code}; Gradle, assets, build/package/install agree')
except (AssertionError, KeyError, ValueError, FileNotFoundError, json.JSONDecodeError) as e:
    print(f'❌ VERSION MISMATCH: {e}',file=sys.stderr)
    sys.exit(1)
PYVERIFY
