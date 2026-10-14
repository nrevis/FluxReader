# Flux Reader 0.2.0-alpha.38

The screen-paging Reader now prioritizes perceived speed: it paints a small page at the saved reading anchor first and completes exact chapter pagination immediately afterwards. The preview cannot leak into the pagination cache or pretend that the chapter contains one page. The next chapter is also paginated ahead for the current viewport after the current page settles. Re-entering Library releases substantially more Reader memory while keeping the persistent parsed-book cache on disk.
