plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/*")
        resources.excludes.add("META-INF/kotlin-jupyter-libraries/libraries.json")
        resources.excludes.add("draftv3/*")
        resources.excludes.add("draftv4/*")
        resources.excludes.add("arrow-git.*")
        resources.excludes.add("protobuf-java")
        resources.excludes.add("com.google.protobuf:protobuf-javalite:3.25.1")
        resources.excludes.add("com.google.protobuf:protobuf-java:3.25.2")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha03")
    implementation("androidx.core:core:1.8.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.core:core:1.0.1")
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
    //Swipe to refresh:
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    //Advanced WebView:
    implementation("com.github.delight-im:Android-AdvancedWebView:v3.2.1")
}