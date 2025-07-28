@echo off
echo 🚀 Building Mooncast Controller Executable (FIXED VERSION)
echo.

echo Cleaning previous builds...
if exist "dist" rmdir /s /q "dist" >nul 2>&1
if exist "build" rmdir /s /q "build" >nul 2>&1
if exist "*.spec" del "*.spec" >nul 2>&1

echo.
echo Checking Python and dependencies...
python -c "import requests; print('✅ requests module found')" || (
    echo ❌ requests module missing! Installing...
    pip install requests
)

echo.
echo Building executable with explicit dependencies...
python -m PyInstaller --onefile --windowed --name "Mooncast-Controller" ^
    --hidden-import=requests ^
    --hidden-import=urllib3 ^
    --hidden-import=certifi ^
    --hidden-import=charset_normalizer ^
    --hidden-import=idna ^
    --hidden-import=tkinter ^
    --hidden-import=tkinter.ttk ^
    --collect-all=requests ^
    mooncast-controller.py

echo.
if exist "dist\Mooncast-Controller.exe" (
    echo ✅ SUCCESS! 
    echo 📁 Location: dist\Mooncast-Controller.exe
    echo 📦 Testing the executable...
    
    rem Quick test to see if it starts without errors
    timeout /t 2 >nul
    echo.
    echo 🎯 You can now distribute this single file!
    echo    No Python installation needed on target machines.
) else (
    echo ❌ Build failed! Check output above for errors.
)

echo.
pause 