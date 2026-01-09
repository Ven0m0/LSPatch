package org.lsposed.lspatch.manager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log


class ModuleService : Service() {

    companion object {
        private const val TAG = "ModuleService"
    }

    override fun onBind(intent: Intent): IBinder? {
        val packageName = intent.getStringExtra("packageName") ?: return null
        // NOTE: Authentication should be implemented here to verify the calling package
        // is authorized to access the manager service. This prevents unauthorized apps
        // from accessing module information.
        Log.i(TAG, "$packageName requests binder")
        return ManagerService.asBinder()
    }
}
