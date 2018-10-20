package com.dede.oledhelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by hsh on 2018/10/11 10:05 AM
 */

class CloseActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLOSE = "com.dede.oledhelper.ACTION_CLOSE"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_CLOSE) return

        val service = Intent(context, OLEDService::class.java)
        service.putExtra(EXTRA_IS_SHOW, false)
        context?.startService(service)// 调用service的onStartCommand方法
    }
}