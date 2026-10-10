package dz.medirappel.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import dz.medirappel.app.R
import dz.medirappel.app.data.AppDatabase
import dz.medirappel.app.data.DoseLogEntity
import dz.medirappel.app.domain.ScheduleCalculator
import dz.medirappel.app.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "dose_reminders"
    private const val CHANNEL_NAME = "Rappels de prise"

    private fun manager(ctx: Context): NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Medication dose reminders / تذكير بالأدوية" }
            manager(ctx).createNotificationChannel(ch)
        }
    }

    fun notificationId(medId: Long, hm: String): Int =
        (medId * 32 + hm.hashCode()).toInt()

    fun showDoseReminder(ctx: Context, medId: Long, name: String, dosage: String, hm: String) {
        ensureChannel(ctx)
        val openApp = PendingIntent.getActivity(
            ctx, notificationId(medId, hm),
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun actionIntent(action: String): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, (action.hashCode() + notificationId(medId, hm)),
                Intent(ctx, DoseActionReceiver::class.java).apply {
                    this.action = action
                    putExtra(AlarmReceiver.EXTRA_MED_ID, medId)
                    putExtra(AlarmReceiver.EXTRA_MED_NAME, name)
                    putExtra(AlarmReceiver.EXTRA_TIME, hm)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        val dose = if (dosage.isBlank()) "" else " — $dosage"
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.notif_title, name))
            .setContentText(ctx.getString(R.string.notif_body, hm) + dose)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, ctx.getString(R.string.action_taken), actionIntent(DoseActionReceiver.ACTION_TAKEN))
            .addAction(0, ctx.getString(R.string.action_skipped), actionIntent(DoseActionReceiver.ACTION_SKIPPED))
            .build()
        manager(ctx).notify(notificationId(medId, hm), notif)
    }

    fun dismiss(ctx: Context, medId: Long, hm: String) {
        manager(ctx).cancel(notificationId(medId, hm))
    }

    suspend fun logQuickAction(ctx: Context, medId: Long, name: String, hm: String, status: String) {
        val db = Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "medirappel.db").build()
        try {
            db.doseLogDao().insert(
                DoseLogEntity(
                    medicationId = medId,
                    medicationName = name,
                    scheduledDay = ScheduleCalculator.dayKey(),
                    scheduledTime = hm,
                    takenAt = if (status == "TAKEN") System.currentTimeMillis() else null,
                    status = status
                )
            )
        } finally {
            db.close()
        }
    }
}
