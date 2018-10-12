package com.dede.oledhelper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * Created by hsh on 2018/6/19.
 */

fun Context.sp(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
}

fun Context.loadAlpha(): Float {
    var alpha = this.sp().getFloat(OLEDController.KEY_ALPHA, OLEDService.DEFAULT_ALPHA)
    if (alpha > OLEDService.MAX_ALPHA) {
        alpha = OLEDService.MAX_ALPHA
        this.sp().edit().putFloat(OLEDController.KEY_ALPHA, alpha).apply()
    }
    return alpha
}

fun Context.saveAlpha(alpha: Float) {
    var a = alpha
    if (a > OLEDService.MAX_ALPHA) {
        a = OLEDService.MAX_ALPHA
    }
    this.sp().edit().putFloat(OLEDController.KEY_ALPHA, a).apply()
}

object Config {

}