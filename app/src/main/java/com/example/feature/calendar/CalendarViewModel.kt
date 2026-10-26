package com.example.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.CalendarRepository
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.TaskRepository
import com.example.core.model.CalendarEvent
import com.example.core.model.Note
import com.example.core.model.Task
import com.example.core.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val nowCal = Calendar.getInstance()
    private val _currentYear = MutableStateFlow(nowCal.get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(nowCal.get(Calendar.MONTH)) // 0-indexed
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _selectedDateEpochMs = MutableStateFlow(DateUtils.getStartOfDayEpochMs(System.currentTimeMillis()))
    val selectedDateEpochMs: StateFlow<Long> = _selectedDateEpochMs.asStateFlow()

    val allEvents: StateFlow<List<CalendarEvent>> = calendarRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = noteRepository.getActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Events, tasks, and notes on the selected date
    val selectedDateEvents: StateFlow<List<CalendarEvent>> = combine(allEvents, _selectedDateEpochMs) { events, selDate ->
        val startOfDay = DateUtils.getStartOfDayEpochMs(selDate)
        val endOfDay = DateUtils.getEndOfDayEpochMs(selDate)
        events.filter { it.startEpochMs in startOfDay..endOfDay || (it.startEpochMs <= endOfDay && it.endEpochMs >= startOfDay) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateTasks: StateFlow<List<Task>> = combine(allTasks, _selectedDateEpochMs) { tasks, selDate ->
        val startOfDay = DateUtils.getStartOfDayEpochMs(selDate)
        val endOfDay = DateUtils.getEndOfDayEpochMs(selDate)
        tasks.filter { it.dueDateEpochMs != null && it.dueDateEpochMs in startOfDay..endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateNotes: StateFlow<List<Note>> = combine(allNotes, _selectedDateEpochMs) { notes, selDate ->
        val startOfDay = DateUtils.getStartOfDayEpochMs(selDate)
        val endOfDay = DateUtils.getEndOfDayEpochMs(selDate)
        notes.filter { it.updatedAtEpochMs in startOfDay..endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(epochMs: Long) {
        _selectedDateEpochMs.value = DateUtils.getStartOfDayEpochMs(epochMs)
    }

    fun previousMonth() {
        if (_currentMonth.value == 0) {
            _currentMonth.value = 11
            _currentYear.value -= 1
        } else {
            _currentMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_currentMonth.value == 11) {
            _currentMonth.value = 0
            _currentYear.value += 1
        } else {
            _currentMonth.value += 1
        }
    }

    fun goToToday() {
        val today = Calendar.getInstance()
        _currentYear.value = today.get(Calendar.YEAR)
        _currentMonth.value = today.get(Calendar.MONTH)
        _selectedDateEpochMs.value = DateUtils.getStartOfDayEpochMs(today.timeInMillis)
    }

    fun createEvent(title: String, description: String = "", isAllDay: Boolean = false) {
        viewModelScope.launch {
            val startMs = _selectedDateEpochMs.value + (10 * 3600_000L) // Default 10:00 AM
            val endMs = startMs + 3600_000L
            calendarRepository.createEvent(
                title = title,
                description = description,
                startEpochMs = startMs,
                endEpochMs = endMs,
                isAllDay = isAllDay
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(eventId)
        }
    }

    companion object {
        fun provideFactory(
            calendarRepository: CalendarRepository,
            taskRepository: TaskRepository,
            noteRepository: NoteRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(calendarRepository, taskRepository, noteRepository) as T
            }
        }
    }
}
