//
// Created by VIP on 2021/4/25.
//

#include "bypass_sig.h"

#include "../src/native_api.h"
#include "elf_util.h"
#include "logging.h"
#include "native_util.h"
#include "patch_loader.h"
#include "utils/hook_helper.hpp"
#include "utils/jni_helper.hpp"

// Declare HookInline function
extern "C" int HookInline(void *original, void *replace, void **backup);

#include <sys/stat.h>
#include <string>

using lsplant::operator""_sym;

namespace lspd {

std::string apkPath;
std::string redirectPath;
std::string enhancedApkPath;

inline static constexpr auto kLibCName = "libc.so";

std::unique_ptr<const SandHook::ElfImg> &GetC(bool release = false) {
    static std::unique_ptr<const SandHook::ElfImg> kImg = nullptr;
    if (release) {
        kImg.reset();
    } else if (!kImg) {
        kImg = std::make_unique<SandHook::ElfImg>(kLibCName);
    }
    return kImg;
}

// 性能优化：使用字符串视图避免频繁分配
inline static thread_local bool is_initialized = false;
inline static thread_local bool should_hook = false;

inline static auto __openat_ =
    "__openat"_sym.hook->*[]<lsplant::Backup auto backup>(int fd, const char *pathname, int flag,
                                                          int mode) static -> int {
    if (!pathname) return backup(fd, pathname, flag, mode);
    
    // 快速路径检查
    if (pathname[0] != '/') return backup(fd, pathname, flag, mode);
    
    if (!is_initialized) {
        is_initialized = true;
        should_hook = (strcmp(pathname, apkPath.c_str()) == 0 || 
                      strcmp(pathname, enhancedApkPath.c_str()) == 0);
    }
    
    if (should_hook) {
        return backup(fd, redirectPath.c_str(), flag, mode);
    }
    return backup(fd, pathname, flag, mode);
};

inline static auto __open_ =
    "__open"_sym.hook->*[]<lsplant::Backup auto backup>(const char *pathname, int flag,
                                                         int mode) static -> int {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(redirectPath.c_str(), flag, mode);
    }
    return backup(pathname, flag, mode);
};

inline static auto __stat_ =
    "__stat"_sym.hook->*[]<lsplant::Backup auto backup>(const char *pathname, struct stat *buf) static -> int {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(redirectPath.c_str(), buf);
    }
    return backup(pathname, buf);
};

inline static auto __fstatat_ =
    "__fstatat"_sym.hook->*[]<lsplant::Backup auto backup>(int dirfd, const char *pathname, struct stat *buf, int flags) static -> int {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(dirfd, redirectPath.c_str(), buf, flags);
    }
    return backup(dirfd, pathname, buf, flags);
};

inline static auto __fstat_ =
    "__fstat"_sym.hook->*[]<lsplant::Backup auto backup>(int fd, struct stat *buf) static -> int {
    // fstat通常用于已打开的文件描述符，这里需要更复杂的处理
    return backup(fd, buf);
};

inline static auto __readlink_ =
    "__readlink"_sym.hook->*[]<lsplant::Backup auto backup>(const char *pathname, char *buf, size_t bufsiz) static -> ssize_t {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(redirectPath.c_str(), buf, bufsiz);
    }
    return backup(pathname, buf, bufsiz);
};

inline static auto __readlinkat_ =
    "__readlinkat"_sym.hook->*[]<lsplant::Backup auto backup>(int dirfd, const char *pathname, char *buf, size_t bufsiz) static -> ssize_t {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(dirfd, redirectPath.c_str(), buf, bufsiz);
    }
    return backup(dirfd, pathname, buf, bufsiz);
};

inline static auto __access_ =
    "__access"_sym.hook->*[]<lsplant::Backup auto backup>(const char *pathname, int mode) static -> int {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(redirectPath.c_str(), mode);
    }
    return backup(pathname, mode);
};

inline static auto __faccessat_ =
    "__faccessat"_sym.hook->*[]<lsplant::Backup auto backup>(int dirfd, const char *pathname, int mode, int flags) static -> int {
    if (pathname && (std::string(pathname) == apkPath || std::string(pathname) == enhancedApkPath)) {
        return backup(dirfd, redirectPath.c_str(), mode, flags);
    }
    return backup(dirfd, pathname, mode, flags);
};

inline static auto __mmap_ =
    "__mmap"_sym.hook->*[]<lsplant::Backup auto backup>(void *addr, size_t length, int prot, int flags, int fd, off_t offset) static -> void * {
    // mmap通常用于内存映射，需要特殊处理文件描述符映射
    return backup(addr, length, prot, flags, fd, offset);
};

bool HookOpenat(const lsplant::InitInfo& init) {
    return init.inline_hooker(init.art_symbol_resolver("__openat"),
                              reinterpret_cast<void *>(+__openat_));
}

bool HookEnhanced(const lsplant::HookHandler &handler) {
    return handler(__openat_) && handler(__open_) && handler(__stat_) && 
           handler(__fstatat_) && handler(__fstat_) && handler(__readlink_) && 
           handler(__readlinkat_) && handler(__access_) && handler(__faccessat_);
}

LSP_DEF_NATIVE_METHOD(void, SigBypass, enableOpenatHook, jstring origApkPath,
                      jstring cacheApkPath) {
    auto r = HookOpenat(lsplant::InitInfo{
        .inline_hooker =
            [](auto t, auto r) {
                void *bk = nullptr;
                return HookInline(t, r, &bk) == 0 ? bk : nullptr;
            },
        .art_symbol_resolver = [](auto symbol) { return GetC()->getSymbAddress(symbol); },
    });
    if (!r) {
        LOGE("Hook __openat fail");
        return;
    }
    lsplant::JUTFString str1(env, origApkPath);
    lsplant::JUTFString str2(env, cacheApkPath);
    apkPath = str1.get();
    redirectPath = str2.get();
    LOGD("apkPath {}", apkPath.c_str());
    LOGD("redirectPath {}", redirectPath.c_str());
    GetC(true);
}

LSP_DEF_NATIVE_METHOD(void, SigBypass, enableEnhancedHook, jstring origApkPath,
                      jstring enhancedPath, jstring cacheApkPath) {
    auto r = HookEnhanced(lsplant::InitInfo{
        .inline_hooker =
            [](auto t, auto r) {
                void *bk = nullptr;
                return HookInline(t, r, &bk) == 0 ? bk : nullptr;
            },
        .art_symbol_resolver = [](auto symbol) { return GetC()->getSymbAddress(symbol); },
    });
    if (!r) {
        LOGE("Hook enhanced fail");
        return;
    }
    
    lsplant::JUTFString str1(env, origApkPath);
    lsplant::JUTFString str2(env, enhancedPath);
    lsplant::JUTFString str3(env, cacheApkPath);
    
    apkPath = str1.get();
    enhancedApkPath = str2.get();
    redirectPath = str3.get();
    
    LOGD("Enhanced hook enabled");
    LOGD("apkPath {}", apkPath.c_str());
    LOGD("enhancedApkPath {}", enhancedApkPath.c_str());
    LOGD("redirectPath {}", redirectPath.c_str());
    
    GetC(true);
}

static JNINativeMethod gMethods[] = {
    LSP_NATIVE_METHOD(SigBypass, enableOpenatHook, "(Ljava/lang/String;Ljava/lang/String;)V"),
    LSP_NATIVE_METHOD(SigBypass, enableEnhancedHook, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V")};

void RegisterBypass(JNIEnv *env) { REGISTER_LSP_NATIVE_METHODS(SigBypass); }

}  // namespace lspd
