package org.openjwc.client.net.chat

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import org.openjwc.client.net.models.ChatRequestBody
import org.openjwc.client.net.models.ChatService
import org.openjwc.client.net.models.ChatErrorResponse
import org.openjwc.client.net.models.NetworkResult

private const val LABEL = "SendMessage"

fun ChatService.sendMessageStream(
    auth: String,
    deviceId: String,
    chatRequestBody: ChatRequestBody,
): Flow<NetworkResult> = flow {
    try {
        val response = postQueryStream("Bearer $auth", deviceId, chatRequestBody)
        Log.d(LABEL, auth)
        Log.d(LABEL, "Response: $response")
        if (!response.isSuccessful) {
            Log.e(LABEL, "Failure: ${response.code()} ${response.message()}")
            emit(NetworkResult.Failure(response.code(), response.message()))
            return@flow
        }
        if (response.code() == 422) {
            Log.e(LABEL, "422: $response")
            val errorBody = response.errorBody()?.string()

            if (errorBody.isNullOrBlank()) {
                Log.e(LABEL, "422: Unknown error")
                emit(NetworkResult.Failure(422, "Unknown error"))
                return@flow
            }

            try {
                val errorObj = Json.decodeFromString<ChatErrorResponse>(errorBody)
                emit(NetworkResult.ValidationError(errorObj))
                return@flow
            } catch (e: Exception) {
                Log.e(LABEL, "Parsing 422 message failure: ${e.message}")
                emit(NetworkResult.Failure(422, "Parsing 422 message failure"))
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

                Log.d(LABEL, "Chunk: [${chunk.replace("\n","\\n")}]")

                if (chunk.contains("[DONE]")) {
                    return@flow
                }

                accumulatedText += chunk
                emit(NetworkResult.Success(accumulatedText))

                if (next == -1L) break
            }
            if (read == -1L && buffer.size == 0L) break
        }

    } catch (e: Exception) {
        emit(NetworkResult.Error("连接中断: ${e.localizedMessage}"))
    }

}.flowOn(Dispatchers.IO)