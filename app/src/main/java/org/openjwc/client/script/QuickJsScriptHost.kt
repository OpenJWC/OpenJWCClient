package org.openjwc.client.script

import app.cash.quickjs.QuickJs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openjwc.client.log.Logger
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val TAG = "ScriptHost"

/** 脚本静态校验的超时（不含任何真实网络请求）。 */
private const val VALIDATE_TIMEOUT_MS = 5_000L

/**
 * QuickJS 脚本宿主。
 *
 * 脚本契约：
 * ```js
 * // @id xxx  @name xxx  @domains example.com
 * function fetchNotices() {
 *   const page = http.get("https://example.com/list.htm");
 *   const rows = JSON.parse(dom.query(page, "li.news"));
 *   return JSON.stringify(rows.map(r => ({
 *     id: util.sha256(r.attrs.href),
 *     label: "通知",
 *     title: r.text,
 *     date: "2024-01-01",
 *     detail_url: r.attrs.href,
 *   })));
 * }
 * ```
 * 注意：桥接全局名为 `http` / `dom` / `util` / `params` / `console`，
 * 脚本里不要用同名局部变量（尤其别把页面内容命名成 `dom`）。
 *
 * 说明：QuickJS 的 `evaluate` 是同步阻塞的，且没有中断 API，
 * 因此超时/取消通过「独立线程执行 + Future.cancel」实现（被放弃的线程会自行跑到结束）。
 * 用可缓存线程池而非单线程，避免被放弃的脚本阻塞后续运行。
 */
