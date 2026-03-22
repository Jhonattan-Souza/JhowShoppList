// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                allWarningsAsErrors.set(true)
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        if (!name.contains("hilt", ignoreCase = true)) {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}
