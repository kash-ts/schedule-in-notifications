package com.example.scheduleinnotifications.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.scheduleinnotifications.data.model.Lesson
import com.example.scheduleinnotifications.data.model.Schedule
import com.example.scheduleinnotifications.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScheduleRepository(application)

    /** Все расписания — LiveData для главного экрана */
    val allSchedules: LiveData<List<Schedule>> = repository.getAllSchedulesLive()

    // ── Schedules ──────────────────────────────────────────────────────────

    fun addSchedule(name: String) = viewModelScope.launch {
        repository.insertSchedule(Schedule(name = name))
    }

    fun updateSchedule(schedule: Schedule) = viewModelScope.launch {
        repository.updateSchedule(schedule)
    }

    fun deleteSchedule(schedule: Schedule) = viewModelScope.launch {
        repository.deleteSchedule(schedule)
    }

    fun setEnabled(scheduleId: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setScheduleEnabled(scheduleId, enabled)
    }

    // ── Lessons ────────────────────────────────────────────────────────────

    private val _currentScheduleId = MutableLiveData<Long>()

    fun selectSchedule(scheduleId: Long) {
        _currentScheduleId.value = scheduleId
    }

    val lessonsForCurrentSchedule: LiveData<List<Lesson>> =
        _currentScheduleId.switchMap { id ->
            repository.getLessonsForScheduleLive(id)
        }

    fun addLesson(scheduleId: Long, name: String, dayOfWeek: Int, startMinute: Int, endMinute: Int) =
        viewModelScope.launch {
            repository.insertLesson(
                Lesson(
                    scheduleId = scheduleId,
                    name = name,
                    dayOfWeek = dayOfWeek,
                    startMinute = startMinute,
                    endMinute = endMinute
                )
            )
        }

    fun updateLesson(lesson: Lesson) = viewModelScope.launch {
        repository.updateLesson(lesson)
    }

    fun deleteLesson(lesson: Lesson) = viewModelScope.launch {
        repository.deleteLesson(lesson)
    }

    /** Импорт уроков из CSV-строки. Формат строки: name,dayOfWeek,HH:mm,HH:mm */
    fun importLessonsFromCsv(scheduleId: Long, csvText: String) = viewModelScope.launch {
        val lessons = parseCsv(scheduleId, csvText)
        if (lessons.isNotEmpty()) repository.insertLessons(lessons)
    }

    private fun parseCsv(scheduleId: Long, csvText: String): List<Lesson> {
        val result = mutableListOf<Lesson>()
        csvText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEach
            val parts = trimmed.split(",").map { it.trim() }
            if (parts.size < 4) return@forEach
            runCatching {
                val name = parts[0]
                val day = parts[1].toInt().coerceIn(1, 7)
                val start = parseTime(parts[2])
                val end = parseTime(parts[3])
                result.add(Lesson(scheduleId = scheduleId, name = name, dayOfWeek = day, startMinute = start, endMinute = end))
            }
        }
        return result
    }

    private fun parseTime(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.trim().toInt() }
        return h * 60 + m
    }
}
