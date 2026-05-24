package com.example.scamshield.util

import android.content.Context
import android.content.pm.PackageManager
import com.example.scamshield.util.logD

/**
 * Resolves Android package names to user-visible application labels.
 *
 * Results are cached in-memory for the process lifetime to avoid repeated
 * PackageManager calls on the hot path (each overlay show triggers one lookup).
 *
 * Thread-safe: the cache is guarded by @Synchronized since both
 * NotificationListenerService and AccessibilityService may resolve names
 * concurrently from their IO coroutine scopes.
 */
object AppNameResolver {

    private const val TAG = "AppNameResolver"

    private val cache = HashMap<String, String>()

    /**
     * Returns the user-visible label for [packageName] (e.g. "Telegram", "WhatsApp").
     *
     * Falls back to a capitalised last segment of the package name when the app
     * is not installed or the label cannot be read
     * (e.g. "org.telegram.messenger" → "Messenger").
     */
    @Synchronized
    fun resolve(context: Context, packageName: String): String {
        return cache.getOrPut(packageName) {
            try {
                val pm   = context.packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString().also {
                    logD(TAG, "Resolved $packageName → \"$it\"")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
                    .substringAfterLast('.')
                    .replaceFirstChar { it.uppercase() }
                    .also { logD(TAG, "Fallback name for $packageName → \"$it\"") }
            }
        }
    }
}
