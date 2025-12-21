# Auto Light: Precision Brightness Control

Auto Light is a specialized replacement for the standard Android adaptive brightness feature. Designed for users who demand absolute control, this app provides a custom-tailored brightness curve based on your specific environment.

This is a power-user tool that may require additionally manual configuration to function effectively.

For a proper function, it requires to disable the built-in Android Adaptive Brightness feature manually before using Auto Light app.

# How It Works

The app utilizes a four-point non-linear mapping model to create a smooth brightness curve. If the predefined settings do not meet your requirements, it allows you to set intermediate points for four distinct ambient light levels and their corresponding intensities. The app then calculates the optimal intermediate values to ensure a smooth, predictable transition.

<div align="center">
<img src="https://github.com/The-First-King/Auto-Light/blob/master/metadata/en-US/images/screenshots/screenshot.png?raw=true" alt="App settings" />
</div>

The main intended working mode is “On screen unlock/rotate” where the app captures ambient light data only during specific triggers (like unlocking or rotating). This sets your brightness once, preventing the “flickering” or “hunting” common with standard auto-brightness.

The Auto Light app utilizes Android’s notification system to ensure stable background performance. Since the app does not send active alerts or messages, you may choose to disable notifications in your Android system settings without affecting core functionality. To do this within the app, tap the **three dots** in the top-right corner, go to **Settings**, and ensure **Notifications** is toggled off.

# Permissions & Transparency

To provide reliable, low-level system control, Auto Light requires the following permissions:

* Modify System Settings: Necessary to adjust the display brightness levels.

* Phone State: Allows the app to pause light sensor polling during incoming calls to prevent interference while the device is in your pocket.

* Notifications (Service Persistence): Used to maintain a foreground service, ensuring the Android system does not terminate the app during background tasks.

* Ignore Battery Optimizations: Prevents the system from suspending the service during periods of inactivity.

* Special Use Background Service: Required for the app to function as a persistent utility outside of standard categories.

* Run at Startup: Ensures your custom brightness profile is active immediately after a device reboot.

---
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
