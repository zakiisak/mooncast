#!/usr/bin/env python3
"""
Mooncast Desktop Controller
A simple desktop app to control your Mooncast Android device
"""

import tkinter as tk
from tkinter import ttk, messagebox
import requests
import json
import os
import re
from threading import Thread
import webbrowser

class MooncastController:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("🌙 Mooncast Controller")
        self.root.geometry("400x500")
        self.root.resizable(False, False)
        
        # Configure style
        self.setup_styles()
        
        # Load saved IP
        self.config_file = "mooncast_config.json"
        self.ip_address = self.load_config()
        
        # Create UI
        self.create_widgets()
        
        # Center window
        self.center_window()
    
    def setup_styles(self):
        """Configure the visual style"""
        self.root.configure(bg='#1a1a2e')
        
        style = ttk.Style()
        style.theme_use('clam')
        
        # Configure styles for modern look
        style.configure('Title.TLabel', 
                       background='#1a1a2e', 
                       foreground='#ffffff', 
                       font=('Arial', 24, 'bold'))
        
        style.configure('Subtitle.TLabel', 
                       background='#1a1a2e', 
                       foreground='#a0aec0', 
                       font=('Arial', 12))
        
        style.configure('Info.TLabel', 
                       background='#1a1a2e', 
                       foreground='#e2e8f0', 
                       font=('Arial', 11))
        
        style.configure('Modern.TEntry',
                       fieldbackground='#2d3748',
                       foreground='#ffffff',
                       bordercolor='#4a5568',
                       lightcolor='#4a5568',
                       darkcolor='#4a5568',
                       font=('Arial', 12))
        
        style.configure('Cast.TButton',
                       background='#48bb78',
                       foreground='white',
                       font=('Arial', 12, 'bold'),
                       focuscolor='none')
        
        style.configure('Stop.TButton',
                       background='#f56565',
                       foreground='white',
                       font=('Arial', 12, 'bold'),
                       focuscolor='none')
        
        style.configure('Link.TButton',
                       background='#4299e1',
                       foreground='white',
                       font=('Arial', 10),
                       focuscolor='none')
    
    def create_widgets(self):
        """Create all UI widgets"""
        main_frame = tk.Frame(self.root, bg='#1a1a2e', padx=40, pady=30)
        main_frame.pack(fill='both', expand=True)
        
        # Logo and title
        title_label = ttk.Label(main_frame, text="🌙", style='Title.TLabel')
        title_label.pack(pady=(0, 10))
        
        title_text = ttk.Label(main_frame, text="Mooncast Controller", style='Title.TLabel')
        title_text.pack(pady=(0, 5))
        
        subtitle = ttk.Label(main_frame, text="Remote control for your streaming device", style='Subtitle.TLabel')
        subtitle.pack(pady=(0, 30))
        
        # IP Address input
        ip_label = ttk.Label(main_frame, text="Device IP Address", style='Info.TLabel')
        ip_label.pack(anchor='w', pady=(0, 5))
        
        self.ip_entry = ttk.Entry(main_frame, style='Modern.TEntry', font=('Arial', 14))
        self.ip_entry.pack(fill='x', pady=(0, 20), ipady=8)
        self.ip_entry.insert(0, self.ip_address)
        
        # Bind Enter key to cast button
        self.ip_entry.bind('<Return>', lambda e: self.cast_desktop())
        
        # Buttons
        button_frame = tk.Frame(main_frame, bg='#1a1a2e')
        button_frame.pack(fill='x', pady=(10, 0))
        
        self.cast_btn = ttk.Button(button_frame, text="🎮 Cast Desktop", 
                                  style='Cast.TButton', command=self.cast_desktop)
        self.cast_btn.pack(fill='x', pady=(0, 10), ipady=10)
        
        self.stop_btn = ttk.Button(button_frame, text="🛑 Stop Casting", 
                                  style='Stop.TButton', command=self.stop_casting)
        self.stop_btn.pack(fill='x', pady=(0, 20), ipady=10)
        
        # Status label
        self.status_label = ttk.Label(main_frame, text="", style='Info.TLabel')
        self.status_label.pack(pady=(10, 0))
        
        # Footer with link
        footer_frame = tk.Frame(main_frame, bg='#1a1a2e')
        footer_frame.pack(side='bottom', fill='x', pady=(20, 0))
        
        port_info = ttk.Label(footer_frame, text="Port 8080 • Mooncast v1.0", style='Subtitle.TLabel')
        port_info.pack()
        
        sunshine_btn = ttk.Button(footer_frame, text="📡 Download Sunshine (Host Software)", 
                                 style='Link.TButton', command=self.open_sunshine_link)
        sunshine_btn.pack(pady=(10, 0))
    
    def center_window(self):
        """Center the window on screen"""
        self.root.update_idletasks()
        width = self.root.winfo_width()
        height = self.root.winfo_height()
        pos_x = (self.root.winfo_screenwidth() // 2) - (width // 2)
        pos_y = (self.root.winfo_screenheight() // 2) - (height // 2)
        self.root.geometry(f'{width}x{height}+{pos_x}+{pos_y}')
    
    def validate_ip(self, ip):
        """Validate IP address format"""
        pattern = r'^(\d{1,3}\.){3}\d{1,3}$'
        if not re.match(pattern, ip):
            return False
        
        parts = ip.split('.')
        return all(0 <= int(part) <= 255 for part in parts)
    
    def load_config(self):
        """Load saved configuration"""
        try:
            if os.path.exists(self.config_file):
                with open(self.config_file, 'r') as f:
                    config = json.load(f)
                    return config.get('ip_address', '192.168.1.100')
        except Exception:
            pass
        return '192.168.1.100'
    
    def save_config(self):
        """Save current configuration"""
        try:
            config = {'ip_address': self.ip_address}
            with open(self.config_file, 'w') as f:
                json.dump(config, f)
        except Exception:
            pass
    
    def update_status(self, message, is_error=False):
        """Update status message"""
        self.status_label.configure(text=message)
        if is_error:
            self.status_label.configure(foreground='#f56565')
        else:
            self.status_label.configure(foreground='#48bb78')
        self.root.update()
    
    def send_request(self, endpoint, action_name):
        """Send HTTP request to Android device"""
        ip = self.ip_entry.get().strip()
        
        if not ip:
            self.update_status("❌ Please enter an IP address", True)
            return
        
        if not self.validate_ip(ip):
            self.update_status("❌ Invalid IP address format", True)
            return
        
        # Save IP address
        self.ip_address = ip
        self.save_config()
        
        # Show loading status
        self.update_status(f"🔄 {action_name}...")
        
        # Disable buttons during request
        self.cast_btn.configure(state='disabled')
        self.stop_btn.configure(state='disabled')
        
        def make_request():
            try:
                url = f"http://{ip}:8080{endpoint}"
                response = requests.post(url, timeout=10)
                
                if response.status_code == 200:
                    self.root.after(0, lambda: self.update_status(f"✅ {action_name} successful!"))
                else:
                    self.root.after(0, lambda: self.update_status(f"❌ {action_name} failed (HTTP {response.status_code})", True))
            
            except requests.exceptions.Timeout:
                self.root.after(0, lambda: self.update_status("❌ Request timeout - check device connection", True))
            except requests.exceptions.ConnectionError:
                self.root.after(0, lambda: self.update_status("❌ Cannot connect to device - check IP and network", True))
            except Exception as e:
                self.root.after(0, lambda: self.update_status(f"❌ Request failed: {str(e)}", True))
            finally:
                # Re-enable buttons
                self.root.after(0, lambda: self.cast_btn.configure(state='normal'))
                self.root.after(0, lambda: self.stop_btn.configure(state='normal'))
        
        # Run request in background thread
        Thread(target=make_request, daemon=True).start()
    
    def cast_desktop(self):
        """Send cast request"""
        self.send_request('/cast', 'Desktop casting')
    
    def stop_casting(self):
        """Send stop request"""
        self.send_request('/stop', 'Stop casting')
    
    def open_sunshine_link(self):
        """Open Sunshine GitHub releases page"""
        webbrowser.open('https://github.com/LizardByte/Sunshine/releases')
    
    def run(self):
        """Start the application"""
        try:
            self.root.mainloop()
        except KeyboardInterrupt:
            pass

if __name__ == "__main__":
    app = MooncastController()
    app.run() 