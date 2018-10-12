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
        updateTile(aidl.isShow)
    }

    override fun onClick() {
        aidl.toggle()
        updateTile()
    }

}