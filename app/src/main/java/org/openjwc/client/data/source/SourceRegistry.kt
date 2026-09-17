package org.openjwc.client.data.source

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openjwc.client.data.dao.SourceDao
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.log.Logger
import org.openjwc.client.script.ScriptManifestParser
import java.io.File

/**
 * 数据源注册表：内置脚本同步、侧载脚本落盘、订阅/删除。
 * 侧载脚本保存在 `filesDir/sources/<id>.js`。
 */
class SourceRegistry(
    private val context: Context,
    private val sourceDao: SourceDao,
    private val settingsDataSource: SettingsDataSource,
) {
    private val tag = "SourceRegistry"

    /** 新装/新增内置数据源时默认订阅的唯一数据源（教务处）。其余学院源默认不订阅。 */
    private val defaultSubscribedId = "seu-jwc"

    private fun sourcesDir(): File = File(context.filesDir, "sources").apply { mkdirs() }

    fun scriptFile(sourceId: String): File = File(sourcesDir(), "$sourceId.js")

    /**
     * 把 assets 里的内置脚本同步进数据库；保留用户已设置的 subscribed / 运行结果。
     * 若该内置脚本存在用户覆盖版本（filesDir/sources/<id>.js），以覆盖版本的 manifest 为准。
     */
    suspend fun syncBuiltIns() {
        val deleted = settingsDataSource.deletedSourceIds()
        for (fileName in BuiltInSources.fileNames(context)) {
            val assetScript = BuiltInSources.read(context, fileName)
            val assetManifest = ScriptManifestParser.parse(assetScript)
            if (assetManifest == null) {
                Logger.w(tag, "内置脚本缺少 @id，跳过: $fileName")
                continue
            }
            // 内置脚本不允许修改：清掉历史版本留下的覆盖文件，一律以 assets 为准
            val leftover = scriptFile(assetManifest.id)
            if (leftover.exists() && leftover.delete()) {
                Logger.w(tag, "内置数据源 ${assetManifest.id} 不允许自定义，已移除遗留覆盖脚本")
            }
            val manifest = assetManifest
            val existing = sourceDao.getById(manifest.id)
            // 老版本允许删除内置源：这里重新装回，但恢复出来的默认不订阅
            val wasDeleted = manifest.id in deleted
            if (wasDeleted) {
                Logger.i(tag, "内置数据源 ${manifest.id} 之前被删除过，重新装回（默认不订阅）")
                settingsDataSource.clearSourceDeleted(manifest.id)
            }
            sourceDao.upsert(
                SourceEntity(
                    id = manifest.id,
                    name = manifest.name,
                    version = manifest.version,
                    origin = SourceEntity.ORIGIN_BUILTIN,
                    scriptFile = fileName,
                    domains = manifest.domains,
                    labels = manifest.labels,
                    scheduleMinutes = manifest.scheduleMinutes,
                    subscribed = existing?.subscribed
                        ?: (!wasDeleted && manifest.id == defaultSubscribedId),
                    lastRunAt = existing?.lastRunAt,
                    lastCount = existing?.lastCount ?: 0,
                    lastError = existing?.lastError,
                )
            )
        }
    }

    /**
     * 注册（或覆盖）一个侧载脚本。
     * @return 解析出的 id；脚本缺少 `@id` 时返回 null。
     */
    suspend fun register(script: String): SourceEntity? {
        val manifest = ScriptManifestParser.parse(script) ?: return null
        // 重新注册（含导入）视为恢复该内置数据源
        settingsDataSource.clearSourceDeleted(manifest.id)
        scriptFile(manifest.id).writeText(script)
        val existing = sourceDao.getById(manifest.id)
        val entity = SourceEntity(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            origin = SourceEntity.ORIGIN_SIDELOAD,
            scriptFile = scriptFile(manifest.id).name,
            domains = manifest.domains,
            labels = manifest.labels,
            scheduleMinutes = manifest.scheduleMinutes,
            subscribed = existing?.subscribed ?: true,
            lastRunAt = existing?.lastRunAt,
            lastCount = existing?.lastCount ?: 0,
            lastError = existing?.lastError,
        )
        sourceDao.upsert(entity)
        return entity
    }

    /**
     * 读取待导入脚本的内容（文件选择器返回的 uri）。
     * 只读文件；是否合法由调用方用 [SourceRunner.validateScript] 校验后再 [register]。
     * @return 文件内容；读取失败返回 null
     */
    suspend fun readScriptFromUri(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
        }.onFailure {
            Logger.e(tag, "读取导入文件失败: ${it.message}", it)
        }.getOrNull()
    }

    /** 内置脚本的原始内容（属性页只读展示）。 */
    fun builtInScript(source: SourceEntity): String =
        BuiltInSources.read(context, source.scriptFile ?: "${source.id}.js")

    /**
     * 保存脚本（仅侧载数据源可改；内置脚本只读）。
     * @return 更新后的数据源；内置脚本、缺少 `@id`、`@id` 与数据源不一致时返回 null
     */
    suspend fun saveScript(source: SourceEntity, script: String): SourceEntity? {
        if (source.isBuiltIn) {
            Logger.w(tag, "内置数据源 ${source.id} 不允许修改脚本，拒绝保存")
            return null
        }
        val manifest = ScriptManifestParser.parse(script) ?: return null
        if (manifest.id != source.id) {
            Logger.w(tag, "脚本 @id=${manifest.id} 与数据源 ${source.id} 不一致，拒绝保存")
            return null
        }
        scriptFile(source.id).writeText(script)
        val updated = source.copy(
            name = manifest.name.ifBlank { source.name },
            version = manifest.version,
            domains = manifest.domains,
            labels = manifest.labels,
            scheduleMinutes = manifest.scheduleMinutes,
        )
        sourceDao.upsert(updated)
        Logger.i(tag, "已保存数据源 ${source.id} 的脚本（v${manifest.version}）")
        return updated
    }

    /**
     * 删除数据源。内置数据源不允许删除。
     * @return 是否真的删除了
     */
    suspend fun delete(sourceId: String): Boolean {
        val source = sourceDao.getById(sourceId)
        if (source?.isBuiltIn == true) {
            Logger.w(tag, "内置数据源 $sourceId 不允许删除")
            return false
        }
        scriptFile(sourceId).delete()
        sourceDao.deleteById(sourceId)
        Logger.i(tag, "已删除侧载数据源 $sourceId")
        return true
    }

    suspend fun setSubscribed(sourceId: String, subscribed: Boolean) {
        sourceDao.setSubscribed(sourceId, subscribed)
    }

    /** 读取某个数据源的脚本：内置脚本来自 assets，侧载脚本来自落盘文件。 */
    fun scriptText(source: SourceEntity): String {
        if (source.isBuiltIn) return builtInScript(source)
        return scriptFile(source.id).takeIf { it.exists() }?.readText().orEmpty()
    }
}
