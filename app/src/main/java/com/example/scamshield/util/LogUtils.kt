package com.example.scamshield.util

import android.util.Log
import com.example.scamshield.BuildConfig

fun logD(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.d(tag, msg)
}

fun logV(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.v(tag, msg)
}
