# Flux Reader 0.2.0-alpha.43

## Library forms / IME / dialog semantics

- Added consistent `Save / Cancel / Close` semantics to library dialogs.
- Folder create/rename auto-focuses the name; rename selects the current name and IME Done submits only valid values.
- Metadata editor now uses single-line fields and IME Next flow, validates a non-blank title, hides the keyboard before form actions, and styles delete as destructive.
- Library search and in-book search use IME Search; in-book search focuses immediately.
- Custom palette name uses IME Done.
- Added pure `LibraryFormPolicy` plus unit tests.
