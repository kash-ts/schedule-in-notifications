package com.example.scheduleinnotifications.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.scheduleinnotifications.data.dao.LessonDao
import com.example.scheduleinnotifications.data.dao.ScheduleDao
import com.example.scheduleinnotifications.data.model.Lesson
import com.example.scheduleinnotifications.data.model.Schedule

@Database(
    entities = [Schedule::class, Lesson::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun lessonDao(): LessonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
