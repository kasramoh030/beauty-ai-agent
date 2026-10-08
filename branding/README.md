# Branding assets

- `icon-3d.png` — the full-resolution 1024x1024 generated app icon (a glossy amber
  four-point sparkle on a teal rounded plate). Source of truth for the artwork.

The app itself ships a 256x256 copy of it at
`app/src/main/res/drawable-nodpi/mark_3d.png` (~98 KB), which is what `AppMark`
renders. Regenerate the bundled copy with:

```sh
python3 tools/resize_png.py branding/icon-3d.png app/src/main/res/drawable-nodpi/mark_3d.png 256
```

The adaptive launcher icon is vector, not raster:
`app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`.
