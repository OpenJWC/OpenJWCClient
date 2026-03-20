package org.openjwc.client.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.openjwc.client.data.settings.Event
import org.openjwc.client.ui.me.settings.MenuSectionCard
import org.openjwc.client.viewmodels.MeViewModel

@Composable
fun MeScreen(
    modifier: Modifier,
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    viewModel: MeViewModel = viewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .let {
                    if (
                        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                    ) it.widthIn(max = 720.dp) else it.fillMaxSize()
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = sections,
                key = { section -> section.title ?: section.items.hashCode() }
            ) { section ->
                MenuSectionCard(
                    section = section,
                    onEvent = {
                        when (it) {
                            is Event.Route -> {
                                navController.navigate(it.route)
                            }

                            is Event.Toggle -> {
                                // TODO: MeScreen 里面暂时没有开关，先不急
                            }

                            is Event.Action -> {
                                it.onAction()
                            }
                        }
                    }
                )
            }

            item(key = "footer_spacer") {
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}