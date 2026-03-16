package org.openjwc.client.ui.me.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.openjwc.client.navigation.Routes.buildSettingsRoute
import org.openjwc.client.data.settings.Event
import org.openjwc.client.data.settings.Menu
import org.openjwc.client.viewmodels.ChatEvent
import org.openjwc.client.viewmodels.SettingsEvent
import org.openjwc.client.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRoute: (String) -> Unit,
    onBack: () -> Unit,
    route: String, // 这个指向设置 viewModel 里面菜单的路径
    viewModel: SettingsViewModel
    // 这个 viewModel 用来监听里边 uiState 然后弹框，以及读取菜单用
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is SettingsEvent.ShowSnackBar -> {
                    // TODO: 显示 SnackBar
                }
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val menu = viewModel.menus.collectAsState().value.find { it.route == route } ?: Menu(
        route = "test",
        title = "空菜单",
        sections = emptyList()
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(menu.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = menu.sections,
                key = { it.title ?: it.hashCode() }
            ) { section ->
                MenuSectionCard(
                    section = section,
                    onEvent = {
                        when (it) {
                            is Event.Route -> {
                                onRoute(buildSettingsRoute(it.route))
                            }

                            is Event.Action -> {
                                // 处理 Action 执行
                                it.onAction()
                            }

                            is Event.Toggle -> {
                                viewModel.toggle(it.id)
                            }
                        }
                    }
                )
            }
        }
    }
}