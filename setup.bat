@echo off
setlocal EnableExtensions
title Chromium UA - Setup

cd /d "%~dp0"
if not exist tools mkdir tools

where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git for Windows is required. Install it from https://git-scm.com/download/win
  pause
  exit /b 1
)

if not exist "%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" if not exist "%ProgramFiles%\Microsoft Visual Studio\Installer\vswhere.exe" (
  echo [WARNING] Visual Studio Installer was not detected.
  echo Install Visual Studio 2022 with "Desktop development with C++" and Windows SDK.
)

if not exist "tools\depot_tools\gclient.bat" (
  echo [1/5] Downloading official depot_tools...
  git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git tools\depot_tools
  if errorlevel 1 goto fail
)

set "PATH=%CD%\tools\depot_tools;%PATH%"
set DEPOT_TOOLS_WIN_TOOLCHAIN=0
set DEPOT_TOOLS_UPDATE=1

if not exist "chromium\src\.gclient" (
  echo [2/5] Fetching official Chromium source. This is a very large download.
  cd /d "%CD%\chromium"
  fetch --nohooks chromium
  if errorlevel 1 goto fail
) else (
  echo [2/5] Chromium source already exists; skipping fetch.
)

cd /d "%~dp0chromium\src"
echo [3/5] Synchronizing Chromium dependencies...
call gclient sync
if errorlevel 1 goto fail

echo [4/5] Generating Release build files...
if not exist out\Release mkdir out\Release
call gn gen out\Release --args="is_debug=false is_component_build=false target_cpu=\"x64\""
if errorlevel 1 goto fail

echo [5/5] Building Chromium...
call autoninja -C out\Release chrome
if errorlevel 1 goto fail

cd /d "%~dp0"
echo.
echo SUCCESS: chromium\src\out\Release\chrome.exe
echo Launch it with run-uk.bat
pause
exit /b 0

:fail
echo.
echo [ERROR] Setup/build failed. Review the messages above.
echo See README.md for prerequisites and official build documentation.
pause
exit /b 1
