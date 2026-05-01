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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import org.openjwc.client.R
import org.openjwc.client.log.Logger

private const val TAG = "ImportWebView"
private const val BRIDGE_NAME = "AndroidBridge"
private const val JS_FILE_NAME = "timetable_extractor.js"

/**
 * 具名桥接类：确保 JS 注解可见性与混淆安全性
 */
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

/**
 * Assets 读取辅助扩展
 */
fun Context.readAssetFile(fileName: String): String {
    return try {
        assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Logger.e("AssetReader", "Error reading $fileName", e)
        ""
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWebViewScreen(
    onDismiss: () -> Unit,
    onDataAcquired: (String) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val extractorJs = remember { context.readAssetFile(JS_FILE_NAME) }
    val loginUrl = "http://ehall.seu.edu.cn/appShow?appId=4770397878132218"

    val webBridge = remember {
        WebAppInterface(
            onData = { json ->
                webViewInstance?.post {
                    onDataAcquired(json)
                }
            },
            onError = { message ->
                webViewInstance?.post {
                    Toast.makeText(appContext, "导入失败: $message", Toast.LENGTH_LONG).show()
                }
            }
        )
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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (extractorJs.isBlank()) {
                        Logger.e(TAG, "JS_ERROR: Extractor script is empty")
                        Toast.makeText(appContext, "错误：无法加载提取脚本", Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    Toast.makeText(appContext, appContext.getString(R.string.extracting_timetable), Toast.LENGTH_SHORT).show()
                    webViewInstance?.evaluateJavascript(extractorJs, null)
                },
                icon = { Icon(Icons.Default.Check, null) },
                text = { Text(stringResource(R.string.click_me_when_you_see_the_timetable)) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                        }
                        loadUrl(loginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}