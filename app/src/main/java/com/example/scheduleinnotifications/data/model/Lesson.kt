package com.example.scheduleinnotifications.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Активность в расписании.
 * dayOfWeek: 1=Пн, 2=Вт, 3=Ср, 4=Чт, 5=Пт, 6=Сб, 7=Вс
 * startTime / endTime хранятся в минутах от начала суток (0..1439)
 */
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId")]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val name: String,
    /** День недели: 1=Пн … 7=Вс */
    val dayOfWeek: Int,
    /** Начало урока в минутах от полуночи (например 8*60 = 480 для 8:00) */
    val startMinute: Int,
    /** Конец урока в минутах от полуночи (например 8*60+40 = 520 для 8:40) */
    val endMinute: Int
)
