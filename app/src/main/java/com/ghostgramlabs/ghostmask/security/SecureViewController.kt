package com.ghostgramlabs.ghostmask.security

import android.app.Activity
import android.view.WindowManager

class SecureViewController(private val activity: Activity) {

    fun setSecure(enabled: Boolean) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
