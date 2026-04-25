// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false

    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}
buildscript {
    val objectboxVersion by extra("4.1.0")
    dependencies {
        // Android Gradle Plugin 4.1.0 or later supported
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}