package com.example.core.data.repository

import com.example.core.database.dao.TaskDao
import com.example.core.database.entity.TaskEntity
import com.example.core.model.Task
import com.example.core.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getActiveTaskCount(): Flow<Int>
    fun getTasksForNote(noteId: String): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun createTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.NONE,
        dueDateEpochMs: Long? = null,
        associatedNoteId: String? = null
    ): Task
    suspend fun updateTask(task: Task)
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean)
    suspend fun deleteTask(id: String)
}

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getActiveTaskCount(): Flow<Int> {
        return taskDao.getActiveTaskCount()
    }

    override fun getTasksForNote(noteId: String): Flow<List<Task>> {
        return taskDao.getTasksForNote(noteId).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueDateEpochMs: Long?,
        associatedNoteId: String?
    ): Task {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotBlank()) { "Task title cannot be blank" }

        val task = Task(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = description.trim(),
            isCompleted = false,
            priority = priority,
            dueDateEpochMs = dueDateEpochMs,
            associatedNoteId = associatedNoteId,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        taskDao.insertTask(TaskEntity.fromDomainModel(task))
        return task
    }

    override suspend fun updateTask(task: Task) {
        val trimmedTitle = task.title.trim()
        require(trimmedTitle.isNotBlank()) { "Task title cannot be blank" }
        taskDao.updateTask(TaskEntity.fromDomainModel(task.copy(title = trimmedTitle, updatedAtEpochMs = System.currentTimeMillis())))
    }

    override suspend fun setTaskCompleted(id: String, isCompleted: Boolean) {
        taskDao.setTaskCompleted(id, if (isCompleted) 1 else 0)
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTask(id)
    }
}
