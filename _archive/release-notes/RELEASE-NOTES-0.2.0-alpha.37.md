# Flux Reader 0.2.0-alpha.37

Reader hot-path and bitmap-lifecycle pass. Very large chapters now paginate with substantially smaller temporary text probes in the common case, illustrated EPUB/FB2 books deduplicate and throttle bitmap decoding, and PDF rendering is serialized/cancellation-safe so obsolete native renders cannot race the current page or retain a stale large bitmap. Zoom/page requests arriving during a render now replace the obsolete request instead of being silently dropped. PDF pixel budgets now scale down on low-memory Android devices.
