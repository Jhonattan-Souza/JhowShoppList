import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    jacoco
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

fun requiredGradleStringProperty(name: String): String {
    return providers.gradleProperty(name)
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing required Gradle property: $name")
}

val appVersionName = requiredGradleStringProperty("APP_VERSION_NAME")
val requireReleaseSigning = providers.gradleProperty("jhow.requireReleaseSigning")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

fun deriveVersionCodeFromSemver(versionName: String): Int {
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
        .matchEntire(versionName)
        ?: error("APP_VERSION_NAME must use MAJOR.MINOR.PATCH semver")

    val (major, minor, patch) = match.destructured
    val majorPart = major.toInt()
    val minorPart = minor.toInt()
    val patchPart = patch.toInt()

    check(minorPart in 0..99 && patchPart in 0..99) {
        "APP_VERSION_NAME minor and patch values must be between 0 and 99"
    }

    val derived = (majorPart * 10_000) + (minorPart * 100) + patchPart
    check(derived > 0) {
        "APP_VERSION_NAME must be greater than 0.0.0"
    }
    return derived
}

val appVersionCode = deriveVersionCodeFromSemver(appVersionName)

val releaseSigningMissingMessage =
    "Release signing is not configured. Set releaseStoreFile, releaseStorePassword, releaseKeyAlias, " +
        "and releaseKeyPassword in keystore.properties or the matching JHOW_SHOPPLIST_RELEASE_* environment variables."

fun signingValue(propertyName: String, envName: String): String? {
    val propertyValue = keystoreProperties.getProperty(propertyName)?.trim().orEmpty()
    if (propertyValue.isNotEmpty()) {
        return propertyValue
    }

    val envValue = System.getenv(envName)?.trim().orEmpty()
    return envValue.ifEmpty { null }
}

val releaseStoreFile = signingValue(
    propertyName = "releaseStoreFile",
    envName = "JHOW_SHOPPLIST_RELEASE_STORE_FILE"
)
val releaseStorePassword = signingValue(
    propertyName = "releaseStorePassword",
    envName = "JHOW_SHOPPLIST_RELEASE_STORE_PASSWORD"
)
val releaseKeyAlias = signingValue(
    propertyName = "releaseKeyAlias",
    envName = "JHOW_SHOPPLIST_RELEASE_KEY_ALIAS"
)
val releaseKeyPassword = signingValue(
    propertyName = "releaseKeyPassword",
    envName = "JHOW_SHOPPLIST_RELEASE_KEY_PASSWORD"
)
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.jhow.shopplist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jhow.shopplist"
        minSdk = 36
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testApplicationId = "com.jhow.shopplist.debug.test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8/minification is intentionally disabled: enabling it requires validated keep rules
            // for Room/Hilt/Compose and transitive dependencies (e.g. xpp3 via dav4jvm). The ProGuard
            // file references are kept so re-enabling isMinifyEnabled later picks up the standard
            // optimize config plus project-specific rules automatically.
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
    kotlin {
        compilerOptions {
            allWarningsAsErrors.set(true)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        // API 36 is the highest stable integer targetSdk currently available in this environment.
        // Lint's OldTargetApi warning becomes actionable again once a newer stable API level can be targeted.
        disable += setOf(
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "OldTargetApi"
        )
    }
}

val lintVariantTaskNames = setOf("lintDebug", "lintRelease")
val signingRequiredTaskNames = setOf("bundleRelease", "installRelease")

afterEvaluate {
    tasks.matching { task -> task.name in lintVariantTaskNames }
        .configureEach { dependsOn("detekt") }
}

tasks.matching { task -> task.name in signingRequiredTaskNames }
    .configureEach {
        doFirst {
            check(hasReleaseSigningConfig) { releaseSigningMissingMessage }
        }
    }

tasks.matching { task -> task.name == "assembleRelease" }.configureEach {
    doFirst {
        if (requireReleaseSigning) {
            check(hasReleaseSigningConfig) { releaseSigningMissingMessage }
        }
    }
    doLast {
        if (!hasReleaseSigningConfig && !requireReleaseSigning) {
            logger.warn(
                "assembleRelease completed without release signing. " +
                    "The generated release APK is unsigned and must not be distributed."
            )
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*_Impl*.*",
    "**/*_Factory*.*",
    "**/*_Provide*Factory*.*",
    "**/*_HiltModules*.*",
    "**/*Hilt*.*",
    "**/MainActivity*.*",
    "**/ShoppListApplication*.*",
    "**/DebugCommandReceiver*.*",
    "**/hilt_aggregated_deps/**",
    "**/*ComposableSingletons*.*"
)

val debugTree = fileTree("${layout.buildDirectory.get().asFile}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
    exclude(jacocoExcludes)
}

val debugJavaTree = fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
    exclude(jacocoExcludes)
}

tasks.register<JacocoReport>("jacocoFullReport") {
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(files(debugTree, debugJavaTree))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        files(
            fileTree("${layout.buildDirectory.get().asFile}/outputs/unit_test_code_coverage/debugUnitTest") {
                include("*.exec")
            },
            fileTree("${layout.buildDirectory.get().asFile}/outputs/code_coverage") {
                include("**/connected/**/*.ec")
            },
            fileTree("${layout.buildDirectory.get().asFile}/outputs/managed_device_code_coverage") {
                include("**/*.ec")
            }
        )
    )
}

tasks.register<JacocoCoverageVerification>("verifyDebugCoverage") {
    dependsOn("jacocoFullReport")

    classDirectories.setFrom(files(debugTree, debugJavaTree))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        files(
            fileTree("${layout.buildDirectory.get().asFile}/outputs/unit_test_code_coverage/debugUnitTest") {
                include("*.exec")
            },
            fileTree("${layout.buildDirectory.get().asFile}/outputs/code_coverage") {
                include("**/connected/**/*.ec")
            },
            fileTree("${layout.buildDirectory.get().asFile}/outputs/managed_device_code_coverage") {
                include("**/*.ec")
            }
        )
    )

    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.dav4jvm) {
        exclude(group = "junit", module = "junit")
    }
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.app.cash.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
