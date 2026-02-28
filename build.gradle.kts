// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.31" apply false // Add KSP plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
}
buildscript {
    extra.apply{
        set("ext.kotlin_version", "2.1.20")
        set("compose_version", "2024.04.01")
    }
    val objectboxVersion by extra("4.1.0")
    dependencies {
        // Android Gradle Plugin 4.1.0 or later supported
        classpath(libs.gradle)
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}