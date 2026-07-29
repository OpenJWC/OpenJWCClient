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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.TimetableViewModelFactory

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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportWebViewScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val appContext = context.applicationContext

    val db = remember { AppDatabase.getDatabase(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, CachedDataSource(context), authDataSource, context) }
    val courseRepository = remember { CourseRepository(db.courseDao(), db.tableDao()) }
    val timetableViewModel: TimetableViewModel = viewModel(
        factory = TimetableViewModelFactory(courseRepository, settingsRepository)
    )

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

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
                    Toast.makeText(appContext, "Import failed: $message", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(appContext, "Import failed: $msg", Toast.LENGTH_LONG).show()
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
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (extractorJs.isBlank()) {
                        Toast.makeText(appContext, "Error: Cannot load extractor script", Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    Toast.makeText(appContext, appContext.getString(R.string.extracting_timetable), Toast.LENGTH_SHORT).show()
                    webViewInstance?.evaluateJavascript(extractorJs, null)
                },
                icon = {
                    if (timetableViewModel.isImporting) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.TwoTone.Check, null)
                    }
                },
                text = {
                    Text(
                        if (timetableViewModel.isImporting) stringResource(R.string.uploading)
                        else stringResource(R.string.click_me_when_you_see_the_timetable)
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(innerPadding).fillMaxSize()) {
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

            if (timetableViewModel.isImporting) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
