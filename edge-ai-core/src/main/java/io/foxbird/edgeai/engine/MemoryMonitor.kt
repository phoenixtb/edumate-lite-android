package io.foxbird.edgeai.engine

import android.app.ActivityManager
import android.content.Context
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class MemoryPressure { NORMAL, MODERATE, CRITICAL }

data class MemorySnapshot(
    val totalMb: Long,
    /** Physical available RAM from /proc/meminfo MemAvailable — more accurate than ActivityManager */
    val availableMb: Long,
    /** Swap total in MB — includes RAM Boost (virtual RAM) on Nothing Phone etc. */
    val swapTotalMb: Long = 0L,
    /** Swap free in MB */
    val swapFreeMb: Long = 0L,
    val usedPercent: Float,
    val pressure: MemoryPressure,
    val isLowMemory: Boolean
) {
    /** Effective available memory: physical available + swap free (conservative: swap at 50% weight due to speed) */
    val effectiveAvailableMb: Long get() = availableMb + (swapFreeMb / 2)
    val hasSwap: Boolean get() = swapTotalMb > 0L
}

class MemoryMonitor(private val context: Context) {

    companion object {
        private const val TAG = "MemoryMonitor"
        private const val MODERATE_THRESHOLD = 0.75f
        private const val CRITICAL_THRESHOLD = 0.90f
        private const val POLL_INTERVAL_MS = 5000L
    }

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _snapshot = MutableStateFlow(getSnapshot())
    val snapshot: StateFlow<MemorySnapshot> = _snapshot.asStateFlow()

    private var onCritical: (suspend () -> Unit)? = null

    fun setOnCritical(handler: suspend () -> Unit) {
        onCritical = handler
    }

    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val snap = getSnapshot()
                _snapshot.value = snap
                if (snap.pressure == MemoryPressure.CRITICAL) {
                    Logger.w(TAG, "Critical memory: ${snap.availableMb}MB free (${snap.usedPercent}% used)")
                    onCritical?.invoke()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun getSnapshot(): MemorySnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)

        val procInfo = readProcMeminfo()
        // /proc/meminfo MemAvailable accounts for reclaimable caches — better than ActivityManager
        val availMb = (procInfo["MemAvailable"] ?: memInfo.availMem) / (1024L * 1024L)
        val swapTotalMb = (procInfo["SwapTotal"] ?: 0L) / (1024L * 1024L)
        val swapFreeMb = (procInfo["SwapFree"] ?: 0L) / (1024L * 1024L)

        val usedPercent = 1f - (availMb.toFloat() / totalMb.coerceAtLeast(1L))
        val pressure = when {
            usedPercent >= CRITICAL_THRESHOLD -> MemoryPressure.CRITICAL
            usedPercent >= MODERATE_THRESHOLD -> MemoryPressure.MODERATE
            else -> MemoryPressure.NORMAL
        }
        return MemorySnapshot(totalMb, availMb, swapTotalMb, swapFreeMb, usedPercent, pressure, memInfo.lowMemory)
    }

    /**
     * Reads /proc/meminfo and returns values in bytes (converted from kB).
     * Keys: MemTotal, MemFree, MemAvailable, SwapTotal, SwapFree, etc.
     */
    private fun readProcMeminfo(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        try {
            File("/proc/meminfo").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val key = parts[0].trimEnd(':')
                        val kbValue = parts[1].toLongOrNull() ?: return@forEachLine
                        result[key] = kbValue * 1024L
                    }
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Could not read /proc/meminfo: ${e.message}")
        }
        return result
    }

    fun canAllocateMb(requiredMb: Long): Boolean {
        return getSnapshot().effectiveAvailableMb > requiredMb + 200
    }
}
