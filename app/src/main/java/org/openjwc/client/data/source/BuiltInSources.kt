package org.openjwc.client.data.source

import android.content.Context
import org.openjwc.client.log.Logger

/** 内置脚本（`assets/sources` 下的 .js 文件）。 */
object BuiltInSources {

    const val ASSET_DIR = "sources"

    fun fileNames(context: Context): List<String> = runCatching {
        context.assets.list(ASSET_DIR)
            ?.filter { it.endsWith(".js", ignoreCase = true) }
            ?.sorted()
            ?: emptyList()
    }.onFailure {
        Logger.e("BuiltInSources", "列出内置脚本失败: ${it.message}", it)
    }.getOrDefault(emptyList())

    fun read(context: Context, fileName: String): String = runCatching {
        context.assets.open("$ASSET_DIR/$fileName").bufferedReader().use { it.readText() }
    }.onFailure {
        Logger.e("BuiltInSources", "读取内置脚本 $fileName 失败: ${it.message}", it)
    }.getOrDefault("")
}
