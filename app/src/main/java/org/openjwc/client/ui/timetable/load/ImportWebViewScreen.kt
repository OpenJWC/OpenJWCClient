package org.openjwc.client.ui.timetable.load

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.openjwc.client.R
import org.openjwc.client.log.Logger
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.viewmodels.TimetableViewModel

private const val TAG = "ImportWebView"
private const val BRIDGE_NAME = "AndroidBridge"
private const val JS_FILE_NAME = "timetable_extractor.js"

@Suppress("unused")
class WebAppInterface(
    private val onData: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun sendData(json: String) {
        Log.d(TAG, "JS_INJECTION: Data acquired")
        onData(json)
    }

    @JavascriptInterface
    fun onError(message: String) {
        Log.e(TAG, "JS_ERROR: $message")
        onError(message)
    }
}

fun Context.readAssetFile(fileName: String): String {
    return try {
        assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Logger.e("AssetReader", "Error reading $fileName", e)
        ""
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportWebViewScreen(navigator: Navigator, timetableViewModel: TimetableViewModel) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }

    val extractorJs = remember { context.readAssetFile(JS_FILE_NAME) }
    val loginUrl = "http://ehall.seu.edu.cn/appShow?appId=4770397878132218"

    val webBridge = remember {
        WebAppInterface(
            onData = { json ->
                webViewInstance?.post {
                    timetableViewModel.handleImportedJson(json)
                }
            },
            onError = { message ->
                webViewInstance?.post {
                    Toast.makeText(appContext, appContext.getString(R.string.import_failed, message), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { timetableViewModel.pendingImport }.collect { import ->
            if (import != null) {
                timetableViewModel.confirmImport(import.metadata)
                navigator.pop()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { timetableViewModel.importErrorMessage }.collect { msg ->
            if (msg != null) {
                webViewInstance?.post {
                    Toast.makeText(appContext, appContext.getString(R.string.import_failed, msg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (extractorJs.isBlank()) {
                        Toast.makeText(appContext, appContext.getString(R.string.extractor_script_error), Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    if (webViewInstance == null || loadError) {
                        Toast.makeText(appContext, appContext.getString(R.string.page_load_failed), Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    Toast.makeText(appContext, appContext.getString(R.string.extracting_timetable), Toast.LENGTH_SHORT).show()
                    webViewInstance?.evaluateJavascript(extractorJs, null)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                if (timetableViewModel.isImporting) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(Icons.TwoTone.Check, null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (timetableViewModel.isImporting) stringResource(R.string.uploading)
                    else stringResource(R.string.click_me_when_you_see_the_timetable)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            key(retryKey) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewInstance = this
                            addJavascriptInterface(webBridge, BRIDGE_NAME)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isPageLoading = true
                                    loadError = false
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isPageLoading = false
                                }
                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        isPageLoading = false
                                        loadError = true
                                    }
                                }
                            }
                            loadUrl(loginUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (loadError) {
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.TwoTone.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.page_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { retryKey++ }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else if (isPageLoading || timetableViewModel.isImporting) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
