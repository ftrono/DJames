plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
    id("io.objectbox")
    kotlin("plugin.serialization") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

android {
    namespace = "com.ftrono.DJames"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ftrono.DJames"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "2.5.2"

        manifestPlaceholders["appAuthRedirectScheme"] = "djames-oauth"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes.add("META-INF/*")
            excludes.add("META-INF/kotlin-jupyter-libraries/libraries.json")
            excludes.add("draftv3/*")
            excludes.add("draftv4/*")
            excludes.add("arrow-git.*")
            excludes.add("protobuf-java")
            excludes.add("com.google.protobuf:protobuf-javalite:3.25.1")
            excludes.add("com.google.protobuf:protobuf-java:3.25.2")
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material3.adaptive.navigation.suite.android)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.media)
    implementation(libs.androidx.browser)
    implementation(libs.material)
    implementation(libs.material3)

    //implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // OkHTTP: define a BOM and its version
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    // OkHTTP: define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    // GSON:
    implementation("com.google.code.gson:gson:2.10.1")
    //KotlinX-Serialization:
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    //DialogFlow:
    implementation("com.google.cloud:google-cloud-dialogflow:4.40.0") {
        exclude("org.apache.commons")
        exclude("commons-logging")
    }
    //GRPC:
    implementation("io.grpc:grpc-okhttp:1.61.0") {
        exclude("com.google.protobuf")
    }
    implementation("io.grpc:grpc-protobuf-lite:1.61.0") {
        exclude("com.google.protobuf")
    }
    implementation("io.grpc:grpc-stub:1.61.0") {
        exclude("com.google.protobuf")
    }
    compileOnly("org.apache.tomcat:annotations-api:6.0.53") {
        exclude("com.google.protobuf")
    }
    //FFmpeg-kit:
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
    //FuzzyWuzzy:
    implementation("me.xdrop:fuzzywuzzy:1.4.0")
    //AppAuth:
    implementation("net.openid:appauth:0.11.1")
    //Coil:
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
}