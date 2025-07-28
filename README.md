# 🌙 Mooncast - Remote Moonlight Controller

A complete remote control solution for Moonlight streaming consisting of:
- **📱 Android Spectator App** - Receives commands and automates Moonlight
- **🖥️ Desktop Controller** - Beautiful web app to send casting commands

## 🚀 Quick Start

### 📱 Android App Setup

1. **Install the Mooncast app** on your Android device
2. **Enable required permissions:**
   - Disable battery optimization
   - Enable accessibility service
3. **Connect to WiFi** - The app will show your IP address prominently
4. **Note the IP address** displayed at the top of the app

### 🖥️ Desktop Controller Setup

1. **Install Sunshine** on your host PC from [GitHub Releases](https://github.com/LizardByte/Sunshine/releases)
2. **Download** `mooncast-controller.html` 
3. **Open in any web browser** (Chrome, Firefox, Safari, Edge)
4. **Enter your Android device's IP address**
5. **Click "Cast Desktop"** to start streaming
6. **Click "Stop Casting"** to end the session

## ✨ Features

### 📱 Android App Features
- **🎯 Fullscreen mode** - No distractions, just the IP
- **🤖 Automatic Moonlight control** - Complete automation
- **🔄 Session resumption** - Handles both new and existing sessions
- **🔧 Debug tools** - Built-in testing and diagnostics
- **⚡ Real-time status** - Shows connection state and network info

### 🖥️ Desktop Controller Features
- **🎨 Beautiful modern UI** - Glassmorphism design with animations
- **💾 Remembers IP address** - Saves your device IP locally
- **⚡ Real-time feedback** - Loading states and success/error messages
- **🎮 Ripple effects** - Satisfying button interactions
- **⌨️ Keyboard shortcuts** - Press Enter to cast
- **🌐 Cross-platform** - Works on Windows, Mac, Linux

## 🔧 How It Works

1. **Host PC** opens the desktop controller
2. **Enters Android device IP** (shown on Android app)
3. **Clicks "Cast Desktop"**
4. **HTTP POST** sent to `http://[device-ip]:8080/cast`
5. **Android device** receives the command
6. **Automatically launches Moonlight** and connects
7. **Session starts** - Desktop streaming begins!

## 🛠️ Technical Details

### Prerequisites
- **Sunshine** must be installed and running on the host PC
- **Moonlight** must be installed on the Android device
- Both devices must be on the same network

### HTTP Endpoints
- **POST** `/cast` - Start desktop streaming
- **POST** `/stop` - Stop streaming and return to Mooncast

### Port
- **Fixed port: 8080** (no configuration needed)

### Automation Flow
1. **Launch Moonlight app**
2. **Click plus button** (add host)
3. **Enter IP address** (automatic)
4. **Click OK** (confirm)
5. **Select host PC** (by name)
6. **Choose Desktop** (streaming option)
7. **Resume/Start session** (handles both scenarios)

## 🎨 Screenshots

### Android App (Fullscreen)
```
┌─────────────────────────────┐
│    🌙 MOONCAST READY        │
│                             │
│      192.168.1.150         │
│                             │
│    📶 MyHomeNetwork         │
└─────────────────────────────┘
```

### Desktop Controller
```
┌─────────────────────────────┐
│           🌙                │
│     Mooncast Controller     │
│                             │
│  Device IP Address          │
│  [192.168.1.150        ]   │
│                             │
│  [🎮 Cast Desktop] [🛑 Stop] │
│                             │
│    Port 8080 • v1.0         │
└─────────────────────────────┘
```

## 🔍 Troubleshooting

### Android App Issues
- **IP not showing?** Check WiFi connection
- **Commands not working?** Verify accessibility service is enabled
- **App keeps closing?** Disable battery optimization

### Desktop Controller Issues
- **Can't connect?** Verify IP address and WiFi network
- **CORS errors?** Use a local server or file:// protocol
- **Buttons not working?** Check browser console for errors

### Network Issues
- **Both devices must be on the same WiFi network**
- **Firewalls might block port 8080**
- **Some routers have AP isolation enabled**

## 🎯 Pro Tips

1. **Pin the IP address** - Take a screenshot for easy reference
2. **Bookmark the controller** - Add to desktop/favorites
3. **Use keyboard shortcuts** - Press Enter to cast quickly
4. **Test the connection** - Use debug buttons in Android app
5. **Keep devices charged** - Streaming uses battery

## 📝 Version History

- **v1.0** - Initial release with full automation
- Enhanced clicking methods for reliable operation
- Session resumption support
- Fullscreen Android interface
- Beautiful desktop controller

## 🤝 Support

If you encounter issues:
1. Check WiFi connection on both devices
2. Verify all permissions are granted
3. Try the debug buttons in the Android app
4. Check the device logs for detailed information

---

**🌙 Mooncast** - Making remote streaming effortless and beautiful. 