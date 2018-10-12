package com.dede.oledhelper

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlin.properties.Delegates


class OLEDService : Service() {

    companion object {
        private const val TAG = "OLEDService"

        const val DEFAULT_ALPHA = .3f
        const val MAX_ALPHA = .8f

        private const val SERVICE_ID = 0xFF00
        private const val NOTIFY_CHANNEL_ID = "OLEDService"
        private const val NOTIFY_CATEGORY = "OLEDService"
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind: ")
        return iBinder
    }

    private val iBinder = object : IOLED.Stub() {

        override fun toggle() {
            controller.toggle()
        }

        override fun show() {
            controller.show()
        }

        override fun dismiss() {
            controller.dismiss()
        }

        override fun getAlpha(): Float {
            return controller.getAlpha()
        }

        override fun updateAlpha(alpha: Float) {
            controller.updateAlpha(alpha)
        }

        override fun isShow(): Boolean {
            return controller.isShow()
        }
    }

    private var controller by Delegates.notNull<OLEDController>()

    private val screenOpenReceiver = object : BroadcastReceiver() {
        private var isShowOnScreenClosed = false// 关闭屏幕时的显示状态
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isShowOnScreenClosed = controller.isShow()
                    if (isShowOnScreenClosed) {
                        controller.dismiss()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (isShowOnScreenClosed) {
                        controller.show()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate: ")
        controller = OLEDController(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOpenReceiver, intentFilter)
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "onRebind: ")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind: ")
        saveAlpha(iBinder.alpha)
        if (!iBinder.isShow) {
            stopSelf()
            return false
        }
        startForeground(SERVICE_ID, createNotify())
        return true
    }

    private fun createNotify(): Notification {
        var intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        var pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                    NOTIFY_CHANNEL_ID,
                    getString(R.string.notify_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
            Notification.Builder(this, NOTIFY_CHANNEL_ID)
                    .setBadgeIconType(Notification.BADGE_ICON_SMALL)
        } else {
            Notification.Builder(this)
                    .setPriority(Notification.PRIORITY_LOW)
        }.setSmallIcon(R.drawable.ic_tile)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setNumber(0)
                .setContentText(getString(R.string.notify_click))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NOTIFY_CATEGORY)
        }

        intent = Intent(CloseActionReceiver.ACTION_CLOSE)
        intent.setPackage(packageName)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val action = Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_tile),
                    getString(R.string.notify_action_close),
                    pendingIntent
            )
                    .build()
            builder.addAction(action)
        } else {
            builder.addAction(R.drawable.ic_tile, getString(R.string.notify_action_close), pendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        iBinder.dismiss()
        stopForeground(true)
        unregisterReceiver(screenOpenReceiver)
        Log.i(TAG, "onDestroy: ")
    }

}