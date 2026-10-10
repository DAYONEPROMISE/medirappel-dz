package dz.medirappel.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import dz.medirappel.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when an exact alarm triggers: show the notification, then re-schedule tomorrow. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val name = intent.getStringExtra(EXTRA_MED_NAME).orEmpty()
        val dosage = intent.getStringExtra(EXTRA_DOSAGE).orEmpty()
        val hm = intent.getStringExtra(EXTRA_TIME).orEmpty()
        if (medId < 0 || name.isEmpty() || hm.isEmpty()) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.showDoseReminder(context, medId, name, dosage, hm)
                // Re-schedule this medication's alarms for the following days.
                val db = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "medirappel.db"
                ).build()
                db.medicationDao().getById(medId)?.let { AlarmScheduler.scheduleMed(context, it) }
                db.close()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MED_ID = "med_id"
        const val EXTRA_MED_NAME = "med_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_TIME = "time"
        const val EXTRA_SLOT = "slot"
    }
}

/** Notification action buttons: mark taken / skipped without opening the app. */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(AlarmReceiver.EXTRA_MED_ID, -1L)
        val name = intent.getStringExtra(AlarmReceiver.EXTRA_MED_NAME).orEmpty()
        val hm = intent.getStringExtra(AlarmReceiver.EXTRA_TIME).orEmpty()
        val action = intent.action ?: return
        if (medId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = if (action == ACTION_TAKEN) "TAKEN" else "SKIPPED"
                NotificationHelper.logQuickAction(context, medId, name, hm, status)
                NotificationHelper.dismiss(context, medId, hm)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "dz.medirappel.app.ACTION_TAKEN"
        const val ACTION_SKIPPED = "dz.medirappel.app.ACTION_SKIPPED"
    }
}

/** After reboot / update / clock change, re-schedule every active medication. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action ?: return
        val relevant = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (a !in relevant && a != "android.intent.action.QUICKBOOT_POWERON") return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "medirappel.db"
                ).build()
                AlarmScheduler.scheduleAll(context, db.medicationDao().getAllActive())
                db.close()
            } finally {
                pending.finish()
            }
        }
    }
}
