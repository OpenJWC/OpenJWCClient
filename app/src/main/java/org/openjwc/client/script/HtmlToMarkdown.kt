package org.openjwc.client.script

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/**
 * 精简版 HTML → Markdown，对齐 JwcCrawler 里 `htmd` 的用法：
 * - 跳过 script / style / colgroup / col 等；
 * - 标题、段落、列表、引用、代码、加粗、斜体、删除线、链接、图片、表格；
 * - 相对链接按 base 转绝对；`icon_*` 的链接与图片丢弃（模板图标）；
 * - 折叠多余空白，段落之间空行，列表项与表格行之间单换行。
 *
 * 纯 Jsoup 实现，可在 JVM 单测里直接验证。
 */
object HtmlToMarkdown {

    private val SKIP_TAGS = setOf(
        "script", "style", "colgroup", "col", "noscript", "head", "meta", "link",
        "title", "iframe", "svg", "form", "input", "button", "select", "option",
    )

    private val BLOCK_TAGS = setOf(
        "p", "div", "section", "article", "figure", "figcaption", "dd", "dt",
        "header", "footer", "main", "aside", "nav", "center", "fieldset",
    )

    private val SPACES = Regex("[ \\t\\u00a0]{2,}")
    private val ORDERED_ITEM = Regex("^\\d+[.)] ")

    /** 把 HTML 片段转成 Markdown；[baseUrl] 用于把相对链接转成绝对链接。 */
    fun convert(html: String, baseUrl: String? = null): String {
        if (html.isBlank()) return ""
        val cleaned = html.replace("&nbsp;", " ").replace('\u00a0', ' ')
        val body = Jsoup.parseBodyFragment(cleaned).body()
        val raw = StringBuilder()
        body.childNodes().forEach { raw.append(renderNode(it, baseUrl)) }
        return cleanup(raw.toString())
    }

    private fun renderNode(node: Node, base: String?): String = when (node) {
        is TextNode -> node.text()
        is Element -> renderElement(node, base)
        else -> ""
    }

    private fun renderChildren(element: Element, base: String?): String =
        element.childNodes().joinToString("") { renderNode(it, base) }

    /** 只渲染内联内容（用于表格单元格、标题、列表项），换行折叠成空格。 */
    private fun inline(element: Element, base: String?): String =
        renderChildren(element, base).replace('\n', ' ')

    private fun renderElement(element: Element, base: String?): String {
        val tag = element.tagName().lowercase()
        if (tag in SKIP_TAGS) return ""

        return when (tag) {
            "br" -> "\n"
            "hr" -> "\n\n---\n\n"
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val text = inline(element, base).trim()
                if (text.isEmpty()) "" else "\n\n${"#".repeat(tag[1] - '0')} $text\n\n"
            }

            "strong", "b" -> wrap(element, base, "**")
            "em", "i" -> wrap(element, base, "*")
            "del", "s", "strike" -> wrap(element, base, "~~")

            "code", "tt" ->
                if (element.parent()?.tagName() == "pre") inline(element, base)
                else "`${inline(element, base).trim()}`"

            "pre" -> "\n\n```\n${element.wholeText().trim()}\n```\n\n"

            "blockquote" -> {
                val text = renderChildren(element, base).trim()
                if (text.isEmpty()) "" else "\n\n" + text.lines().joinToString("\n") { "> $it" } + "\n\n"
            }

            "a" -> link(element, base)
            "img" -> image(element, base)
            "ul", "ol" -> list(element, base, ordered = tag == "ol")
            "table" -> table(element, base)
            // 表格相关标签由 table() 统一处理
            "tr", "td", "th", "thead", "tbody", "tfoot", "caption" -> ""

            in BLOCK_TAGS -> {
                // 块级容器要用结构性渲染，保留内部段落/列表的换行（用 inline 会把正文压成一行）
                val text = renderChildren(element, base).trim()
                if (text.isEmpty()) "" else "\n\n$text\n\n"
            }

            else -> renderChildren(element, base)
        }
    }

    private fun wrap(element: Element, base: String?, marker: String): String {
        val text = inline(element, base).trim()
        return if (text.isEmpty()) "" else "$marker$text$marker"
    }

    private fun link(element: Element, base: String?): String {
        val text = inline(element, base).trim()
        val href = absolute(element.attr("href"), base) ?: return text
        if (href.contains("icon_")) return text
        return if (text.isEmpty()) href else "[$text]($href)"
    }

    private fun image(element: Element, base: String?): String {
        val raw = element.attr("src").ifBlank { element.attr("pdfsrc") }
        val src = absolute(raw, base) ?: return ""
        if (src.contains("icon_")) return ""
        val alt = element.attr("alt").ifBlank { element.attr("title") }
        return "![$alt]($src)"
    }

    private fun list(element: Element, base: String?, ordered: Boolean): String {
        val items = element.children().filter { it.tagName() == "li" }
        if (items.isEmpty()) return ""
        val body = items.mapIndexed { index, li ->
            val marker = if (ordered) "${index + 1}. " else "- "
            val text = renderChildren(li, base)
                .replace('\n', ' ')
                .replace(SPACES, " ")
                .trim()
            marker + text
        }.joinToString("\n")
        return "\n\n$body\n\n"
    }

    /** 表格 → Markdown 表格（合并单元格会被拍平，只保留文字）。 */
    private fun table(element: Element, base: String?): String {
        val rows = element.select("tr").filter { it.select("td, th").isNotEmpty() }
        if (rows.isEmpty()) return ""
        val matrix = rows.map { row ->
            row.select("td, th").map { cell ->
                inline(cell, base).replace("|", "\\|").replace(SPACES, " ").trim()
            }
        }
        val columns = matrix.maxOf { it.size }
        if (columns == 0) return ""
        val padded = matrix.map { row -> row + List(columns - row.size) { "" } }
        return buildString {
            append("\n\n| ").append(padded.first().joinToString(" | ")).append(" |\n")
            append("| ").append(List(columns) { "---" }.joinToString(" | ")).append(" |\n")
            padded.drop(1).forEach { row ->
                append("| ").append(row.joinToString(" | ")).append(" |\n")
            }
            append("\n")
        }
    }

    private fun absolute(raw: String, base: String?): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.startsWith("javascript:", ignoreCase = true)) return null
        if (value.startsWith("#")) return null
        if (value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            return value
        }
        if (base.isNullOrBlank()) return value
        return runCatching { URI(base).resolve(value).toString() }.getOrDefault(value)
    }

    private fun cleanup(raw: String): String {
        val lines = raw.replace('\u00a0', ' ')
            .replace(SPACES, " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val result = StringBuilder()
        lines.forEachIndexed { index, line ->
            result.append(line)
            if (index < lines.lastIndex) {
                val next = lines[index + 1]
                val tight = (line.startsWith("|") && next.startsWith("|")) ||
                    (isListItem(line) && isListItem(next)) ||
                    (line.startsWith("> ") && next.startsWith("> "))
                result.append(if (tight) "\n" else "\n\n")
            }
        }
        return result.toString()
            .replace("****", "")
            .trim()
    }

    private fun isListItem(line: String): Boolean =
        line.startsWith("- ") || line.startsWith("* ") || ORDERED_ITEM.containsMatchIn(line)
}
