package com.zeiterfassung.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zeiterfassung.app.ui.ZeiterfassungNavGraph
import com.zeiterfassung.app.ui.theme.ZeiterfassungTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZeiterfassungTheme {
                ZeiterfassungNavGraph()
            }
        }
    }
}
