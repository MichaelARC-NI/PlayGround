#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

PROJECT_DIR="$PWD"
PORT="${PORT:-3000}"
OPENCODE_WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
PREVIEW_DIR="$PROJECT_DIR/dist/preview"
APK_PATH="$PROJECT_DIR/dist/VoltTrigger-1.0.0-debug.apk"

[[ "$PORT" =~ ^[0-9]+$ ]] || { echo "Invalid PORT: $PORT" >&2; exit 1; }
/usr/bin/time -p test -n "$OPENCODE_WEB_DIR"
/usr/bin/time -p test -f web/index.html
/usr/bin/time -p test -f web/styles.css
/usr/bin/time -p test -f web/app.js
/usr/bin/time -p test -f web/screenshots/01-main.png

/usr/bin/time -p rm -rf "$PREVIEW_DIR"
/usr/bin/time -p mkdir -p "$PREVIEW_DIR/screenshots"
/usr/bin/time -p cp web/index.html web/styles.css web/app.js "$PREVIEW_DIR/"
/usr/bin/time -p cp web/screenshots/01-main.png "$PREVIEW_DIR/screenshots/"
/usr/bin/time -p cp web/screenshots/05-hold-both.png "$PREVIEW_DIR/screenshots/"
/usr/bin/time -p cp web/screenshots/08-editor.png "$PREVIEW_DIR/screenshots/"
/usr/bin/time -p cp web/screenshots/09-bubble.png "$PREVIEW_DIR/screenshots/"
/usr/bin/time -p cp web/screenshots/10-landscape-hold.png "$PREVIEW_DIR/screenshots/"

/usr/bin/time -p test -f "$APK_PATH" || {
  echo "APK not found; building it now." >&2
  /usr/bin/time -p ./gradlew assembleDebug --console=plain || echo "APK build failed; serving the web preview anyway." >&2
  /usr/bin/time -p mkdir -p "$PROJECT_DIR/dist"
  if [[ -f app/build/outputs/apk/debug/app-debug.apk ]]; then
    /usr/bin/time -p cp app/build/outputs/apk/debug/app-debug.apk "$APK_PATH"
  fi
}
if [[ -f "$APK_PATH" ]]; then
  /usr/bin/time -p cp "$APK_PATH" "$PREVIEW_DIR/VoltTrigger-debug.apk"
fi

/usr/bin/time -p mkdir -p "$OPENCODE_WEB_DIR"
/usr/bin/time -p node -e 'const fs = require("node:fs"); fs.writeFileSync(process.argv[1], JSON.stringify({ project: process.argv[2], directory: process.argv[3] }) + "\n")' "$OPENCODE_WEB_DIR/deployment-output.json" "$PROJECT_DIR" "$PREVIEW_DIR"
/usr/bin/time -p cat "$OPENCODE_WEB_DIR/deployment-output.json"
/usr/bin/time -p node web/server.mjs
