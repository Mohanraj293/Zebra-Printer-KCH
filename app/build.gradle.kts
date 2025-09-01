plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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

        buildConfigField("String", "EMPLOYEE_ID", "\"300000195955986\"")
        buildConfigField("String", "ORGANIZATION_CODE", "\"KDH\"")
        buildConfigField("String", "DEFAULT_SUBINVENTORY", "\"DHHPHMAIN\"")
        buildConfigField("String", "DEFAULT_LOCATOR", "\"0.0.0\"")

        buildConfigField("boolean", "OCR_FAKE", "true")
        buildConfigField("String", "OCR_FAKE_ASSET", "\"\"")
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

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material3)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout)

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

    implementation(libs.play.services.mlkit.document.scanner)
    implementation(libs.play.services.base)
    implementation(libs.coil.compose)

    // ML Kit Google Code Scanner (QR scanner UI)
    implementation(libs.play.services.code.scanner)

    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf.ktx)
    implementation(platform(libs.firebase.bom))

}
