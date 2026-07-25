package com.example.scheduleinnotifications.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.scheduleinnotifications.data.model.Lesson

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons WHERE scheduleId = :scheduleId ORDER BY dayOfWeek ASC, startMinute ASC")
    fun getForScheduleLive(scheduleId: Long): LiveData<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE scheduleId = :scheduleId ORDER BY dayOfWeek ASC, startMinute ASC")
    suspend fun getForSchedule(scheduleId: Long): List<Lesson>

    @Query("SELECT * FROM lessons WHERE scheduleId = :scheduleId AND dayOfWeek = :day ORDER BY startMinute ASC")
    suspend fun getForScheduleAndDay(scheduleId: Long, day: Int): List<Lesson>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: Lesson): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<Lesson>)

    @Update
    suspend fun update(lesson: Lesson)

    @Delete
    suspend fun delete(lesson: Lesson)

    @Query("DELETE FROM lessons WHERE scheduleId = :scheduleId")
    suspend fun deleteAllForSchedule(scheduleId: Long)
}
