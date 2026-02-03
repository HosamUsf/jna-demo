# Windows Event Monitor with JNA

A Java application that monitors Windows system events such as lock/unlock, sleep/resume, and shutdown/restart using Java Native Access (JNA).

## Overview

This project demonstrates how to use JNA (Java Native Access) to interact with Windows API and monitor system-level events without requiring JNI (Java Native Interface) code. The application creates an invisible Windows message window to receive system notifications and provides a clean event-driven API for handling these events.

## Features

- **Session Lock/Unlock Detection**: Monitors when the user locks (Win+L) or unlocks the workstation
- **Power Management Events**: Detects when the system goes to sleep/hibernate or resumes
- **Shutdown/Restart Detection**: Captures shutdown and restart events
- **Event Logging**: Automatically logs all events to a file with timestamps
- **Thread-Safe**: Uses proper synchronization for multi-threaded environments
- **Singleton Pattern**: Ensures only one monitor instance is active
- **Graceful Cleanup**: Properly unregisters and cleans up Windows resources

## Requirements

- **Operating System**: Windows (any version supporting WTS and Power Management APIs)
- **Java**: JDK 8 or higher (configured for Java 17 in properties, compiled to Java 8)
- **Build Tool**: Maven 3.x
- **Dependencies**: 
  - JNA Platform 5.14.0

## Project Structure

```
jna-demo/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
├── windows-events.log               # Event log file (created at runtime)
└── src/
    └── main/
        └── java/
            └── com/
                └── hosam/
                    ├── Main.java                    # Application entry point
                    └── WindowsEventMonitor.java     # Core monitoring implementation
```

## Installation

1. **Clone or download the project**:
   ```powershell
   git clone <repository-url>
   cd jna-demo
   ```

2. **Build the project**:
   ```powershell
   mvn clean package
   ```

   This will:
   - Compile the Java source code
   - Download dependencies (JNA Platform)
   - Create an executable JAR with dependencies in the `target/` directory

## Usage

### Running the Application

#### Option 1: Using Maven
```powershell
mvn clean compile exec:java -Dexec.mainClass="com.hosam.Main"
```

#### Option 2: Using Compiled Classes
```powershell
mvn clean compile
java -cp target/classes;%USERPROFILE%\.m2\repository\net\java\dev\jna\jna-platform\5.14.0\jna-platform-5.14.0.jar;%USERPROFILE%\.m2\repository\net\java\dev\jna\jna\5.14.0\jna-5.14.0.jar com.hosam.Main
```

#### Option 3: Using the Packaged JAR
```powershell
mvn clean package
java -jar target/jna-demo-1.0-SNAPSHOT.jar
```

### Testing the Monitor

Once the application is running, you can test it by:

1. **Lock Event**: Press `Win+L` to lock your PC
2. **Unlock Event**: Unlock your PC with your password/PIN
3. **Sleep Event**: Put your system to sleep (Start Menu → Power → Sleep)
4. **Resume Event**: Wake your system (press power button or keyboard)
5. **Shutdown Event**: Shutdown or restart your system
   - After restart, check `windows-events.log` to verify the event was captured

### Example Output

```
Windows Event Monitor - Started
Monitoring: Lock, Unlock, Sleep, Resume, Shutdown/Restart
Events will be logged to: windows-events.log

Monitor is now active. Try the following actions:
  - Lock your pc (Win+L)
  - Put system to sleep
  - Hibernate the system
  - Shutdown/Restart (check windows-events.log after restart)

Press Ctrl+C to stop monitoring...

>>> EVENT DETECTED: PC Locked
>>> EVENT DETECTED: PC Unlocked
>>> EVENT DETECTED: System Going to Sleep/Hibernate
>>> EVENT DETECTED: System Resumed from Sleep/Hibernate
```

## Code Architecture

### WindowsEventMonitor

The core class that handles Windows API interactions:

