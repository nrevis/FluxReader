#!/usr/bin/env python3
"""Static regression audit for alpha.93 GitHub updates; does NOT replace Android compilation."""
from pathlib import Path
import xml.etree.ElementTree as ET
r = Path(__file__).resolve().parent.parent
update = (r/'app/src/main/java/io/fluxreader/app/update/GitHubUpdateManager.kt').read_text()
policy = (r/'app/src/main/java/io/fluxreader/app/update/ReleaseVersion.kt').read_text()
settings = (r/'app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt').read_text()
ui = (r/'app/src/main/java/io/fluxreader/app/ui/GitHubUpdateControls.kt').read_text()
model = (r/'app/src/main/java/io/fluxreader/app/AppViewModel.kt').read_text()
manifest = (r/'app/src/main/AndroidManifest.xml').read_text()
checks = [
    ('repo pinned', 'nrevis/FluxReader' in update),
    ('http TLS', 'url.protocol != "https"' in update),
    ('no silent install', 'Intent(Intent.ACTION_VIEW)' in update),
    ('needs install permission', 'canRequestPackageInstalls' in update),
    ('file provider', 'FileProvider.getUriForFile' in update),
    ('checksum required', 'нет SHA-256' in update and 'sha256(partial)' in update),
    ('package validation', 'archive.packageName != app.packageName' in update),
    ('versionCode validation', 'archive.longVersionCode <= installed.longVersionCode' in update),
    ('signer verification', 'newSigners != oldSigners' in update),
    ('prereleases', 'includePreReleases' in update),
    ('daily auto check', 'AUTO_CHECK_INTERVAL_MS' in update),
    ('numeric version policy', 'compareValues(alpha, other.alpha)' in policy),
    ('compose in About', 'GitHubUpdateControls(viewModel)' in settings),
    ('download/install actions', 'downloadGitHubUpdate' in ui and 'installGitHubUpdate' in ui),
    ('state in ViewModel', 'val githubUpdates = updateManager.state' in model),
    ('internet permission', 'android.permission.INTERNET' in manifest),
    ('sideload permission', 'android.permission.REQUEST_INSTALL_PACKAGES' in manifest),
    ('FileProvider declaration', 'updates.fileprovider' in manifest),
    ('provider XML', (r/'app/src/main/res/xml/update_file_paths.xml').is_file()),
    ('publication script', (r/'scripts/publish-github-release.command').is_file()),
]
ET.parse(r/'app/src/main/AndroidManifest.xml')
ET.parse(r/'app/src/main/res/xml/update_file_paths.xml')
for label, good in checks:
    if not good:
        raise SystemExit('FAIL: '+label)
print(f'alpha.93 GitHub update static critic: {len(checks)}/{len(checks)}')
