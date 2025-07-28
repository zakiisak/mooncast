# 🌙 Mooncast - Remote Moonlight Controller

Mooncast is a smart Android app that acts as a **Moonlight spectator device** controlled remotely from your desktop. It automatically connects to Moonlight using Android's accessibility service when triggered via HTTP commands from your host PC running **Sunshine**.

## ✨ Features

### 📱 **Android App**
- **HTTP Server** on port 8080 for remote commands
- **Automated Moonlight Connection** via accessibility service
- **Prominent IP Display** in fullscreen mode
- **Battery Optimization Exempt** - stays running
- **Screen Always On** while active
- **Session Resumption** - handles paused streams
- **Host PC Name Storage** - remembers your computers

### 🖥️ **Desktop Controller**
- **No CORS Issues** - native Python app (not HTML)
- **Modern UI** with dark theme and smooth animations
- **IP Address Memory** - saves your device IP
- **One-Click Casting** - start streaming instantly
- **Sunshine Integration** - direct link to releases

---

## 🚀 Quick Setup

### 📋 **Prerequisites**
1. **Host PC**: Install [Sunshine](https://github.com/LizardByte/Sunshine/releases) (streaming host)
2. **Android Device**: Install the Mooncast app + Moonlight app
3. **Desktop**: Either:
   - 🎯 **Use the pre-built executable** (no installation needed!)
   - 🐍 **Or Python 3.6+** (if running from source)

---

## 📱 **Android App Setup**

### 1. **Install & Configure**
```bash
# Build and install the APK
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. **Required Permissions**
The app will prompt you to enable:
- ✅ **Accessibility Service** (for Moonlight automation)
- ✅ **Battery Optimization Exemption** (keeps app alive)

### 3. **WiFi Connection**
- Connect your Android device to the **same WiFi network** as your host PC
- The app will display your device's IP address prominently
- **Example**: `192.168.1.246` (note this IP for the desktop controller)

---

## 🖥️ **Desktop Controller Setup**

### 🎯 **Option 1: Ready-to-Use Executable (Recommended)**
```bash
# No installation needed! Just download and run:
1. Download: Mooncast-Controller.exe
2. Double-click to run
3. Enter your Android device's IP address
4. Click "🎮 Cast Desktop" to start streaming
```

### 🐍 **Option 2: Python Script (For Developers)**
```bash
# Install required packages
pip install -r requirements.txt

# Or manually:
pip install requests

# Run the Python script
python mooncast-controller.py
```

### 🔨 **Building Your Own Executable**
```bash
# Install PyInstaller
pip install pyinstaller

# Build the executable
build-simple.bat

# Result: dist\Mooncast-Controller.exe (~10 MB)
```

### 3. **Configure & Use**
1. **Enter your Android device's IP** (from the Mooncast app)
2. **Click "🎮 Cast Desktop"** to start streaming
3. **Click "🛑 Stop Casting"** to end the session
4. **IP is saved automatically** for next time

---

## 🔧 **How It Works**

### 🌐 **Network Flow**
```
[Host PC] → HTTP POST → [Android:8080] → [Accessibility Service] → [Moonlight App]
    ↓                                                                        ↓
[Sunshine]                                                            [Stream Connection]
```

### 🤖 **Automation Steps**
1. **Receive Command**: Android HTTP server gets `/cast` request
2. **Launch Moonlight**: Opens the Moonlight app automatically  
3. **Click Plus Button**: Finds and taps the "+" to add host
4. **Enter IP Address**: Types the host PC's IP address
5. **Find Host Button**: Smart detection avoids settings buttons
6. **Select Desktop**: Chooses "Desktop" streaming option
7. **Handle Sessions**: Enhanced resumption with 7 click methods

### 📡 **API Endpoints**
- **`POST /cast`** - Start streaming session
- **`POST /stop`** - Stop current session

---

## ⚠️ **Troubleshooting**

### 🔴 **"Accessibility Service Needs Re-enabling"**
**SOLUTION**: This is common! The app now monitors this automatically:
- **Check every 30 seconds** if the service is still enabled
- **Auto-prompts re-enabling** when service gets disabled
- **Enhanced persistence** with additional service flags
- **Manual Fix**: Go to Settings → Accessibility → Mooncast → Enable

### 🔴 **"CORS Error" (HTML Controller)**
**SOLUTION**: Use the **executable or Python controller**:
```bash
# Option 1: Use the ready-to-run executable (RECOMMENDED)
Mooncast-Controller.exe

# Option 2: Use the Python script
python mooncast-controller.py
```

### 🔴 **"App doesn't resume fullscreen"**
**SOLUTION**: This is now fixed automatically:
- **Fullscreen re-enabled** every time you return to the app
- **Persistent across app switching** and notifications
- **No manual intervention** needed

### 🔴 **"Can't Connect to Device"**
**SOLUTION**: Network connectivity
- Ensure both devices are on the **same WiFi network**
- Check the **IP address** matches what's shown in the Mooncast app
- Try pinging: `ping 192.168.1.XXX`

### 🔴 **"Moonlight Automation Fails"**
**SOLUTION**: Check logs and retry
- Open Android Studio or use `adb logcat | grep Moonlight`
- Look for detailed step-by-step automation logs
- The app has multiple fallback methods for unreliable UI elements

### 🔴 **"Clicks Settings Instead of PC Button"**
**SOLUTION**: Enhanced host button detection (now fixed)
- **Smart filtering** avoids settings/menu buttons
- **Explicit PC name matching** with fallback methods
- **Detailed logging** shows which buttons are considered

### 🔴 **"Resume Session Button Won't Click"**
**SOLUTION**: Enhanced session dialog handling (now fixed)
- **7 different click methods**: Standard, parent, focus, position, delayed, grandparent, sibling
- **Multiple detection approaches**: Exact text, partial text, fallback elements
- **Comprehensive logging** shows all attempted methods

### 🔴 **"App Keeps Getting Killed"**
**SOLUTION**: Battery optimization settings
- Go to Settings → Apps → Mooncast → Battery → "Don't optimize"  
- The app requests this automatically, but double-check manually

---

## 🛠️ **Advanced Configuration**

### 🔧 **Custom Port** (if needed)
```kotlin
// In HttpServerService.kt, change:
private val PORT = 8080  // Change to your preferred port
```

### 🔧 **More Aggressive Accessibility Monitoring**
```kotlin
// In MainActivity.kt, uncomment this line:
requestAccessibilityPermission() // Auto-prompt re-enabling
```

### 🔧 **Debug Mode**
Enable detailed logging in the Android app:
```bash
adb shell setprop log.tag.MoonlightA11yService DEBUG
adb logcat | grep MoonlightA11yService
```

---

## 🎯 **Pro Tips**

### 🚀 **Performance**
- **Keep Android plugged in** during long streaming sessions
- **Use 5GHz WiFi** for better streaming quality
- **Close other apps** on Android to free up resources

### 🔒 **Security**
- **LAN only**: The HTTP server only accepts local network connections
- **No authentication**: Designed for trusted home networks
- **Firewall friendly**: No special ports needed (just 8080)

### 🎮 **Gaming**
- **Game Mode**: Enable in Android settings for better performance
- **Do Not Disturb**: Prevent notifications during streaming
- **Landscape Lock**: Some games work better in specific orientations

---

## 📚 **Technical Details**

### 🏗️ **Architecture**
- **Android**: Kotlin + Jetpack Compose + Accessibility Service
- **Desktop**: Python + Tkinter (or standalone executable)
- **Protocol**: Simple HTTP POST with JSON responses
- **Automation**: Android AccessibilityService API

### 📦 **Dependencies**
- **Android**: Android SDK 21+ (Lollipop+)
- **Desktop Executable**: None! Self-contained (~10 MB)
- **Python Version**: 3.6+ with `requests` library (if running from source)
- **Network**: WiFi with multicast/broadcast support

### 🔗 **Integration**
- **Sunshine**: [LizardByte/Sunshine](https://github.com/LizardByte/Sunshine/releases)
- **Moonlight**: Available on Google Play Store
- **Protocol**: Compatible with standard Moonlight/GameStream

---

## 🆘 **Support & Issues**

### 🐛 **Reporting Bugs**
Include these details:
- **Android Version** (e.g., Android 6.0 Marshmallow)
- **Mooncast App Version**
- **Logcat Output** (`adb logcat | grep Mooncast`)
- **Network Setup** (router model, WiFi vs Ethernet)

### 💡 **Feature Requests**
Current roadmap:
- Multiple PC support
- Custom streaming options (resolution, FPS)
- Voice control integration
- Auto-discovery of Sunshine hosts

---

## 📄 **License**

Open source project - feel free to modify and distribute!

---

**Happy Streaming! 🎮🌙** 