- **Singleton Pattern**: `getInstance()` ensures only one monitor exists
- **Message Loop**: Creates a hidden window to receive system messages
- **Event Types**: Monitors three categories of Windows messages:
  - `WM_WTSSESSION_CHANGE`: Session lock/unlock events
  - `WM_POWERBROADCAST`: Power management events (sleep/resume)
  - `WM_QUERYENDSESSION`/`WM_ENDSESSION`: Shutdown/restart events
- **Listener Pattern**: Supports multiple event listeners via callback interface

### WindowsEvent Enum

Available event types:
- `LOCK`: Workstation was locked
- `UNLOCK`: Workstation was unlocked
- `SLEEP`: System is entering sleep/hibernate mode
- `RESUME`: System resumed from sleep/hibernate
- `SHUTDOWN`: System is shutting down or restarting

### Main Class

Demonstrates usage of the monitor:
- Registers an event listener
- Logs events to console and file
- Provides placeholders for custom logic
- Handles graceful shutdown

## Customization

To add your own event handling logic, modify the event listener in `Main.java`:

```java
monitor.addListener((event, sessionId) -> {
    switch (event) {
        case LOCK:
            // Your custom logic when PC is locked
            // Example: pause background tasks, lock sensitive operations
            break;
        
        case UNLOCK:
            // Your custom logic when PC is unlocked
            // Example: resume background tasks
            break;
        
        case SLEEP:
            // Your custom logic before sleep
            // Example: save state, close connections, pause downloads
            break;
        
        case RESUME:
            // Your custom logic after resume
            // Example: restore state, reconnect, resume downloads
            break;
        
        case SHUTDOWN:
            // Your custom logic before shutdown
            // Example: final cleanup, save data urgently
            break;
    }
});
```

## Technical Details

### Windows APIs Used

1. **WTSRegisterSessionNotification**: Registers the window to receive session change notifications
2. **RegisterClassEx**: Registers a window class for the message window
3. **CreateWindowEx**: Creates the hidden message window
4. **GetMessage/DispatchMessage**: Message loop for receiving Windows events
5. **WTSUnRegisterSessionNotification**: Cleanup when stopping

### Thread Safety

- Listener list uses synchronized blocks for thread-safe access
- Message loop runs on a dedicated daemon thread
- Proper cleanup in finally blocks ensures resources are released

### Known Limitations

- **Windows-Only**: This application only works on Windows due to dependency on Windows API
- **Shutdown Timing**: Shutdown events have limited time (a few seconds) before Windows terminates the process
- **No Restart Distinction**: Windows API doesn't distinguish between shutdown and restart

## Troubleshooting

### Application doesn't detect events

1. **Run as Administrator**: Some events may require elevated privileges
   ```powershell
   # Run PowerShell as Administrator, then:
   java -jar target/jna-demo-1.0-SNAPSHOT.jar
   ```

2. **Check Windows Version**: Ensure you're running on Windows (not Linux/Mac)

3. **Verify JNA Version**: Ensure JNA Platform 5.14.0 is properly downloaded

### Build Failures

```powershell
# Clean Maven cache and rebuild
mvn clean install -U
```

### Events not logged to file

- Verify write permissions in the application directory
- Check if `windows-events.log` can be created/modified

## Dependencies

- **JNA Platform 5.14.0**: Provides access to Windows API
  - Includes `jna-platform` which extends `jna` with platform-specific utilities
  - Automatically handles native library loading

## License

This project is provided as-is for educational and demonstration purposes.

## Contributing

Feel free to enhance this project by:
- Adding more Windows event types (logoff, user switch, etc.)
- Implementing a GUI for event monitoring
- Adding configuration file support
- Creating unit tests (where possible)
- Improving error handling and logging

## Author

Created as a demonstration of JNA usage for Windows system event monitoring.

## Additional Resources

- [JNA Documentation](https://github.com/java-native-access/jna)
- [Windows API Reference](https://docs.microsoft.com/en-us/windows/win32/api/)
- [WTS Session Notifications](https://docs.microsoft.com/en-us/windows/win32/termserv/wm-wtssession-change)
- [Power Management Events](https://docs.microsoft.com/en-us/windows/win32/power/wm-powerbroadcast)
