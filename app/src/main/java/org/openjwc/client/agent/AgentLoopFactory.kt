package org.openjwc.client.agent

import org.openjwc.client.data.datastore.LlmKeyStore
import org.openjwc.client.data.datastore.LlmSettingsDataSource
import org.openjwc.client.data.repository.DailyReportSource
import org.openjwc.client.data.repository.NoticeCorpus
import org.openjwc.client.data.repository.TimetableSource
import org.openjwc.client.net.llm.LlmClientFactory

/**
 * 按当前用户配置构造 [AgentLoop]（聊天与日报共用）。
 * Key 从加密存储读取，不落普通日志。
 */
class AgentLoopFactory(
    private val llmSettingsDataSource: LlmSettingsDataSource,
    private val llmKeyStore: LlmKeyStore,
    private val corpus: NoticeCorpus,
    /** 课表读取能力（可空；为空时不暴露 get_timetable 工具）。 */
    private val timetable: TimetableSource? = null,
    private val budget: AgentBudget = AgentBudget(),
) {
    /**
     * 日报只读能力。`DailyReportRepository` 自身依赖本工厂，构造上存在循环，
     * 因此由 NavContainer 创建好仓库后回填。
     */
    var dailyReportSource: DailyReportSource? = null

    suspend fun create(): AgentLoop {
        val config = llmSettingsDataSource.current()
        val apiKey = llmKeyStore.get(config.providerId).orEmpty()
        return AgentLoop(
            client = LlmClientFactory.create(config, apiKey),
            tools = AgentTools(corpus, timetable, dailyReportSource),
            repository = corpus,
            budget = budget,
        )
    }
}
