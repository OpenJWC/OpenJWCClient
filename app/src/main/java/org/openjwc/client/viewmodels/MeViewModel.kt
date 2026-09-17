package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.CachedMotto
import org.openjwc.client.data.datastore.MottoCacheDataSource
import org.openjwc.client.data.models.Motto
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.hitokoto.HitokotoClient

/**
 * 「我的」页：每日一言。
 * 在线模式按天缓存（官方接口 QPS 限制 2），失败时回退到缓存或本地文本。
 */
class MeViewModel(
    private val repository: SettingsRepository,
    private val mottoCache: MottoCacheDataSource,
) : ViewModel() {

    private val tag = "MeViewModel"

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    /** 是否启用了在线一言（决定「我的」页能否手动刷新）。 */
    val onlineMode: StateFlow<Boolean> = repository.userSettings
        .map { it.mottoOnline }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val motto: StateFlow<Motto> = combine(
        repository.userSettings,
        mottoCache.flow,
    ) { settings, cached ->
        if (settings.mottoOnline) {
            if (cached.date.isBlank()) {
                Logger.d(tag, "在线一言尚无缓存，使用占位一言")
            }
            cached.toMotto()
        } else {
            Motto.local(settings.mottoText, settings.mottoAuthor)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Motto.DEFAULT_ONLINE,
    )

    /** 进入「我的」页时调用：在线模式且当天还没抓过才刷新。 */
    fun refreshMottoLazily() {
        viewModelScope.launch {
            if (!repository.getSettingsSnapshot().mottoOnline) {
                Logger.d(tag, "本地一言模式，跳过自动刷新")
                return@launch
            }
            val cached = mottoCache.current()
            if (cached.isFresh) {
                Logger.d(tag, "在线一言当天已刷新（${cached.date}），跳过")
                return@launch
            }
            Logger.i(tag, "在线一言缓存过期（${cached.date.ifBlank { "无" }}），开始刷新")
            refreshMotto()
        }
    }

    /** 手动刷新（仅在线模式有意义）。 */
    fun refreshMotto() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSnapshot()
                val response = HitokotoClient.fetch(
                    category = settings.hitokotoCategory.takeIf { it.isNotBlank() },
                    maxLength = settings.hitokotoMaxLength,
                )
                val motto = Motto(
                    text = response.hitokoto.trim(),
                    author = response.fromWho?.trim()?.takeIf { it.isNotEmpty() && it != Motto.ANONYMOUS },
                    source = response.from?.trim()?.takeIf { it.isNotEmpty() },
                    uuid = response.uuid.takeIf { it.isNotBlank() },
                    online = true,
                )
                mottoCache.save(CachedMotto.from(motto))
                Logger.i(
                    tag,
                    "在线一言已更新：uuid=${response.uuid} type=${response.type} length=${response.length}",
                )
            } catch (e: Exception) {
                Logger.e(tag, "获取一言失败: ${e.message}", e)
                uiEvent.send(
                    UiEvent.ShowToast(
                        UiText.DynamicString(e.localizedMessage ?: "获取一言失败")
                    )
                )
            } finally {
                _refreshing.value = false
            }
        }
    }
}

class MeViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val mottoCache: MottoCacheDataSource,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeViewModel(settingsRepository, mottoCache) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
