#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
PACKAGE_NAME="com.jhow.shopplist"
ACTIVITY_NAME="com.jhow.shopplist.MainActivity"
BUILD_TASK="assembleRelease"
VARIANT_NAME="release"
SCRIPT_DISPLAY_NAME="scripts/deploy-release.sh"

property_is_set() {
  local property_name="$1"
  local properties_file="$2"

  [[ -f "$properties_file" ]] && grep -Eq "^[[:space:]]*${property_name}[[:space:]]*=" "$properties_file"
}

ensure_release_signing_config() {
  local properties_file="$ROOT_DIR/keystore.properties"
  local -a missing_items=()
  local property_name
  local env_name

  for property_name in releaseStoreFile releaseStorePassword releaseKeyAlias releaseKeyPassword; do
    case "$property_name" in
      releaseStoreFile)
        env_name="JHOW_SHOPPLIST_RELEASE_STORE_FILE"
        ;;
      releaseStorePassword)
        env_name="JHOW_SHOPPLIST_RELEASE_STORE_PASSWORD"
        ;;
      releaseKeyAlias)
        env_name="JHOW_SHOPPLIST_RELEASE_KEY_ALIAS"
        ;;
      releaseKeyPassword)
        env_name="JHOW_SHOPPLIST_RELEASE_KEY_PASSWORD"
        ;;
    esac

    if [[ -n "${!env_name:-}" ]]; then
      continue
    fi

    if property_is_set "$property_name" "$properties_file"; then
      continue
    fi

    missing_items+=("$property_name or $env_name")
  done

  if [[ ${#missing_items[@]} -gt 0 ]]; then
    printf 'Release signing is not configured. Set the following values in keystore.properties or environment variables:\n' >&2
    printf '  %s\n' "${missing_items[@]}" >&2
    exit 1
  fi
}

for argument in "$@"; do
  if [[ "$argument" == "--help" ]]; then
    source "$ROOT_DIR/scripts/_deploy-phone-common.sh"
    deploy_phone "$@"
    exit 0
  fi
done

ensure_release_signing_config

source "$ROOT_DIR/scripts/_deploy-phone-common.sh"

deploy_phone "$@"
