package com.dede.oledhelper

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Created by hsh on 2018/6/19.
 */
@SuppressLint("NewApi")
class TService : TileService(), ServiceConnection {

    companion object {
        const val FROM_TILE = "from_tile"
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    private var controller: OLEDController? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null && service is OLEDController) {
            controller = service
        }
    }

    /**
     * 添加到快捷通知栏以后，绑定OLEDService
     */
    override fun onBind(intent: Intent?): IBinder {
        val service = Intent(this, OLEDService::class.java)
        service.putExtra(FROM_TILE, true)
        startService(service)
        bindService(service, this, Context.BIND_AUTO_CREATE)
        return super.onBind(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    /**
     * 更新Tile状态
     */
    private fun update() {
        qsTile.state = if (controller?.isShow() == true) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        controller?.toggle()
        update()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}