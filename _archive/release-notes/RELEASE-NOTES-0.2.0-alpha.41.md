# Flux Reader 0.2.0-alpha.41

UI/navigation consistency pass.

## Library

- Book cards no longer carry a separate Speed Reader icon in every Grid/Compact/List item.
- Tap opens the book; long press and `⋮` open the same complete actions menu.
- Book actions have one stable order: Open → Speed Reading → Edit → Copy/Move/Duplicate/Export → Delete.
- Folder actions follow the same structure: Open → Create/Rename → Copy/Move → Delete.
- The Continue card now supports the same long-press actions.
- Delete is visually marked as a destructive action.

## Back navigation

- Removed duplicate `speedPause()` from the Speed Reader top-bar Back action.
- `AppBackDestinationPolicy` now defines route destinations in one place: Reader/PDF return to Library, Speed returns to the route it was opened from, Settings returns to the previous route.
- Added `AppBackDestinationPolicyTest`.

## Cleanup

- Removed a literal duplicated `supporting.isNotBlank()` branch from the shared library action row.

## Packaging

- Clean ZIPs now always contain one `FluxReader-<version>-clean/` root directory, matching the install helper and README commands.
