plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.lazymohan.zebraprinter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lazymohan.zebraprinter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // === BuildConfig constants used by Fusion client / GRN flow ===
        buildConfigField("String", "FUSION_BASE_URL", "\"https://effb-test.fa.em3.oraclecloud.com/\"") // must end with /
        buildConfigField("String", "FUSION_USERNAME", "\"RCA.Automation\"")
        buildConfigField("String", "FUSION_PASSWORD", "\"Kch12345\"")

        buildConfigField("String", "EMPLOYEE_ID", "\"300000023921708\"")
        buildConfigField("String", "ORGANIZATION_CODE", "\"KDH\"")
        buildConfigField("String", "DEFAULT_SUBINVENTORY", "\"DHHPHMAIN\"")
        buildConfigField("String", "DEFAULT_LOCATOR", "\"0.0.0\"")
        // =============================================================
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(files("libs/ZSDK_ANDROID_API.jar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.tarkaui)
    implementation(libs.tarkaui.icons)
    implementation(libs.fluent.system.icons)
    implementation(libs.jackson.databind)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.sheets)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.bundles.retrofit)

    // ML Kit dependencies
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.mlkit:object-detection:17.0.0")

    // Document Scanner Options:
    // Option 1: Try the beta version
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.android.gms:play-services-base:18.5.0")

//    // Option 2: If document scanner not available, use CameraX for custom document scanning
//    implementation("androidx.camera:camera-core:1.4.0")
//    implementation("androidx.camera:camera-camera2:1.4.0")
//    implementation("androidx.camera:camera-lifecycle:1.4.0")
//    implementation("androidx.camera:camera-view:1.4.0")
//    implementation("androidx.camera:camera-extensions:1.4.0")
//
//    // Image processing libraries
//    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("io.coil-kt:coil-compose:2.6.0")
//    implementation("androidx.activity:activity-compose:1.9.2")
}
