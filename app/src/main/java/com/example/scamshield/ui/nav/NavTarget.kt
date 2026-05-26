package com.example.scamshield.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Single-shot navigation targets set by [com.example.scamshield.MainActivity]
 * when the app is opened via a notification action button.
 *
 * [outer] targets routes in ScamShieldNavHost (e.g. BlockedNumbers).
 * [inner] targets tabs inside MainShell (e.g. History).
 */
object NavTarget {
    val outer = MutableStateFlow<String?>(null)
    val inner = MutableStateFlow<String?>(null)
}
