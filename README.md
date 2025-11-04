# Reclaim - Mindful App Usage

Reclaim helps promote mindful app usage by adding a customizable delay when opening selected apps. This creates a moment of pause before accessing potentially distracting applications.

## Features

✅ **Select Apps to Monitor**: Choose which apps you want to add a delay to  
✅ **Customizable Delay**: Set different delay times (1-300 seconds) for each app (default: 45 seconds)  
✅ **Beautiful Overlay**: Shows an animated countdown screen before allowing app access  
✅ **Easy Management**: Add, remove, and configure monitored apps from settings  
✅ **Smooth Animation**: Progress bar animates from bottom to top during the delay  

## How It Works

1. **Select Apps**: Tap the + FAB button to see all installed apps
2. **Choose & Save**: Select apps you want to monitor and save
3. **Enable Services**: Grant required permissions (overlay and accessibility)
4. **Automatic Monitoring**: When you open a monitored app, a delay screen appears
5. **Wait & Reflect**: Watch the countdown and reflect on your usage
6. **Access App**: After the delay completes, the app opens normally

## Permissions Required

### 1. Draw Over Other Apps (Overlay Permission)
- **Purpose**: Display the delay screen over other apps
- **Requested**: On first FAB click
- **Grant**: Settings → Apps → Reclaim → Display over other apps

### 2. Accessibility Service
- **Purpose**: Detect when monitored apps are opened
- **Requested**: On first FAB click
- **Grant**: Settings → Accessibility → Reclaim → Enable

### 3. Query All Packages
- **Purpose**: List all installed apps for selection
- **Granted**: Automatically via manifest

## Usage Guide

### Adding Apps to Monitor

1. Open Reclaim
2. Tap the **+** (FAB) button
3. Grant permissions if prompted
4. Select apps from the list
5. Tap **Save**

### Managing Monitored Apps

1. Tap the **Settings** menu (three dots)
2. View all monitored apps
3. **Edit Delay**: Tap "Edit" to change delay time
4. **Remove App**: Tap the delete icon to stop monitoring

### Changing Delay Time

1. Go to Settings
2. Find the app you want to modify
3. Tap **Edit**
4. Enter new delay time (1-300 seconds)
5. Tap **Save**

## Architecture

### Components

- **MainActivity**: Main entry point with FAB and permission checks
- **AppListActivity**: Displays all installed apps for selection
- **SettingsActivity**: Manage monitored apps and delays
- **AppMonitoringService**: Accessibility service that detects app launches
- **OverlayService**: Shows the delay overlay with animation
- **AppRepository**: Manages data storage using SharedPreferences

### Data Flow

```
User opens monitored app
    ↓
AccessibilityService detects launch
    ↓
Checks if app is in monitored list
    ↓
Starts OverlayService
    ↓
Shows fullscreen overlay with countdown
    ↓
After delay completes → Dismisses overlay
    ↓
User enters the app
```

## Technical Details

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Language**: Java
- **Storage**: SharedPreferences with Gson
- **UI**: Material Design 3 components
- **Architecture**: Service-based with Repository pattern

## Building the App

1. Open project in Android Studio
2. Sync Gradle
3. Build and run on device (emulator may have limited accessibility support)
4. Grant all required permissions
5. Test with any app

## Important Notes

⚠️ **Test on Real Device**: Accessibility services work best on physical devices  
⚠️ **System Apps**: By default, system apps are filtered out (can be modified in `AppListActivity.java`)  
⚠️ **Battery Optimization**: Android may kill the service; request exemption if needed  
⚠️ **No Bypass**: The overlay is designed to be strict with no skip button  

## Troubleshooting

### Overlay Not Showing

**Step 1: Verify Permissions**

1. Open Reclaim app
2. Check if you see "Please enable required permissions" message
3. If yes, follow the prompts to grant both permissions

**Step 2: Check Accessibility Service**

1. Go to Android Settings → Accessibility
2. Find "Reclaim" in the list
3. Make sure it's **turned ON**
4. If it was off, turn it ON and try again

**Step 3: Check Overlay Permission**

1. Go to Android Settings → Apps → Reclaim
2. Tap "Display over other apps" or "Appear on top"
3. Make sure permission is **allowed**

**Step 4: Check Monitored Apps**

1. Open Reclaim
2. Verify your apps are listed on the main screen
3. If not, tap + button and select apps again

**Step 5: View Debug Logs**

1. Connect device to computer
2. Open Android Studio → Logcat
3. Filter by "AppMonitoringService" and "OverlayService"
4. Open a monitored app and watch the logs:
   - Should see: "App opened: com.instagram.android"
   - Should see: "Showing overlay for: Instagram"
   - Should see: "Overlay view added to window"

**Step 6: Test with Different Apps**

Try monitoring a simple app like Chrome first to verify it works.

**Step 7: Restart Accessibility Service**

1. Go to Settings → Accessibility → Reclaim
2. Turn OFF
3. Wait 5 seconds
4. Turn ON
5. Try opening monitored app again

### Service Stops Working

1. Check battery optimization settings
2. Disable battery optimization for Reclaim
3. Re-enable accessibility service
4. Restart device

### Apps Not Listed

The app now shows all launchable apps. If some apps are still missing, they might not have a launcher icon or are hidden by the system.

### Common Issues

**"Reclaim has stopped"**
- Check Android version compatibility (requires Android 7.0+)
- Review error logs in Logcat

**Overlay shows but I can still use the app**
- This is fixed in the latest version
- The overlay should now block all touches

**Overlay doesn't cover status bar**
- This is normal Android behavior for security
- The overlay covers the app area only

## Customization

### Change Default Delay

Edit `MonitoredApp.java` line 10:
```java
this.delaySeconds = 45; // Change to your preferred default
```

### Change Animation Duration

Edit `OverlayService.java` to modify animation behavior

### Change Overlay Design

Edit `app/src/main/res/layout/activity_overlay.xml`

## Future Enhancements

- [ ] Usage statistics and analytics
- [ ] Daily/weekly usage reports
- [ ] Schedule-based monitoring (e.g., only during work hours)
- [ ] Custom messages per app
- [ ] Breathing exercises during delay
- [ ] Dark mode support

## License

This project is open source. Feel free to modify and use as needed.

## Credits

Built with ❤️ for mindful digital wellbeing

