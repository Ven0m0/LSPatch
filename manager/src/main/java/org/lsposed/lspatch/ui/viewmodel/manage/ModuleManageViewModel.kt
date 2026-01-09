package org.lsposed.lspatch.ui.viewmodel.manage

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import org.lsposed.lspatch.util.LSPPackageManager

class ModuleManageViewModel : ViewModel() {

    companion object {
        private const val TAG = "ModuleManageViewModel"
    }

    class XposedInfo(
        val api: Int,
        val description: String,
        val scope: List<String>
    )

    val appList: List<Pair<LSPPackageManager.AppInfo, XposedInfo>> by derivedStateOf {
        LSPPackageManager.appList.mapNotNull { appInfo ->
            val metaData = appInfo.app.metaData ?: return@mapNotNull null
            val xposedMinVersion = metaData.getInt("xposedminversion", -1)
            if (xposedMinVersion == -1) return@mapNotNull null
            
            // NOTE: Module scope information is stored in the database but requires
            // async database access. Current architecture uses derivedStateOf which is synchronous.
            // Scope data should be loaded separately via database queries in a proper coroutine scope.
            val scope = emptyList<String>()
            
            appInfo to XposedInfo(
                xposedMinVersion,
                metaData.getString("xposeddescription") ?: "",
                scope
            )
        }.also {
            Log.d(TAG, "Loaded ${it.size} Xposed modules")
        }
    }
}
