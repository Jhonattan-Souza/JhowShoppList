#!/usr/bin/env python3

from __future__ import annotations

import json
import re
import unicodedata
import urllib.request
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
CACHE_DIR = SCRIPT_DIR / ".cache"
SOURCE_URL = "https://raw.githubusercontent.com/openfoodfacts/openfoodfacts-server/main/taxonomies/food/categories.txt"
SOURCE_CACHE_PATH = CACHE_DIR / "categories.txt"
MAPPING_PATH = SCRIPT_DIR / "off_to_bucket.json"
OUTPUT_LANGUAGES = ("pt", "en")
LANGUAGE_LINE_PATTERN = re.compile(r"^([a-z]{2}(?:_[a-z]{2})?):\s*(.+)$")
PARENT_LINE_PATTERN = re.compile(r"^<\s+(.+)$")
VALID_BUCKETS = {
    "fruit",
    "vegetable",
    "dairy",
    "egg",
    "cheese",
    "meat",
    "fish",
    "deli-cold-cuts",
    "bread",
    "grain",
    "pasta",
    "beverages-cold",
    "beverages-hot",
    "alcohol",
    "frozen",
    "snacks",
    "sweets",
    "condiments",
    "staples",
    "pantry",
    "cleaning",
    "personal-care",
    "pet",
    "baby",
}


@dataclass
class TaxonomyEntry:
    entry_id: str | None = None
    parents: set[str] = field(default_factory=set)
    names: dict[str, list[str]] = field(default_factory=dict)


def main() -> None:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    taxonomy_text = download_taxonomy()
    entries = parse_taxonomy(taxonomy_text)
    mapping = load_mapping()
    validate_mapping(mapping, entries)
    emit_base_dictionaries(entries, mapping)


def download_taxonomy() -> str:
    with urllib.request.urlopen(SOURCE_URL, timeout=60) as response:
        payload = response.read().decode("utf-8")

    SOURCE_CACHE_PATH.write_text(payload, encoding="utf-8")
    return payload


def load_mapping() -> dict[str, str]:
    mapping = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))
    invalid_buckets = sorted({bucket for bucket in mapping.values() if bucket not in VALID_BUCKETS})
    if invalid_buckets:
        raise SystemExit(f"Unknown bucket ids in {MAPPING_PATH.name}: {', '.join(invalid_buckets)}")
    return mapping


def parse_taxonomy(text: str) -> dict[str, TaxonomyEntry]:
    entries: dict[str, TaxonomyEntry] = {}
    current = TaxonomyEntry()

    def flush_current() -> None:
        nonlocal current
        if current.entry_id:
            merged = entries.setdefault(current.entry_id, TaxonomyEntry(entry_id=current.entry_id))
            merged.parents.update(current.parents)
            for language, terms in current.names.items():
                merged.names.setdefault(language, []).extend(terms)
        current = TaxonomyEntry()

    for raw_line in text.splitlines():
        line = raw_line.strip()

        if not line:
            flush_current()
            continue

        if line.startswith("#"):
            continue

        parent_match = PARENT_LINE_PATTERN.match(line)
        if parent_match:
            current.parents.add(canonicalize_reference(parent_match.group(1)))
            continue

        language_match = LANGUAGE_LINE_PATTERN.match(line)
        if language_match:
            language = language_match.group(1)
            names = split_terms(language_match.group(2))
            if not names:
                continue
            if current.entry_id is None:
                current.entry_id = canonicalize_identifier(language, names[0])
            current.names.setdefault(language, []).extend(names)
            continue

    flush_current()
    return entries


def emit_base_dictionaries(entries: dict[str, TaxonomyEntry], mapping: dict[str, str]) -> None:
    resolved_cache: dict[str, str | None] = {}
    base_dictionaries = {language: {} for language in OUTPUT_LANGUAGES}

    for entry_id, entry in entries.items():
        bucket = resolve_bucket(entry_id, entries, mapping, resolved_cache)
        if bucket is None:
            continue

        for language in OUTPUT_LANGUAGES:
            for term in entry.names.get(language, []):
                normalized_term = normalize_term(term)
                if normalized_term:
                    base_dictionaries[language][normalized_term] = bucket

    for language, payload in base_dictionaries.items():
        output_path = CACHE_DIR / f"{language}.base.json"
        output_path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8"
        )
        print(f"{language}: cached {len(payload)} OFF-derived terms at {output_path.relative_to(SCRIPT_DIR)}")


def resolve_bucket(
    entry_id: str,
    entries: dict[str, TaxonomyEntry],
    mapping: dict[str, str],
    cache: dict[str, str | None]
) -> str | None:
    if entry_id in cache:
        return cache[entry_id]

    if entry_id in mapping:
        cache[entry_id] = mapping[entry_id]
        return cache[entry_id]

    queue = deque(sorted(entries.get(entry_id, TaxonomyEntry()).parents))
    visited = {entry_id}

    while queue:
        current = queue.popleft()
        if current in visited:
            continue
        visited.add(current)

        if current in mapping:
            cache[entry_id] = mapping[current]
            return cache[entry_id]

        queue.extend(sorted(entries.get(current, TaxonomyEntry()).parents))

    cache[entry_id] = None
    return None


def validate_mapping(mapping: dict[str, str], entries: dict[str, TaxonomyEntry]) -> None:
    unknown_ids = sorted(entry_id for entry_id in mapping if entry_id not in entries)
    if unknown_ids:
        formatted = "\n".join(f"  - {entry_id}" for entry_id in unknown_ids)
        raise SystemExit(f"Unknown OFF category ids in {MAPPING_PATH.name}:\n{formatted}")


def normalize_term(term: str) -> str:
    collapsed = re.sub(r"\s+", " ", term.strip())
    return collapsed.lower()


def split_terms(raw_names: str) -> list[str]:
    return [part.strip() for part in raw_names.split(",") if part.strip()]


def canonicalize_reference(reference: str) -> str:
    language, _, name = reference.partition(":")
    if not language or not name:
        raise ValueError(f"Invalid taxonomy reference: {reference}")
    return canonicalize_identifier(language.strip(), name.strip())


def canonicalize_identifier(language: str, name: str) -> str:
    slug = slugify(name)
    if not slug:
        raise ValueError(f"Unable to slugify taxonomy name: {language}:{name}")
    return f"{language}:{slug}"


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value.strip().lower())
    without_diacritics = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    without_quotes = re.sub(r"[’'`]", "", without_diacritics)
    with_word_and = without_quotes.replace("&", " and ")
    slug = re.sub(r"[^a-z0-9]+", "-", with_word_and)
    slug = slug.strip("-")
    if slug:
        return slug

    unicode_slug = re.sub(r"[^\w]+", "-", value.strip().lower(), flags=re.UNICODE)
    return unicode_slug.strip("-")


if __name__ == "__main__":
    main()
