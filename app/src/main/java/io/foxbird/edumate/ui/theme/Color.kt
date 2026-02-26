package io.foxbird.edumate.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Accent ──────────────────────────────────────────────────────────
val EduPurple      = Color(0xFF7C4DFF)
val EduPurpleLight = Color(0xFFB47CFF)
val EduPurpleDark  = Color(0xFF6200EA)
val EduIndigo      = Color(0xFF5C6BC0)

val EduTeal   = Color(0xFF00BFA5)
val EduAmber  = Color(0xFFFFAB00)
val EduRose   = Color(0xFFFF5252)
val EduBlue   = Color(0xFF448AFF)
val EduGreen  = Color(0xFF69F0AE)

// ── Status badges ────────────────────────────────────────────────────────────
val StatusActive              = Color(0xFF4CAF50)
val StatusActiveContainer     = Color(0xFF1B3A1B)
val StatusExperimental        = Color(0xFFFF5252)
val StatusExperimentalContainer = Color(0xFF3A1B1B)
val StatusBundled             = Color(0xFF7C4DFF)
val StatusBundledContainer    = Color(0xFF2A1B4E)
val StatusDownloading         = Color(0xFF448AFF)
val StatusDownloadingContainer = Color(0xFF1B2A4E)

// ── Processing pipeline step colors ─────────────────────────────────────────
val PipelineComplete = Color(0xFF4CAF50)
val PipelineActive   = Color(0xFF7C4DFF)
val PipelinePending  = Color(0xFF757575)

// ── Feature card palette (Add-Material banner + Processing card) ─────────────
// Sampled from the Flutter reference design (FlexScheme.indigo dark, blended).
// These are intentional brand surface tokens, not generic M3 containers.
val FeatureCardBody    = Color(0xFF1E2870)   // card / banner background
val FeatureCardHeader  = Color(0xFF141C62)   // darker strip inside card
val FeatureCardAccent  = Color(0xFF3D55C5)   // icon bg, badge, progress fill
val FeatureCardOnColor = Color(0xFFDDE1FF)   // primary text on card
val FeatureCardSubtext = Color(0xFFB0BAFF)   // secondary text on card
val FeatureCardPending = Color(0xFF3A3F70)   // pending step circle bg
