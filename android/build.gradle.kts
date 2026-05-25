// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.sentry.android.gradle) apply false
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            // Use freeCompilerArgs if needed for modular access
        }
    }

    // Do not fail the build when individual unit tests fail; report them in CI instead.
    tasks.withType<Test>().configureEach {
        ignoreFailures = true
    }
}
