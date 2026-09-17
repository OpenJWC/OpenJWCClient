package org.openjwc.client.ui.component

/**
 * GFM 的 autolink 会把紧跟 URL 的非空白字符（包括中文/中文标点）吞进链接，
 * 导致 `https://x.com/a这是文字` 整段都变成链接。
 *
 * 这里在渲染前把裸 URL 用 `<...>` 包起来（严格 autolink），并裁掉尾部的中文与中文标点，
 * 使链接只包含真正的 URL。代码块（``` / ~~~）内不做处理。
 */
object MarkdownLinkNormalizer {
    private val FENCED_CODE = Regex("(?s)(```.*?```|~~~.*?~~~)")
    private val BARE_URL = Regex("""(?<![\w(\[<])(https?://[^\s<>()\[\]"'`]+)""")

    fun normalize(markdown: String): String {
        if (markdown.isEmpty()) return markdown
        val sb = StringBuilder(markdown.length + 16)
        var last = 0
        for (match in FENCED_CODE.findAll(markdown)) {
            sb.append(normalizeSegment(markdown.substring(last, match.range.first)))
            sb.append(match.value)
            last = match.range.last + 1
        }
        sb.append(normalizeSegment(markdown.substring(last)))
        return sb.toString()
    }

    private fun normalizeSegment(segment: String): String =
        BARE_URL.replace(segment) { match ->
            val raw = match.value
            val url = raw.trimEnd { isCjk(it) }
            if (url.length == raw.length) raw else "<$url>${raw.substring(url.length)}"
        }

    /** 中文汉字、中文标点、假名、韩文、全角字符 */
    private fun isCjk(c: Char): Boolean {
        val code = c.code
        return code in 0x3000..0x303F || // CJK 标点
            code in 0x3040..0x30FF ||    // 日文假名
            code in 0x3400..0x4DBF ||    // CJK 扩展 A
            code in 0x4E00..0x9FFF ||    // CJK 统一表意
            code in 0xAC00..0xD7AF ||    // 韩文
            code in 0xF900..0xFAFF ||    // CJK 兼容表意
            code in 0xFF00..0xFFEF       // 全角
    }
}
