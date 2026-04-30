#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

"${SCRIPT_DIR}/ingest_off.py"

python3 - <<'PY' "${SCRIPT_DIR}" "${REPO_ROOT}"
import json
import sys
from pathlib import Path

script_dir = Path(sys.argv[1])
repo_root = Path(sys.argv[2])


def load_json(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8") as handle:
        raw = handle.read()
    stripped = "\n".join(
        line for line in raw.splitlines()
        if not line.lstrip().startswith("//")
    )
    return json.loads(stripped)


def write_json(path: Path, payload: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2, sort_keys=True)
        handle.write("\n")


for language in ("pt", "en"):
    base_path = script_dir / ".cache" / f"{language}.base.json"
    overlay_path = script_dir / "overlays" / f"{language}.overlay.json"
    output_path = repo_root / "app" / "src" / "main" / "assets" / "icons" / f"dictionary-{language}.json"

    merged = load_json(base_path) | load_json(overlay_path)
    write_json(output_path, merged)
    print(f"{language}: wrote {len(merged)} terms to {output_path.relative_to(repo_root)}")
PY
