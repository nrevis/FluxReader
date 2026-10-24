#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PYVERIFY'
from pathlib import Path
import json, re, shutil, subprocess, sys
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
    # A clean source ZIP has no Git history. In a checkout, reject stale versions
    # even when VERSION.txt, Gradle and build-info all agree with each other.
    checkout = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True) if shutil.which('git') else None
    if checkout and checkout.returncode == 0 and Path(checkout.stdout.strip()).resolve() == root.resolve():
        tags = subprocess.check_output(['git', 'tag', '--list', 'v0.2.0-alpha.*'], text=True).splitlines()
        releases = [(int(tag.rsplit('.', 1)[1]), tag) for tag in tags
                    if re.fullmatch(r'v0\.2\.0-alpha\.\d+', tag)]
        if releases:
            latest_alpha, latest_tag = max(releases)
            current_alpha = int(name.rsplit('.', 1)[1])
            assert current_alpha >= latest_alpha, f'{name} is older than existing release {latest_tag}'
            prior = subprocess.check_output(['git', 'show', f'{latest_tag}:VERSION.txt'], text=True)
            prior_settings = dict(line.split('=', 1) for line in prior.splitlines() if '=' in line)
            prior_code = int(prior_settings['versionCode'])
            if current_alpha > latest_alpha:
                assert code > prior_code, f'New release versionCode must exceed {prior_code} ({latest_tag})'
            else:
                assert code == prior_code, f'Existing release {latest_tag} must retain versionCode {prior_code}'
    print(f'✅ Version consistency: {name}, code={code}; Gradle, assets, build/package/install agree')
except (AssertionError, KeyError, ValueError, FileNotFoundError, json.JSONDecodeError) as e:
    print(f'❌ VERSION MISMATCH: {e}',file=sys.stderr)
    sys.exit(1)
PYVERIFY
