package com.example.scamshield.service

import android.accessibilityservice.AccessibilityService
import com.example.scamshield.util.logD
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scamshield.data.MonitoringStore
import com.example.scamshield.data.MonitorSource
import com.example.scamshield.overlay.OverlayManager
import com.example.scamshield.util.AppNameResolver

class ScamShieldAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "ScamShieldA11y"

        private const val MAX_NODE_DEPTH = 12
        private const val MAX_TEXT_LEN   = 600

        val MONITORED_PACKAGES = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
        )
    }

    private val base = object : BaseMessageAnalysisService() {
        override val monitorSource = MonitorSource.ACCESSIBILITY_SERVICE
        override val appContext    get() = applicationContext
        override val tag           = TAG
    }

    override fun onServiceConnected() {
        logD(TAG, "Connected — monitoring ${MONITORED_PACKAGES.size} packages")
        MonitoringStore.setServiceActive(MonitorSource.ACCESSIBILITY_SERVICE, true)
    }

    override fun onInterrupt() {
        logD(TAG, "Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        base.destroy()
        MonitoringStore.clearLiveEvent()
        MonitoringStore.setServiceActive(MonitorSource.ACCESSIBILITY_SERVICE, false)
        OverlayManager.dismiss()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (pkg !in MONITORED_PACKAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                handleWindowEvent(event, pkg)

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                handleNotificationEvent(event, pkg)
        }
    }

    private fun handleWindowEvent(event: AccessibilityEvent, pkg: String) {
        val rootNode = event.source
        if (rootNode != null) {
            try {
                val texts = mutableListOf<String>()
                collectTexts(rootNode, texts, depth = 0)
                val combined = texts.joinToString(" | ").take(MAX_TEXT_LEN).trim()
                base.analyzeAndReport(combined, pkg, AppNameResolver.resolve(applicationContext, pkg))
            } finally {
                // recycle() is a no-op on API 33+ but required on older APIs to
                // release the native AccessibilityNodeInfo reference.
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }
        } else {
            val text = event.text.joinToString(" ").trim().take(MAX_TEXT_LEN)
            base.analyzeAndReport(text, pkg, AppNameResolver.resolve(applicationContext, pkg))
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent, pkg: String) {
        val text = event.text.joinToString(" ").trim().take(MAX_TEXT_LEN)
        base.analyzeAndReport(text, pkg, AppNameResolver.resolve(applicationContext, pkg))
    }

    private fun collectTexts(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        depth: Int,
    ) {
        if (depth > MAX_NODE_DEPTH) return

        val text        = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()

        when {
            !text.isNullOrEmpty()        -> texts.add(text)
            !contentDesc.isNullOrEmpty() -> texts.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectTexts(child, texts, depth + 1)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }
}
