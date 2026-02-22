package io.foxbird.edgeai.util

import android.util.Log

object Logger {
    private const val DEFAULT_TAG = "EdgeAI"
    var appTag: String = DEFAULT_TAG
    var isDebugEnabled = true

    fun d(tag: String, message: String) {
        if (isDebugEnabled) Log.d("$appTag/$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$appTag/$tag", message)
    }

    fun w(tag: String, message: String) {
        Log.w("$appTag/$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$appTag/$tag", message, throwable)
        } else {
            Log.e("$appTag/$tag", message)
        }
    }
}
