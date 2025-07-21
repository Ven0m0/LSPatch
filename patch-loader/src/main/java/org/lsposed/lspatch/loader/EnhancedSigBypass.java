package org.lsposed.lspatch.loader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EnhancedSigBypass {
    private static final String TAG = "LSPatch-EnhancedSigBypass";
    
    // 智能路径缓存
    private static final Map<String, String> pathCache = new HashMap<>();
    
    // 证书链缓存
    private static final Map<String, byte[]> certCache = new HashMap<>();
    
    /**
     * 智能路径识别（性能与效果平衡版）
     * 保证通用过签名校验的有效性
     */
    public static String getOptimalApkPath(Context context) {
        String packageName = context.getPackageName();
        
        // 快速缓存检查
        if (pathCache.containsKey(packageName)) {
            return pathCache.get(packageName);
        }
        
        String originalPath = context.getPackageResourcePath();
        
        // 效果保障：处理所有关键场景
        String optimalPath = originalPath;
        
        // 场景1：Split APK处理
        if (originalPath.contains("/split/")) {
            optimalPath = handleSplitApkPath(context, originalPath);
        }
        // 场景2：外置存储处理
        else if (originalPath.contains("/mnt/expand/")) {
            optimalPath = handleExternalStoragePath(context, originalPath);
        }
        // 场景3：Instant App处理
        else if (originalPath.contains("/instant/")) {
            optimalPath = handleInstantAppPath(context, originalPath);
        }
        // 场景4：A/B系统更新
        else if (originalPath.endsWith(".apk.new")) {
            optimalPath = handleAbUpdatePath(context, originalPath);
        }
        
        pathCache.put(packageName, optimalPath);
        return optimalPath;
    }
    
    /**
     * 路径分析器
     * 分析安装路径特征，识别不同的安装场景
     */
    private static String analyzePath(Context context, String originalPath) {
        File apkFile = new File(originalPath);
        
        // 场景1：Split APK检测
        if (originalPath.contains("/split/") || originalPath.matches(".*split_config.*\\.apk")) {
            return handleSplitApkPath(context, originalPath);
        }
        
        // 场景2：外置存储检测
        if (originalPath.contains("/mnt/expand/")) {
            return handleExternalStoragePath(context, originalPath);
        }
        
        // 场景3：Instant App检测
        if (originalPath.contains("/instant/")) {
            return handleInstantAppPath(context, originalPath);
        }
        
        // 场景4：A/B系统更新检测
        if (originalPath.matches(".*\\.apk\\.new")) {
            return handleAbUpdatePath(context, originalPath);
        }
        
        // 默认场景：标准安装
        return originalPath;
    }
    
    /**
     * 处理Split APK路径
     */
    private static String handleSplitApkPath(Context context, String originalPath) {
        try {
            String baseName = new File(originalPath).getName();
            String cacheDir = context.getCacheDir().getAbsolutePath();
            String lspatchPath = cacheDir + "/lspatch/splits/" + baseName;
            
            // 确保目录存在
            File lspatchDir = new File(cacheDir + "/lspatch/splits/");
            if (!lspatchDir.exists()) {
                lspatchDir.mkdirs();
            }
            
            return lspatchPath;
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle split APK path", e);
            return originalPath;
        }
    }
    
    /**
     * 处理外置存储路径
     */
    private static String handleExternalStoragePath(Context context, String originalPath) {
        try {
            String storageId = getExternalStorageId();
            String cacheDir = context.getCacheDir().getAbsolutePath();
            
            // 重定向到缓存，避免外置存储权限问题
            String relativePath = originalPath.substring(originalPath.lastIndexOf('/'));
            return cacheDir + "/lspatch/external/" + storageId + relativePath;
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle external storage path", e);
            return originalPath;
        }
    }
    
    /**
     * 处理Instant App路径
     */
    private static String handleInstantAppPath(Context context, String originalPath) {
        try {
            String cacheDir = context.getCacheDir().getAbsolutePath();
            String instantId = originalPath.replaceAll(".*instant/([^/]+).*", "$1");
            return cacheDir + "/lspatch/instant/" + instantId + "/base.apk";
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle instant app path", e);
            return originalPath;
        }
    }
    
    /**
     * 处理A/B系统更新路径
     */
    private static String handleAbUpdatePath(Context context, String originalPath) {
        try {
            String cacheDir = context.getCacheDir().getAbsolutePath();
            String baseName = new File(originalPath).getName();
            return cacheDir + "/lspatch/ab/" + baseName.replace(".new", "");
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle A/B update path", e);
            return originalPath;
        }
    }
    
    /**
     * 获取外置存储ID
     */
    private static String getExternalStorageId() {
        try {
            File expandDir = new File("/mnt/expand/");
            File[] dirs = expandDir.listFiles();
            if (dirs != null && dirs.length > 0) {
                return dirs[0].getName();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get external storage ID", e);
        }
        return "emulated";
    }
    
    /**
     * 证书链验证绕过
     * 为不同签名方案生成合适的证书链
     */
    public static byte[] getCertificateChain(Context context, String packageName) {
        if (certCache.containsKey(packageName)) {
            return certCache.get(packageName);
        }
        
        try {
            // 获取原始签名信息
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                byte[] originalCert = packageInfo.signatures[0].toByteArray();
                certCache.put(packageName, originalCert);
                return originalCert;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get certificate chain", e);
        }
        
        return new byte[0];
    }
    
    /**
     * 清理缓存
     */
    public static void clearCache() {
        pathCache.clear();
        certCache.clear();
    }
    
    /**
     * 获取系统信息用于路径分析
     */
    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Build.VERSION.SDK_INT: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        info.append("Build.MANUFACTURER: ").append(android.os.Build.MANUFACTURER).append("\n");
        info.append("Build.BRAND: ").append(android.os.Build.BRAND).append("\n");
        info.append("Build.MODEL: ").append(android.os.Build.MODEL).append("\n");
        
        try {
            File expandDir = new File("/mnt/expand/");
            if (expandDir.exists()) {
                File[] dirs = expandDir.listFiles();
                info.append("External storage count: ").append(dirs != null ? dirs.length : 0).append("\n");
            }
        } catch (Exception e) {
            info.append("External storage error: ").append(e.getMessage()).append("\n");
        }
        
        return info.toString();
    }
}