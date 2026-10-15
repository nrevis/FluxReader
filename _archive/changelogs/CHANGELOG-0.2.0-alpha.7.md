# Flux Reader 0.2.0-alpha.7

Reader link navigation on top of alpha.6 fixed pagination.

- Preserves EPUB inline links with exact character ranges.
- Resolves relative EPUB hrefs and fragment anchors against the current XHTML path.
- Preserves `id`, `xml:id`, and named anchors on reader blocks.
- Internal links can jump across spine chapters directly to the anchored block.
- Link navigation stores the exact source viewport and exposes an in-reader return action.
- External links are delegated to Android's URI handler; the app still requires no runtime INTERNET permission.
- Reader tap zones ignore short taps already consumed by clickable child content, preventing link taps from also turning a page.
- Added parser coverage for cross-chapter footnote-style links and anchors.
