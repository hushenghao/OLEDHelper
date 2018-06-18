package com.dede.oledhelper

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.support.annotation.IntRange
import android.view.View
import android.view.WindowManager

class OLEDController(private val context: Context) : Binder() {

    private var isShowing = false

    private val manager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val view by lazy {
        val view = View(context.applicationContext)
        view.setBackgroundColor(Color.BLACK)
        view
    }

    private val params by lazy {
        val params = WindowManager.LayoutParams()
        params.width = -1
        params.height = -1
        params.alpha = OLEDService.DEFAULT_ALPHA
        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        }
        params.format = PixelFormat.TRANSLUCENT
        params
    }

    fun isShow(): Boolean {
        return isShowing
    }

    fun open() {
        if (!isShowing) {
            manager.addView(view, params)
            isShowing = true
        }
    }

    fun close() {
        if (isShowing) {
            manager.removeView(view)
            isShowing = false
        }
    }

    fun updateAlpha(@IntRange(from = 0, to = 1) alpha: Float) {
        params.alpha = alpha
        if (isShowing) {
            manager.updateViewLayout(view, params)
        }
    }

    fun getAlpha():Float {
        return params.alpha
    }
}