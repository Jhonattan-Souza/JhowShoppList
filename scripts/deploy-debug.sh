#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.jhow.shopplist.debug"
ACTIVITY_NAME="com.jhow.shopplist.MainActivity"
BUILD_TASK="assembleDebug"
VARIANT_NAME="debug"
SCRIPT_DISPLAY_NAME="scripts/deploy-debug.sh"

source "$ROOT_DIR/scripts/_deploy-phone-common.sh"

deploy_phone "$@"
