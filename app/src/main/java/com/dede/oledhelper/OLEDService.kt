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


private val isMoreThanO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

class OLEDService : Service() {

    companion object {
        private const val TAG = "OLEDService"

        /**
         * 0 透明    1 不透明
         * @see android.view.WindowManager.LayoutParams.alpha
         */
        const val DEFAULT_ALPHA = .3f// 遮罩的默认不透明度
        const val MAX_ALPHA = .8f// 遮罩最大不透明度

        private const val NOTIFY_SERVICE_ID = 0xFF00
        const val NOTIFY_CHANNEL_ID = "OLEDService"
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
        private val spKey = "status_on_screen_screen"
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    val showStatusOnScreenClosed = controller.isShow()// 关闭屏幕时的显示状态
                    // 有些设备深度休眠时会停止当前Service，唤醒时再恢复Service。
                    sp().edit().putBoolean(spKey, showStatusOnScreenClosed).apply()
                    if (showStatusOnScreenClosed) {
                        controller.dismiss()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    val showStatusOnScreenClosed = sp().getBoolean(spKey, false)
                    if (showStatusOnScreenClosed) {
                        controller.show()
                        sp().edit().putBoolean(spKey, false).apply()
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

        createNotifyChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val has = intent?.hasExtra(EXTRA_IS_SHOW) ?: false
        if (has) {
            val isShow = intent!!.getBooleanExtra(EXTRA_IS_SHOW, false)
            if (isShow) {
                iBinder.show()
            } else {
                iBinder.dismiss()
                stopForeground(true)// 取消通知
            }
        }
        return super.onStartCommand(intent, flags, startId)
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
        startForeground(NOTIFY_SERVICE_ID, createNotify())
        return true
    }

    private fun createNotifyChannel() {
        if (!isMoreThanO) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationChannel = notificationManager.getNotificationChannel(NOTIFY_CHANNEL_ID)
        if (notificationChannel == null) {
            val channel = NotificationChannel(
                    NOTIFY_CHANNEL_ID,
                    getString(R.string.notify_channel_name),
                    NotificationManager.IMPORTANCE_MIN
            )
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotify(): Notification {
        var intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        var pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = if (isMoreThanO) {
            createNotifyChannel()
            Notification.Builder(this, NOTIFY_CHANNEL_ID)
                    .setBadgeIconType(Notification.BADGE_ICON_SMALL)
        } else {
            Notification.Builder(this)
                    .setPriority(Notification.PRIORITY_MIN)
        }.setSmallIcon(R.drawable.ic_tile)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setNumber(0)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notify_click))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NOTIFY_CATEGORY)
                    .setVisibility(Notification.VISIBILITY_SECRET)
        }

        intent = Intent(CloseActionReceiver.ACTION_CLOSE)
        intent.setPackage(packageName)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val action = Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_close_black),
                    getString(R.string.notify_action_close),
                    pendingIntent
            )
                    .build()
            builder.addAction(action)
        } else {
            builder.addAction(R.drawable.ic_close_black, getString(R.string.notify_action_close), pendingIntent)
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