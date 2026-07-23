import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "com.podbelly"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.podbelly"
        minSdk = 26
        targetSdk = 35
        versionCode = 93
        versionName = "1.6.45"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            val keystorePath = System.getenv("DEBUG_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        // Release/upload signing. Populated only from env vars so the keystore
        // never lives in the repo; CI decodes the secret to a temp file and
        // exports UPLOAD_KEYSTORE_PATH. When the env vars are absent (local
        // dev, PR CI) the config stays empty and release builds are produced
        // unsigned — enough to verify the bundle assembles.
        create("release") {
            val keystorePath = System.getenv("UPLOAD_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Only attach the signing config when a keystore is actually
            // configured; otherwise leave the build unsigned so `bundleRelease`
            // still succeeds in CI without the signing secrets.
            signingConfig = if (System.getenv("UPLOAD_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            configure<CrashlyticsExtension> {
                // Only push the R8 mapping to Firebase for a real (signed)
                // release upload; unsigned verify builds stay offline.
                mappingFileUploadEnabled = System.getenv("UPLOAD_KEYSTORE_PATH") != null
            }
        }
        debug {
            isMinifyEnabled = false
            // Unit-test coverage for the root jacocoFullReport task.
            enableUnitTestCoverage = true
            signingConfig = signingConfigs.getByName("debug")
            firebaseAppDistribution {
                artifactType = "APK"
                groups = project.findProperty("appDistributionGroup")?.toString() ?: "testers"
                releaseNotes = project.findProperty("appDistributionReleaseNotes")?.toString()
            }
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
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Disable release unit tests -- R8 minification conflicts with Robolectric
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach {
    enabled = false
}

// ── Google Play publishing (Gradle Play Publisher) ─────────────────────
// Uploads the signed release App Bundle to the Play Console. Credentials are
// supplied by CI via the PLAY_SERVICE_ACCOUNT_JSON env var (a service-account
// key). Only the `publish*` tasks need them, so ordinary builds are unaffected.
// First releases target the `internal` testing track; promote in the console.
play {
    System.getenv("PLAY_SERVICE_ACCOUNT_JSON")?.let {
        serviceAccountCredentials.set(file(it))
    }
    defaultToAppBundles.set(true)
    track.set(System.getenv("PLAY_TRACK")?.takeIf { it.isNotBlank() } ?: "internal")
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:playback"))
    implementation(project(":core:common"))
    implementation(project(":feature:home"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:podcast"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.firebase.appdistribution.api)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.firebase.appdistribution)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.navigation.testing)
    testImplementation(libs.work.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
