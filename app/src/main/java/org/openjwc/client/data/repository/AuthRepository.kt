package org.openjwc.client.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.net.auth.deviceUnbind
import org.openjwc.client.net.auth.devicesQuery
import org.openjwc.client.net.auth.login
import org.openjwc.client.net.auth.register
import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.LoginSuccessResponse
import org.openjwc.client.net.models.NetClient
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import java.security.MessageDigest
import java.util.Locale

class AuthRepository(
    private val authDataSource: AuthDataSource,
    private val settingsDataSource: SettingsDataSource
) {
    private val salt = "oPeNjWc_!!$%!$!(!(*!)_"
    private fun String.sha256(): String {
        val bytes = this.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.fold("") { str, it ->
            str + "%02x".format(Locale.ROOT, it)
        }
    }

    val authSession = authDataSource.authSession

    suspend fun getOrCreateUuid(): String {
        return authDataSource.getOrCreateUuid()
    }

    suspend fun getOrCreateDeviceName(): String {
        return authDataSource.getOrCreateDeviceName()
    }

    suspend fun login(
        account: String,
        password: String
    ): NetworkResult<SuccessResponse<LoginSuccessResponse>> {
        val settings = settingsDataSource.userSettings.first()
        try {
            val apiService =
                NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)
            val session = authDataSource.authSession.first()
            val deviceId = session.uuid

            val passwordHash = (password + salt).sha256()
            val result = apiService.login(
                session.token, deviceId, account, passwordHash,
                authDataSource.getOrCreateDeviceName()
            )

            if (result is NetworkResult.Success) {
                logout()
                authDataSource.saveSession(
                    result.response.data.username,
                    result.response.data.email,
                    result.response.data.token
                )
            }
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return NetworkResult.Error(e.localizedMessage ?: "Unknown Error")
        }
    }

    suspend fun register(
        username: String,
        password: String,
        email: String
    ): NetworkResult<SuccessResponse<Map<String, String>>> {
        val settings = settingsDataSource.userSettings.first()
        try {
            val apiService =
                NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)
            val deviceId = authDataSource.getOrCreateUuid()

            val passwordHash = (password + salt).sha256()
            val session = authDataSource.authSession.first()
            val result = apiService.register(
                session.token, deviceId, username, passwordHash,
                email
            )
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return NetworkResult.Error(e.localizedMessage ?: "Unknown Error")
        }
    }

    suspend fun deviceUnbind(uuid: String): NetworkResult<DevicesUnbindSuccessResponse> {
        val settings = settingsDataSource.userSettings.first()
        try {
            val apiService =
                NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)
            val session = authDataSource.authSession.first()
            val result = apiService.deviceUnbind(
                session.token ?: throw Exception("No token"),
                session.uuid,
                uuid
            )
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return NetworkResult.Error(e.localizedMessage ?: "Unknown Error")
        }
    }

    suspend fun deviceQuery(): NetworkResult<SuccessResponse<DevicesQueryResponseData>> {
        val settings = settingsDataSource.userSettings.first()
        try {
            val apiService =
                NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)
            val session = authDataSource.authSession.first()
            val result = apiService.devicesQuery(
                session.token ?: throw Exception("No token"),
                session.uuid
            )
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return NetworkResult.Error(e.localizedMessage ?: "Unknown Error")
        }
    }


    suspend fun logout(): NetworkResult<DevicesUnbindSuccessResponse> {
        val result = deviceUnbind(authDataSource.getOrCreateUuid())
        authDataSource.clearSession()
        return result
    }

    suspend fun clearSession() = authDataSource.clearSession()
}