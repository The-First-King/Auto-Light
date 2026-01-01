# Auto Light: Precision Brightness Control

Auto Light is a specialized replacement for the standard Android adaptive brightness feature. Designed for users who demand absolute control, this app provides a custom-tailored brightness curve based on your specific environment.

This is a power-user tool that may require additionally manual configuration to function effectively.

For a proper function, it requires to disable the built-in Android Adaptive Brightness feature manually before using Auto Light app.

## How It Works

The app utilizes a four-point non-linear mapping model to create a smooth brightness curve. If the predefined settings do not meet your requirements, it allows you to set intermediate points for four distinct ambient light levels and their corresponding intensities. The app then calculates the optimal intermediate values to ensure a smooth, predictable transition.

<div align="center">
<img src="https://github.com/The-First-King/Auto-Light/blob/master/metadata/en-US/images/phoneScreenshots/01.png?raw=true" alt="App settings" width="405" />
</div>

The main intended working mode is “On screen unlock/rotate” where the app captures ambient light data only during specific triggers (like unlocking or rotating). This sets your brightness once, preventing the “flickering” or “hunting” common with standard auto-brightness.

The Auto Light app utilizes Android’s notification system to ensure stable background performance. Since the app does not send active alerts or messages, you may choose to disable notifications in your Android system settings without affecting core functionality. To do this within the app, tap the **three dots** in the top-right corner, go to **Settings**, and ensure **Notifications** is toggled off.

## Permissions & Privacy

To provide reliable system control without compromising privacy, Auto Light uses:

* Modify System Settings: Necessary to adjust the display brightness levels.
* Notifications (Service Persistence): Used to maintain a Foreground Service, ensuring the Android system does not terminate the app while it's monitoring light levels.
* Run at Startup: Ensures your custom brightness profile is active immediately after a device reboot.
* User Present & Configuration Change: Used strictly to trigger brightness adjustments when you unlock your phone or rotate the screen (No privacy-sensitive data is accessed).

**Note:** This app does NOT require Internet access, Location data, or Phone/Call logs, making it a privacy-respecting utility.

## Installation & License

<a href="https://github.com/The-First-King/Auto-Light/releases"><img src="images/GitHub.png" alt="Get it on GitHub" height="60"></a>
<a href="https://apt.izzysoft.de/packages/com.mine.autolight"><img src="images/IzzyOnDroid.png" alt="Get it at IzzyOnDroid" height="60"></a>

---

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

---
