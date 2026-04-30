# JhowShoppList — Domain Context

## Glossary

### Manual sync
A sync gesture explicitly initiated by the user — currently, tapping the sync badge in the top app bar. Manual syncs surface a prominent, centered progress indicator on the shopping list screen for the duration of the in-flight sync (and any sync work that overlaps with it).

### Background sync
A sync triggered as a side effect of a local mutation (add, purchase, unpurchase, delete) or by a scheduler/lifecycle event (app resume, network reconnect, periodic tick). Background syncs surface only a small spinner in the top app bar; they never take over the screen.

The distinction between manual and background syncs is a presentation concern. The sync engine itself does not behave differently based on who asked.

### Offline-first
The shopping list is the source of truth on the device. All mutations apply locally and immediately, regardless of network or sync state. Sync reconciles with the remote CalDAV list in the background; UI never blocks on sync, and a failed/missing sync never prevents the user from using the list.

### Sync configured
The user has supplied valid CalDAV credentials and selected (or created) a target list. Until sync is configured, the sync badge is hidden — the settings cog is the only path to configuration.

### Item icon
A small visual marker shown next to a shopping item to help the user scan the list. The icon is **decorative only**: it has no functional role (no grouping, no sorting, no filtering, no stats). A wrong icon is mildly ugly; it never blocks the user. When the system has low confidence, it shows a neutral generic icon rather than guessing — silence is better than a wrong guess.

The icon coverage is **grocery-first**: food, drinks, cleaning products, personal care, pet, and baby items get specific icons; anything outside that scope (hardware, electronics, clothing, etc.) falls through to the generic icon. Non-grocery is not a bug — it's the designed boundary.

The icon is **derived state, never stored**: it is computed from the item name at render time, like a syntax-highlighting color. It is not part of the ShoppingItem model, not in the database schema, and not synced through CalDAV. Improving the dictionary later improves every item retroactively, with no migration or re-resolve pass.

Day-one languages for icon resolution are **PT-BR and EN**. Both are loaded together regardless of system locale, because users routinely mix English brand and product names ("shampoo Pantene", "whey", "ração Whiskas") into Portuguese lists. Additional languages are pure data additions — the matcher does not know specific languages, only language packs.

The icon system uses **~25 buckets** (medium granularity). Buckets are stable, language-independent identifiers (`fruit`, `vegetable`, `dairy`, `meat`, `fish`, `bread`, `grain`, `beverages-cold`, `beverages-hot`, `alcohol`, `frozen`, `snacks`, `sweets`, `condiments`, `oils`, `pantry-canned`, `cleaning`, `personal-care`, `pet`, `baby`, etc., plus `generic`). The icon for each bucket is decided once in code; the dictionary maps terms to buckets, never directly to icons. Adding or swapping an icon library is a one-file change.

The matcher is **shallow by design**: normalize → exact match → alias match → token-head match → generic. No stemming, no fuzzy/Levenshtein, no embeddings, no ML. Plurals and inflections are absorbed by the aliases table, not by runtime morphology. Quantities and units (`2kg`, `500ml`, `pacote`, `2x`) are stripped during normalization so token-head match works on "arroz", not "2kg arroz". This guarantees lookups are pure HashMap reads, fully memoizable, and never produce a "creative" wrong icon — either the term is known, or the generic icon is shown.

**No per-item user overrides in v1.** The icon is a pure function of the item name; if it's wrong, it's wrong everywhere that name appears, and it falls to the generic icon if confidence is too low. Overrides are deferred until real usage shows the matcher's coverage is genuinely insufficient — adding them later is a purely additive change (a single nullable column or side table); removing them after shipping would be breaking.

The dictionary lives as **JSON files in `assets/`**, loaded once into an in-memory `HashMap<String, Bucket>` on first list render (off the main thread). The icon system never touches Room — the dictionary is static data, not persisted state. Updates to the dictionary ship in the APK; there is no first-launch seed path, no DAO, no migration story for icon data.

The dictionary is built **hybrid**: a build-time script ingests Open Food Facts taxonomy as the broad base (filtered through a hand-written OFF-category-to-bucket map), then a per-language overlay file applies hand-curated corrections and PT-BR-specific additions on top. Re-ingesting OFF is a one-command refresh that never overwrites the overlay. The runtime only sees the final merged JSON.

The item icon **replaces** the per-row state icon (no separate purchase-state glyph). State information — pending vs purchased, selected for batch action vs not — is carried by the section split, container color, text decoration, and row alpha; the icon is no longer load-bearing for state. Tap affordance for marking purchased is the whole-row click, not the icon. The generic icon is **always rendered** when the matcher misses, so the list's left edge stays aligned.
