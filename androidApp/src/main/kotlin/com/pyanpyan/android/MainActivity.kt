package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pyanpyan.android.ui.checklist.ChecklistScreen
import com.pyanpyan.android.ui.theme.PyanpyanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyanpyanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChecklistScreen()
                }
            }
        }
    }
}
