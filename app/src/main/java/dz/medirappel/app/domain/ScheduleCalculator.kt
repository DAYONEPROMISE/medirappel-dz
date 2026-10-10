package dz.medirappel.app.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pure, unit-testable scheduling helpers. No Android dependencies. */
object ScheduleCalculator {

    fun dayKey(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun isValidTime(value: String): Boolean =
        value.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d"))

    fun normalizeTimes(raw: Iterable<String>): List<String> =
        raw.map { it.trim() }.filter(::isValidTime).distinct().sorted()

    /**
     * Next dose datetime in millis for [times] ("HH:mm") after [nowMillis].
     * Returns null when the list is empty.
     */
    fun nextDoseMillis(times: List<String>, nowMillis: Long = System.currentTimeMillis()): Long? {
        val clean = normalizeTimes(times)
        if (clean.isEmpty()) return null
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidates = clean.mapNotNull { t ->
            val (h, m) = t.split(":").map { it.toInt() }
            val c = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= nowMillis) c.add(Calendar.DAY_OF_YEAR, 1)
            c.timeInMillis
        }
        return candidates.minOrNull()
    }

    /** Doses due today (or overdue by less than [graceMinutes]) given "HH:mm" now. */
    fun dueNow(times: List<String>, nowHm: String, graceMinutes: Int = 15): List<String> {
        fun toMin(hm: String): Int {
            val (h, m) = hm.split(":").map { it.toInt() }
            return h * 60 + m
        }
        val nowMin = toMin(nowHm)
        return normalizeTimes(times).filter { t ->
            val d = nowMin - toMin(t)
            d in 0..graceMinutes
        }
    }

    fun adherencePercent(taken: Int, total: Int): Int {
        if (total <= 0) return 100
        return ((taken * 100.0) / total).toInt().coerceIn(0, 100)
    }
}
