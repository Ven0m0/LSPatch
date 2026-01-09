package org.lsposed.lspatch.manager

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.lsposed.lspatch.config.ConfigManager
import org.lsposed.lspatch.lspApp
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService

object ManagerService : ILSPApplicationService.Stub() {

    private const val TAG = "ManagerService"

    override fun isLogMuted(): Boolean {
        return false
    }

    override fun getLegacyModulesList(): List<Module> {
        val app = lspApp.packageManager.getNameForUid(Binder.getCallingUid())
        val list = app?.let {
            runBlocking { ConfigManager.getModuleFilesForApp(it) }
        }.orEmpty()
        Log.d(TAG, "$app calls getLegacyModulesList: $list")
        return list
    }

    override fun getModulesList(): List<Module> {
        return emptyList()
    }

    override fun getPrefsPath(packageName: String): String {
        val callingUid = Binder.getCallingUid()
        val pm = lspApp.packageManager
        val packagesForUid = pm.getPackagesForUid(callingUid) ?: emptyArray()
        if (!packagesForUid.contains(packageName)) {
            Log.w(TAG, "Unauthorized getPrefsPath access for $packageName from uid $callingUid (packages=${packagesForUid.toList()})")
            throw SecurityException("Not allowed to access preferences for package: $packageName")
        }
        return "${android.os.Environment.getDataDirectory()}/data/$packageName/shared_prefs/"
    }

    override fun requestInjectedManagerBinder(binder: List<IBinder>?): ParcelFileDescriptor? {
        return null
    }
}
