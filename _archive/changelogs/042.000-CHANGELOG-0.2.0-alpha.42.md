# Flux Reader 0.2.0-alpha.42

## Modal navigation consistency

- Reader TOC, book search and reader settings now have an explicit close action in a shared sheet header.
- Reader advanced settings treat system Back as navigation to the parent settings page instead of dismissing the entire sheet.
- Speed Reader nested settings use the same rule: Back returns to the settings hub, while Close dismisses settings completely.
- Speed Reader settings hub and all nested pages use the shared modal header.
- Search keeps a single dismissal path, so close/scrim/back all clear the active search through the caller.

## Destructive dialogs

- Single-book, folder and batch deletion now use one shared destructive confirmation component.
- Confirm actions use the Material error color consistently; Cancel stays non-destructive.

## Cleanup

- Removed the duplicate Speed Reader sheet-header implementation.
- Added alpha.42 regression guards for nested Back behavior, shared modal headers and destructive confirmations.
