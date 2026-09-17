package org.openjwc.client.data.models

/**
 * 每日一言。
 * 支持两种来源：
 * - 本地自定义文本（对齐后端 `motto_text` / `motto_author`）；
 * - 在线一言（hitokoto.cn），带出处与 uuid，可跳转 `https://hitokoto.cn?uuid=<uuid>`。
 */
data class Motto(
    val text: String,
    val author: String? = null,
    /** 出处作品（在线一言的 `from`）。 */
    val source: String? = null,
    val uuid: String? = null,
    val online: Boolean = false,
) {
    /** 一言官网的该条详情页；本地自定义文本没有。 */
    val permalink: String? get() = uuid?.takeIf { it.isNotBlank() }?.let { "https://hitokoto.cn?uuid=$it" }

    companion object {
        const val DEFAULT_TEXT = "笃学尚行"
        const val ANONYMOUS = "佚名"

        /** 在线模式首次进入（尚未抓到）时的占位一言。 */
        val DEFAULT_ONLINE = Motto(
            text = "所谓觉悟，就是在漆黑的荒野中，开辟出一条理所应当前进的光明大道。",
            author = "乔鲁诺·乔巴纳",
            online = true,
        )

        /** 本地自定义文本；作者为空或「佚名」时按无作者处理。 */
        fun local(text: String, author: String): Motto {
            val trimmedAuthor = author.trim()
            return Motto(
                text = text.trim().ifEmpty { DEFAULT_TEXT },
                author = trimmedAuthor.takeIf { it.isNotEmpty() && it != ANONYMOUS },
            )
        }
    }
}
