package io.foxbird.doclibrary.config

/**
 * Feature flags for toggling experimental or in-progress doc-library capabilities.
 * Provided as a Koin singleton; change values here to enable a feature globally.
 */
data class FeatureFlags(
    val vlmExtractionEnabled: Boolean = false
)
