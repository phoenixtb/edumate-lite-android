package io.foxbird.edgeai.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class DeviceInfo(private val context: Context) {

    private val activityManager: ActivityManager
        get() = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun getTotalRamGB(): Float {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024f * 1024f * 1024f)
    }

    fun getAvailableRamGB(): Float {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024f * 1024f * 1024f)
    }

    fun getOptimalThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return (cores / 2).coerceIn(2, 4)
    }

    fun getDeviceSummary(): DeviceSummary {
        return DeviceSummary(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            totalRamGB = String.format("%.1f", getTotalRamGB()),
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }
}

data class DeviceSummary(
    val manufacturer: String,
    val model: String,
    val totalRamGB: String,
    val cpuCores: Int
)
