package org.openjwc.client.script

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlToMarkdownTest {

    @Test
    fun `标题段落与加粗转成 markdown`() {
        val html = """
            <div class="Article_Content">
              <h2>报名通知</h2>
              <p>本次<strong>四六级</strong>报名时间为 <em>9月10日</em>。</p>
              <p>逾期不再受理。</p>
            </div>
        """.trimIndent()
        val md = HtmlToMarkdown.convert(html, "https://jwc.seu.edu.cn/2026/0910/x/page.htm")
        assertTrue(md.contains("## 报名通知"), md)
        assertTrue(md.contains("**四六级**"), md)
        assertTrue(md.contains("*9月10日*"), md)
        // 段落之间空行
        assertTrue(md.contains("。\n\n逾期不再受理。"), md)
    }

    @Test
    fun `相对链接转绝对并丢弃模板图标`() {
        val html = """
            <p><a href="/2026/0901/a.htm">附件说明</a>
            <a href="icon_dot.gif">·</a>
            <img src="/_upload/icon_new.gif"/>
            <img src="/_upload/pic.png" alt="示意图"/></p>
        """.trimIndent()
        val md = HtmlToMarkdown.convert(html, "https://jwc.seu.edu.cn/zxdt/list.htm")
        assertTrue(md.contains("[附件说明](https://jwc.seu.edu.cn/2026/0901/a.htm)"), md)
        assertFalse(md.contains("icon_"), md)
        assertTrue(md.contains("![示意图](https://jwc.seu.edu.cn/_upload/pic.png)"), md)
    }

    @Test
    fun `列表与表格`() {
        val html = """
            <ul><li>第一项</li><li>第二项</li></ul>
            <table>
              <tr><th>课程</th><th>时间</th></tr>
              <tr><td>高等数学</td><td>周一</td></tr>
            </table>
        """.trimIndent()
        val md = HtmlToMarkdown.convert(html, null)
        assertTrue(md.contains("- 第一项\n- 第二项"), md)
        assertTrue(md.contains("| 课程 | 时间 |"), md)
        assertTrue(md.contains("| --- | --- |"), md)
        assertTrue(md.contains("| 高等数学 | 周一 |"), md)
    }

    @Test
    fun `跳过脚本样式并折叠空白`() {
        val html = """
            <div><style>.a{color:red}</style><script>var a=1;</script>
            <p>正文   里有    很多空格</p>
            <p>   </p>
            <p>第二段</p></div>
        """.trimIndent()
        val md = HtmlToMarkdown.convert(html)
        assertFalse(md.contains("var a=1"), md)
        assertFalse(md.contains("color:red"), md)
        assertTrue(md.contains("正文 里有 很多空格"), md)
        assertEquals("正文 里有 很多空格\n\n第二段", md)
    }

    @Test
    fun `空输入返回空串`() {
        assertEquals("", HtmlToMarkdown.convert(""))
        assertEquals("", HtmlToMarkdown.convert("<div></div>"))
    }
}
