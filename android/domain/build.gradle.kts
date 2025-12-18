plugins {
    alias(libs.plugins.kotlin.android) // Using android plugin to allow android-library consumption, but keeping it pure where possible or just java-library
    alias(libs.plugins.android.library) // Pivot: easier to manage as android lib in mixed project, but will strictly avoid android.* imports in code
}

android {
    namespace = "com.codekhoda.domain"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
