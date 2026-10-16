# Flux Reader 0.2.0-alpha.42

This build finishes the modal-navigation audit started in alpha.41.

The visible rule is now consistent: **Back navigates one level up when a modal has an internal hierarchy; Close exits the modal entirely.** Root sheets still dismiss normally with Back or the scrim.

Reader TOC/search/settings and Speed Reader settings now share the same close/header language. Delete confirmations also share one destructive-dialog primitive so single, folder and batch deletion no longer drift in styling or button behavior.
