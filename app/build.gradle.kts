import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().also { props ->
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(props::load)
}

android {
    namespace = "com.example.pixelhunt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pixelhunt"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String", "BASE_URL",
            "\"${localProperties.getProperty("BASE_URL", "https://your-api-url-here.ngrok-free.app/")}\""
        )
        buildConfigField(
            "String", "GOOGLE_TTS_KEY",
            "\"${localProperties.getProperty("GOOGLE_TTS_KEY", "")}\""
        )
        buildConfigField(
            "String", "VOICERSS_KEY",
            "\"${localProperties.getProperty("VOICERSS_KEY", "")}\""
        )
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
        buildConfig = true
    }
}

dependencies {
    // Jetpack Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Material Icons Extended (Pusula ve diğer ikonlar için gerekli)
    implementation("androidx.compose.material:material-icons-extended")

    // CameraX Kütüphaneleri
    val camerax_version = "1.3.2"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Sunucu ile iletişim için OkHttp ve Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)

    // Coil — async image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
