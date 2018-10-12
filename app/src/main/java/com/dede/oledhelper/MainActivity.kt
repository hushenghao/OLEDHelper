package com.dede.oledhelper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
private val isMoreThanO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

private const val REQUEST_DRAW_OVERLAY_CODE = 10
private const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_CODE = 20

class MainActivity : Activity(), ServiceConnection {

    private var aidl: IOLED by Delegates.notNull()

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        aidl = IOLED.Stub.asInterface(service)
        seek_bar.progress = ((1 - aidl.alpha) * 100).toInt()
        switch_.isChecked = aidl.isShow
    }

    @SuppressLint("BatteryLife")
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
            }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isMoreThanM && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            tv_ignore_battery_optimization.visibility = View.VISIBLE
        }
        tv_ignore_battery_optimization.setOnClickListener {
            if (!isMoreThanM) return@setOnClickListener

            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_CODE)
        }

        if (isMoreThanO) {
            tv_close_lock_notify.visibility = View.VISIBLE
        }
        tv_close_lock_notify.setOnClickListener {
            if (!isMoreThanO) return@setOnClickListener

            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, OLEDService.NOTIFY_CHANNEL_ID)
            startActivity(intent)
        }

        if (isMoreThanO) {
            tv_close_drawoverlay_notify.visibility = View.VISIBLE
        }
        tv_close_drawoverlay_notify.setOnClickListener {
            goSystemNotifySetting()
        }

        requestDrawOverlays()
    }

    private fun requestDrawOverlays() {
        if (!isMoreThanM || Settings.canDrawOverlays(this)) return

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
            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_CODE -> {
                tv_ignore_battery_optimization.visibility =
                        if (resultCode == RESULT_OK) View.GONE else View.VISIBLE
            }
            REQUEST_DRAW_OVERLAY_CODE -> {
                if (!isMoreThanO) return
                AlertDialog.Builder(this)
                        .setTitle(R.string.close_system_notify)
                        .setMessage(R.string.apio_close_notify)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.go_setting) { _, _ ->
                            goSystemNotifySetting()
                        }
                        .create()
                        .show()

                if (isMoreThanM && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.draw_overlay_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}
