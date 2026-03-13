package org.openjwc.client.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

class MainViewModel : ViewModel() {
    private val label = "MainViewModel"
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Chat)
    val currentTab = _currentTab.asStateFlow()

    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
        Log.d(label, "Update tab to $tab")

    }

    private val _themeColor = MutableStateFlow<ColorType>(ColorType.Dynamic())
    val themeColor = _themeColor.asStateFlow()

    fun updateThemeColor(color: ColorType) {
        _themeColor.value = color
    }

    private val _darkThemeStyle = MutableStateFlow(DarkThemeStyle.Auto)
    val darkThemeStyle = _darkThemeStyle.asStateFlow()

    fun updateDarkThemeStyle(style: DarkThemeStyle) {
        _darkThemeStyle.value = style
    }
}