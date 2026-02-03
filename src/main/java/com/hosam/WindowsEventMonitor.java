package com.hosam;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;

import java.util.ArrayList;
import java.util.List;

public class WindowsEventMonitor {

    private static final int WM_WTSSESSION_CHANGE = 0x02B1;
    private static final int WM_QUERYENDSESSION = 0x0011;
    private static final int WM_ENDSESSION = 0x0016;
    private static final int WM_POWERBROADCAST = 0x0218;
    private static final int PBT_APMSUSPEND = 0x0004;
    private static final int PBT_APMRESUMESUSPEND = 0x0007;
    private static final int PBT_APMRESUMEAUTOMATIC = 0x0012;
    private static final int ENDSESSION_LOGOFF = 0x80000000;

    private static WindowsEventMonitor instance;
    private final List<WindowsEventListener> listeners;
    private HWND messageWindow;
    private Thread messageLoopThread;
    private volatile boolean running = false;

    private WindowsEventMonitor() {
        this.listeners = new ArrayList<>();
    }

    public static synchronized WindowsEventMonitor getInstance() {
        if (instance == null) {
            instance = new WindowsEventMonitor();
        }
        return instance;
    }

    public void addListener(WindowsEventListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        messageLoopThread = new Thread(this::runMessageLoop, "WindowsEventMonitor");
        messageLoopThread.setDaemon(false);
        messageLoopThread.start();
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;

        if (messageWindow != null) {
            User32.INSTANCE.PostMessage(messageWindow, WinUser.WM_QUIT, new WPARAM(0), new LPARAM(0));
        }

        if (messageLoopThread != null) {
            try {
                messageLoopThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runMessageLoop() {
        try {
            HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle("");
            String windowClassName = "WindowsEventMonitorClass_" + System.currentTimeMillis();

            WNDCLASSEX wClass = new WNDCLASSEX();
            wClass.hInstance = hInst;
            wClass.lpfnWndProc = new WindowMessageProcessor();
            wClass.lpszClassName = windowClassName;

            if (User32.INSTANCE.RegisterClassEx(wClass).intValue() == 0) {
                running = false;
                return;
            }

            messageWindow = User32.INSTANCE.CreateWindowEx(
                    0, windowClassName, "Windows Event Monitor", 0,
                    0, 0, 0, 0, null, null, hInst, null
            );

            if (messageWindow == null) {
                running = false;
                return;
            }

            Wtsapi32.INSTANCE.WTSRegisterSessionNotification(
                    messageWindow, Wtsapi32.NOTIFY_FOR_THIS_SESSION);

            MSG msg = new MSG();
            int result;

            while (running && (result = User32.INSTANCE.GetMessage(msg, null, 0, 0)) != 0) {
                if (result == -1) {
                    break;
                }
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }

            if (messageWindow != null) {
                Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(messageWindow);
                User32.INSTANCE.DestroyWindow(messageWindow);
            }

            User32.INSTANCE.UnregisterClass(windowClassName, hInst);

        } catch (Exception e) {
            // Ignore exceptions during cleanup
        } finally {
            running = false;
        }
    }

    private class WindowMessageProcessor implements WindowProc {
        @Override
        public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
            try {
                switch (uMsg) {
                    case WM_WTSSESSION_CHANGE:
                        handleSessionChange(wParam.intValue(), lParam.intValue());
                        return new LRESULT(0);

                    case WM_POWERBROADCAST:
                        handlePowerEvent(wParam.intValue());
                        return new LRESULT(1);

                    case WM_QUERYENDSESSION:
                    case WM_ENDSESSION:
                        handleShutdown(lParam.intValue());
                        return new LRESULT(1);

                    case WinUser.WM_DESTROY:
                        User32.INSTANCE.PostQuitMessage(0);
                        return new LRESULT(0);

                    default:
                        return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
                }
            } catch (Exception e) {
                return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
        }
    }

    private void handleSessionChange(int eventType, int sessionId) {
        WindowsEvent event = null;

        if (eventType == Wtsapi32.WTS_SESSION_LOCK) {
            event = WindowsEvent.LOCK;
        } else if (eventType == Wtsapi32.WTS_SESSION_UNLOCK) {
            event = WindowsEvent.UNLOCK;
        }

        if (event != null) {
            notifyListeners(event, sessionId);
        }
    }

    private void handlePowerEvent(int eventType) {
        WindowsEvent event = null;

        if (eventType == PBT_APMSUSPEND) {
            event = WindowsEvent.SLEEP;
        } else if (eventType == PBT_APMRESUMESUSPEND || eventType == PBT_APMRESUMEAUTOMATIC) {
            event = WindowsEvent.RESUME;
        }

        if (event != null) {
            notifyListeners(event, 0);
        }
    }

    private void handleShutdown(int flags) {
        // Windows doesn't distinguish between shutdown and restart
        // This event covers both cases
        if ((flags & ENDSESSION_LOGOFF) == 0) {
            notifyListeners(WindowsEvent.SHUTDOWN, 0);
        }
    }

    private void notifyListeners(WindowsEvent event, int sessionId) {
        synchronized (listeners) {
            for (WindowsEventListener listener : listeners) {
                try {
                    listener.onWindowsEvent(event, sessionId);
                } catch (Exception e) {
                    // Ignore listener errors
                }
            }
        }
    }

    public enum WindowsEvent {
        LOCK,
        UNLOCK,
        SLEEP,
        RESUME,
        SHUTDOWN  // Covers both shutdown and restart (Windows doesn't distinguish)
    }

    public interface WindowsEventListener {
        void onWindowsEvent(WindowsEvent event, int sessionId);
    }
}