import java.io.File
import java.util.Properties

// --- Auto-incrementing build version -------------------------------------
// Android requires versionCode to be a strictly monotonic integer (equal or
// lower => the hub sees it as the same/older install and may preserve data),
// while versionName is the human-readable string shown on the device.
//
// We hand out a NEW number per SUCCESSFUL `gradlew assembleDebug` by keeping a
// counter in the gitignored Gradle cache dir (`android/.gradle/hubdash-version/`).
// Storing it there (NOT under `build/`) means it survives `gradlew clean`, and
// it is never committed to git.
val _versionDir = File(project.rootDir, ".gradle/hubdash-version")
val _versionFile = File(_versionDir, "build.properties")

fun _lastBuildNumber(): Int {
    if (!_versionFile.exists()) return 0
    try {
        val props = Properties()
        _versionFile.inputStream().use { props.load(it) }
        return (props.getProperty("buildNumber") ?: "0").toInt()
    } catch (_: Exception) {
        return 0
    }
}

// Read once at configuration time. The file is only mutated AFTER a successful
// build, so a failed build reuses the same number and nothing is skipped.
val _buildNumber: Int = _lastBuildNumber()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tim.hubitatdash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tim.hubitatdash"
        minSdk = 26
        targetSdk = 35
        versionCode = _buildNumber + 1
        versionName = "1.0." + (_buildNumber + 1)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.bcrypt)
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play)
    implementation(libs.osmdroid.android)
    debugImplementation(libs.compose.ui.tooling)
}

// Persist the incremented build number AFTER a successful build. doLast only
// runs when the task completes successfully, so a failed build leaves the
// counter untouched and reuses the number on retry. tasks.matching(...).
//configureEach picks up both `assemble` and `assembleDebug` regardless of
// creation order.
tasks.matching { it.name == "assembleDebug" || it.name == "assemble" }.configureEach {
    doLast {
        _versionDir.mkdirs()
        val props = Properties()
        props.setProperty("buildNumber", ( _buildNumber + 1).toString())
        _versionFile.outputStream().use { props.store(it, "Incremented per successful Android build") }
        println("  ✔ Build version 1.0." + (_buildNumber + 1) + " (versionCode " + (_buildNumber + 1) + ")")
    }
}
