package com.example.focusblocker;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class FocusAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // App detection logic runs here
    }

    @Override
    public void onInterrupt() {
    }
}
