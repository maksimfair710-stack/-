# LIQRA — Android (AyuGram4A)

This branch contains the Android build configuration for LIQRA.

## Source base

- Upstream: https://github.com/AyuGram/AyuGram4A
- Branch: `rewrite`
- Pinned upstream commit: `7013145676d36d82ee13c02a89f72097b7490dcd`
- AyuGram4A is based on exteraGram.
- The private AyuGram proprietary submodule is deliberately not fetched.
- LIQRA provides an open-source replacement for the missing `AyuMessageUtils` adapter.

## Build

GitHub Actions workflow: `.github/workflows/build-liqra.yml`

Required repository secrets:
- `LIQRA_API_ID` — numeric Telegram API ID.
- `LIQRA_API_HASH` — Telegram API hash.

Start it from **Actions → Build LIQRA Android (AyuGram) → Run workflow**, selecting
the `liqra-android-ayugram` branch. The workflow creates a universal debug APK
and uploads it as `LIQRA-AyuGram-Android-debug`.

The build uses the standard Android debug signing key. It is for testing, not a
production-signed release.

## Current scope

The build configuration applies LIQRA package/name branding, disables the upstream
Firebase project integration, and supplies the missing message serialization adapter.
A successful CI build is still required before calling this APK installable. Liquid
Glass UI work is a separate next stage; this branch does not claim the entire AyuGram
interface has already been redesigned.
