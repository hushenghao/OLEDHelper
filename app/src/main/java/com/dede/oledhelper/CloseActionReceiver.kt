package com.dede.oledhelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by hsh on 2018/10/11 10:05 AM
 */

class CloseActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CloseActionReceiver"
        const val ACTION_CLOSE = "com.dede.oledhelper.ACTION_CLOSE"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive: ")
        if (intent?.action != ACTION_CLOSE) return

        val service = Intent(context, OLEDService::class.java)
        context?.stopService(service)
    }
}