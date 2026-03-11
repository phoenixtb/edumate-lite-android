package io.foxbird.edgeai.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Discriminated token event produced by [parseThinkTags].
 *
 * - [ContentToken] — normal response text to surface to the user.
 * - [ThinkingToken] — internal chain-of-thought text emitted between the model's
 *   think tags (e.g. `<think>…</think>` for Qwen3/3.5).
 */
sealed class StreamEvent {
    data class ContentToken(val text: String) : StreamEvent()
    data class ThinkingToken(val text: String) : StreamEvent()
}

/**
 * Transforms a raw token [Flow] into a [Flow] of [StreamEvent] by splitting
 * content that falls between [openTag]/[closeTag] into [StreamEvent.ThinkingToken]
 * events, and everything else into [StreamEvent.ContentToken] events.
 *
 * Handles tokens that arrive mid-tag (partial tag buffering) so the parsing is
 * robust to any tokenisation granularity.
 *
 * If [openTag] is null the raw tokens are emitted unchanged as [StreamEvent.ContentToken].
 */
fun Flow<String>.parseThinkTags(
    openTag: String?,
    closeTag: String?
): Flow<StreamEvent> {
    if (openTag == null || closeTag == null) {
        return flow { collect { emit(StreamEvent.ContentToken(it)) } }
    }

    return flow {
        val buffer = StringBuilder()
        var inThinking = false

        collect { token ->
            buffer.append(token)

            // Greedily flush the buffer, emitting completed segments.
            while (true) {
                if (inThinking) {
                    val closeIdx = buffer.indexOf(closeTag)
                    if (closeIdx >= 0) {
                        // Flush thinking content up to (but not including) close tag.
                        if (closeIdx > 0) emit(StreamEvent.ThinkingToken(buffer.substring(0, closeIdx)))
                        buffer.delete(0, closeIdx + closeTag.length)
                        inThinking = false
                    } else {
                        // Might be mid-close-tag at the end of buffer — keep a tail suffix.
                        val safeLen = buffer.length - (closeTag.length - 1)
                        if (safeLen > 0) {
                            emit(StreamEvent.ThinkingToken(buffer.substring(0, safeLen)))
                            buffer.delete(0, safeLen)
                        }
                        break
                    }
                } else {
                    val openIdx = buffer.indexOf(openTag)
                    if (openIdx >= 0) {
                        // Flush content before the open tag.
                        if (openIdx > 0) emit(StreamEvent.ContentToken(buffer.substring(0, openIdx)))
                        buffer.delete(0, openIdx + openTag.length)
                        inThinking = true
                    } else {
                        // Might be mid-open-tag at the end of buffer — keep a tail suffix.
                        val safeLen = buffer.length - (openTag.length - 1)
                        if (safeLen > 0) {
                            emit(StreamEvent.ContentToken(buffer.substring(0, safeLen)))
                            buffer.delete(0, safeLen)
                        }
                        break
                    }
                }
            }
        }

        // Flush any remaining buffer after stream ends.
        if (buffer.isNotEmpty()) {
            if (inThinking) emit(StreamEvent.ThinkingToken(buffer.toString()))
            else emit(StreamEvent.ContentToken(buffer.toString()))
        }
    }
}
