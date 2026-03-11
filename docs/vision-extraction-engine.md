# Vision Extraction Engine — Feature Spec

## Problem

The current ingestion pipeline uses ML Kit Text Recognition (OCR) for scanned pages and image documents. OCR extracts printed characters but is **semantically blind** to visual content:

- A circuit diagram → returns only component labels, not the circuit topology
- A biology figure → returns the caption, not what the diagram shows
- A maths page with rendered equations → returns LaTeX source if it was typeset, nothing if hand-drawn
- A flowchart → returns node labels, not the flow relationships

Vision-language models (VLMs) understand images semantically. Gemma 3n and Qwen3-VL (two models already in the project's model catalogue) can describe diagrams, extract equation meaning, and produce rich structured text from any page image — the same quality a human reader would achieve.

---

## Goal

Add a `VisionGenerationEngine` that feeds page bitmaps to a VLM natively (not as base64 in a text prompt). Integrate it as a third extraction path in `DocumentProcessor.THOROUGH` mode, sitting above MLKit OCR for pages that contain diagrams or low text density.

---

## Architecture

```
PdfInputAdapter.renderPageToImage(uri, idx) → Bitmap
        │
        ▼
extractionMethod decision (per page in THOROUGH mode):
  ├─ page.text has ≥ MIN_WORDS → keep PDFBox text           [fast, accurate]
  ├─ page.isScanned, orchestrator.supportsVision() == false → MLKit OCR   [fast, text-only]
  └─ page.isScanned OR hasDiagrams hint, orchestrator.supportsVision() → VisionEngine.describe(bitmap)
                                                                          [slower, semantic]
```

---

## Engine: MediaPipe LLM Inference for LiteRT (Gemma 3n)

### Why MediaPipe, not litertlm

The project's current `LiteRtGenerationEngine` uses `com.google.ai.edge.litertlm` which only accepts text prompts. MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai`, already in deps at `0.10.32`) exposes `LlmInference` with a multimodal session API:

```kotlin
val session = llmInference.createSession(
    LlmInference.LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setTopK(40).setTemperature(0.1f).build()
)
session.addQueryChunk(InputImage.createFromBitmap(bitmap, 0))
session.addQueryChunk("Describe all text, equations, and diagrams visible on this page. Be thorough.")
val result = session.generateResponse()   // or generateResponseAsync for streaming
```

The model file format is `.task` — same extension Gemma models are already downloaded in.

### Required dependency addition

```kotlin
// already present in edge-ai-core/build.gradle.kts
implementation(libs.mediapipe.tasks.genai)  // com.google.mediapipe:tasks-genai:0.10.32
```

No new dependency needed — it is already imported for other MediaPipe use.

---

## Engine: Qwen3-VL via llama.cpp (GGUF)

Qwen3-VL GGUF requires two files:
- `qwen3-vl-4b-Q4_K_M.gguf` — the main LLM weights
- `mmproj-qwen3-vl-4b.gguf` — the multimodal projector (CLIP-style image encoder)

Llamatik 0.16.0 does **not** expose a multimodal API. Options:

1. **Wait for Llamatik update** — track https://github.com/llamatik/llamatik for vision support
2. **Custom JNI bridge** — thin Kotlin/JNI layer calling `llama_model_load` + `clip_model_load` + `llava_image_embed_make_with_bytes` from llama.cpp directly. Significant native build effort.
3. **Use MediaPipe path only** — restrict vision extraction to Gemma 3n `.task` models for now; add GGUF vision later when the library supports it.

**Recommendation:** Ship with MediaPipe/Gemma path (option 3), defer GGUF vision.

---

## New Interfaces and Classes

### `GenerationEngine` — add optional vision method

```kotlin
// edge-ai-core/.../engine/GenerationEngine.kt
interface GenerationEngine {
    // ... existing methods ...

    /** True if this engine can process image inputs. Default false. */
    fun supportsVision(): Boolean = false

    /**
     * Generate a text response from a prompt + image. Default impl returns an unsupported error so
     * existing engines don't need to change.
     */
    suspend fun generateWithImage(
        prompt: String,
        bitmap: Bitmap,
        params: GenerationParams = GenerationParams()
    ): AppResult<String> = AppError.Llm.GenerationFailed("Vision not supported by this engine").left()
}
```

### `EngineCapabilities` — add vision flag

```kotlin
data class EngineCapabilities(
    val supportsGpu: Boolean,
    val supportsNpu: Boolean,
    val supportsStreaming: Boolean,
    val supportedFormats: Set<String>,
    val supportsVision: Boolean = false      // new
)
```

### `ModelConfig` — add vision projector path

```kotlin
data class ModelConfig(
    // ... existing fields ...
    val visionProjectorPath: String? = null,   // for GGUF vision models (future)
    val supportsVision: Boolean = false
)
```

### `MediaPipeVisionEngine : GenerationEngine`

```
edge-ai-core/src/main/java/io/foxbird/edgeai/engine/MediaPipeVisionEngine.kt
```

Key responsibilities:
- Load a `.task` model via `LlmInference.createFromOptions()`
- Implement `supportsVision() = true`
- Implement `generateWithImage(prompt, bitmap, params)` using `LlmInferenceSession.addQueryChunk(InputImage)`
- Implement `generate(prompt, params)` for text-only use (same session, no image chunk) so it can serve as a drop-in inference engine too
- Implement `tokenCount(text)` via character estimation (MediaPipe doesn't expose tokenizer)
- **Does NOT implement `EmbeddingEngine`** — keep embedding on `LiteRtEmbeddingEngine`

### `EngineOrchestrator` — add vision delegation

```kotlin
fun supportsVision(): Boolean =
    _activeInferenceEngine.value?.let { generationEngines[it]?.supportsVision() } == true

suspend fun generateWithImage(prompt: String, bitmap: Bitmap, params: GenerationParams): AppResult<String> {
    val engine = _activeInferenceEngine.value?.let { generationEngines[it] }
        ?: return AppError.Llm.GenerationFailed("No inference model loaded").left()
    return engine.generateWithImage(prompt, bitmap, params)
}
```

---

## Ingestion Integration

### `PdfInputAdapter` — add `extractPageViaVlm(uri, pageIndex, orchestrator)`

Rename the current placeholder back from `extractPageViaOcr` to a dispatch method:

```kotlin
suspend fun extractPageBest(uri: Uri, pageIndex: Int, orchestrator: EngineOrchestrator): ExtractedPage {
    val bitmap = renderPageToImage(uri, pageIndex)
        ?: return ExtractedPage(pageIndex + 1, "", extractionMethod = "render_failed", isScanned = true)

    return if (orchestrator.supportsVision()) {
        extractPageViaVlm(bitmap, pageIndex, orchestrator)   // semantic, diagram-aware
    } else {
        extractPageViaOcr(bitmap, pageIndex)                 // MLKit, text-only
    }
}
```

### `DocumentProcessor.THOROUGH` mode

```kotlin
ProcessingMode.THOROUGH -> {
    textPages.forEachIndexed { idx, textPage ->
        val wordCount = ...
        val needsVisual = textPage.isScanned || wordCount < MIN_WORDS_FOR_TEXT_EXTRACTION
        val finalPage = if (needsVisual) {
            pdfAdapter.extractPageBest(uri, idx, orchestrator)   // VLM if available, else OCR
        } else {
            textPage
        }
        rawPages.add(finalPage)
    }
}
```

### `ImageInputAdapter` — same dispatch pattern

```kotlin
suspend fun extractText(uri: Uri, pageNumber: Int, orchestrator: EngineOrchestrator): ExtractedPage =
    if (orchestrator.supportsVision()) extractViaVlm(uri, pageNumber, orchestrator)
    else extractViaOcr(uri, pageNumber)
```

---

## Model Catalogue Changes

### New model entry type in `AppModelConfigs`

```kotlin
ModelConfig(
    id = "gemma-3n-4b-vision",
    displayName = "Gemma 3n 4B (Vision)",
    fileName = "gemma-3n-E4B-it-int4.task",
    engineType = EngineType.MEDIA_PIPE,    // new enum value
    contextSize = 8192,
    supportsVision = true,
    // ... download URL, size, etc.
)
```

### `EngineType` enum

Add `MEDIA_PIPE` alongside `LLAMA_CPP` and `LITE_RT`.

### `ModelManager` / `EngineOrchestrator.loadModel()`

When `config.engineType == EngineType.MEDIA_PIPE`, route to `MediaPipeVisionEngine` instead of `LiteRtGenerationEngine`.

---

## VLM Prompt Design

```
You are an academic document extraction assistant. The image shows a single page from an educational document.

Extract ALL of the following:
1. All visible text, preserving paragraph structure and headings.
2. All mathematical equations — write them in LaTeX notation (e.g., \frac{a}{b}, \sum_{i=0}^{n}).
3. Describe any diagrams, figures, or charts: what type it is, what it shows, and key labels.
4. Any tables — reproduce them as markdown tables.

Output only the extracted content. No commentary.
```

Keep `maxTokens = 2048` for a full page. Temperature = 0.1.

---

## Fallback Chain (per page in THOROUGH mode)

```
PDFBox text (≥ MIN_WORDS)           → use as-is
PDFBox text (< MIN_WORDS, no vision) → MLKit OCR
PDFBox text (< MIN_WORDS, vision)   → VLM describe
VLM fails                           → MLKit OCR as fallback
MLKit OCR empty                     → log warning, empty page
```

`extractionMethod` field on `PageEntity` tracks which path was used: `"text"`, `"ocr"`, `"vlm"`, `"vlm_fallback_ocr"`, `"ocr_empty"`.

---

## Implementation Order

1. Add `supportsVision()` + `generateWithImage()` defaults to `GenerationEngine` interface — **no breaking change**
2. Add `MEDIA_PIPE` to `EngineType` enum and `supportsVision` to `ModelConfig`
3. Implement `MediaPipeVisionEngine` (text + vision)
4. Wire into `EngineOrchestrator.loadModel()` routing
5. Add `extractPageBest()` dispatch to `PdfInputAdapter` and `ImageInputAdapter`
6. Update `DocumentProcessor.THOROUGH` to call `extractPageBest`
7. Add Gemma 3n vision model entry to `AppModelConfigs`
8. UI: show "Vision" badge on models that support it; surface `extractionMethod = "vlm"` in document detail

---

## Open Questions

- **Memory**: MediaPipe `LlmInference` and the existing `LiteRtEmbeddingEngine` (GemmaEmbeddingModel) cannot both be resident simultaneously on a 6 GB device. Strategy: unload embedding model before loading vision engine, or require the user to pick one mode.
- **Speed**: VLM page description is 5–30 seconds per page depending on device. THOROUGH mode on a 200-page textbook with many diagram pages could take 30+ minutes. Consider a cap: "vision extraction for first N pages, OCR for the rest."
- **Model download size**: Gemma 3n 4B `.task` is ~3.5 GB. Add a separate download prompt in the model picker that warns about size and explains the vision capability.
- **Qwen3-VL GGUF**: Revisit when Llamatik exposes `llava_image_embed_*` APIs or when a community wrapper appears.
