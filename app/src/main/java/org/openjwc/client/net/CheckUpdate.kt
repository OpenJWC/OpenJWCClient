package org.openjwc.client.net

import org.openjwc.client.net.models.CheckUpdateService
import org.openjwc.client.net.models.GitHubRelease
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.fetch

suspend fun CheckUpdateService.getLatestRelease(): NetworkResult<GitHubRelease> = fetch { getLatest() }