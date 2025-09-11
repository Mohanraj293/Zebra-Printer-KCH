// app/build.gradle.kts
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
        buildConfigField("String", "FUSION_BASE_URL", "\"https://effb-test.fa.em3.oraclecloud.com/\"")

        buildConfigField("String", "EMPLOYEE_ID", "\"300000195955986\"")
        buildConfigField("String", "ORGANIZATION_CODE", "\"KDH\"")
        buildConfigField("String", "DEFAULT_SUBINVENTORY", "\"DHHPHMAIN\"")
        buildConfigField("String", "DEFAULT_LOCATOR", "\"0.0.0\"")

        buildConfigField("boolean", "OCR_FAKE", "false")
        buildConfigField("String", "OCR_FAKE_ASSET", "\"\"")

        // === OAuth / IDCS config ===
        buildConfigField(
            "String",
            "IDCS_BASE_URL",
            "\"https://idcs-7925a9f1719b425ca9dfb3c81b384f63.identity.oraclecloud.com\""
        )

        // OAuth endpoints (appended to IDCS_BASE_URL)
        buildConfigField("String", "AUTH_ENDPOINT", "\"/oauth2/v1/authorize\"")
        buildConfigField("String", "TOKEN_ENDPOINT", "\"/oauth2/v1/token\"")

        // Client settings
        buildConfigField("String", "OAUTH_CLIENT_ID", "\"71a511d8fd9c4982836311524928e982\"")
        buildConfigField("String", "OAUTH_CLIENT_SECRET", "\"idcscs-8ec1b40a-72d2-4b01-a149-0ce75011a8fd\"")
        buildConfigField(
            "String",
            "OAUTH_SCOPE",
            "\"urn:opc:resource:fa:instanceid=564123563urn:opc:resource:consumer::all\""
        )

        // Redirect configuration — scheme is shared with Manifest placeholder
        buildConfigField("String", "APP_AUTH_REDIRECT_SCHEME", "\"com.lazymohan.zebraprinter\"")
        manifestPlaceholders["appAuthRedirectScheme"] = "com.lazymohan.zebraprinter"
        buildConfigField("String","REDIRECT_URI","\"com.lazymohan.zebraprinter:/oauth2redirect\"")

        // === Microsoft Graph (client credentials) ===
        buildConfigField("String", "GRAPH_TENANT_ID", "\"d2624b5b-89fd-468e-b504-6570779aa3b6\"")
        buildConfigField("String", "GRAPH_CLIENT_ID", "\"2aeb3d3f-23dd-4a1b-a65d-98c5e14e7e6e\"")
        buildConfigField("String", "GRAPH_CLIENT_SECRET", "\"u.R8Q~VRnquVagmRMmG.Ie5ySYddDvpIQQmVEcnC\"")
        buildConfigField("String", "GRAPH_SCOPES", "\"https://graph.microsoft.com/.default\"")

        // Target drive & paths
        buildConfigField("String", "GRAPH_DRIVE_ID", "\"b!ZAGC8ntOU0OjB2mN-10Bkd1EJurDv09Gg7ZZMhLEf0O0SZRxQIK0TIeQ97KgkPrY\"")
        buildConfigField("String", "GRAPH_FOLDER_PATH", "\"KCH Mediscan\"")
        buildConfigField("String", "MASTER_CSV_FILE", "\"OnHandExport.csv\"")
        buildConfigField("String", "COUNTS_CSV_FILE", "\"PhysicalCountTemplate.csv\"")
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
    implementation(libs.androidx.leanback)

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
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(libs.play.services.mlkit.document.scanner)
    implementation(libs.play.services.base)
    implementation(libs.coil.compose)

    // ML Kit Google Code Scanner (QR scanner UI)
    implementation(libs.play.services.code.scanner)

    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf.ktx)
    implementation(platform(libs.firebase.bom))

    // OAuth/AppAuth bits
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.browser:browser:1.8.0")
}
