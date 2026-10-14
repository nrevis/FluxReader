# Flux Reader 0.2.0-alpha.92 — Library navigation & collections

- Library folder is owned by AppViewModel and saved in private preferences. It survives Settings, Reader, Speed Reader and process restart.
- Renaming, moving and deleting a folder rebase the remembered path to an existing destination/parent.
- One palette catalogue is shared across the original interface and experimental shells; Wasteland / Cyberpunk remain separate frame choices.
- AUDIO filter added to the top type-filter strip in both library shells. Audio tracks are listed exclusively when selected.
- Multi-select menu can create a new folder and move selected books there, with rollback of memberships if anything fails.
- Removed redundant upper-right search icon in the experimental library: the permanent search field remains; the hardware-rail search shortcut remains.

Device rendering and Android SDK compilation are not available in the build environment: run Gradle on a Mac and inspect the interface on device.
