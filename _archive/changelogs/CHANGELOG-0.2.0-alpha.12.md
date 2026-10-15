# Flux Reader 0.2.0-alpha.12

Smart Speed 2.0: punctuation-aware micro-pauses and adaptive pacing.

- Added fixed-millisecond micro-pauses after sentence endings, clauses and dash boundaries so semantic pauses remain perceptible even at high WPM.
- Added separate paragraph pause duration; paragraph pauses can remain enabled independently from punctuation micro-pauses.
- Sentence-end timing distinguishes `.`, `!?`, and ellipsis; `;` / `:` are slightly stronger than a comma.
- Added optional sentence-length adaptation: long sentences receive a bounded extra processing window after the final punctuation.
- Long-word slowdown is now user-configurable instead of being fully hard-coded.
- Added gradual acceleration after Play with configurable starting percentage and ramp duration; the ramp uses smoothstep interpolation and never exceeds target WPM.
- Added rhythm presets: Gentle / Balanced / Comprehension, while keeping all parameters manually adjustable.
- Constant mode remains strictly cadence-based; Smart uses configured semantic timing; Deep scales semantic and complexity adjustments upward.
- Added pure-Kotlin regression coverage for sentence micro-pauses, adaptive sentence length and WPM ramping.
