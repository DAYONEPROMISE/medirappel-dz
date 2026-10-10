package dz.medirappel.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ScheduleCalculatorTest {

    private fun millis(h: Int, m: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun `normalizeTimes sorts dedupes and drops invalid`() {
        assertEquals(
            listOf("08:00", "13:00", "20:30"),
            ScheduleCalculator.normalizeTimes(listOf("20:30", "08:00", "08:00", "25:99", "13:00", "  "))
        )
    }

    @Test fun `isValidTime rejects out-of-range`() {
        assertTrue(ScheduleCalculator.isValidTime("00:00"))
        assertTrue(ScheduleCalculator.isValidTime("23:59"))
        assertFalse(ScheduleCalculator.isValidTime("24:00"))
        assertFalse(ScheduleCalculator.isValidTime("8:00"))
        assertFalse(ScheduleCalculator.isValidTime("abc"))
    }

    @Test fun `nextDoseMillis picks later time same day`() {
        val now = millis(9, 0)
        val next = ScheduleCalculator.nextDoseMillis(listOf("08:00", "13:00"), now)!!
        val c = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(13, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test fun `nextDoseMillis rolls to tomorrow when all passed`() {
        val now = millis(22, 0)
        val next = ScheduleCalculator.nextDoseMillis(listOf("08:00"), now)!!
        assertTrue(next > now)
        val c = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(8, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test fun `nextDoseMillis null on empty`() {
        assertNull(ScheduleCalculator.nextDoseMillis(emptyList()))
    }

    @Test fun `dueNow respects grace window`() {
        assertEquals(listOf("08:00"), ScheduleCalculator.dueNow(listOf("08:00", "20:00"), "08:10"))
        assertTrue(ScheduleCalculator.dueNow(listOf("08:00"), "08:16").isEmpty())
        assertTrue(ScheduleCalculator.dueNow(listOf("08:00"), "07:59").isEmpty())
    }

    @Test fun `adherencePercent handles edges`() {
        assertEquals(100, ScheduleCalculator.adherencePercent(0, 0))
        assertEquals(75, ScheduleCalculator.adherencePercent(3, 4))
        assertEquals(100, ScheduleCalculator.adherencePercent(9, 4).coerceAtMost(100))
    }

    @Test fun `dayKey format is stable`() {
        assertTrue(ScheduleCalculator.dayKey().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}
