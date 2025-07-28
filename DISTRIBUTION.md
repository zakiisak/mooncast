# 📦 Mooncast Controller Distribution Guide

## 🎯 **Quick Distribution (Recommended)**

### **For End Users (No Technical Knowledge Required)**
Just share this single file:
- **`dist/Mooncast-Controller.exe`** (~10 MB)

**Instructions for recipients:**
1. Download the `.exe` file
2. Double-click to run (no installation needed)
3. Enter your Android device's IP address
4. Click "🎮 Cast Desktop" to start streaming

---

## 🐍 **For Developers/Power Users**

### **Python Source Files**
```
mooncast-controller.py     # Main controller script
requirements.txt           # Python dependencies
run-controller.bat         # Windows batch file to run Python version
```

### **Build Files**
```
build-simple.bat           # Simple build script
mooncast-controller.spec   # PyInstaller spec file (advanced)
Mooncast-Controller.spec   # Auto-generated spec
```

---

## 🔨 **Building from Source**

### **Method 1: Simple Build**
```bash
# Install PyInstaller
pip install pyinstaller

# Run the build script
build-simple.bat

# Result: dist\Mooncast-Controller.exe
```

### **Method 2: Direct Command**
```bash
python -m PyInstaller --onefile --windowed --name "Mooncast-Controller" mooncast-controller.py
```

---

## 🚀 **Distribution Options**

### **🎯 Option 1: Single Executable (Best for Most Users)**
**Pros:**
- ✅ No Python installation required
- ✅ No dependency management
- ✅ Works on any Windows machine
- ✅ Single file distribution

**Cons:**
- ❌ Larger file size (~10 MB)
- ❌ Windows only

### **🐍 Option 2: Python Script (Best for Developers)**
**Pros:**
- ✅ Small file size
- ✅ Cross-platform (Windows, Mac, Linux)
- ✅ Easy to modify and debug
- ✅ Readable source code

**Cons:**
- ❌ Requires Python installation
- ❌ Need to manage dependencies

---

## 📋 **System Requirements**

### **For Executable (.exe)**
- Windows 7 or newer
- No other requirements!

### **For Python Script**
- Python 3.6 or newer
- `requests` library (`pip install requests`)

---

## 🔧 **Troubleshooting Distribution**

### **"Windows protected your PC" message**
This is normal for unsigned executables:
1. Click "More info"
2. Click "Run anyway"
3. The app is safe - it's just not code-signed

### **Antivirus false positives**
Some antivirus may flag the executable:
- This is common with PyInstaller executables
- The app only makes HTTP requests to your local network
- You can whitelist the executable or run the Python version instead

### **Different versions**
- Keep the executable and Python script in sync
- Both should have the same functionality
- Use version numbers in filenames if distributing multiple versions

---

## 🎯 **Recommended Distribution Strategy**

1. **For non-technical users**: Share just the `.exe` file
2. **For developers**: Share the full source code
3. **For mixed audiences**: Provide both options with clear instructions
4. **Include**: Brief setup instructions and your Android device's IP address

**Example distribution package:**
```
📁 Mooncast-Controller-v1.0/
├── 🎯 Mooncast-Controller.exe          # Ready to use
├── 📝 README.txt                       # Quick instructions
├── 🐍 Source/                          # For developers
│   ├── mooncast-controller.py
│   ├── requirements.txt
│   └── build-simple.bat
└── 📱 Android-APK/                     # Android app
    └── mooncast-app.apk
``` 