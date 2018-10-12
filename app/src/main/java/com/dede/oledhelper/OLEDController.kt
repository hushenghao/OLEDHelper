package com.dede.oledhelper

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager

class OLEDController(private val context: Context) {

    companion object {
        const val KEY_ALPHA = "alpha"
        private const val TAG = "OLEDController"
    }

    private var isShowing = false

    private val manager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val view by lazy {
        val view = View(context.applicationContext)
        view.setBackgroundColor(Color.BLACK)
        view
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val params by lazy {
        val params = WindowManager.LayoutParams()
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.alpha = context.loadAlpha()
        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 8.0以上无法使用值小于2035的Type，蒙版无法覆盖状态栏和虚拟按键栏
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        }
        params.format = PixelFormat.TRANSLUCENT
        params
    }

    fun isShow(): Boolean {
        return isShowing
    }

    fun show() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) return
        }
        if (isShowing) return
        handler.post {
            manager.addView(view, params)
        }
        isShowing = true
    }

    fun dismiss() {
        if (!isShowing) return
        handler.post {
            manager.removeView(view)
        }
        isShowing = false
    }

    fun updateAlpha(alpha: Float) {
        if (alpha > OLEDService.MAX_ALPHA)
            params.alpha = OLEDService.MAX_ALPHA
        else
            params.alpha = alpha
        if (isShowing) {
            handler.post {
                manager.updateViewLayout(view, params)
            }
        }
    }

    fun toggle() {
        if (isShowing) {
            dismiss()
        } else {
            show()
        }
    }

    fun getAlpha(): Float {
        return params.alpha
    }
}