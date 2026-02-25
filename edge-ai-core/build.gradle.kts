plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.foxbird.edgeai"
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
    // Llamatik (llama.cpp Kotlin wrapper)
    api(libs.llamatik)

    // LiteRT (on-device AI with hardware acceleration)
    api(libs.litert.core)
    api(libs.litert.lm)

    // TFLite Support — provides SentencePiece JNI tokenizer for embedding models
    implementation(libs.tensorflow.lite.support)

    // Arrow (functional error handling)
    api(libs.arrow.core)

    // Kotlinx
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Ktor Client (model downloads)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}
