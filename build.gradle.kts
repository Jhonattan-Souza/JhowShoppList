// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        if (!name.contains("hilt", ignoreCase = true)) {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}
