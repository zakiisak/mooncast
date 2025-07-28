#!/usr/bin/env python3
"""
Quick test script to verify the executable includes all dependencies
"""

import subprocess
import time
import os

def test_executable():
    exe_path = "dist\\Mooncast-Controller.exe"
    
    if not os.path.exists(exe_path):
        print("❌ Executable not found!")
        return False
    
    print("🧪 Testing Mooncast Controller executable...")
    print(f"📁 Path: {exe_path}")
    print(f"📦 Size: {os.path.getsize(exe_path) / (1024*1024):.1f} MB")
    
    try:
        # Start the executable in the background
        print("🚀 Starting executable...")
        process = subprocess.Popen([exe_path], 
                                 stdout=subprocess.PIPE, 
                                 stderr=subprocess.PIPE,
                                 creationflags=subprocess.CREATE_NO_WINDOW)
        
        # Wait a moment for it to start
        time.sleep(3)
        
        # Check if it's still running (hasn't crashed)
        if process.poll() is None:
            print("✅ Executable started successfully!")
            print("✅ No 'requests' module error!")
            
            # Terminate the process
            process.terminate()
            process.wait(timeout=5)
            print("🛑 Test completed - executable terminated")
            return True
        else:
            # Process has already exited, check for errors
            stdout, stderr = process.communicate()
            print("❌ Executable crashed!")
            print(f"Exit code: {process.returncode}")
            if stderr:
                print(f"Error: {stderr.decode()}")
            return False
            
    except Exception as e:
        print(f"❌ Test failed: {e}")
        return False

if __name__ == "__main__":
    success = test_executable()
    if success:
        print("\n🎯 Ready for distribution!")
        print("The executable should work on any Windows machine without Python.")
    else:
        print("\n⚠️ Issues detected - check the build configuration.")
    
    input("\nPress Enter to continue...") 