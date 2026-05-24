plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry.android.gradle)
    id("kotlin-kapt")
}

android {
    namespace = "dev.abbasian.raybod"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.abbasian.raybod"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.0.0-alpha.7"

        manifestPlaceholders["sentryRelease"] = "$applicationId@$versionName+$versionCode"
        manifestPlaceholders["sentryEnvironment"] = "debug"

        testInstrumentationRunner = "dev.abbasian.raybod.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEY_ALIAS") ?: "release"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
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
    buildTypes {
        debug {
            manifestPlaceholders["sentryEnvironment"] = "debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["sentryEnvironment"] = "production"
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":presentation"))
    implementation(project(":agent"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)

    
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    kapt(libs.hilt.compiler)
    kapt(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Hilt testing
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler.testing)

    implementation(libs.sentry.android)
}

sentry {
    org.set("dubai-j4")
    projectName.set("android")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN") ?: "")
    includeProguardMapping.set(true)
    autoUploadProguardMapping.set(!System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty())
    tracingInstrumentation {
        enabled.set(true)
    }
}
