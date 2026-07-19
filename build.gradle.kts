plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.paparazzi) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    // Applied for real (not `apply false`): the aggregated jacocoFullReport
    // task below lives on the root project and needs the plugin's conventions
    // (jacocoClasspath, report defaults) here, not just in subprojects.
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

// ─── Unit-test coverage (JaCoCo) ────────────────────────────────────────────
//
// Every module's testDebugUnitTest run writes a build/jacoco/*.exec file;
// `./gradlew jacocoFullReport` merges them into one HTML + XML report at
// build/reports/jacoco/jacocoFullReport/. CI runs it on every PR and posts a
// per-area summary to the job page.

subprojects {
    apply(plugin = "jacoco")

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            // Robolectric loads instrumented classes with no on-disk location;
            // without this its tests contribute nothing to the report.
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}

// Generated code has no behavior of its own — measuring it only dilutes the
// numbers for the code we actually write.
val coverageExclusions = listOf(
    // Android build artifacts
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    // Hilt / Dagger generated
    "**/*_Factory*.*",
    "**/*_MembersInjector*.*",
    "**/Hilt_*.*",
    "**/*_HiltModules*.*",
    "**/dagger/**",
    "**/hilt_aggregated_deps/**",
    "**/di/**",
    // Room generated
    "**/*_Impl*.*",
    // Compose compiler generated
    "**/ComposableSingletons*.*",
)

tasks.register<JacocoReport>("jacocoFullReport") {
    group = "verification"
    description = "Aggregated unit-test line coverage across all modules."

    val coveredModules = subprojects.filter { it.subprojects.isEmpty() }
    dependsOn(coveredModules.map { "${it.path}:testDebugUnitTest" })

    executionData.setFrom(
        // The Gradle jacoco plugin writes build/jacoco/<task>.exec; AGP's own
        // coverage support writes under build/outputs/unit_test_code_coverage.
        // Accept either so the report never silently skips on empty data.
        fileTree(rootDir) {
            include(
                "**/build/jacoco/*.exec",
                "**/build/outputs/unit_test_code_coverage/**/*.exec",
            )
        }
    )
    classDirectories.setFrom(
        coveredModules.map { module ->
            fileTree(module.layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(coverageExclusions)
            }
        }
    )
    sourceDirectories.setFrom(coveredModules.map { "${it.projectDir}/src/main/java" })

    reports {
        xml.required.set(true)
        html.required.set(true)
        // Pinned so the CI summary/upload steps always find them.
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoFullReport/jacocoFullReport.xml")
        )
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/jacoco/jacocoFullReport/html")
        )
    }
}
