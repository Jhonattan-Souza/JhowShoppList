#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPERTIES_FILE="$ROOT_DIR/keystore.properties"

usage() {
  cat <<'EOF'
Usage: scripts/backup-release-keystore.sh DESTINATION_DIR

Copies the local release keystore and keystore.properties into a timestamped backup directory.
Store the backup in a secure location because it contains signing secrets.
EOF
}

if [[ $# -eq 1 && "$1" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

DESTINATION_ROOT="$1"

if [[ ! -f "$PROPERTIES_FILE" ]]; then
  printf 'Missing %s. Configure release signing first.\n' "$PROPERTIES_FILE" >&2
  exit 1
fi

release_store_file="$(grep -E '^[[:space:]]*releaseStoreFile[[:space:]]*=' "$PROPERTIES_FILE" | tail -n 1 | cut -d '=' -f 2-)"

if [[ -z "$release_store_file" ]]; then
  printf 'releaseStoreFile is missing from %s\n' "$PROPERTIES_FILE" >&2
  exit 1
fi

if [[ "$release_store_file" != /* ]]; then
  release_store_file="$ROOT_DIR/$release_store_file"
fi

if [[ ! -f "$release_store_file" ]]; then
  printf 'Release keystore not found at %s\n' "$release_store_file" >&2
  exit 1
fi

mkdir -p "$DESTINATION_ROOT"

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_dir="$DESTINATION_ROOT/jhow-shopplist-release-$timestamp"

mkdir -p "$backup_dir"
cp "$PROPERTIES_FILE" "$backup_dir/keystore.properties"
cp "$release_store_file" "$backup_dir/$(basename "$release_store_file")"
chmod 600 "$backup_dir/keystore.properties" "$backup_dir/$(basename "$release_store_file")"

printf 'Release signing backup created at %s\n' "$backup_dir"
printf 'Keep this directory secure. Losing it can block future in-place release updates.\n'
