# LIQRA — Android source kit

This folder contains the LIQRA Android setup package metadata.

## Important
The full Android project is not stored in this repository yet. The setup kit downloads the official Telegram Android source and its submodules on the Windows PC, then applies LIQRA branding.

See the project setup instructions in the LIQRA Android Source Kit archive. Before publishing, API credentials have been removed from the public configuration. Add your own Telegram API ID and API hash locally; do not commit private credentials.

## Build target
- App name: LIQRA
- Application ID: com.liqra.messenger
- Base source: https://github.com/DrKLO/Telegram
- Build: :TMessagesProj_App:assembleAfatDebug
