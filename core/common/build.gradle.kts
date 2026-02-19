plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.podbelly.core.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(project(":core:database"))
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
