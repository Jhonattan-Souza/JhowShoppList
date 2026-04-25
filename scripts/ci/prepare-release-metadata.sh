#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/ci/prepare-release-metadata.sh validate-tag <tag> <gradle-properties>
  scripts/ci/prepare-release-metadata.sh verify-apk <apk-path>
  scripts/ci/prepare-release-metadata.sh build-verification <apk-path> <output-path>
EOF
}

read_property() {
  local key="$1"
  local file="$2"
  local value
  local match_count

  match_count="$(awk -F= -v key="$key" '
    {
      key_part = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", key_part)
      if (key_part == key) {
        count++
      }
    }
    END { print count + 0 }
  ' "$file")"

  if [[ "$match_count" -gt 1 ]]; then
    printf 'Property %s is defined multiple times in %s\n' "$key" "$file" >&2
    exit 1
  fi

  value="$(awk -F= -v key="$key" '
    {
      key_part = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", key_part)
      if (key_part == key) {
        value = substr($0, index($0, "=") + 1)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      }
    }
    END {
      if (value != "") {
        print value
      }
    }
  ' "$file" | tr -d '\r')"
  if [[ -z "$value" ]]; then
    printf 'Missing property %s in %s\n' "$key" "$file" >&2
    exit 1
  fi

  printf '%s' "$value"
}

validate_tag() {
  local tag="$1"
  local properties_file="$2"
  local version_name
  local expected_tag

  version_name="$(read_property "APP_VERSION_NAME" "$properties_file")"
  expected_tag="v${version_name}"

  if [[ "$tag" != "$expected_tag" ]]; then
    printf 'Tag %s does not match APP_VERSION_NAME %s\n' "$tag" "$version_name" >&2
    exit 1
  fi
}

find_apksigner() {
  if command -v apksigner >/dev/null 2>&1; then
    command -v apksigner
    return
  fi

  local sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  local newest=""
  local candidate

  if [[ -z "$sdk_root" ]]; then
    printf 'ANDROID_HOME or ANDROID_SDK_ROOT must be set\n' >&2
    exit 1
  fi

  shopt -s nullglob
  newest="$(printf '%s\n' "$sdk_root"/build-tools/*/apksigner | sort -V | tail -n 1)"
  shopt -u nullglob

  if [[ -z "$newest" ]]; then
    printf 'Unable to find apksigner under %s/build-tools\n' "$sdk_root" >&2
    exit 1
  fi

  printf '%s' "$newest"
}

verify_apk() {
  local apk_path="$1"
  local apksigner

  if [[ ! -f "$apk_path" ]]; then
    printf 'Missing APK at %s\n' "$apk_path" >&2
    exit 1
  fi

  apksigner="$(find_apksigner)"

  # Fail loudly if the APK is unsigned or the signature is invalid.
  "$apksigner" verify --verbose "$apk_path" >&2
}

build_verification() {
  local apk_path="$1"
  local output_path="$2"
  local apksigner
  local certs_output
  local digest
  local apk_sha256

  if [[ ! -f "$apk_path" ]]; then
    printf 'Missing APK at %s\n' "$apk_path" >&2
    exit 1
  fi

  apksigner="$(find_apksigner)"

  # Capture --print-certs output (apksigner may write it to stderr, so merge both streams).
  if ! certs_output="$("$apksigner" verify --print-certs "$apk_path" 2>&1)"; then
    printf 'apksigner verify failed for %s:\n%s\n' "$apk_path" "$certs_output" >&2
    exit 1
  fi
  digest="$(printf '%s\n' "$certs_output" | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)"

  if [[ -z "$digest" ]]; then
    printf 'Unable to extract SHA-256 digest from %s\n' "$apk_path" >&2
    printf 'apksigner output was:\n%s\n' "$certs_output" >&2
    exit 1
  fi

  apk_sha256="$(sha256sum "$apk_path" | awk '{ print $1 }')"

  cat > "$output_path" <<EOF
## Verification

- Package name: \`com.jhow.shopplist\`
- SHA-256 hash of signing certificate: \`$digest\`
- SHA-256 of \`$(basename "$apk_path")\`: \`$apk_sha256\` (also published as \`$(basename "$apk_path").sha256\`)
- Verify the signing certificate with \`apksigner verify --print-certs $(basename "$apk_path")\`
- Verify the APK bytes with \`sha256sum -c $(basename "$apk_path").sha256\`
- Obtainium source: \`https://github.com/Jhonattan-Souza/JhowShoppList\`
EOF
}

main() {
  if [[ $# -lt 1 ]]; then
    usage
    exit 1
  fi

  case "$1" in
    validate-tag)
      [[ $# -eq 3 ]] || {
        usage
        exit 1
      }
      validate_tag "$2" "$3"
      ;;
    verify-apk)
      [[ $# -eq 2 ]] || {
        usage
        exit 1
      }
      verify_apk "$2"
      ;;
    build-verification)
      [[ $# -eq 3 ]] || {
        usage
        exit 1
      }
      build_verification "$2" "$3"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
