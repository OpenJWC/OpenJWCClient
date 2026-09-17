package org.openjwc.client.agent

/**
 * 单次 Agent 运行的资源上限。
 * 默认值对齐后端 `agent_*` 系统设置，硬编码不可被远端覆盖。
 */
data class AgentBudget(
    val maxModelRounds: Int = 8,
    val maxToolCalls: Int = 16,
    val maxToolsPerRound: Int = 4,
    /** 单次工具结果字符上限（按 UTF-8 字节计）。 */
    val maxToolResultBytes: Int = 16_000,
    /** 累计工具结果上限。 */
    val maxTotalToolBytes: Int = 96_000,
    val modelTimeoutMs: Long = 45_000,
    val runTimeoutMs: Long = 120_000,
) {
    init {
        require(maxTotalToolBytes >= maxToolResultBytes) { "累计工具结果预算不得低于单次上限" }
        require(runTimeoutMs >= modelTimeoutMs) { "总超时不得低于模型超时" }
    }
}
