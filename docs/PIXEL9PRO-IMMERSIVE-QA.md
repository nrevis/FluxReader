# Pixel 9 Pro fullscreen / camera-hole QA — alpha.79

## Actual screenshot grounding

The user-provided screenshots are 688×1536 pixels (scaled representation of the Pixel 9 Pro's tall display). In previous alpha.78 immersive Speed Reader screenshots the content/progress boundary was approximately y=128px. This was noticeably larger than the physical centered hole-punch and remained after OS status icons disappeared.

The side-by-side annotated overlay provided separately is an *inset-budget schematic*, not a screenshot of alpha.79 running on a device. The green band shows the proposed usable viewport. 22dp fallback camera clearance corresponds to approximately 39px assuming 393dp screen width mapped to 688px. The actual `WindowInsets.displayCutout` reports the hardware top inset when available; the Kotlin calculation uses the larger of those values.

## Device tests to be completed before stable release

1. Enable Wasteland experimental design. In ordinary Reader confirm clock, connectivity, Wi-Fi and battery remain visible, with no content under status/navigation bars.
2. Press the Reader fullscreen button while the auto-hide option is disabled. Confirm Android status/navigation bars vanish, text moves into previously reserved space, and no line intersects the physical camera hole.
3. Tap page center and verify toolbars return. Confirm scrolling position and chapter progress do not jump (both scrolling and page modes).
4. Repeat with Speed Reader: fullscreen progress begins just under camera guard, word centered, voice/play controls unobstructed, exit restores bars and frame insets.
5. Repeat with Cyberpunk and the standard theme (experimental checkbox off).
6. Enter/exit fullscreen repeatedly, rotate device if enabled, change font size, pause TTS while leaving fullscreen, and navigate back into Library. Check no stuck immersive state.
7. Capture UI hierarchy bounds / screenshot before and after; verify top/bottom reserved heights and positions with layout inspector. Screenshots alone cannot prove actual touch bounds.

## Release safety

`python3 scripts/critic_pixel_immersive.py` asserts parent/child inset contracts; `python3 scripts/test_pixel_immersive_policy.py` compiles and executes the standalone Kotlin geometry policy when `kotlinc` is installed. Neither substitutes for a full Android Gradle compile and device tests.
