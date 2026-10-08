import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ------------------------------------------------------------
// Load the single user-facing config file (config.properties).
// This is where APP_NAME / WEBSITE_URL come from automatically.
// ------------------------------------------------------------
val cfg = Properties().apply {
    val f = rootProject.file("config.properties")
    require(f.exists()) { "config.properties not found at project root. Create it with APP_NAME and WEBSITE_URL." }
    f.inputStream().use { load(it) }
}

val cfgAppName: String = cfg.getProperty("APP_NAME", "My Web App").trim()
val cfgUrl: String = cfg.getProperty("WEBSITE_URL", "https://example.com").trim()
val cfgAppId: String = cfg.getProperty("APP_ID", "com.webview.app").trim()
val cfgVersionName: String = cfg.getProperty("VERSION_NAME", "1.0").trim()
val cfgVersionCode: Int = cfg.getProperty("VERSION_CODE", "1").trim().toInt()

android {
    namespace = cfgAppId
    compileSdk = 34

    defaultConfig {
        applicationId = cfgAppId
        minSdk = 23
        targetSdk = 34
        versionCode = cfgVersionCode
        versionName = cfgVersionName

        // Inject the user's values so code + resources pick them up.
        resValue("string", "app_name", cfgAppName)
        buildConfigField("String", "WEBSITE_URL", "\"$cfgUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Debug signing so `assembleRelease` yields an installable APK
            // without you needing to create a keystore first.
            signingConfig = signingConfigs.getByName("debug")
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.webkit:webkit:1.11.0")
}
