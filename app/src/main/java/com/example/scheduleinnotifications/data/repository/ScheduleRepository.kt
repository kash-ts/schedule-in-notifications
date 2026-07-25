package com.example.scheduleinnotifications.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.scheduleinnotifications.data.db.AppDatabase
import com.example.scheduleinnotifications.data.model.Lesson
import com.example.scheduleinnotifications.data.model.Schedule

class ScheduleRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val scheduleDao = db.scheduleDao()
    private val lessonDao = db.lessonDao()

    // ── Schedules ──────────────────────────────────────────────────────────

    fun getAllSchedulesLive(): LiveData<List<Schedule>> = scheduleDao.getAllLive()

    suspend fun getEnabledSchedules(): List<Schedule> = scheduleDao.getEnabled()

    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insert(schedule)

    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.update(schedule)

    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.delete(schedule)

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) =
        scheduleDao.setEnabled(id, enabled)

    // ── Lessons ────────────────────────────────────────────────────────────

    fun getLessonsForScheduleLive(scheduleId: Long): LiveData<List<Lesson>> =
        lessonDao.getForScheduleLive(scheduleId)

    suspend fun getLessonsForSchedule(scheduleId: Long): List<Lesson> =
        lessonDao.getForSchedule(scheduleId)

    suspend fun getLessonsForDay(scheduleId: Long, dayOfWeek: Int): List<Lesson> =
        lessonDao.getForScheduleAndDay(scheduleId, dayOfWeek)

    suspend fun insertLesson(lesson: Lesson): Long = lessonDao.insert(lesson)

    suspend fun insertLessons(lessons: List<Lesson>) = lessonDao.insertAll(lessons)

    suspend fun updateLesson(lesson: Lesson) = lessonDao.update(lesson)

    suspend fun deleteLesson(lesson: Lesson) = lessonDao.delete(lesson)

    suspend fun deleteAllLessons(scheduleId: Long) = lessonDao.deleteAllForSchedule(scheduleId)
}
