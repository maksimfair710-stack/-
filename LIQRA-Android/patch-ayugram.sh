#!/usr/bin/env bash
set -euo pipefail

ROOT="$1"
ROOT="$(cd "$ROOT" && pwd)"
WORKSPACE="$GITHUB_WORKSPACE"
PATCH_SOURCE="$WORKSPACE/LIQRA-Android/src"
DEST="$ROOT/TMessagesProj/src/main/java/com/radolyn/ayugram/proprietary"

test -f "$ROOT/gradlew" || { echo "AyuGram source checkout is incomplete"; exit 1; }
for CLASS in AyuMessageUtils AyuHistoryHook; do
  test -f "$PATCH_SOURCE/$CLASS.java" || { echo "Missing LIQRA $CLASS implementation"; exit 1; }
done

# Replace the private upstream submodule with the public GPL compatibility sources.
rm -rf "$DEST"
mkdir -p "$DEST"
cp "$PATCH_SOURCE/AyuMessageUtils.java" "$DEST/AyuMessageUtils.java"
cp "$PATCH_SOURCE/AyuHistoryHook.java" "$DEST/AyuHistoryHook.java"
rm -f "$ROOT/.gitmodules"

python3 - "$ROOT" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])

props = root / "gradle.properties"
text = props.read_text(encoding="utf-8")
for key, value in {
    "APP_VERSION_NAME": "1.0.0",
    "APP_VERSION_CODE": "1",
    "APP_PACKAGE": "com.liqra.messenger",
}.items():
    pattern = rf"(?m)^{re.escape(key)}=.*$"
    text, count = re.subn(pattern, f"{key}={value}", text, count=1)
    if count != 1:
        raise SystemExit(f"Could not set {key} in gradle.properties")
props.write_text(text, encoding="utf-8")

build = root / "TMessagesProj/build.gradle"
text = build.read_text(encoding="utf-8")
text, count = re.subn(r"signingConfig signingConfigs\.release", "signingConfig signingConfigs.debug", text, count=1)
if count != 1:
    raise SystemExit("Could not switch debug build to the standard debug keystore")

pattern = r"variantFilter\s*\{\s*variant\s*->.*?\n\s*\}"
replacement = """variantFilter { variant ->
        def names = variant.flavors*.name
        setIgnore(names.contains("beta") && variant.buildType.name != "debug")
    }"""
text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
if count != 1:
    raise SystemExit("Could not update AyuGram variant filter")

dollar = chr(36)
old_name = "ayuGram-" + dollar + "{buildType}" + dollar + "{abi}-" + dollar + "{formattedDate}.apk"
new_name = "LIQRA-" + dollar + "{buildType}" + dollar + "{abi}-" + dollar + "{formattedDate}.apk"
text = text.replace(old_name, new_name)
text = text.replace("apply plugin: 'com.google.gms.google-services'", "")
text = text.replace("apply plugin: 'com.google.firebase.crashlytics'", "")
build.write_text(text, encoding="utf-8")

constants = root / "TMessagesProj/src/main/java/com/radolyn/ayugram/AyuConstants.java"
text = constants.read_text(encoding="utf-8")
text = text.replace('public static String APP_NAME = "AyuGram";',
                    'public static String APP_NAME = "LIQRA";')
text = text.replace('public static String APP_GITHUB = "AyuGram/AyuGram4A";',
                    'public static String APP_GITHUB = "maksimfair710-stack/-";')
constants.write_text(text, encoding="utf-8")

resource_root = root / "TMessagesProj/src/main/res"
changed = 0
for path in resource_root.rglob("strings.xml"):
    original = path.read_text(encoding="utf-8")
    updated = re.sub(r'(<string\s+name="AppName">).*?(</string>)', r'\1LIQRA\2', original)
    updated = re.sub(r'(<string\s+name="AppNameBeta">).*?(</string>)', r'\1LIQRA Beta\2', updated)
    if updated != original:
        path.write_text(updated, encoding="utf-8")
        changed += 1
if changed == 0:
    raise SystemExit("Could not locate AppName strings in AyuGram resources")

(root / "TMessagesProj/google-services.json").unlink(missing_ok=True)
print(f"Patched LIQRA identity and AyuGram compatibility layer; updated {changed} string resource files.")
PY

cat > "$ROOT/README.md" <<'EOF'
# LIQRA for Android

LIQRA is an Android Telegram client fork based on the open-source AyuGram4A project
(which itself is based on exteraGram). The AyuGram private submodule is not included.
The missing message serialization adapter is implemented in this repository.

## Build

The GitHub Actions workflow builds a universal debug APK. Required repository secrets:
**LIQRA_API_ID** and **LIQRA_API_HASH**.

## Upstream credits

- AyuGram4A: https://github.com/AyuGram/AyuGram4A
- exteraGram: https://github.com/exteraSquad/exteraGram
- Telegram for Android: https://github.com/DrKLO/Telegram

Please retain upstream copyright notices and license files when redistributing.
EOF
