# One-tap purchase as the primary action; long-press for multi-select

Tapping a pending row marks it purchased immediately. Long-pressing a row enters multi-select mode where additional taps add or remove rows from the selection; an action triggered from there (purchase or delete) applies to the whole selection. There is no primary FAB for "purchase selected" — multi-select surfaces its own contextual action affordance only while the selection is non-empty.

## Why

A future reader will see two things that look like missing features and aren't:

1. **No "purchase" FAB.** Earlier the screen had a bulk-purchase FAB that fade-appeared whenever a pending row was selected (`ShoppingListScreen.kt:498–524` in the pre-change code). It's gone on purpose. The dominant flow during a real shopping run is one item at a time — see thing → cart → mark purchased — and routing that through "tap to select, then reach for the FAB" added a step to every interaction to optimize for the rare case. Removing the FAB makes the dominant flow a single tap and confines batch behavior to an explicit mode entered with intent (long-press).

2. **Tap is destructive-ish without a confirm dialog.** A mistapped row jumps into Purchased with no modal. Recovery is via the Purchased section itself (tap a purchased row to restore) — the section doubles as a visible undo log, which matches how the user already uses it (looking at Purchased only when recalling what's been bought). For *delete* (a swipe action), an undo snackbar handles the same job because deletion removes the row from both lists; for *purchase* the section split is the recovery affordance and a snackbar would just spam during a run of taps.

## Considered alternatives

- **Keep tap-to-select + FAB-to-purchase (the previous model).** Rejected: optimizes for the rare batch case at the cost of a friction tax on every single-item action. Animated FAB shape-morph polish was explicitly considered as a way to keep the FAB feeling premium and rejected for the same reason — better polish on an unneeded control is still an unneeded control.
- **Swipe right = purchase, swipe left = delete.** Rejected: overloads the same gesture surface with two destructive actions, doubles the chance of misfires, and complicates the swipe-to-dismiss/undo affordance that already covers delete.
- **Two-step tap (tap once = highlight, tap again = purchase).** Rejected: indistinguishable from "lag" to the user and creates a phantom mode between selected-and-not.

## Consequences

- **Long-press is now a load-bearing gesture.** It is the *only* path into multi-select. It must therefore have its own haptic on entry (heavier than the per-tap purchase haptic, so the mode change feels like a different event) and its own visible state on the row to confirm the mode change registered. Without those, multi-select is undiscoverable.
- **The Purchased section's "tap to restore" affordance is now the recovery UI for accidental purchases.** It must remain prominent even when auto-collapsed: the collapsed header has to be tappable, the count visible, and tap-to-restore must keep working on the same row interaction model as pending. A future change that makes restore harder (e.g., "swipe to restore" instead of tap) breaks the implicit undo contract this decision relies on.
- **The "selected" container color must be unmistakable on its own.** With the FAB gone there is no separate visual anchor announcing "you're in multi-select mode" — the row container color and a contextual action affordance are the only signals. This compounds with ADR-0001's earlier observation that selection state can no longer lean on a check icon.
- **Frequency-based sort becomes more important, not less.** With one-tap purchase, the items the user touches most often need to sit at the top of the pending list so they're immediately reachable; manual drag-to-reorder was rejected partly on this basis (Q11 of the design session) — the auto-sort *is* the ergonomic argument.
