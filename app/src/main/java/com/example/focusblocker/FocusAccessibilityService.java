package com.example.focusblocker;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

public class FocusAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageNameSeq = event.getPackageName();
            if (packageNameSeq == null) return;
            String packageName = packageNameSeq.toString();

            // Skip your own app, launchers, and system UI
            if (packageName.equals(getApplicationContext().getPackageName()) || 
                packageName.contains("launcher") || 
                packageName.contains("systemui")) {
                return;
            }

            // Fast check via SharedPreferences
            SharedPreferences prefs = getSharedPreferences("FocusBlockerPrefs", Context.MODE_PRIVATE);
            boolean isBlocked = prefs.getBoolean(packageName, false);

            if (isBlocked) {
                // Instantly force user back to home screen
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
