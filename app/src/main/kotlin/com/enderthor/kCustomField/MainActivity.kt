package com.enderthor.kCustomField

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.enderthor.kCustomField.screens.TabLayout
import com.enderthor.kCustomField.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                TabLayout()
            }
        }

    }
}