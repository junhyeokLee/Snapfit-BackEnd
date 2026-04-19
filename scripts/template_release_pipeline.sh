#!/usr/bin/env bash
set -euo pipefail

# Backend workspace wrapper for template release pipeline.
# Actual implementation lives in frontend workspace:
#   /Users/devsheep/SnapFit/SnapFit/scripts/template_release_pipeline.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FRONT_DIR="${ROOT_DIR}/SnapFit"
FRONT_SCRIPT="${FRONT_DIR}/scripts/template_release_pipeline.sh"

if [[ ! -f "${FRONT_SCRIPT}" ]]; then
  echo "template release script not found: ${FRONT_SCRIPT}" >&2
  exit 1
fi

if [[ ! -x "${FRONT_SCRIPT}" ]]; then
  chmod +x "${FRONT_SCRIPT}"
fi

cd "${FRONT_DIR}"
exec "${FRONT_SCRIPT}" "$@"
