package org.openjwc.client.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.utils.changeAppLanguage

private const val label = "SettingsViewModel"

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = UserSettings())

    fun updateFreshDays(freshDays: Int) = viewModelScope.launch { settingsRepository.updateFreshDays(freshDays) }

    fun updateCrawlDaysGap(days: Int) = viewModelScope.launch { settingsRepository.updateCrawlDaysGap(days) }

    fun updateMotto(text: String, author: String) =
        viewModelScope.launch { settingsRepository.updateMotto(text, author) }

    fun updateMottoOnline(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.updateMottoOnline(enabled) }

    fun updateHitokotoCategory(code: String) =
        viewModelScope.launch { settingsRepository.updateHitokotoCategory(code) }

    fun updateHitokotoMaxLength(length: Int) =
        viewModelScope.launch { settingsRepository.updateHitokotoMaxLength(length) }

    fun updateBackground(uri: Uri) = viewModelScope.launch {
        val success = settingsRepository.updateBackground(uri)
        if (!success) uiEvent.send(UiEvent.ShowToast(UiText.DynamicString("设置背景失败")))
    }

    fun deleteBackground() = viewModelScope.launch { settingsRepository.deleteBackground() }
    fun updateBackgroundAlpha(alpha: Float) = viewModelScope.launch { settingsRepository.updateBackgroundAlpha(alpha) }

    fun updateLanguage(code: String?) = viewModelScope.launch {
        changeAppLanguage(code)
        settingsRepository.updateLanguageCode(code)
    }

    fun updateShowTimeline(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowTimeline(show) }
    fun updateShowDate(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowDate(show) }
    fun updateShowPeriodTime(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowPeriodTime(show) }
    fun updateShowNonCurrentWeek(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowNonCurrentWeek(show) }

    fun updateNewsNotificationEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.updateNewsNotificationEnabled(enabled) }

    fun updateNewsCheckIntervalMinutes(minutes: Int) =
        viewModelScope.launch { settingsRepository.updateNewsCheckIntervalMinutes(minutes) }

    fun updateCourseReminderEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.updateCourseReminderEnabled(enabled) }

    fun updatePermissionReminderDismissed(dismissed: Boolean) =
        viewModelScope.launch { settingsRepository.updatePermissionReminderDismissed(dismissed) }

    fun updateAutoStartEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.updateAutoStartEnabled(enabled) }

    fun updateDailyReportEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.updateDailyReportEnabled(enabled) }

    fun updateDailyReportTime(time: String) =
        viewModelScope.launch { settingsRepository.updateDailyReportTime(time) }

}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
