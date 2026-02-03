package com.hosam;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static final String LOG_FILE = "windows-events.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {

        System.out.println("Windows Event Monitor - Started");
        System.out.println("Monitoring: Lock, Unlock, Sleep, Resume, Shutdown/Restart");
        System.out.println("Events will be logged to: " + LOG_FILE);

        // Get monitor instance
        WindowsEventMonitor monitor = WindowsEventMonitor.getInstance();

        // Add event listener
        monitor.addListener((event, sessionId) -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String message = "";

            switch (event) {
                case LOCK:
                    message = "PC Locked";
                    System.out.println(">>> EVENT DETECTED: " + message);
                    // Add your custom logic here
                    break;

                case UNLOCK:
                    message = "PC Unlocked";
                    System.out.println(">>> EVENT DETECTED: " + message);
                    // Add your custom logic here
                    break;

                case SLEEP:
                    message = "System Going to Sleep/Hibernate";
                    System.out.println(">>> EVENT DETECTED: " + message);
                    // Add your custom logic here - save state, close connections, etc.
                    break;

                case RESUME:
                    message = "System Resumed from Sleep/Hibernate";
                    System.out.println(">>> EVENT DETECTED: " + message);
                    // Add your custom logic here - restore state, reconnect, etc.
                    break;

                case SHUTDOWN:
                    message = "System Shutting Down or Restarting";
                    System.out.println(">>> EVENT DETECTED: " + message);
                    // Add your custom logic here - final cleanup, save data, etc.
                    break;
            }

            // Log to file for testing (especially useful for shutdown/restart)
            logToFile(timestamp, event.name(), message);
        });

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping monitor...");
            logToFile(LocalDateTime.now().format(formatter), "SHUTDOWN_HOOK", "Application shutting down");
            monitor.stop();
        }));

        // Start monitoring
        monitor.start();

        System.out.println("\nMonitor is now active. Try the following actions:");
        System.out.println("  - Lock your pc (Win+L)");
        System.out.println("  - Put system to sleep");
        System.out.println("  - Hibernate the system");
        System.out.println("  - Shutdown/Restart (check " + LOG_FILE + " after restart)");
        System.out.println("\nPress Ctrl+C to stop monitoring...\n");

        // Keep main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\nMonitor stopped.");
        }
    }

    private static void logToFile(String timestamp, String eventType, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(timestamp + " | " + eventType + " | " + message);
            writer.flush();
        } catch (Exception e) {
            // Ignore logging errors
        }
    }
}
