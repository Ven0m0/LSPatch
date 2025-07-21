import java.util.Locale
import java.util.Random

plugins {
    alias(libs.plugins.agp.app)
}

fun randomString(length: Int): String {
    val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { Random().nextInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

android {
    defaultConfig {
        multiDexEnabled = false
        buildConfigField("String", "OBFUSCATED_TAG", "\"LSPatch-${randomString(8)}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }
    namespace = "org.lsposed.lspatch.loader"
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.uppercase() }

    task<Copy>("copyDex$variantCapped") {
        dependsOn("assemble$variantCapped")
        from("$buildDir/intermediates/dex/${variant.name}/mergeDex$variantCapped/classes.dex")
        rename("classes.dex", "loader.dex")
        into("${rootProject.projectDir}/out/assets/${variant.name}/lspatch")
    }

    task<Copy>("copySo$variantCapped") {
        dependsOn("assemble$variantCapped")
        dependsOn("strip${variantCapped}DebugSymbols")
        val libDir = variant.name + "/strip${variantCapped}DebugSymbols"
        from(
            fileTree(
                "dir" to "$buildDir/intermediates/stripped_native_libs/$libDir/out/lib",
                "include" to listOf("**/liblspatch.so")
            )
        )
        into("${rootProject.projectDir}/out/assets/${variant.name}/lspatch/so")
    }

    task("copy$variantCapped") {
        dependsOn("copySo$variantCapped")
        dependsOn("copyDex$variantCapped")

        doLast {
            println("Dex and so files has been copied to ${rootProject.projectDir}${File.separator}out")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.core)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.share.android)
    implementation(projects.share.java)

    implementation(libs.gson)
}
