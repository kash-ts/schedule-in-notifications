package com.example.scheduleinnotifications.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Расписание (например: "Школа", "Кружок", "Работа")
 */
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
