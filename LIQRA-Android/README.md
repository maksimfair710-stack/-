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


## Автоматическая сборка APK через GitHub Actions

Workflow добавлен: `.github/workflows/build-liqra.yml`. Он скачивает официальный исходный код Telegram Android с подмодулями, применяет LIQRA branding, собирает debug APK и прикрепляет его к запуску как artifact.

Перед первым запуском добавь в **Settings → Secrets and variables → Actions → New repository secret** три секрета:

- `LIQRA_API_ID` — числовой Telegram API ID.
- `LIQRA_API_HASH` — Telegram API hash.
- `LIQRA_GOOGLE_SERVICES_JSON_BASE64` — содержимое файла `google-services.json`, закодированное в Base64. Firebase-конфигурация должна содержать Android-приложения с ID `com.liqra.messenger` и `com.liqra.messenger.beta`.

После добавления секретов открой **Actions → Build LIQRA APK → Run workflow**. APK будет доступен в разделе Artifacts завершённого запуска. Любое изменение в `LIQRA-Android/**` также запускает сборку автоматически.

Это debug APK для тестирования, не подписанный production-релиз. Liquid Glass пока добавлен как ресурсная основа, а не как полностью переработанный интерфейс всех экранов. Функции AyuGram в этой сборке не заявлены.
