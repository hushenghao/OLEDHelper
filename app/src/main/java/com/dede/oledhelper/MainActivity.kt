package com.dede.oledhelper

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.properties.Delegates


private val isMoreThanM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
private val isMoreThanN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
private val isMoreThanO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

private const val REQUEST_DRAW_OVERLAY_CODE = 10
private const val KEY_FIRST_IN = "first_in"
private const val MAX_DELAY = 5

class MainActivity : Activity(), ServiceConnection {

    private var aidl: IOLED by Delegates.notNull()
    private val delayHandler = Handler()

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        aidl = IOLED.Stub.asInterface(service)
        seek_bar.progress = ((1 - aidl.alpha) * 100).toInt()
        switch_.isChecked = aidl.isShow
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectCustomSlowCalls()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build())
            val builder = StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                builder.detectLeakedRegistrationObjects()
            }
            StrictMode.setVmPolicy(builder.penaltyLog().build())
        }
        setContentView(R.layout.activity_main)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        seek_bar.max = (OLEDService.MAX_ALPHA * 100).toInt()

        val intent = Intent(this, OLEDService::class.java)
        startService(intent)
        bindService(intent, this, Context.BIND_AUTO_CREATE)

        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (switch_.isChecked) {
                    aidl.updateAlpha(getAlpha(seekBar))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                aidl.updateAlpha(getAlpha(seekBar))
            }

            private fun getAlpha(seekBar: SeekBar): Float {
                return 1 - seekBar.progress.toFloat() / 100f
            }
        })

        switch_.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                aidl.dismiss()
            } else {
                aidl.show()
                val firstIn = sp().getBoolean(KEY_FIRST_IN, true)
                if (firstIn) {
                    showFirstInDialog()
                }
            }
        }

        if (isMoreThanN) {// 主要针对Android N的Tile不可用的情况
            tv_ignore_battery_optimization.visibility = View.VISIBLE
            tv_ignore_battery_optimization.setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        if (isMoreThanO) {
            tv_close_drawoverlay_notify.visibility = View.VISIBLE
            tv_close_drawoverlay_notify.setOnClickListener {
                goSystemNotifySetting()
            }
        }

        requestDrawOverlays()

        val filter = IntentFilter(ACTION_TILE_CLICK)
        registerReceiver(tileClickReceiver, filter)
    }

    private val tileClickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_TILE_CLICK) return
            val isShow = intent.getBooleanExtra(EXTRA_IS_SHOW, false)
            switch_.isChecked = isShow// 更新界面
        }
    }

    /**
     * 第一次运行的弹窗，5秒不点击自动关闭
     */
    private fun showFirstInDialog() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.first_in_title)
                .setMessage(getString(R.string.first_in_msg, MAX_DELAY))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    delayHandler.removeCallbacksAndMessages(null)
                    sp().edit().putBoolean(KEY_FIRST_IN, false).apply()
                }
                .setCancelable(false)
                .create()
        var time = 0
        var runnable: Runnable? = null
        runnable = Runnable {
            val t = MAX_DELAY - time
            if (t <= 0) {
                switch_.isChecked = false
                dialog.dismiss()
                return@Runnable
            }
            dialog.setMessage(getString(R.string.first_in_msg, t))
            time++
            delayHandler.postDelayed(runnable, 1000)
        }
        delayHandler.postDelayed(runnable, 1000)
        dialog.show()
    }

    private fun requestDrawOverlays() {
        if (!isMoreThanM || canDrawOverlays()) return

        AlertDialog.Builder(this)
                .setTitle(R.string.request_draw_overlay)
                .setMessage(R.string.label_draw_overlay)
                .setPositiveButton(R.string.go_setting) { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))
                    startActivityForResult(intent, REQUEST_DRAW_OVERLAY_CODE)
                }
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show()
    }

    /**
     * 跳转到系统通知设置页面，关闭在应用上层显示的通知
     */
    private fun goSystemNotifySetting() {
        if (!isMoreThanO) return
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, "android")
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_DRAW_OVERLAY_CODE -> {
                if (!canDrawOverlays()) {
                    Toast.makeText(this, R.string.draw_overlay_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        unbindService(this)
        unregisterReceiver(tileClickReceiver)
        super.onDestroy()
    }
}
