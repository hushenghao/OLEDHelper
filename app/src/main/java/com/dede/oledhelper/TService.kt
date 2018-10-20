package com.dede.oledhelper

import android.annotation.TargetApi
import android.content.*
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlin.properties.Delegates

/**
 * Created by hsh on 2018/6/19.
 */
const val ACTION_TILE_CLICK = "com.dede.oledhelper.TService.onClick"
const val EXTRA_IS_SHOW = "is_show"

@TargetApi(Build.VERSION_CODES.N)
class TService : TileService(), ServiceConnection {

    companion object {
        const val FROM_TILE = "from_tile"
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    private var aidl: IOLED by Delegates.notNull()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        aidl = IOLED.Stub.asInterface(service)
        updateTile()
    }

    private val innerCloseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            unbindService(this@TService)
            updateTile(false)
        }
    }

    /**
     * 显示快捷通知栏以后，绑定OLEDService
     */
    override fun onStartListening() {
        val service = Intent(this, OLEDService::class.java)
        service.putExtra(FROM_TILE, true)
        startService(service)
        bindService(service, this, Context.BIND_AUTO_CREATE)

        val intentFilter = IntentFilter(CloseActionReceiver.ACTION_CLOSE)
        registerReceiver(innerCloseReceiver, intentFilter)
    }

    override fun onStopListening() {
        unregisterReceiver(innerCloseReceiver)
        unbindService(this)
    }

    /**
     * 更新Tile状态
     */
    private fun updateTile(isShow: Boolean) {
        if (isShow) {
            qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
            qsTile.state = Tile.STATE_ACTIVE
        } else {
            qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_2)
            qsTile.state = Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    private fun updateTile() {
        val isShow = aidl.isShow
        updateTile(isShow)
        val intent = Intent(ACTION_TILE_CLICK)
                .putExtra(EXTRA_IS_SHOW, isShow)
        sendBroadcast(intent)// 发送tile点击广播
    }

    override fun onClick() {
        aidl.toggle()
        updateTile()
    }

}