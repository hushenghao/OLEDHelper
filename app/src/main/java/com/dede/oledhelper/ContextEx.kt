package com.dede.oledhelper

import android.content.Context
import android.app.AppOpsManager
import android.os.Build
import android.provider.Settings


/**
 * Check if application can draw over other apps
 * @return Boolean
 */
fun Context.canDrawOverlays(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            // Sometimes Settings.canDrawOverlays returns false after allowing permission.
            // Google Issue Tracker: https://issuetracker.google.com/issues/66072795
            val appOpsMgr = this.getSystemService(AppOpsManager::class.java)
            return if (appOpsMgr != null) {
                val mode = appOpsMgr.checkOpNoThrow(
                        "android:system_alert_window",
                        android.os.Process.myUid(),
                        this.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED
            } else {
                false
            }
        }
        return Settings.canDrawOverlays(this)
    }
    return true
}