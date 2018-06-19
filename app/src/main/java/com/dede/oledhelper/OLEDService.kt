package com.dede.oledhelper

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat


class OLEDService : Service() {

    companion object {
        const val DEFAULT_ALPHA = .3f
        const val MAX_ALPHA = .8f
    }

    private val closeRec by lazy { CloseBroadcastReceiver() }

    override fun onBind(intent: Intent?): IBinder {
        return iBinder
    }

    private lateinit var iBinder: OLEDController

    override fun onCreate() {
        super.onCreate()
        iBinder = OLEDController(this)
    }

    override fun onRebind(intent: Intent?) {
        if (intent?.getBooleanExtra(TService.FROM_TILE, false) == false) {
            unregisterReceiver(closeRec)
            stopForeground(true)
        }
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        saveAlpha(iBinder.getAlpha())
        if (!iBinder.isShow()) {
            stopSelf()
            return super.onUnbind(intent)
        }
        val builder = NotificationCompat.Builder(this, "OLED")
        val pendingIntent = PendingIntent
                .getActivity(this,
                        0,
                        Intent(this, MainActivity::class.java),
                        0
                )
        builder.setContentTitle(resources.getString(R.string.service_name))
                .setContentText("正在运行")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.priority = Notification.PRIORITY_MIN
        }
        val closeBroadcastIntent = PendingIntent
                .getBroadcast(
                        this,
                        1,
                        Intent(CloseBroadcastReceiver().action),
                        0
                )
        builder.addAction(
                NotificationCompat.Action.Builder(0, "关闭", closeBroadcastIntent)
                        .build()
        )
        val notification = builder.build()
        startForeground(3333, notification)

        val intentFilter = IntentFilter()
        intentFilter.addAction(CloseBroadcastReceiver().action)
        registerReceiver(closeRec, intentFilter)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        iBinder.close()
    }

    inner class CloseBroadcastReceiver : BroadcastReceiver() {

        val action = "com.dede.action.CLOSE"

        override fun onReceive(context: Context?, intent: Intent?) {
            this@OLEDService.stopSelf()
        }
    }

}