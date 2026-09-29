#!/bin/bash
# Turns the desktop module into an OS installer image. The package workflow calls it per runner.
set -euo pipefail

root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$root"
version=$(sed -n 's/.*APP_VERSION = "\([^"]*\)".*/\1/p' desktop/src/window/Logic.kt | head -1)
version=${version:-2.0.0}
name="HL7 Lookup"
app_id=com.haasele.hl7lookup
stage="$root/build/bundle-stage"
input="$stage/input"
dist="$root/build/dist"
bundles="$root/build/bundles"
icon_png="$root/desktop/resources/icons/hl7.png"
web_dir=${WEB_DIR:-}

case "$(uname -s)" in
  Linux*) os=linux ;;
  Darwin*) os=macos ;;
  MINGW*|MSYS*|CYGWIN*) os=windows ;;
  *) echo "unsupported OS $(uname -s)" >&2; exit 1 ;;
esac
machine=$(uname -m)
case "$machine" in
  x86_64|amd64) arch=x64; appimage_arch=x86_64 ;;
  aarch64|arm64) arch=arm64; appimage_arch=aarch64 ;;
  *) arch=$machine; appimage_arch=$machine ;;
esac

native() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s\n' "$1"; fi
}
first_file() {
  local found="" candidate
  while IFS= read -r candidate; do found=$candidate; break; done < <(find "$1" -name "$2" -type f)
  printf '%s\n' "$found"
}

if [ -z "$web_dir" ]; then
  web_mjs=$(first_file build web.mjs)
  web_dir=$(dirname "$web_mjs")
fi
jar=$(first_file build '*-jvm-executable.jar')
if [ -z "$jar" ] || [ -z "$web_dir" ] || [ ! -f "$web_dir/web.mjs" ]; then
  echo "missing executable jar or web bundle (jar=$jar web=$web_dir)" >&2
  exit 1
fi

rm -rf "$stage" "$dist"
mkdir -p "$input" "$bundles" "$stage/extract"
(cd "$stage/extract" && jar xf "$jar")
classes="$stage/extract/BOOT-INF/classes"
mkdir -p "$classes"
find "$web_dir" -maxdepth 1 -type f \( -name '*.mjs' -o -name '*.wasm' -o -name '*.js' \) -exec cp {} "$classes/" \;
rm -f "$classes/META-INF/MANIFEST.MF"
jar cfe "$input/hl7-lookup.jar" hl7lookup.desktop.window.IndexKt -C "$classes" .
cp "$stage/extract/BOOT-INF/lib/"*.jar "$input/"

runnable="$bundles/hl7-lookup-${version}-${os}-${arch}.jar"
cp "$jar" "$runnable"
web_entries=()
while IFS= read -r file; do
  web_entries+=("BOOT-INF/classes/${file##*/}")
done < <(find "$classes" -maxdepth 1 -type f \( -name '*.mjs' -o -name '*.wasm' -o -name '*.js' \))
if [ ${#web_entries[@]} -gt 0 ]; then
  (cd "$stage/extract" && jar uf "$(native "$runnable")" "${web_entries[@]}")
fi

icon_arg=()
case "$os" in
  linux)
    icon_arg=(--icon "$(native "$icon_png")")
    ;;
  windows)
    python3 "$root/packaging/make-ico.py" || python "$root/packaging/make-ico.py"
    icon_arg=(--icon "$(native "$root/build/hl7.ico")")
    wix="$stage/wix"
    mkdir -p "$wix"
    curl -fsSL -o "$stage/wix.zip" https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip
    (cd "$wix" && jar xf "$stage/wix.zip")
    export PATH="$wix:$PATH"
    ;;
  macos)
    iconset="$stage/icon.iconset"
    mkdir -p "$iconset"
    for size in 16 32 64 128 256 512; do
      sips -z "$size" "$size" "$icon_png" --out "$iconset/icon_${size}x${size}.png" >/dev/null
      double=$((size * 2))
      sips -z "$double" "$double" "$icon_png" --out "$iconset/icon_${size}x${size}@2x.png" >/dev/null
    done
    iconutil -c icns "$iconset" -o "$stage/hl7.icns"
    icon_arg=(--icon "$(native "$stage/hl7.icns")")
    ;;
esac

