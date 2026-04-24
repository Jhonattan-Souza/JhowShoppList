import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)
}

// Prevent versionCatalogUpdate from stripping versions referenced only via libs.versions.xxx.get()
versionCatalogUpdate {
    keep {
        keepUnusedVersions = true
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<DetektExtension> {
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        allRules = false
        source.setFrom("$projectDir/src/main")
    }

    tasks.withType<JavaCompile>().configureEach {
        if (!name.contains("hilt", ignoreCase = true)) {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}
