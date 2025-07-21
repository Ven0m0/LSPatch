import java.util.Random

val apiCode: Int by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra
val coreVerCode: Int by rootProject.extra
val coreVerName: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

plugins {
    id("java-library")
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

fun randomString(length: Int): String {
    val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
    return (1..length)
        .map { Random().nextInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
}

buildConfig {
    packageName("org.lsposed.lspatch.share")
    buildConfigField("String", "OBFUSCATED_METADATA_KEY", "\"${randomString(16)}\"")
}

val generateTask = task<Copy>("generateJava") {
    val template = mapOf(
        "apiCode" to apiCode,
        "verCode" to verCode,
        "verName" to verName,
        "coreVerCode" to coreVerCode,
        "coreVerName" to coreVerName
    )
    inputs.properties(template)
    from("src/template/java")
    into("$buildDir/generated/java")
    expand(template)
}

sourceSets["main"].java.srcDir("$buildDir/generated/java")
tasks["compileJava"].dependsOn(generateTask)
