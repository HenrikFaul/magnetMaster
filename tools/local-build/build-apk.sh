#!/usr/bin/env bash
#
# Self-contained APK builder for Magnet Master.
#
# Builds a signed, installable debug APK WITHOUT Gradle, Android Studio or the
# Google-hosted Android SDK. Every tool is fetched from a publicly reachable
# mirror (GitHub / Maven Central / storage.googleapis.com), so it works in
# locked-down environments where dl.google.com / maven.google.com are blocked.
#
# Requirements: bash, curl, unzip, a JDK 17+ (provides java/javac/jar/keytool).
# Output: tools/local-build/out/MagnetMaster.apk
#
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PROJ="$(cd "$HERE/../.." && pwd)"
TC="$HERE/.toolchain"
OUT="$HERE/out"
WORK="$HERE/.work"
mkdir -p "$TC" "$OUT"
rm -rf "$WORK"; mkdir -p "$WORK"/{compiled,gen,dex,apk}

# Pinned, reachable artifact sources
KOTLIN_VER="1.9.24"
SDKTOOLS_VER="34.0.3"
R8_VER="8.3.37"
APKSIG_VER="2.3.0"
KOTLIN_URL="https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VER}/kotlin-compiler-${KOTLIN_VER}.zip"
SDKTOOLS_URL="https://github.com/lzhiyong/android-sdk-tools/releases/download/${SDKTOOLS_VER}/android-sdk-tools-static-x86_64.zip"
ANDROIDJAR_URL="https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar"
R8_URL="https://storage.googleapis.com/r8-releases/raw/${R8_VER}/r8.jar"
APKSIG_URL="https://repo1.maven.org/maven2/com/android/tools/build/apksig/${APKSIG_VER}/apksig-${APKSIG_VER}.jar"

fetch(){ [ -e "$2" ] || { echo "  fetching $(basename "$2")"; curl -fsSL -o "$2" "$1"; }; }

echo "==> [0/8] fetch toolchain"
fetch "$ANDROIDJAR_URL" "$TC/android.jar"
fetch "$R8_URL"         "$TC/r8.jar"
fetch "$APKSIG_URL"     "$TC/apksig.jar"
if [ ! -x "$TC/kotlinc/bin/kotlinc" ]; then
  fetch "$KOTLIN_URL" "$TC/kotlin.zip"; unzip -q -o "$TC/kotlin.zip" -d "$TC"
fi
if [ ! -x "$TC/sdktools/build-tools/aapt2" ]; then
  fetch "$SDKTOOLS_URL" "$TC/sdktools.zip"; unzip -q -o "$TC/sdktools.zip" -d "$TC/sdktools"
fi

KOTLINC="$TC/kotlinc/bin/kotlinc"
AAPT2="$TC/sdktools/build-tools/aapt2"
ZIPALIGN="$TC/sdktools/build-tools/zipalign"
ANDROID_JAR="$TC/android.jar"
R8="$TC/r8.jar"
APKSIG="$TC/apksig.jar"
chmod +x "$AAPT2" "$ZIPALIGN" 2>/dev/null || true

cd "$WORK"
echo "==> [1/8] aapt2 compile resources"
"$AAPT2" compile --dir "$PROJ/app/src/main/res" -o compiled/res.zip

echo "==> [2/8] inject package + version into manifest"
sed 's#<manifest xmlns:android="http://schemas.android.com/apk/res/android">#<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.magnetmaster.game" android:versionCode="1" android:versionName="1.0.0">#' \
    "$PROJ/app/src/main/AndroidManifest.xml" > AndroidManifest.xml

echo "==> [3/8] aapt2 link"
"$AAPT2" link -o apk/base.apk -I "$ANDROID_JAR" --manifest AndroidManifest.xml \
    -R compiled/res.zip --java gen --min-sdk-version 24 --target-sdk-version 34 \
    --version-code 1 --version-name 1.0.0 --auto-add-overlay

echo "==> [4/8] kotlinc compile -> app.jar"
KT_SOURCES=$(find "$PROJ/app/src/main/java" -name "*.kt")
"$KOTLINC" -jvm-target 1.8 -classpath "$ANDROID_JAR" -include-runtime -d app.jar $KT_SOURCES

echo "==> [5/8] d8 dex (desugared, min-api 24)"
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 24 --lib "$ANDROID_JAR" --output dex app.jar

echo "==> [6/8] assemble unsigned apk"
cp apk/base.apk apk/unsigned.apk
(cd dex && zip -q -X ../apk/unsigned.apk classes*.dex)

echo "==> [7/8] zipalign"
"$ZIPALIGN" -f -p 4 apk/unsigned.apk apk/aligned.apk

echo "==> [8/8] sign (v2)"
KS="$TC/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storetype PKCS12 -storepass android \
    -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
[ -f "$TC/ApkSignerTool.class" ] || javac -cp "$APKSIG" -d "$TC" "$HERE/ApkSignerTool.java"
java --add-exports java.base/sun.security.x509=ALL-UNNAMED \
     -cp "$APKSIG:$TC" ApkSignerTool "$KS" android androiddebugkey android \
     apk/aligned.apk "$OUT/MagnetMaster.apk" 24

echo "==============================================="
ls -la "$OUT/MagnetMaster.apk"
echo "DONE -> $OUT/MagnetMaster.apk"
