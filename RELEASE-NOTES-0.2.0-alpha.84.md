# Flux Reader 0.2.0-alpha.84 — Standard Reader Regression Fix

- Standard Reader (non-experimental) places toolbar and bottom controls inside system safe drawing insets while chrome is visible (Android edge-to-edge).
- Immersive mode continues to reclaim system UI space; physical camera cutout is still protected.
- Visible bottom narrator play/pause button added to both normal and experimental Reader, independent of the overflow menu; TTS starts at the visible page when not resuming.
- The isolated one-word page “Выжившим” may represent an actual dedication chapter; no chapter text is synthesized or appended.
- Static guards added. Full Android compilation and on-device interaction require separate verification.
