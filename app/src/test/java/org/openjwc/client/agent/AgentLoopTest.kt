package org.openjwc.client.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.openjwc.client.data.dao.LabelCount
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.repository.CorpusCatalog
import org.openjwc.client.data.repository.NoticeCorpus
import org.openjwc.client.net.llm.LlmClient
import org.openjwc.client.net.llm.LlmDelta
import org.openjwc.client.net.llm.LlmMessage
import org.openjwc.client.net.llm.LlmProviderConfig
import org.openjwc.client.net.llm.LlmToolSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 逐次返回预置增量的假客户端。 */
private class FakeLlmClient(
    private val rounds: List<List<LlmDelta>>,
) : LlmClient {
    override val config = LlmProviderConfig()
    var calls = 0
        private set
    var lastTools: List<LlmToolSpec> = emptyList()
        private set

    override fun streamChat(messages: List<LlmMessage>, tools: List<LlmToolSpec>): Flow<LlmDelta> = flow {
        lastTools = tools
        val index = calls.coerceAtMost(rounds.size - 1)
        calls++
        rounds[index].forEach { emit(it) }
    }
}

/** 内存语料：一条资讯。 */
private class FakeCorpus : NoticeCorpus {
    private val notice = NoticeEntity(
        id = "notice-1",
        sourceId = "seu-jwc",
        label = "教务信息",
        title = "关于期末考试安排的通知",
        publishedAt = 1_700_000_000_000L,
        publishedDay = "2026-09-01",
        detailUrl = "https://jwc.seu.edu.cn/1",
        isPage = true,
        content = "考试时间：2026-09-20。",
        attachments = null,
        fetchedAt = 1_700_000_000_000L,
    )

    override suspend fun searchNotices(
        query: String, label: String, sourceId: String?, fromDay: String,
        toDay: String, favoriteOnly: Boolean, relevance: Boolean, limit: Int, offset: Int,
    ): List<NoticeEntity> = listOf(notice)

    override suspend fun countNotices(
        query: String, label: String, sourceId: String?, fromDay: String, toDay: String,
        favoriteOnly: Boolean,
    ): Int = 1

    override suspend fun findNotice(id: String): NoticeEntity? =
        notice.takeIf { it.id == id }

    override suspend fun corpusLabels(): List<LabelCount> = listOf(LabelCount("教务信息", 1))

    override suspend fun subscribedSources(): List<SourceEntity> = listOf(
        SourceEntity(
            id = "seu-jwc",
            name = "东南大学教务处",
            version = "1.0.1",
            origin = SourceEntity.ORIGIN_BUILTIN,
            labels = listOf("教务信息"),
            subscribed = true,
        )
    )

    override suspend fun corpusCatalog(): CorpusCatalog =
        CorpusCatalog(total = 1, firstDay = "2026-09-01", lastDay = "2026-09-01")
}

private fun toolCall(id: String, name: String, arguments: String) = listOf(
    LlmDelta.ToolCallDelta(index = 0, id = id, name = name, argumentsChunk = arguments),
    LlmDelta.Finished("tool_calls"),
)

private fun answer(text: String) = listOf(LlmDelta.Text(text), LlmDelta.Finished("stop"))

class AgentLoopTest {

    private fun loop(client: LlmClient) = AgentLoop(
        client = client,
        tools = AgentTools(FakeCorpus()),
        repository = FakeCorpus(),
    )

    @Test
    fun `工具轮之后用禁用工具的流式轮产出最终答案`() = runBlocking {
        val client = FakeLlmClient(
            listOf(
                toolCall("call_1", AgentTools.TOOL_SEARCH, """{"query":"考试"}"""),
                answer("期末考试安排在 2026-09-20。"),
            )
        )
        val events = loop(client).run(AgentRequest(query = "最近有什么考试通知？")).toList()

        assertTrue(events.first() is AgentEvent.RunStarted)
        assertEquals(1, events.count { it is AgentEvent.ToolStarted })
        assertEquals(1, events.count { it is AgentEvent.ToolCompleted })
        val completed = events.filterIsInstance<AgentEvent.ToolCompleted>().single()
        assertEquals(AgentEvent.STATUS_COMPLETED, completed.status)
        val deltas = events.filterIsInstance<AgentEvent.AnswerDelta>()
        assertEquals(AgentEvent.DELIVERY_STREAMING, deltas.single().delivery)
        assertTrue(deltas.single().text.contains("2026-09-20"))
        assertTrue(events.last() is AgentEvent.RunCompleted)
        // 工具轮 → 给出终答的工具轮 → 禁用工具的流式收束轮 = 3 次
        assertEquals(3, client.calls)
        assertTrue(client.lastTools.isEmpty())
    }

    @Test
    fun `未使用工具时也走一次禁用工具的流式收束`() = runBlocking {
        val client = FakeLlmClient(listOf(answer("你好，我是教务资讯助手。")))
        val events = loop(client).run(AgentRequest(query = "你好")).toList()

        val deltas = events.filterIsInstance<AgentEvent.AnswerDelta>()
        assertEquals(AgentEvent.DELIVERY_STREAMING, deltas.single().delivery)
        assertTrue(events.last() is AgentEvent.RunCompleted)
        // 工具轮 + 收束轮
        assertEquals(2, client.calls)
        assertTrue(client.lastTools.isEmpty())
    }

    @Test
    fun `工具失败只作为失败观察，运行仍可成功`() = runBlocking {
        val client = FakeLlmClient(
            listOf(
                toolCall("call_1", AgentTools.TOOL_READ, """{"id":"missing"}"""),
                answer("没有找到该资讯。"),
            )
        )
        val events = loop(client).run(AgentRequest(query = "读一下 missing")).toList()

        val completed = events.filterIsInstance<AgentEvent.ToolCompleted>().single()
        assertEquals(AgentEvent.STATUS_FAILED, completed.status)
        assertEquals("tool_not_found", completed.code)
        assertTrue(events.last() is AgentEvent.RunCompleted)
        assertEquals(3, client.calls)
    }

    @Test
    fun `每轮工具数超过上限时收束并补齐未执行观察`() = runBlocking {
        val many = (0..5).flatMap { index ->
            listOf(LlmDelta.ToolCallDelta(index, "call_$index", AgentTools.TOOL_LABELS, "{}"))
        } + LlmDelta.Finished("tool_calls")
        val client = FakeLlmClient(listOf(many, answer("已停止检索。")))
        val events = loop(client).run(AgentRequest(query = "列出栏目")).toList()

        assertTrue(events.none { it is AgentEvent.ToolStarted })
        assertTrue(events.last() is AgentEvent.RunCompleted)
        // 工具轮 + 收束轮
        assertEquals(2, client.calls)
    }
}
