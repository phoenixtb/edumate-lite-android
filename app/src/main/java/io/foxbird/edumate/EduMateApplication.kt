package io.foxbird.edumate

import android.app.Application
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.di.appModules
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class EduMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.appTag = "EduMate"
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@EduMateApplication)
            modules(appModules)
        }

        val memoryMonitor: MemoryMonitor by inject()
        val orchestrator: EngineOrchestrator by inject()
        memoryMonitor.setOnCritical { orchestrator.handleMemoryPressure() }
        memoryMonitor.startMonitoring()
    }
}
