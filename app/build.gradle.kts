plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.webpoema.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.webpoema.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Shared "poem of the day" published by the CDK stack. Override with
        // -PtodayJsonUrl=... (e.g. after deploying to another account/region).
        val todayJsonUrl = (project.findProperty("todayJsonUrl") as String?)
            ?: "https://poemoftheday-outb8d7ccdd-gydlpcby3qdo.s3.us-east-1.amazonaws.com/today.json"
        buildConfigField("String", "TODAY_JSON_URL", "\"$todayJsonUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.webkit:webkit:1.10.0")
}
