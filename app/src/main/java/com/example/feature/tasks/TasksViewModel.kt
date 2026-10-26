package com.example.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.TaskRepository
import com.example.core.model.Task
import com.example.core.model.TaskFilter
import com.example.core.model.TaskPriority
import com.example.core.model.TaskSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _sortOrder = MutableStateFlow(TaskSortOrder.DUE_DATE)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        taskRepository.getAllTasks(),
        _filter,
        _sortOrder,
        _searchQuery
    ) { allTasks, currentFilter, currentSort, query ->
        var filtered = when (currentFilter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.ACTIVE -> allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }

        when (currentSort) {
            TaskSortOrder.DUE_DATE -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.dueDateEpochMs ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAtEpochMs }
            )
            TaskSortOrder.PRIORITY -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending {
                        when (it.priority) {
                            TaskPriority.HIGH -> 3
                            TaskPriority.MEDIUM -> 2
                            TaskPriority.LOW -> 1
                            TaskPriority.NONE -> 0
                        }
                    }
                    .thenByDescending { it.createdAtEpochMs }
            )
            TaskSortOrder.TITLE -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            TaskSortOrder.DATE_CREATED -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAtEpochMs }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = taskRepository.getActiveTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(newFilter: TaskFilter) {
        _filter.value = newFilter
    }

    fun setSortOrder(newSort: TaskSortOrder) {
        _sortOrder.value = newSort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun createTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.NONE,
        dueDateEpochMs: Long? = null,
        onCreated: (Task) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val task = taskRepository.createTask(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDateEpochMs = dueDateEpochMs
                )
                onCreated(task)
            } catch (_: Exception) {}
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    companion object {
        fun provideFactory(taskRepository: TaskRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasksViewModel(taskRepository) as T
                }
            }
    }
}
