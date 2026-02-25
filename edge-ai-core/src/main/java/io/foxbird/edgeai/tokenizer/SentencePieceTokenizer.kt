package io.foxbird.edgeai.tokenizer

import io.foxbird.edgeai.util.Logger
import org.tensorflow.lite.support.text.SentencepieceTokenizer
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Wraps the TFLite Support Library's native SentencePiece JNI tokenizer.
 *
 * The `.model` file is memory-mapped once at construction time for efficiency.
 * Thread-safe for read-only encode calls; [close] must be called when done.
 *
 * @param modelPath Absolute path to the `sentencepiece.model` file.
 */
class SentencePieceTokenizer(modelPath: String) : Tokenizer {

    companion object {
        private const val TAG = "SentencePieceTokenizer"
        // Gemma BOS token ID (beginning-of-sequence)
        private const val BOS_ID = 2
    }

    private val tokenizer: SentencepieceTokenizer

    init {
        val buffer = FileInputStream(modelPath).channel.use { channel ->
            channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        }
        tokenizer = SentencepieceTokenizer(buffer)
        Logger.d(TAG, "SentencePiece tokenizer loaded from: $modelPath")
    }

    /**
     * Encodes [text] into SentencePiece token IDs.
     *
     * Prepends BOS token (ID=2) as required by Gemma-based embedding models.
     * Result is truncated to [maxLength] if necessary.
     */
    override fun encode(text: String, maxLength: Int): IntArray {
        return try {
            val result = tokenizer.tokenize(text)
            // result.ids gives the SentencePiece integer IDs for each piece
            val rawIds = result.ids
            val ids = IntArray(minOf(rawIds.size + 1, maxLength))
            ids[0] = BOS_ID
            for (i in 1 until ids.size) {
                ids[i] = rawIds[i - 1]
            }
            ids
        } catch (e: Exception) {
            Logger.e(TAG, "Tokenization failed, returning empty sequence", e)
            IntArray(0)
        }
    }

    override fun close() {
        // SentencepieceTokenizer does not implement Closeable; no-op but defined for future safety
    }
}