common=(
  --name "$name"
  --app-version "$version"
  --vendor "HL7 Lookup"
  --input "$(native "$input")"
  --main-jar hl7-lookup.jar
  --main-class hl7lookup.desktop.window.IndexKt
  --dest "$(native "$dist")"
  --java-options "-Dfile.encoding=UTF-8"
  "${icon_arg[@]}"
)

jpackage "${common[@]}" --type app-image
image="$dist/$name"

case "$os" in
  windows)
    py() { if command -v python3 >/dev/null 2>&1; then python3 "$@"; else python "$@"; fi; }
    py - "$bundles/hl7-lookup-${version}-windows-${arch}-portable" "$dist" "$name" << 'PY'
import shutil, sys
shutil.make_archive(sys.argv[1], "zip", sys.argv[2], sys.argv[3])
PY
    jpackage "${common[@]}" --type msi --win-menu --win-shortcut --win-dir-chooser
    mv "$dist"/*.msi "$bundles/hl7-lookup-${version}-windows-${arch}.msi"
    ;;
  macos)
    jpackage "${common[@]}" --type pkg --mac-package-identifier "$app_id" --mac-package-name "HL7 Lookup"
    mv "$dist"/*.pkg "$bundles/hl7-lookup-${version}-macos-${arch}.pkg"
    ;;
  linux)
    appdir="$stage/HL7Lookup.AppDir"
    mkdir -p "$appdir/usr/lib" "$appdir/usr/share/applications" "$appdir/usr/share/icons/hicolor/256x256/apps"
    cp -a "$image" "$appdir/usr/lib/hl7-lookup"
    cp "$icon_png" "$appdir/hl7-lookup.png"
    cp "$icon_png" "$appdir/usr/share/icons/hicolor/256x256/apps/hl7-lookup.png"
    sed 's/^Exec=.*/Exec=hl7-lookup/' packaging/hl7-lookup.desktop > "$appdir/hl7-lookup.desktop"
    cp "$appdir/hl7-lookup.desktop" "$appdir/usr/share/applications/hl7-lookup.desktop"
    cat > "$appdir/AppRun" << EOF
#!/bin/sh
HERE=\$(dirname "\$(readlink -f "\$0")")
exec "\$HERE/usr/lib/hl7-lookup/bin/$name" "\$@"
EOF
    chmod +x "$appdir/AppRun"
    tool=${APPIMAGETOOL:-appimagetool}
    ARCH=$appimage_arch "$tool" "$appdir" "$bundles/hl7-lookup-${version}-linux-${arch}.AppImage"

    src="$stage/flatpak-src"
    mkdir -p "$src"
    cp -a "$image" "$src/image"
    cp "$icon_png" "$src/hl7.png"
    cat > "$src/hl7-lookup.sh" << EOF
#!/bin/sh
exec "/app/hl7-lookup/bin/$name" "\$@"
EOF
    chmod +x "$src/hl7-lookup.sh"
    sed "s/^Icon=.*/Icon=$app_id/" packaging/hl7-lookup.desktop | sed 's/^Exec=.*/Exec=hl7-lookup/' > "$src/hl7-lookup.desktop"
    cat > "$stage/com.haasele.HL7Lookup.yml" << EOF
app-id: $app_id
runtime: org.freedesktop.Platform
runtime-version: '24.08'
sdk: org.freedesktop.Sdk
command: hl7-lookup
finish-args:
  - --share=ipc
  - --socket=fallback-x11
  - --socket=wayland
  - --device=dri
  - --share=network
  - --filesystem=home
modules:
  - name: hl7-lookup
    buildsystem: simple
    build-commands:
      - mkdir -p /app/hl7-lookup /app/bin /app/share/applications /app/share/icons/hicolor/256x256/apps
      - cp -a image/. /app/hl7-lookup/
      - install -m 755 hl7-lookup.sh /app/bin/hl7-lookup
      - install -m 644 hl7-lookup.desktop /app/share/applications/$app_id.desktop
      - install -m 644 hl7.png /app/share/icons/hicolor/256x256/apps/$app_id.png
    sources:
      - type: dir
        path: flatpak-src
EOF
    flatpak-builder --disable-rofiles-fuse --force-clean --repo "$stage/repo" "$stage/flatpak-build" "$stage/com.haasele.HL7Lookup.yml"
    flatpak build-bundle "$stage/repo" "$bundles/hl7-lookup-${version}-linux-${arch}.flatpak" "$app_id"
    ;;
esac

echo "bundles:"
ls -lh "$bundles"
