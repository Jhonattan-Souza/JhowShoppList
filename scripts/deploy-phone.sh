#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.jhow.shopplist.debug"
ACTIVITY_NAME="com.jhow.shopplist.MainActivity"

usage() {
  cat <<'EOF'
Usage: scripts/deploy-phone.sh [--device SERIAL] [--launch]

Builds the debug APK and installs it as a normal update on a connected phone.
This keeps the debug app's existing database and app data intact.

Options:
  --device SERIAL  Target a specific adb device serial.
  --launch         Launch the app after installation.
  --help           Show this help message.
EOF
}

DEVICE_SERIAL="${ANDROID_SERIAL:-}"
LAUNCH_APP=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      if [[ $# -lt 2 ]]; then
        printf 'Missing value for --device\n' >&2
        exit 1
      fi
      DEVICE_SERIAL="$2"
      shift 2
      ;;
    --launch)
      LAUNCH_APP=true
      shift
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n' "$1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

mapfile -t DEVICES < <(adb devices | tail -n +2 | awk '$2 == "device" { print $1 }')

if [[ -z "$DEVICE_SERIAL" ]]; then
  if [[ ${#DEVICES[@]} -eq 0 ]]; then
    printf 'No adb device detected. Connect your phone and enable USB debugging.\n' >&2
    exit 1
  fi

  if [[ ${#DEVICES[@]} -gt 1 ]]; then
    printf 'Multiple adb devices detected. Re-run with --device SERIAL.\n' >&2
    printf 'Available devices:\n' >&2
    printf '  %s\n' "${DEVICES[@]}" >&2
    exit 1
  fi

  DEVICE_SERIAL="${DEVICES[0]}"
fi

if ! adb -s "$DEVICE_SERIAL" get-state >/dev/null 2>&1; then
  printf 'Device %s is not available to adb.\n' "$DEVICE_SERIAL" >&2
  exit 1
fi

printf 'Building debug APK...\n'
"$ROOT_DIR/gradlew" assembleDebug

if [[ ! -f "$APK_PATH" ]]; then
  printf 'Expected APK not found at %s\n' "$APK_PATH" >&2
  exit 1
fi

printf 'Installing %s on %s as an in-place update...\n' "$PACKAGE_NAME" "$DEVICE_SERIAL"
adb -s "$DEVICE_SERIAL" install -r "$APK_PATH"

if [[ "$LAUNCH_APP" == true ]]; then
  printf 'Launching app...\n'
  adb -s "$DEVICE_SERIAL" shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" >/dev/null
fi

printf 'Deploy complete. %s was updated without clearing app data.\n' "$PACKAGE_NAME"
