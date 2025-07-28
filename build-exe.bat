@echo off
echo 🚀 Building Mooncast Controller Executable
echo.
echo Cleaning previous builds...
if exist "dist" rmdir /s /q "dist"
if exist "build" rmdir /s /q "build"
if exist "__pycache__" rmdir /s /q "__pycache__"

echo.
echo Building single-file executable...
echo This may take a few minutes...
echo.

rem Build using the spec file
python -m PyInstaller mooncast-controller.spec

echo.
if exist "dist\Mooncast Controller.exe" (
    echo ✅ SUCCESS! Executable created successfully
    echo.
    echo 📁 Location: dist\Mooncast Controller.exe
    echo 📦 Size: 
    dir "dist\Mooncast Controller.exe" | find ".exe"
    echo.
    echo You can now distribute this single file - no Python installation needed!
    echo.
    echo Testing the executable...
    echo.
    start "" "dist\Mooncast Controller.exe"
) else (
    echo ❌ FAILED! Executable was not created
    echo Check the output above for errors
)

echo.
pause 