package com.rr.numio.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rr.numio.clock.ui.bedtime.BedtimeScreen
import com.rr.numio.clock.ui.theme.ClockByNumioTheme

/** Opens from the bedtime reminder (or the "Sleep by" line on the Alarm tab). */
class BedtimeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockByNumioTheme {
                BedtimeScreen(onClose = { finish() })
            }
        }
    }
}
