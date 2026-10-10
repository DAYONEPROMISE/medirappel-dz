package dz.medirappel.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dz.medirappel.app.MainApplication
import dz.medirappel.app.R
import dz.medirappel.app.alarm.AlarmScheduler
import dz.medirappel.app.data.MedicationEntity
import dz.medirappel.app.domain.ScheduleCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Routes {
    const val HOME = "home"
    const val ADD = "add?id={id}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediRappelApp(app: MainApplication, vm: MedViewModel) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.HOME) },
                    label = { Text("💊") }, icon = {})
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.HISTORY) },
                    label = { Text(stringResource(R.string.history_title)) }, icon = {})
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.SETTINGS) },
                    label = { Text(stringResource(R.string.settings_title)) }, icon = {})
            }
        }
    ) { pad ->
        NavHost(nav, Routes.HOME, Modifier.padding(pad)) {
            composable(Routes.HOME) { HomeScreen(vm, nav) }
            composable(
                Routes.ADD,
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "0" })
            ) { back ->
                val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                AddEditScreen(vm, nav, id)
            }
            composable(Routes.HISTORY) { HistoryScreen(vm) }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen(vm: MedViewModel, nav: NavHostController) {
    val meds by vm.medications.collectAsState()
    val ctx = LocalContext.current
    var toDelete by remember { mutableStateOf<MedicationEntity?>(null) }

    // Re-schedule alarms whenever the list changes (offline, on-device).
    LaunchedEffect(meds) {
        if (meds.isNotEmpty()) AlarmScheduler.scheduleAll(ctx, meds)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("add?id=0") }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    ) { pad ->
        if (meds.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(meds, key = { it.id }) { med ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(med.name, style = MaterialTheme.typography.titleMedium)
                                Switch(checked = med.active, onCheckedChange = { vm.setActive(med, it) })
                            }
                            if (med.dosage.isNotBlank()) Text(med.dosage, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(med.times().joinToString("  •  "), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                                    val due = med.times().firstOrNull() ?: now
                                    vm.logNow(med, due, "TAKEN")
                                }) { Text(stringResource(R.string.action_taken)) }
                                OutlinedButton(onClick = { nav.navigate("add?id=${med.id}") }) { Text(stringResource(R.string.edit_title)) }
                                TextButton(onClick = { toDelete = med }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(med)
                    AlarmScheduler.cancelMed(ctx, med.id)
                    toDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
            text = { Text(stringResource(R.string.confirm_delete)) }
        )
    }
}

@Composable
fun AddEditScreen(vm: MedViewModel, nav: NavHostController, id: Long) {
    val meds by vm.medications.collectAsState()
    val existing = meds.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var dosage by remember { mutableStateOf(existing?.dosage ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var times by remember { mutableStateOf(existing?.timesCsv ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (id == 0L) R.string.add_title else R.string.edit_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
        OutlinedTextField(dosage, { dosage = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_dosage)) }, singleLine = true)
        OutlinedTextField(times, { times = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_times)) }, singleLine = true, placeholder = { Text("08:00, 13:00, 20:30") })
        OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_notes)) })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                try {
                    vm.save(name, dosage, times, notes, id) { savedId ->
                        // Schedule alarms right away for the saved medication.
                        val check = if (id == 0L) savedId else id
                        (meds.firstOrNull { it.id == check })?.let { AlarmScheduler.scheduleMed(ctx, it) }
                        nav.popBackStack()
                    }
                } catch (e: IllegalArgumentException) {
                    error = e.message
                }
            }) { Text(stringResource(R.string.save)) }
            OutlinedButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
fun HistoryScreen(vm: MedViewModel) {
    val logs by vm.history.collectAsState()
    val ctx = LocalContext.current
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs, key = { it.id }) { log ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Text(log.medicationName, style = MaterialTheme.typography.titleMedium)
                        Text("${log.scheduledDay} • ${log.scheduledTime}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        if (log.status == "TAKEN") "✓ ${ctx.getString(R.string.taken)}"
                        else "— ${ctx.getString(R.string.skipped)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var canExact by remember { mutableStateOf(AlarmScheduler.canScheduleExact(ctx)) }
    var adherence by remember { mutableStateOf<Int?>(null) }

    // Refresh exact-alarm state when returning from system settings.
    remember(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) canExact = AlarmScheduler.canScheduleExact(ctx)
        }
        lifecycle.addObserver(obs)
        obs
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.exact_alarm_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.exact_alarm_desc), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(if (canExact) R.string.exact_alarm_ok else R.string.exact_alarm_missing),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }) { Text(stringResource(R.string.exact_alarm_grant)) }
                }
            }
        }
        Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.bodyMedium)
        adherence?.let { Text(stringResource(R.string.adherence_week, it), style = MaterialTheme.typography.bodyLarge) }
    }
}

fun Context.alarmMgr(): AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
