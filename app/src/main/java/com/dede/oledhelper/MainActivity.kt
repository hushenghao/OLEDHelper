package com.dede.oledhelper

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.provider.Settings
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity(), ServiceConnection {

    private var controller: OLEDController? = null

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null && service is OLEDController) {
            controller = service
            seek_bar.progress = (controller!!.getAlpha() * 100).toInt()
            switch_.isChecked = controller!!.isShow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
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
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = progress.toFloat() / 100f
                controller?.updateAlpha(alpha)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        switch_.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                controller?.close()
            } else {
                controller?.open()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return
        if (Settings.canDrawOverlays(this))
            return
        AlertDialog.Builder(this)
                .setTitle("请求权限")
                .setMessage("添加屏幕蒙版，需要“允许出现在其他应用上”的权限。")
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))
                    startActivityForResult(intent, 10)
                }
                .create()
                .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 10) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "权限异常", Toast.LENGTH_SHORT).show()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AlertDialog.Builder(this)
                            .setTitle("关闭通知栏通知")
                            .setMessage("Android 8.0及以上系统，添加屏幕悬浮窗后，会在通知栏显示通知，是否到设置关闭？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("去设置") { _, _ ->
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "android")
                                startActivity(intent)
                            }
                            .create()
                            .show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}
