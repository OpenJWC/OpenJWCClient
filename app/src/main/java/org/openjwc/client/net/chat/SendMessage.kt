package org.openjwc.client.net.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.ChatRequestBody
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.ChatErrorResponse
import org.openjwc.client.net.models.ChatNetworkResult

private const val LABEL = "SendMessage"

fun NetService.sendMessageStream(
    auth: String,
    deviceId: String,
    chatRequestBody: ChatRequestBody,
): Flow<ChatNetworkResult> = flow {
    try {
        val response = postChatQueryStream("Bearer $auth", deviceId, chatRequestBody)
        Logger.d(LABEL, "Response: $response")
        if (!response.isSuccessful) {
            Logger.e(LABEL, "Failure: ${response.code()} ${response.message()}")
            emit(ChatNetworkResult.Failure(response.code(), response.message()))
            return@flow
        }
        if (response.code() == 422) {
            Logger.e(LABEL, "422: $response")
            val errorBody = response.errorBody()?.string()

            if (errorBody.isNullOrBlank()) {
                Logger.e(LABEL, "422: Unknown error")
                emit(ChatNetworkResult.Failure(422, "Unknown error"))
                return@flow
            }

            try {
                val errorObj = Json.decodeFromString<ChatErrorResponse>(errorBody)
                emit(ChatNetworkResult.ValidationError(errorObj))
                return@flow
            } catch (e: Exception) {
                Logger.e(LABEL, "Parsing 422 message failure: ${e.message}")
                emit(ChatNetworkResult.Failure(422, "Parsing 422 message failure"))
                return@flow
            }
        }
        val source = response.body()?.source() ?: return@flow
        val buffer = okio.Buffer()
        var accumulatedText = ""
        val prefix = "data:"
        val separator = "\n\ndata:"

        while (true) {
            val read = source.read(buffer, 8192)
            while (true) {
                val start = buffer.indexOf(prefix.encodeUtf8())
                if (start == -1L) break

                val next = buffer.indexOf(separator.encodeUtf8(), start + prefix.length)

                if (next == -1L && read != -1L) break

                val payloadLength = if (next != -1L) {
                    next - start - prefix.length
                } else {
                    buffer.size - start - prefix.length
                }
                buffer.skip(start + prefix.length)

                var chunk = buffer.readUtf8(payloadLength)
                if (chunk.startsWith(" ")) chunk = chunk.substring(1)

                if (next != -1L) {
                    buffer.skip(separator.length.toLong() - prefix.length.toLong())
                }

                Logger.v(LABEL, "Chunk: [${chunk.replace("\n","\\n")}]")

                if (chunk.contains("[DONE]")) {
                    return@flow
                }

                accumulatedText += chunk
                emit(ChatNetworkResult.Success(accumulatedText))

                if (next == -1L) break
            }
            if (read == -1L && buffer.size == 0L) break
        }

    } catch (e: Exception) {
        emit(ChatNetworkResult.Error("连接中断: ${e.localizedMessage}"))
    }

}.flowOn(Dispatchers.IO)