class QuickJsScriptHost(
    private val httpClient: OkHttpClient,
    private val userAgent: String = "OpenJWC/1.0 (+https://github.com/OpenJWC)",
) {
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "quickjs-host")
    }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun run(
        script: String,
        sandbox: ScriptSandbox,
        params: ScriptRunParams = ScriptRunParams(),
        /** 脚本的实时日志（console.log / report.warn / report.progress）。 */
        onLog: ((String) -> Unit)? = null,
        /** 脚本上报的整体进度比例 0..1（report.progress 的 fraction）。 */
        onProgress: ((Double, String) -> Unit)? = null,
    ): ScriptOutcome {
        // 用 CompletableDeferred 把「阻塞的脚本执行」桥接到协程：
        // 这样超时和外部取消都能立刻让挂起点返回，而不用等脚本自己跑完。
        val deferred = CompletableDeferred<ScriptOutcome>()
        val future = executor.submit {
            try {
                deferred.complete(executeBlocking(script, sandbox, params, onLog, onProgress))
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
            }
        }
        try {
            return withTimeout(sandbox.timeoutMs) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            future.cancel(true)
            throw ScriptTimeoutException()
        } catch (e: CancellationException) {
            // 外部取消（用户点了取消）：放弃当前脚本，立刻向上传播
            future.cancel(true)
            throw e
        }
    }

    /**
     * 静态校验脚本：在**没有任何真实桥接**的沙箱里求值（顶层不应真的抓取），
     * 再检查是否定义了 `fetchNotices`。
     * @return 错误信息；脚本合法时返回 null
     */
    suspend fun validate(script: String, timeoutMs: Long = VALIDATE_TIMEOUT_MS): String? =
        withContext(Dispatchers.IO) {
            val future = executor.submit(Callable { validateBlocking(script) })
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                "脚本校验超时"
            } catch (e: ExecutionException) {
                (e.cause ?: e).message ?: "脚本校验失败"
            }
        }

    private fun validateBlocking(script: String): String? {
        val quickJs = QuickJs.create()
        try {
            quickJs.set("http", ScriptHttpApi::class.java, StubHttpBridge())
            quickJs.set("dom", ScriptHtmlApi::class.java, StubHtmlBridge())
            quickJs.set("util", ScriptUtilApi::class.java, StubUtilBridge())
            quickJs.set("console", ScriptConsoleApi::class.java, ConsoleBridge(null))
            quickJs.set("params", ScriptParamsApi::class.java, StubParamsBridge())
            quickJs.set("report", ScriptReportApi::class.java, StubReportBridge())
            quickJs.evaluate(script, "source.js")
            val type = quickJs.evaluate("typeof fetchNotices", "source-check")
            return if (type == "function") null else "脚本必须定义 function fetchNotices()"
        } catch (e: Throwable) {
            return e.message ?: e.toString()
        } finally {
            runCatching { quickJs.close() }
        }
    }

    /** 校验用的惰性桥接：只保证顶层求值不因缺少全局而失败。 */
    private class StubHttpBridge : ScriptHttpApi {
        override fun get(url: String): String = ""
        override fun post(url: String, body: String, contentType: String): String = ""
    }

    private class StubHtmlBridge : ScriptHtmlApi {
        override fun query(html: String, selector: String): String = "[]"
        override fun text(html: String, selector: String): String = ""
        override fun attr(html: String, selector: String, name: String): String = ""
        override fun markdown(html: String, selector: String, baseUrl: String): String = ""
    }

    private class StubUtilBridge : ScriptUtilApi {
        override fun sha256(text: String): String = ""
        override fun resolveUrl(base: String, relative: String): String = relative
        override fun now(): Double = 0.0
    }

    private class StubParamsBridge : ScriptParamsApi {
        override fun crawlDaysGap(): Int = 0
        override fun crawlCutoffDate(): String = ""
        override fun knownIdsJson(): String = "[]"
    }

    private class StubReportBridge : ScriptReportApi {
        override fun progress(scanned: Int, skipped: Int, failed: Int, fraction: Double, detail: String) = Unit

        override fun stats(
            scanned: Int,
            skipped: Int,
            failed: Int,
            noContent: Int,
            restricted: Int,
            skippedOld: Int,
            skippedDuplicate: Int,
            skippedKnown: Int,
        ) = Unit

        override fun warn(message: String) = Unit
    }

    private fun executeBlocking(
        script: String,
        sandbox: ScriptSandbox,
        params: ScriptRunParams,
        onLog: ((String) -> Unit)?,
        onProgress: ((Double, String) -> Unit)?,
    ): ScriptOutcome {
        val quickJs = QuickJs.create()
        val report = ReportBridge(onLog, onProgress)
        try {
            quickJs.set("http", ScriptHttpApi::class.java, HttpBridge(httpClient, sandbox, userAgent))
            quickJs.set("dom", ScriptHtmlApi::class.java, HtmlBridge())
            quickJs.set("util", ScriptUtilApi::class.java, UtilBridge())
            quickJs.set("console", ScriptConsoleApi::class.java, ConsoleBridge(onLog))
            quickJs.set("params", ScriptParamsApi::class.java, ParamsBridge(params))
            quickJs.set("report", ScriptReportApi::class.java, report)
            // 脚本与调用放在同一次 evaluate 中，避免函数声明不进入全局作用域
            val combined = script + "\n;JSON.stringify(fetchNotices());"
            val result = quickJs.evaluate(combined, "source.js")
            val text = result as? String
                ?: throw ScriptException("脚本 fetchNotices() 必须返回数组（内部会 JSON.stringify）")
            val notices = if (text.isBlank() || text == "null" || text == "undefined") {
                emptyList()
            } else {
                json.decodeFromString<List<ScriptNotice>>(text)
            }
            return ScriptOutcome(
                notices = notices,
                scanned = report.scanned,
                skipped = report.skipped,
                failed = report.failed,
                noContent = report.noContent,
                restricted = report.restricted,
                skippedOld = report.skippedOld,
                skippedDuplicate = report.skippedDuplicate,
                skippedKnown = report.skippedKnown,
                warnings = report.warnings,
            )
        } catch (e: ScriptSandboxException) {
            throw e
        } catch (e: ScriptTimeoutException) {
            throw e
        } catch (e: Throwable) {
            Logger.e(TAG, "脚本执行失败: ${e.message}", e)
            throw ScriptException("脚本执行失败: ${e.message}", e)
        } finally {
            runCatching { quickJs.close() }
        }
    }

    private class ParamsBridge(private val params: ScriptRunParams) : ScriptParamsApi {
        override fun crawlDaysGap(): Int = params.crawlDaysGap

        override fun crawlCutoffDate(): String =
            java.time.LocalDate.now().minusDays(params.crawlDaysGap.toLong()).toString()

        override fun knownIdsJson(): String =
            buildJsonArray { params.knownIds.forEach { add(it) } }.toString()
    }

    private class HttpBridge(
        private val client: OkHttpClient,
        private val sandbox: ScriptSandbox,
        private val userAgent: String,
    ) : ScriptHttpApi {

        private val textType = "text/plain; charset=utf-8".toMediaType()

        override fun get(url: String): String = execute(url, null, null)

        override fun post(url: String, body: String, contentType: String): String {
            val type = if (contentType.isBlank()) textType else contentType.toMediaType()
            return execute(url, body.toRequestBody(type), null)
        }

        private fun execute(
            url: String,
            body: okhttp3.RequestBody?,
            extraHeaders: Map<String, String>?,
        ): String {
            sandbox.checkUrl(url)
            sandbox.onHttpCall()
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "*/*")
            extraHeaders?.forEach { (k, v) -> builder.header(k, v) }
            if (body == null) builder.get() else builder.post(body)
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ScriptException("HTTP ${response.code} $url")
                }
                val text = response.body?.string().orEmpty()
                sandbox.onBytes(text.length.toLong())
                return text
            }
        }
    }

    private class HtmlBridge : ScriptHtmlApi {
        override fun query(html: String, selector: String): String {
            val doc = parse(html)
            val elements = if (selector.isBlank()) doc.allElements else doc.select(selector)
            return buildJsonArray {
                elements.forEach { element ->
                    addJsonObject {
                        put("tag", element.tagName())
                        put("text", element.text())
                        put("html", element.outerHtml())
                        putJsonObject("attrs") {
                            element.attributes().forEach { attribute ->
                                put(attribute.key, attribute.value)
                            }
                        }
                    }
                }
            }.toString()
        }

        override fun text(html: String, selector: String): String =
            parse(html).selectFirst(selector)?.text().orEmpty()

        override fun attr(html: String, selector: String, name: String): String =
            parse(html).selectFirst(selector)?.attr(name).orEmpty()

        override fun markdown(html: String, selector: String, baseUrl: String): String {
            val element = parse(html).selectFirst(selector) ?: return ""
            return HtmlToMarkdown.convert(element.html(), baseUrl)
        }

        /** 行片段（`<tr>`/`<td>`）需要包一层 table，否则会被 HTML 解析器丢弃。 */
        private fun parse(html: String): Document {
            val trimmed = html.trimStart()
            val lower = trimmed.take(4).lowercase()
            return if (lower.startsWith("<tr") || lower.startsWith("<td") || lower.startsWith("<th")) {
                Jsoup.parse("<table><tbody>$html</tbody></table>")
            } else {
                Jsoup.parseBodyFragment(html)
            }
        }
    }

    private class UtilBridge : ScriptUtilApi {
        override fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        override fun resolveUrl(base: String, relative: String): String =
            runCatching { URI(base).resolve(relative).toString() }.getOrDefault(relative)

        override fun now(): Double = System.currentTimeMillis().toDouble()
    }

    /** 收集脚本上报的统计与警告（有界）。 */
    private class ReportBridge(
        private val onLog: ((String) -> Unit)?,
        private val onProgress: ((Double, String) -> Unit)?,
    ) : ScriptReportApi {
        var scanned = 0
            private set
        var skipped = 0
            private set
        var failed = 0
            private set
        var noContent = 0
            private set
        var restricted = 0
            private set
        var skippedOld = 0
            private set
        var skippedDuplicate = 0
            private set
        var skippedKnown = 0
            private set
        val warnings = mutableListOf<String>()

        override fun stats(
            scanned: Int,
            skipped: Int,
            failed: Int,
            noContent: Int,
            restricted: Int,
            skippedOld: Int,
            skippedDuplicate: Int,
            skippedKnown: Int,
        ) {
            this.scanned = scanned.coerceAtLeast(0)
            this.skipped = skipped.coerceAtLeast(0)
            this.failed = failed.coerceAtLeast(0)
            this.noContent = noContent.coerceAtLeast(0)
            this.restricted = restricted.coerceAtLeast(0)
            this.skippedOld = skippedOld.coerceAtLeast(0)
            this.skippedDuplicate = skippedDuplicate.coerceAtLeast(0)
            this.skippedKnown = skippedKnown.coerceAtLeast(0)
        }

        override fun warn(message: String) {
            if (warnings.size < MAX_WARNINGS) warnings += message.take(200)
            onLog?.invoke("· $message")
        }

        override fun progress(scanned: Int, skipped: Int, failed: Int, fraction: Double, detail: String) {
            onProgress?.invoke(fraction.coerceIn(0.0, 1.0), detail)
            onLog?.invoke(
                "· $detail（扫描 $scanned，跳过 $skipped，失败 $failed）"
            )
        }

        private companion object {
            const val MAX_WARNINGS = 20
        }
    }

    private class ConsoleBridge(private val onLog: ((String) -> Unit)?) : ScriptConsoleApi {
        override fun log(message: String) {
            Logger.d(TAG, "js: $message")
            onLog?.invoke("· $message")
        }
    }
}
