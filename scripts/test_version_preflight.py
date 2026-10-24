#!/usr/bin/env python3
"""Prove the version preflight rejects stale release/installer metadata."""
from pathlib import Path
import shutil, subprocess, tempfile

ROOT=Path(__file__).resolve().parent.parent
version_data=dict(line.split('=',1) for line in (ROOT/'VERSION.txt').read_text().splitlines() if '=' in line)
VERSION=version_data['versionName']
CODE=version_data['versionCode']
INSTALLER=f'FluxReader-{VERSION}-build-install.command'
files=[
    'VERSION.txt','app/build.gradle.kts','app/src/main/assets/build-info.json',
    'scripts/verify-version.command','scripts/build-release.command',
    'scripts/build-debug.command','scripts/install-release.command',
    'scripts/install-debug.command','scripts/package-release.command',
    INSTALLER,
]
with tempfile.TemporaryDirectory(prefix='flux-version-test-') as tmp:
    temp=Path(tmp)
    for rel in files:
        target=temp/rel
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(ROOT/rel,target)
    def verify(should_pass:bool):
        result=subprocess.run(['bash','scripts/verify-version.command'],cwd=temp,capture_output=True,text=True)
        assert (result.returncode==0)==should_pass,(result.returncode,result.stdout,result.stderr)
    verify(True)
    cases=[
        ('app/build.gradle.kts', VERSION, '0.2.0-alpha.79'),
        ('app/build.gradle.kts',f'versionCode = {CODE}','versionCode = 20097'),
        ('app/src/main/assets/build-info.json',f'"versionCode": {CODE}','"versionCode": 20097'),
        ('scripts/build-release.command', 'VERSION="$(sed -n', 'VERSION="0.2.0-alpha.79" #'),
        (INSTALLER,'APK="$PWD/dist/FluxReader-$VERSION-release.apk"','APK="$PWD/dist/FluxReader-0.2.0-alpha.79-release.apk"'),
    ]
    for rel, correct, broken in cases:
        target=temp/rel
        content=target.read_text()
        assert correct in content,(rel,correct)
        target.write_text(content.replace(correct,broken,1))
        verify(False)
        target.write_text(content)
        verify(True)
    def git(*args):
        return subprocess.run(['git', *args], cwd=temp, check=True, capture_output=True, text=True)
    git('init', '-q')
    git('add', 'VERSION.txt')
    git('-c', 'user.name=Version Test', '-c', 'user.email=version-test@example.invalid',
        '-c', 'commit.gpgsign=false', 'commit', '-qm', 'Release metadata fixture')
    # Matching metadata must still be rejected when a newer release already exists.
    alpha = int(VERSION.rsplit('.', 1)[1])
    future_tag = f'v0.2.0-alpha.{alpha + 1}'
    git('tag', future_tag)
    verify(False)
    git('tag', '-d', future_tag)
    # A new version name without an increased Android code is also invalid.
    previous_tag = f'v0.2.0-alpha.{alpha - 1}'
    git('tag', previous_tag)
    verify(False)
    git('tag', '-d', previous_tag)
    git('tag', f'v{VERSION}')
    verify(True)
print('✅ Version preflight: metadata mismatches, stale versions and reused Android codes rejected')
