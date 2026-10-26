package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun getStartOfDayEpochMs(epochMs: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDayEpochMs(epochMs: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun isSameDay(epochMs1: Long, epochMs2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = epochMs1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = epochMs2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1 -> if (isLeapYear(year)) 29 else 28 // 0-indexed month: 1 is February
            3, 5, 8, 10 -> 30 // April, June, September, November (0-indexed)
            else -> 31
        }
    }

    /**
     * Returns day of week for 1st day of given year and month.
     * 1 = Sunday, 2 = Monday, ..., 7 = Saturday (Calendar.SUNDAY..Calendar.SATURDAY)
     */
    fun getFirstDayOfWeekForMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return formatter.format(cal.time)
    }

    fun formatDate(epochMs: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun formatTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun formatDateTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun getRelativeDateString(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDayEpochMs(now)
        val todayEnd = getEndOfDayEpochMs(now)
        val tomorrowEnd = todayEnd + 86400000L
        val yesterdayStart = todayStart - 86400000L

        return when {
            epochMs in todayStart..todayEnd -> "Today"
            epochMs in (todayEnd + 1)..tomorrowEnd -> "Tomorrow"
            epochMs in yesterdayStart until todayStart -> "Yesterday"
            else -> formatDate(epochMs)
        }
    }
}
