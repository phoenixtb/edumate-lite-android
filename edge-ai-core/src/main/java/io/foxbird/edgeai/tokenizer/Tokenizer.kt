package io.foxbird.edgeai.tokenizer

/**
 * Contract for text-to-token-ID encoding. Implementations are responsible for loading
 * the vocab/model file and performing the actual piece splitting and ID lookup.
 *
 * Token IDs are integers in the model's vocabulary space. Callers are responsible
 * for padding/truncating the returned array to the model's max sequence length.
 */
interface Tokenizer {
    /** Encode [text] into a sequence of vocabulary IDs, truncated to [maxLength]. */
    fun encode(text: String, maxLength: Int = 2048): IntArray

    /** Attention mask corresponding to the last [encode] call: 1 for real tokens, 0 for padding. */
    fun attentionMask(encodedLength: Int, maxLength: Int): IntArray =
        IntArray(maxLength) { if (it < encodedLength) 1 else 0 }

    fun close()
}
