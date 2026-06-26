# Local APK build (no Gradle / no Google SDK)

`build-apk.sh` produces a **signed, installable** `MagnetMaster.apk` using only
publicly reachable mirrors — useful in CI/sandbox environments where
`dl.google.com` and `maven.google.com` are blocked.

```bash
tools/local-build/build-apk.sh
# → tools/local-build/out/MagnetMaster.apk
```

Requirements: `bash`, `curl`, `unzip`, and a JDK 17+ (for `java`/`javac`/`jar`/`keytool`).

## Pipeline

| Step | Tool | Source |
|---|---|---|
| Compile resources + link manifest | `aapt2` (static x86_64) | github.com/lzhiyong/android-sdk-tools |
| Compile Kotlin → `app.jar` | `kotlinc` | github.com/JetBrains/kotlin |
| Compile classpath | `android.jar` (API 34) | github.com/Sable/android-platforms |
| Dex + desugar | `d8` (from `r8.jar`) | storage.googleapis.com/r8-releases |
| Align | `zipalign` (static) | github.com/lzhiyong/android-sdk-tools |
| Sign (v2) | `apksig` + `ApkSignerTool.java` | repo1.maven.org (Maven Central) |

The Kotlin is compiled with `-jvm-target 1.8` and dexed through `d8` with
desugaring, so the result is a valid `minSdk 24` APK. The signing key is a
throwaway debug key generated on first run (`.toolchain/debug.keystore`).

For a Play-store upload, build `assembleRelease` via Gradle/Android Studio and
sign with your own upload key instead.
