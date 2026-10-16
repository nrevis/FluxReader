# Flux Reader 0.2.0-alpha.58

## Deep experimental theme pass

- Wasteland CRT is now a full skin rather than a palette swap: oxidized/rust texture overlay, heavier CRT scanlines, metal edge plates, terminal frame, bolt heads and faded caution marks.
- Cyberpunk is stronger: real micro-grid/circuit texture, double HUD frame, neon rails, nodes and static glitch bars.
- Added separate custom vector icon families for Wasteland and Cyberpunk. The icon geometry is different per skin instead of tinting the same Material glyphs.
- Key Library, Reader and Settings actions now use the themed icon family.
- Appearance settings show a live texture/icon preview for both experimental skins.
- Wasteland Material shapes use bevel/cut corners and stronger brass/rust/phosphor colors.
- Cyberpunk uses stronger cyan/magenta/violet contrast and more angular surface identity.

## Performance / input safety

- Texture and frame layers are static resources with no timer, animation loop or pointer input.
- Reader gesture handlers are unchanged.
- The textures are transparent overlays sized for low runtime overhead.

## Version

- versionName: `0.2.0-alpha.58`
- versionCode: `20075`
