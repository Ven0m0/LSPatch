package org.lsposed.lspatch

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.lspatch.config.Configs
import org.lsposed.lspatch.config.MyKeyStore
import org.lsposed.lspatch.share.Constants
import org.lsposed.lspatch.share.PatchConfig
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.IOException
import java.util.Collections.addAll

object Patcher {

    class Options(
        private val injectDex: Boolean,
        private val config: PatchConfig,
        private val apkPaths: List<String>,
        private val embeddedModules: List<String>?
    ) {
        fun toStringArray(): Array<String> {
            return buildList {
                addAll(apkPaths)
                add("-o"); add(lspApp.tmpApkDir.absolutePath)
                if (config.debuggable) add("-d")
                add("-l"); add(config.sigBypassLevel.toString())
                if (config.useManager) add("--manager")
                if (config.overrideVersionCode) add("-r")
                if (Configs.detailPatchLogs) add("-v")
                embeddedModules?.forEach {
                    add("-m"); add(it)
                }
                if(injectDex) add("--injectdex")
                if (!MyKeyStore.useDefault) {
                    addAll(arrayOf("-k", MyKeyStore.file.path, Configs.keyStorePassword, Configs.keyStoreAlias, Configs.keyStoreAliasPassword))
                }
            }.toTypedArray()
        }
    }

    suspend fun patch(logger: Logger, options: Options) {
        withContext(Dispatchers.IO) {
            LSPatch(logger, *options.toStringArray()).doCommandLine()

            val uri = Configs.storageDirectory?.toUri()
                ?: throw IOException("Uri is null")
            val root = DocumentFile.fromTreeUri(lspApp, uri)
                ?: throw IOException("DocumentFile is null")

            // Delete old patched files (both bundles and individual APKs)
            root.listFiles().forEach {
                if (it.name?.endsWith(Constants.PATCH_FILE_SUFFIX) == true ||
                    it.name?.endsWith(Constants.PATCH_BUNDLE_SUFFIX) == true) {
                    it.delete()
                }
            }

            // Check for bundle file first (.apks), then individual APKs
            val bundleFile = lspApp.tmpApkDir.listFiles()
                ?.find { it.name.endsWith(Constants.PATCH_BUNDLE_SUFFIX) }

            if (bundleFile != null) {
                // Copy single bundle file (use octet-stream to avoid adding .zip extension)
                val file = root.createFile("application/octet-stream", bundleFile.name)
                    ?: throw IOException("Failed to create output bundle file")
                val output = lspApp.contentResolver.openOutputStream(file.uri)
                    ?: throw IOException("Failed to open output stream")
                output.use {
                    bundleFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                logger.i("Bundle saved to ${root.uri.lastPathSegment}/${bundleFile.name}")
            } else {
                // Fallback: copy individual APK files (single APK case)
                lspApp.tmpApkDir.walk()
                    .filter { it.name.endsWith(Constants.PATCH_FILE_SUFFIX) }
                    .forEach { apk ->
                        val file = root.createFile("application/vnd.android.package-archive", apk.name)
                            ?: throw IOException("Failed to create output file")
                        val output = lspApp.contentResolver.openOutputStream(file.uri)
                            ?: throw IOException("Failed to open output stream")
                        output.use {
                            apk.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                logger.i("Patched files are saved to ${root.uri.lastPathSegment}")
            }
        }
    }
}
