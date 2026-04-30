# Item icon as derived presentation, not stored data or control

The grocery icon shown next to each shopping item is **derived from the item name at render time** and **replaces** the per-row purchase-state icon (radio button / check / history). Two linked decisions, one philosophy: the icon is *visual feedback on data*, never *data* itself and never *a tappable control*.

## Why

A future reader will see two things that look like bugs and aren't:

1. **No `iconBucket` column on `ShoppingItem`, no entry in the CalDAV format, no Room migration for icons.** This is deliberate. Caching the bucket on the entity would buy negligible render-time speedup (HashMap lookup is sub-microsecond, memoizable) at the cost of a schema migration, a sync-format pollution question, a "stale icon after dictionary update" class of bugs, and a re-resolve pass when items arrive via CalDAV import. Treating the icon as derived state — like a syntax-highlighting color — keeps the data model and CalDAV payload identical to today and lets every dictionary improvement update existing items for free.

2. **The radio-button / check / history state icon is gone, replaced by the grocery icon; the whole row is the tap target.** State (pending / selected / purchased) is already carried redundantly by the section split, container color animation, text decoration, and row alpha — the state icon was a fourth or fifth signal. The grocery icon adds *identity* information (what the thing is) which the state icon never carried, while the row-click affordance covers what the state icon used to invite. Bring! and AnyList work the same way.

## Considered alternatives

- **Cache the icon bucket on the entity.** Rejected: zero render-time win after memoization, real schema and sync cost.
- **Two leading icons side-by-side (`[state] [grocery] text`).** Rejected: visually heavier, doubles iconography, doesn't pay for itself once you accept that section + color + alpha already convey state.
- **Grocery icon on the trailing edge.** Rejected: trailing slot conventionally signals "action" (delete, more); putting a descriptor there invites mis-taps and reserves the slot from future actions.
- **No icon when matcher misses.** Rejected: a jagged left edge is uglier than a row of consistent icons with one being a faint generic basket. Generic is always rendered.

## Consequences

- **Per-item user overrides become a real change** if we ever ship them — they re-introduce stored state and break the "icon is purely a function of name" invariant. Deferring overrides was a deliberate part of this decision.
- **The selected-row container color must be visually unmistakable on its own**, since the `Check` icon is no longer there to reinforce batch-selection state.
- **The matcher must be fast and side-effect-free.** Anything heavier than a HashMap lookup against a normalized term breaks the "compute on every render" assumption. This is why the matcher is shallow by design (no stemming, no fuzzy, no ML).
