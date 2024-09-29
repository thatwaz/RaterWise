plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt") // Kotlin annotation processing
    id("com.google.dagger.hilt.android") // Hilt for Dependency Injection
}

android {
    namespace = "com.thatwaz.raterwise"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thatwaz.raterwise"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Update to latest stable version
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.txt",
                "/META-INF/{AL2.0,LGPL2.1}"
            )
        }
    }
}




dependencies {

// Core and Compose dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room for local database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("com.google.code.gson:gson:2.8.9")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    // Lifecycle and ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // Navigation component for Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Accompanist libraries
    implementation("com.google.accompanist:accompanist-insets:0.31.1-alpha")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.31.1-alpha")

    // Material and UI libraries
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.6.8") // Ensure Compose UI library is included
    implementation("androidx.compose.ui:ui-text:1.6.8") // Include text input utilities

    // Dagger Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt(libs.hilt.android.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Unit Testing dependencies
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2") // Standard JUnit for unit testing
    testImplementation("androidx.arch.core:core-testing:2.1.0") // For InstantTaskExecutorRule
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For coroutines test
    testImplementation("io.mockk:mockk:1.13.5") // For mocking in tests
    testImplementation("org.slf4j:slf4j-api:2.0.5") // Updated SLF4J for better compatibility
    testImplementation("org.slf4j:slf4j-simple:2.0.5") // Simple binding for SLF4J

    // Android Instrumentation Tests
    androidTestImplementation(libs.androidx.junit)
    // JUnit 5 for unit tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // If you need to run JUnit 5 tests with Gradle
    testImplementation("org.junit.platform:junit-platform-runner:1.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")

    // JUnit 5 for Android instrumented tests (if applicable)
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    androidTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.1") // Compose UI testing

    // Debugging tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}