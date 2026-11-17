# TT MOD

Android wrapper project (Kotlin) with a TikTok web feed and local settings.

## Included
- TikTok website opened in an in-app WebView.
- Region preference defaults to Ukraine (UA); selectable local region setting.
- Basic appearance preference and extra settings screen.
- GitHub Actions workflow builds a debug APK.

## Important limitations
This is an independent wrapper, not a modified or official TikTok application. The saved region is a local app preference and does not spoof TikTok server-side region, SIM, IP, or account signals. TikTok controls its own feed and features. This starter does not remove ads or bypass watermark/download restrictions.

## Build
Run gradle assembleDebug with JDK 17 and Gradle 8.9, or run the GitHub Actions workflow named Build TT MOD APK. The output is app/build/outputs/apk/debug/app-debug.apk.
