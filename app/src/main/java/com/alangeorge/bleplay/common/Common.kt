package com.alangeorge.bleplay.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") {
        String.format("%02X", it)
    }

fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}