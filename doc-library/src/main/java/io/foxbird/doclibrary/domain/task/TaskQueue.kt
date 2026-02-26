package io.foxbird.doclibrary.domain.task

import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

data class AiTask(
    val id: String,
    val type: TaskType,
    val title: String,
    val priority: Int = 0,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Float = 0f,
    val error: String? = null,
    val action: suspend (onProgress: (Float) -> Unit) -> Unit
)

enum class TaskType {
    DOCUMENT_PROCESSING,
    CONCEPT_EXTRACTION,
    WORKSHEET_GENERATION,
    MODEL_DOWNLOAD
}

enum class TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}

class TaskQueue(private val scope: CoroutineScope) {

    companion object {
        private const val TAG = "TaskQueue"
    }

    private val mutex = Mutex()
    private val pending = ConcurrentLinkedQueue<AiTask>()

    private val _tasks = MutableStateFlow<List<AiTask>>(emptyList())
    val tasks: StateFlow<List<AiTask>> = _tasks.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun enqueue(task: AiTask) {
        pending.add(task)
        updateTaskList()
        processNext()
    }

    fun cancel(taskId: String) {
        pending.removeAll { it.id == taskId }
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId && task.status == TaskStatus.PENDING) {
                task.copy(status = TaskStatus.CANCELLED)
            } else task
        }
    }

    private fun processNext() {
        scope.launch {
            mutex.withLock {
                if (_isProcessing.value) return@launch
                val task = pending.poll() ?: return@launch
                _isProcessing.value = true

                val runningTask = task.copy(status = TaskStatus.RUNNING)
                updateTask(runningTask)

                try {
                    task.action { progress -> updateTask(runningTask.copy(progress = progress)) }
                    updateTask(runningTask.copy(status = TaskStatus.COMPLETED, progress = 1f))
                    Logger.i(TAG, "Task completed: ${task.title}")
                } catch (e: Exception) {
                    Logger.e(TAG, "Task failed: ${task.title}", e)
                    updateTask(runningTask.copy(status = TaskStatus.FAILED, error = e.message))
                }

                _isProcessing.value = false
                processNext()
            }
        }
    }

    private fun updateTask(task: AiTask) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) current[index] = task else current.add(task)
        _tasks.value = current
    }

    private fun updateTaskList() {
        val current = _tasks.value.toMutableList()
        pending.forEach { task ->
            if (current.none { it.id == task.id }) current.add(task)
        }
        _tasks.value = current
    }

    fun clearCompleted() {
        _tasks.value = _tasks.value.filter {
            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED
        }
    }

    val activeCount: Int
        get() = _tasks.value.count { it.status == TaskStatus.PENDING || it.status == TaskStatus.RUNNING }
}
