package com.dede.oledhelper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log


class OLEDService : Service() {

    private val logTag = if (BuildConfig.DEBUG) "OLEDService" else ""

    companion object {
        const val DEFAULT_ALPHA = .3f
        const val MAX_ALPHA = .8f
    }


    override fun onBind(intent: Intent?): IBinder {
        Log.i(logTag, "onBind: ")
        return iBinder
    }

    private lateinit var iBinder: OLEDController

    override fun onCreate() {
        iBinder = OLEDController(this)
    }

    override fun onRebind(intent: Intent?) {
        Log.i(logTag, "onRebind: ")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(logTag, "onUnbind: ")
        saveAlpha(iBinder.getAlpha())
        if (!iBinder.isShow()) {
            stopSelf()
            return false
        }
        return true
    }

    override fun onDestroy() {
        iBinder.close()
        Log.i(logTag, "onDestroy: ")
    }

}