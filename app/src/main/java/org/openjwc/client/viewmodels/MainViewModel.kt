package org.openjwc.client.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openjwc.client.ui.main.MainTab

class MainViewModel : ViewModel() {
    private val label = "MainViewModel"
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Chat)
    val currentTab = _currentTab.asStateFlow()

    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
        Log.d(label, "Update tab to $tab")

    }
}