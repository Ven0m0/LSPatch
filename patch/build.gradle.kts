val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

plugins {
    id("java-library")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
    sourceSets {
        main {
            java.srcDirs("libs/manifest-editor/lib/src/main/java")
            resources.srcDirs("libs/manifest-editor/lib/src/main")
        }
    }
}

val copyLspatchAssets by tasks.registering(Copy::class) {
    val variant = "release"
    dependsOn(":meta-loader:copy${variant.replaceFirstChar { it.uppercase() }}")
    dependsOn(":patch-loader:copy${variant.replaceFirstChar { it.uppercase() }}")

    from("${rootProject.projectDir}/out/assets/$variant/lspatch")
    into("$projectDir/src/main/resources/assets/lspatch")
}

tasks.named("compileJava") {
    dependsOn(copyLspatchAssets)
}

tasks.named("processResources") {
    dependsOn(copyLspatchAssets)
}

dependencies {
    implementation(projects.axml)
    implementation(projects.apkzlib)
    implementation(projects.share.java)

    implementation(lspatch.commons.io)
    implementation(lspatch.beust.jcommander)
    implementation(lspatch.google.gson)
}
