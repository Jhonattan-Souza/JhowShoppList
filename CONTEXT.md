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
