package org.openjwc.client.ui.me.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.openjwc.client.navigation.Routes.buildSettingsRoute
import org.openjwc.client.data.settings.Event
import org.openjwc.client.data.settings.Menu
import org.openjwc.client.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    route: String, // 这个指向设置 viewModel 里面菜单的路径
    viewModel: SettingsViewModel
    // 这个 viewModel 用来监听里边 uiState 然后弹框，以及读取菜单用
) {
    val menu = viewModel.menus.collectAsState().value.find { it.route == route } ?: Menu (
        route = "test",
        title = "空菜单",
        sections = emptyList()
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(menu.title) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
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
                                // 处理 Menu 跳转
                                navController.navigate(buildSettingsRoute(it.route))
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