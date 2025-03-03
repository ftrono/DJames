// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0" apply false
}
buildscript {
    extra.apply{
        set("compose_version", "1.1.1")
    }
    val objectboxVersion by extra("4.0.3")
    dependencies {
        // Android Gradle Plugin 4.1.0 or later supported
        classpath(libs.gradle)
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}