#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")"
./scripts/install-release.command "$@"
