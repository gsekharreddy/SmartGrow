import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// 1. Load local.properties safely
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.smartgrow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartgrow"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. Pull values from local.properties for Auto-Login!
        val baseUrl = localProperties.getProperty("THINGSBOARD_BASE_URL") ?: "https://thingsboard.cloud"
        val deviceId = localProperties.getProperty("THINGSBOARD_DEVICE_ID") ?: "YOUR_DEVICE_ID"
        val username = localProperties.getProperty("THINGSBOARD_USERNAME") ?: "tenant@thingsboard.org"
        val password = localProperties.getProperty("THINGSBOARD_PASSWORD") ?: "sysadmin"

        buildConfigField("String", "THINGSBOARD_BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "THINGSBOARD_DEVICE_ID", "\"$deviceId\"")
        buildConfigField("String", "THINGSBOARD_USERNAME", "\"$username\"")
        buildConfigField("String", "THINGSBOARD_PASSWORD", "\"$password\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}