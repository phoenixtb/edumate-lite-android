// Placeholder module — no implementation yet.
// Future: implement IAgentOrchestrator using Koog (github.com/JetBrains/koog)
// Koog v0.6.2 (Feb 2026) — Kotlin multiplatform, Android-compatible, Apache 2.0
// Will wrap EngineOrchestrator as a KoogLiteRtLlm provider for multi-step tool-use agents.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.foxbird.agentcore"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":edge-ai-core"))
    api(project(":doc-library"))
}
