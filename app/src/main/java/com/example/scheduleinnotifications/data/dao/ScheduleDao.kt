package com.example.scheduleinnotifications.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.scheduleinnotifications.data.model.Schedule

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules ORDER BY createdAt ASC")
    fun getAllLive(): LiveData<List<Schedule>>

    @Query("SELECT * FROM schedules ORDER BY createdAt ASC")
    suspend fun getAll(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1")
    suspend fun getEnabled(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): Schedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("UPDATE schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
