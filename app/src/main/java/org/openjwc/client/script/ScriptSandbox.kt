package org.openjwc.client.script

/** 单次脚本执行的沙箱限制。 */
class ScriptSandbox(
    val allowedDomains: Set<String> = emptySet(),
    val timeoutMs: Long = 60_000L,
    val maxHttpCalls: Int = 200,
    val maxBytes: Long = 8L * 1024 * 1024,
) {
    private var httpCalls = 0
    private var bytes = 0L

    @Synchronized
    fun checkUrl(url: String) {
        val host = runCatching { java.net.URI(url).host }.getOrNull()?.lowercase()
            ?: throw ScriptSandboxException("非法 URL: $url")
        if (allowedDomains.isEmpty()) return
        val allowed = allowedDomains.any { domain ->
            val d = domain.lowercase()
            host == d || host.endsWith(".$d")
        }
        if (!allowed) throw ScriptSandboxException("域名不在白名单: $host")
    }

    @Synchronized
    fun onHttpCall() {
        httpCalls++
        if (httpCalls > maxHttpCalls) {
            throw ScriptSandboxException("HTTP 调用次数超过上限 ($maxHttpCalls)")
        }
    }

    @Synchronized
    fun onBytes(count: Long) {
        bytes += count
        if (bytes > maxBytes) throw ScriptSandboxException("下载数据超过上限")
    }
}
