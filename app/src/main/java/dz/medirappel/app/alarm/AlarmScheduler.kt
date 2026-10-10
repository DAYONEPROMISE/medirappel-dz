package dz.medirappel.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dz.medirappel.app.data.MedicationEntity

/** Schedules one exact alarm per (medication, time) pair. */
object AlarmScheduler {

    private fun alarmManager(ctx: Context): AlarmManager =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager(ctx).canScheduleExactAlarms()
        } else {
            true
        }

    fun scheduleAll(ctx: Context, meds: List<MedicationEntity>) {
        meds.filter { it.active }.forEach { scheduleMed(ctx, it) }
    }

    fun scheduleMed(ctx: Context, med: MedicationEntity) {
        cancelMed(ctx, med.id)
        med.times().forEachIndexed { index, hm ->
            val triggerAt = nextTriggerMillis(hm)
            val pi = pendingFor(ctx, med.id, index, med.name, med.dosage, hm)
            try {
                alarmManager(ctx).setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pi
                )
            } catch (_: SecurityException) {
                // Exact-alarm permission revoked: fall back to an inexact window.
                alarmManager(ctx).setWindow(
                    AlarmManager.RTC_WAKEUP, triggerAt, 15 * 60 * 1000L, pi
                )
            }
        }
    }

    fun cancelMed(ctx: Context, medicationId: Long, slots: Int = 8) {
        repeat(slots) { index ->
            val pi = pendingFor(ctx, medicationId, index, "", "", "00:00", forCancel = true)
            alarmManager(ctx).cancel(pi)
            pi.cancel()
        }
    }

    private fun nextTriggerMillis(hm: String): Long {
        val (h, m) = hm.split(":").map { it.toInt() }
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, h)
            set(java.util.Calendar.MINUTE, m)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun pendingFor(
        ctx: Context, medId: Long, index: Int,
        name: String, dosage: String, hm: String, forCancel: Boolean = false
    ): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MED_ID, medId)
            putExtra(AlarmReceiver.EXTRA_MED_NAME, name)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage)
            putExtra(AlarmReceiver.EXTRA_TIME, hm)
            putExtra(AlarmReceiver.EXTRA_SLOT, index)
        }
        val requestCode = (medId * 16 + index).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, requestCode, intent, flags)
    }
}
