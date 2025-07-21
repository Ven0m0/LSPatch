package org.lsposed.lspd.nativebridge;

public class SigBypass {
    public static native void enableOpenatHook(String origApkPath, String cacheApkPath);
    public static native void enableEnhancedHook(String origApkPath, String enhancedApkPath, String cacheApkPath);
}
