package org.openjwc.client.net.hitokoto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 一言（hitokoto.cn）返回的字段。 */
@Serializable
data class HitokotoResponse(
    val id: Long = 0,
    val uuid: String = "",
    /** 一言正文。 */
    val hitokoto: String = "",
    /** 分类字母，见 [HitokotoCategory]。 */
    val type: String = "",
    /** 出处作品。 */
    val from: String? = null,
    /** 作者。 */
    @SerialName("from_who") val fromWho: String? = null,
    val length: Int = 0,
)

/** 一言分类（`c` 参数）。 */
enum class HitokotoCategory(val code: String, val label: String) {
    ANIME("a", "动画"),
    COMIC("b", "漫画"),
    GAME("c", "游戏"),
    LITERATURE("d", "文学"),
    ORIGINAL("e", "原创"),
    INTERNET("f", "来自网络"),
    OTHER("g", "其他"),
    MOVIE("h", "影视"),
    POETRY("i", "诗词"),
    PHILOSOPHY("k", "哲学"),
    WIT("l", "抖机灵");

    companion object {
        fun fromCode(code: String): HitokotoCategory? = entries.firstOrNull { it.code == code }
    }
}

/**
 * 一言公开接口客户端。
 *
 * 接口说明：https://developer.hitokoto.cn/sentence/
 * - 官方为公益服务，QPS 限制 2，因此**只在按天缓存失效或用户手动刷新时**请求；
 * - 按官方请求，界面在在线模式下附上 `https://hitokoto.cn?uuid=<uuid>` 出处链接。
 */
object HitokotoClient {

    private const val BASE_URL = "https://v1.hitokoto.cn/"
    private const val USER_AGENT = "OpenJWC/1.0 (+https://github.com/OpenJWC)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetch(
        category: String? = null,
        minLength: Int = 0,
        maxLength: Int = 30,
    ): HitokotoResponse = withContext(Dispatchers.IO) {
        val base = BASE_URL.toHttpUrlOrNull()
            ?: throw IOException("一言地址无效")
        val url = base.newBuilder()
            .addQueryParameter("encode", "json")
            .addQueryParameter("charset", "utf-8")
            .addQueryParameter("min_length", minLength.coerceAtLeast(0).toString())
            .addQueryParameter("max_length", maxLength.coerceIn(1, 100).toString())
            .apply { category?.takeIf { it.isNotBlank() }?.let { addQueryParameter("c", it) } }
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("一言 HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<HitokotoResponse>(body) }.getOrElse {
                throw IOException("一言响应解析失败")
            }
            if (parsed.hitokoto.isBlank()) throw IOException("一言返回空内容")
            parsed
        }
    }
}
