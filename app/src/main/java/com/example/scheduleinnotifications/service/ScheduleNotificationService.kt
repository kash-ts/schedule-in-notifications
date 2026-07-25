package com.example.scheduleinnotifications.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.scheduleinnotifications.R
import com.example.scheduleinnotifications.data.model.Lesson
import com.example.scheduleinnotifications.data.repository.ScheduleRepository
import com.example.scheduleinnotifications.ui.MainActivity
import kotlinx.coroutines.*
import java.util.*

/**
 * Foreground Service, который поддерживает постоянные уведомления для
 * каждого включённого расписания и обновляет их каждую минуту.
 */
class ScheduleNotificationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "schedule_channel"
        /** Базовый ID уведомления для сервиса; уведомления расписаний начинаются с NOTIF_BASE */
        private const val FOREGROUND_NOTIF_ID = 1
        private const val NOTIF_BASE_ID = 100
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: ScheduleRepository
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = ScheduleRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startServiceForeground()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        // Убрать все уведомления расписаний
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    // ──────────────────────────────────────────────────────────────────────

    private fun startServiceForeground() {
        // Показываем «тихое» системное уведомление сервиса
        val notification = buildServiceNotification()
        startForeground(FOREGROUND_NOTIF_ID, notification)
        scheduleUpdates()
    }

    private fun scheduleUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                updateAllNotifications()
                // Ждём до следующей чётной минуты
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    private suspend fun updateAllNotifications() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val enabledSchedules = repository.getEnabledSchedules()

        if (enabledSchedules.isEmpty()) {
            // Сервис работает, но показывать нечего
            nm.notify(
                FOREGROUND_NOTIF_ID,
                buildServiceNotification("Нет активных расписаний")
            )
            return
        }

        val cal = Calendar.getInstance()
        val dayOfWeek = calendarDayToLocal(cal.get(Calendar.DAY_OF_WEEK))
        val nowMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        enabledSchedules.forEachIndexed { index, schedule ->
            val lessons = repository.getLessonsForDay(schedule.id, dayOfWeek)
            val notifId = NOTIF_BASE_ID + index
            val notif = buildScheduleNotification(schedule.name, lessons, nowMinute)
            nm.notify(notifId, notif)
        }

        // Обновляем системное уведомление
        nm.notify(FOREGROUND_NOTIF_ID, buildServiceNotification())
    }

    // ──────────────────────────────────────────────────────────────────────
    // Notification builders
    // ──────────────────────────────────────────────────────────────────────

    private fun buildServiceNotification(subtitle: String? = null): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(subtitle ?: getString(R.string.notif_service_running))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun buildScheduleNotification(
        scheduleName: String,
        lessons: List<Lesson>,
        nowMinute: Int
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, scheduleName.hashCode(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val current = lessons.firstOrNull { nowMinute in it.startMinute until it.endMinute }
        val next = lessons.firstOrNull { it.startMinute > nowMinute }

        val (title, text) = when {
            current != null -> {
                val remaining = current.endMinute - nowMinute
                Pair(
                    "$scheduleName: ${current.name}",
                    "${formatTime(current.startMinute)}–${formatTime(current.endMinute)}  •  Осталось $remaining мин"
                )
            }
            next != null -> {
                val until = next.startMinute - nowMinute
                Pair(
                    "$scheduleName: Следующий — ${next.name}",
                    "Начало в ${formatTime(next.startMinute)}  •  через $until мин"
                )
            }
            else -> Pair(scheduleName, getString(R.string.notif_no_lessons_today))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /** Переводит Calendar.DAY_OF_WEEK (1=вс) → локальный (1=пн…7=вс) */
    private fun calendarDayToLocal(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    private fun formatTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
