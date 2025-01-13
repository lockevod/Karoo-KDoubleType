package com.enderthor.kCustomField.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HeadwindSharedViewModel : ViewModel() {
    private val _isHeadwindEnabled = MutableStateFlow(false)
    val isGeneralHeadwindEnabled: StateFlow<Boolean> = _isHeadwindEnabled

    fun setHeadwindEnabled(enabled: Boolean) {
        _isHeadwindEnabled.value = enabled
    }
}