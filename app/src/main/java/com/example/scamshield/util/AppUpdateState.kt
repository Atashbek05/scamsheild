package com.example.scamshield.util

import kotlinx.coroutines.flow.MutableStateFlow

object AppUpdateState {
    val showFlexibleSnackbar = MutableStateFlow(false)
    var onStartFlexibleInstall: (() -> Unit)? = null
}
