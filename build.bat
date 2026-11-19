@echo off
setlocal
cd /d "%~dp0"
if not exist "tools\depot_tools\gclient.bat" (
  echo Run setup.bat first.
  pause
  exit /b 1
)
set "PATH=%CD%\tools\depot_tools;%PATH%"
set DEPOT_TOOLS_WIN_TOOLCHAIN=0
cd /d "%~dp0chromium\src"
call gclient sync
if errorlevel 1 goto fail
call gn gen out\Release --args="is_debug=false is_component_build=false target_cpu=\"x64\""
if errorlevel 1 goto fail
call autoninja -C out\Release chrome
if errorlevel 1 goto fail
echo Build complete: out\Release\chrome.exe
pause
exit /b 0
:fail
echo Build failed.
pause
exit /b 1
