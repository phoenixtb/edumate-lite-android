plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.foxbird.doclibrary"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Edge AI Core (IEmbeddingService, EngineOrchestrator, Logger, AppResult)
    api(project(":edge-ai-core"))

    // Room Database
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Koin DI (api so :app screens can call koinViewModel<LibraryViewModel>())
    api(libs.koin.android)
    api(libs.koin.androidx.compose)

    // PDF extraction
    api(libs.pdfbox.android)

    // Serialization + Coroutines
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)

    // ViewModel
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)

    // Arrow (functional error handling, re-exported for consumers)
    api(libs.arrow.core)
}
