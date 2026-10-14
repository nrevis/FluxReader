# Flux Reader 0.2.0-alpha.10

Library/Home 2.0 and Reader Themes 2.0.

- Rebuilt the main Library around covers, Continue Reading, search, list/grid modes and persistent sorting.
- Added sorting by recent, title, author, series, date added and reading progress.
- Added custom book groups plus shelf grouping by custom group, series or author.
- Added first-class series + series-index metadata.
- Added metadata editor for title, author, series, index, groups, publisher, year, language and description.
- Multi-book import now accepts several documents at once.
- Smart Import extracts EPUB/FB2 metadata, series information and embedded covers, with filename fallbacks.
- SHA-256 duplicate detection prevents importing the same book twice.
- EPUB2/EPUB3/FB2 embedded covers are stored locally; users can choose or remove custom covers.
- Cover previews use sampled bitmap decoding; custom covers are validated for file size and dimensions.
- Added page-turn effects: None, Slide, Fade and Zoom with adjustable duration.
- Added OLED, Dark, Paper, Sepia, Nord, Dracula, Solarized Dark, Catppuccin and Gruvbox themes.
- Added a named custom theme with exact RGB controls for background, surface, text and accent.
- Expanded Speed Reader appearance with OLED/Paper/Cyber/Nord/Dracula/Sepia presets and an optional adjustable focus band.
- New library/theme/animation/speed appearance preferences persist across app restarts.
