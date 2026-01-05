# vw-LSPatch Build Instructions

## Fork Information

- **Upstream**: [JingMatrix/LSPatch](https://github.com/JingMatrix/LSPatch)
- **This Fork**: [vwww-droid/vw-LSPatch](https://github.com/vwww-droid/vw-LSPatch)
- **Core Dependency**: [vwww-droid/vw-LSPosed](https://github.com/vwww-droid/vw-LSPosed)

## Why This Fork

1. Prevent upstream deletion/breaking changes
2. Lock verified working commit versions
3. Add Chinese documentation and analysis notes
4. Safe environment for experiments and modifications

## Verified Build Environment

- ✅ **OS**: macOS 14+
- ✅ **Java**: 21 (OpenJDK/Corretto)
- ✅ **NDK**: 29.0.13113456 (exact version required)
- ✅ **Build Date**: 2026-01-06
- ✅ **Status**: Successfully built debug + release

## Build Steps

### 1. Prerequisites

- Java 21 (use jenv or JAVA_HOME)
- Android SDK with NDK 29.0.13113456
- Git with SSH keys configured

### 2. Clone Repository

```bash
git clone --recursive git@github.com:vwww-droid/vw-LSPatch.git
cd vw-LSPatch
```

### 3. Setup libxposed Dependencies

These are built separately and installed to Maven Local:

```bash
mkdir -p libxposed

# Clone libxposed/api to specific commit
git clone https://github.com/libxposed/api.git libxposed/api
cd libxposed/api
git checkout 54582730315ba4a3d7cfaf9baf9d23c419e07006
cd ../..

# Clone libxposed/service to latest
git clone https://github.com/libxposed/service.git libxposed/service
```

### 4. Build libxposed Dependencies

```bash
# Build api
cd libxposed/api
echo 'org.gradle.jvmargs=-Xmx2048m' >> gradle.properties
./gradlew :api:publishApiPublicationToMavenLocal

# Build service
cd ../service
echo 'org.gradle.jvmargs=-Xmx2048m' >> gradle.properties
./gradlew :interface:publishInterfacePublicationToMavenLocal
cd ../..
```

### 5. Configure Build Environment

```bash
# Set Java version
jenv local 21
java -version  # verify

# Add gradle properties (already in this fork)
# org.gradle.parallel=true
# org.gradle.jvmargs=-Xmx2048m
# android.native.buildOutput=verbose
```

### 6. Build LSPatch

```bash
./gradlew buildAll
```

### 7. Output

Build artifacts are in:

```
out/
├── debug/
│   ├── jar-v0.7-435-debug.jar          (11MB)
│   └── manager-v0.7-435-debug.apk      (80MB)
└── release/
    ├── jar-v0.7-435-release.jar        (11MB)
    └── manager-v0.7-435-release.apk    (8.4MB)
```

## Key Differences from Upstream

- ✅ `.gitmodules`: Points to `vwww-droid/vw-LSPosed` instead of `JingMatrix/LSPosed`
- ✅ `gradle.properties`: Added build optimization configs
- ✅ `docs/`: Added build documentation
- ✅ `.gitignore`: Ignores `libxposed/` and build artifacts

## Troubleshooting

### NDK Version Mismatch

**Symptom**: Build fails with C++ compilation errors

**Solution**: Must use exact NDK version `29.0.13113456`. Install via Android Studio SDK Manager or modify `build.gradle.kts`:

```kotlin
val androidCompileNdkVersion by extra("29.0.13113456")
```

### libxposed Not Found

**Symptom**: Gradle errors about missing `libxposed.api` or `libxposed.service`

**Solution**: Ensure you've built and published both to Maven Local (step 4)

### Java Version

**Symptom**: `invalid source release: 21`

**Solution**: 
```bash
jenv local 21
# or
export JAVA_HOME=/path/to/jdk-21
```

## Sync with Upstream

To pull latest changes from JingMatrix/LSPatch:

```bash
git remote add upstream https://github.com/JingMatrix/LSPatch.git
git fetch upstream
git merge upstream/master  # or rebase
git push
```

## Reference

- [GitHub Actions Config](../.github/workflows/main.yml) - Official CI build steps
- [LSPatch README](../README.md) - Original project documentation

