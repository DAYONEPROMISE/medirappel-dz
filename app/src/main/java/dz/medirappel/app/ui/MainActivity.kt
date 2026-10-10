package dz.medirappel.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dz.medirappel.app.MainApplication
import dz.medirappel.app.ui.theme.MediRappelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MainApplication
        val vm: MedViewModel by viewModels { MedViewModel.factory(app) }
        setContent {
            MediRappelTheme {
                MediRappelApp(app, vm)
            }
        }
    }
}
