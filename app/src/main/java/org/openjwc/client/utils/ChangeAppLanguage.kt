package org.openjwc.client.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.openjwc.client.R
import org.openjwc.client.log.Logger
import org.openjwc.client.viewmodels.UiText

/**
 * 修改应用语言
 * @param languageCode 语言代码 (如 "zh", "en")。
 * 如果传入 null 或空字符串，则恢复为跟随系统语言。
 */
fun changeAppLanguage(languageCode: String?) {
    val appLocale: LocaleListCompat = if (!languageCode.isNullOrBlank()) {
        LocaleListCompat.forLanguageTags(languageCode)
    } else {
        LocaleListCompat.getEmptyLocaleList()
    }
    Logger.d("changeAppLanguage", "updated to $languageCode")
    AppCompatDelegate.setApplicationLocales(appLocale)
}

val languages = mapOf(
    null to UiText.StringResource(R.string.follow_system),
    "zh" to UiText.StringResource(R.string.simplified_chinese),
    "en" to UiText.StringResource(R.string.english)
)