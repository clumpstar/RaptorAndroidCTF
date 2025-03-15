package com.example.bdwisher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        Log.d("WhatsAppAutoSend", "Event detected from: $packageName")

        if (packageName == "com.whatsapp") {
            Log.d("WhatsAppAutoSend", "WhatsApp screen detected")

            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val sendButton = findSendButton(rootNode)
                if (sendButton != null) {
                    Log.d("WhatsAppAutoSend", "Send button found")
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("WhatsAppAutoSend", "Send button clicked")

                    // Wait a bit to ensure the message is sent before navigating back
                    Handler(Looper.getMainLooper()).postDelayed({
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        Log.d("WhatsAppAutoSend", "Navigated back to the app")
                    }, 2000) // Delay for 2 seconds (adjust if necessary)
                } else {
                    Log.d("WhatsAppAutoSend", "Send button NOT found")
                }
            } else {
                Log.d("WhatsAppAutoSend", "Root node is NULL")
            }
        }
    }

    override fun onInterrupt() {
        Log.d("WhatsAppAutoSend", "Service Interrupted")
    }

    private fun findSendButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue

            // Check if the button has "Send" text or is an ImageButton
            if ((child.className == "android.widget.ImageButton" || child.className == "android.widget.Button") &&
                (child.contentDescription?.contains("Send", true) == true)) {
                return child
            }

            // Recursively check children
            val found = findSendButton(child)
            if (found != null) return found
        }
        return null
    }
}
