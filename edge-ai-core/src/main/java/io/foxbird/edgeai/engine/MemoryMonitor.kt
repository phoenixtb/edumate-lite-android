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

enum class MemoryPressure { NORMAL, MODERATE, CRITICAL }

data class MemorySnapshot(
    val totalMb: Long,
    val availableMb: Long,
    val usedPercent: Float,
    val pressure: MemoryPressure,
    val isLowMemory: Boolean
)

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
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedPercent = 1f - (availMb.toFloat() / totalMb)
        val pressure = when {
            usedPercent >= CRITICAL_THRESHOLD -> MemoryPressure.CRITICAL
            usedPercent >= MODERATE_THRESHOLD -> MemoryPressure.MODERATE
            else -> MemoryPressure.NORMAL
        }
        return MemorySnapshot(totalMb, availMb, usedPercent, pressure, memInfo.lowMemory)
    }

    fun canAllocateMb(requiredMb: Long): Boolean {
        return getSnapshot().availableMb > requiredMb + 200
    }
}
