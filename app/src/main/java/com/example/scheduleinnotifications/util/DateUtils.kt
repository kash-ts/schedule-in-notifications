package com.example.scheduleinnotifications.util

import java.util.Calendar

/**
 * Утилиты для работы с датой/временем.
 */
object DateUtils {

    /**
     * Переводит Calendar.DAY_OF_WEEK (Calendar.SUNDAY = 1) →
     * локальный формат (1 = Пн … 7 = Вс).
     */
    fun calendarDayToLocal(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY    -> 1
        Calendar.TUESDAY   -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY  -> 4
        Calendar.FRIDAY    -> 5
        Calendar.SATURDAY  -> 6
        Calendar.SUNDAY    -> 7
        else               -> 1
    }

    /**
     * Возвращает текущий день недели в локальном формате (1 = Пн … 7 = Вс).
     */
    fun todayLocal(): Int = calendarDayToLocal(
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    )
}
