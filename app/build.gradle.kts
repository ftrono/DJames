plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.ftrono.DJames"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ftrono.DJames"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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
    //val nav_version = "2.7.7"
    val compose_version = rootProject.extra.get("compose_version") as String

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
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material3.adaptive.navigation.suite.android)
    implementation(libs.androidx.runtime.livedata)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.media)
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
    //Picasso:
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation("jp.wasabeef:picasso-transformations:2.4.0")
    //Kotlin DataFrames:
    implementation("org.jetbrains.kotlinx:dataframe:0.12.1") {
        exclude("org.apache.commons")
        exclude("commons-logging")
    }
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
    //Advanced WebView:
    implementation("com.github.delight-im:Android-AdvancedWebView:v3.2.1")
}