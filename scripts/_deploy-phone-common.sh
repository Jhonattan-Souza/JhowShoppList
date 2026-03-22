#!/usr/bin/env bash

set -euo pipefail

deploy_phone() {
  : "${ROOT_DIR:?}"
  : "${APK_PATH:?}"
  : "${PACKAGE_NAME:?}"
  : "${ACTIVITY_NAME:?}"
  : "${BUILD_TASK:?}"
  : "${VARIANT_NAME:?}"
  : "${SCRIPT_DISPLAY_NAME:?}"

  usage() {
    cat <<EOF
Usage: ${SCRIPT_DISPLAY_NAME} [--device SERIAL] [--launch]

Builds the ${VARIANT_NAME} APK and installs it as an in-place update on a connected phone.
This script only uses adb install -r, so it will never uninstall the existing app as a fallback.

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

  INSTALLED_PACKAGE_PATH="$(adb -s "$DEVICE_SERIAL" shell pm path "$PACKAGE_NAME" 2>/dev/null | tr -d '\r')"

  printf 'Preparing %s deploy\n' "$VARIANT_NAME"
  printf '  device: %s\n' "$DEVICE_SERIAL"
  printf '  package: %s\n' "$PACKAGE_NAME"
  printf '  gradle task: %s\n' "$BUILD_TASK"
  printf '  apk path: %s\n' "$APK_PATH"

  if [[ -n "$INSTALLED_PACKAGE_PATH" ]]; then
    printf 'Installed package detected. The deploy will update it in place and keep app data if the signing key matches.\n'
  else
    printf 'Package not currently installed on the device. The deploy will perform a first-time install.\n'
  fi

  printf 'Building %s APK...\n' "$VARIANT_NAME"
  "$ROOT_DIR/gradlew" "$BUILD_TASK"

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

  printf 'Deploy complete. %s was installed without any uninstall step.\n' "$PACKAGE_NAME"
}
