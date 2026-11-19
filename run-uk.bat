@echo off
setlocal
set "CHROME=%~dp0chromium\src\out\Release\chrome.exe"
if not exist "%CHROME%" (
  echo Chromium is not built yet. Run setup.bat first.
  pause
  exit /b 1
)
rem --lang selects Ukrainian UI; --accept-lang prioritizes Ukrainian content.
rem Translate remains enabled in Chromium preferences.
start "" "%CHROME%" --lang=uk --accept-lang=uk,uk-UA,en-US,en
