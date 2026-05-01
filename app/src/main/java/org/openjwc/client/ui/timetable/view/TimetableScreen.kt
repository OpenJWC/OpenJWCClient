package org.openjwc.client.ui.timetable.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.timetable.edit.EmptyGuidePlaceholder
import org.openjwc.client.ui.timetable.edit.tables.TableConfigDialog
import org.openjwc.client.ui.timetable.view.components.TimetableOverlayHost
import org.openjwc.client.ui.timetable.view.components.TimetableTopAppBar
import org.openjwc.client.ui.timetable.view.grid.TimetableGrid
import org.openjwc.client.viewmodels.TimetableViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: TimetableViewModel,
    onImportRequest: () -> Unit,
    contentPadding: PaddingValues
) {
    val tableMetadata by viewModel.currentTable.collectAsState()
    val currentTableCourses by viewModel.currentTableCourses.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val allTables by viewModel.allTables.collectAsState(initial = emptyList())
    val activePeriodIndex by viewModel.activePeriodIndex.collectAsState()
    val displayPrefs by viewModel.displayPrefs.collectAsState()

    val showTimeline = displayPrefs.showTimeline
    val showDate = displayPrefs.showDate
    val showPeriodTime = displayPrefs.showPeriodTime
    val showNonCurrentWeek = displayPrefs.showNonCurrentWeek

    val uiState = viewModel.uiState.collectAsState().value

    val totalWeeks = tableMetadata?.semesterConfig?.weeks ?: 1
    val pagerState = rememberPagerState(
        initialPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0)),
        pageCount = { totalWeeks }
    )

    LaunchedEffect(pagerState.settledPage) {
        val targetWeek = pagerState.settledPage + 1
        if (currentWeek != targetWeek) {
            viewModel.setWeek(targetWeek, fromPager = true)
        }
    }

    LaunchedEffect(currentWeek) {
        val targetPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetPage) {
            if (viewModel.isInternalWeekUpdate) {
                pagerState.scrollToPage(targetPage)
                viewModel.consumeInternalUpdate()
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val isImporting = viewModel.isImporting

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                tableMetadata?.let { metadata ->
                    TimetableTopAppBar(
                        timetableName = metadata.tableName,
                        currentWeek = currentWeek,
                        onTitleClick = { viewModel.updateUiState { it.copy(showTableConfigDialog = true) } },
                        canPrev = currentWeek > 1,
                        canNext = currentWeek < metadata.semesterConfig.weeks,
                        onPrev = { viewModel.prevWeek() },
                        onNext = { viewModel.nextWeek() }
                    )
                } ?: TopAppBar(title = { Text(stringResource(R.string.timetable)) })

                if (isImporting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isImporting) {
                FloatingActionButton(onClick = { viewModel.updateUiState { it.copy(showActionSheet = true) } }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.operating_menu)
                    )
                }
            }
        },
        bottomBar = { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxSize()
        ) {
            val currentTable = tableMetadata

            if (currentTable == null) {
                EmptyGuidePlaceholder(
                    onImport = onImportRequest,
                    onCreate = { viewModel.updateUiState { it.copy(showTableConfigDialog = true) } }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    TimetableGrid(
                        tableMetadata = currentTable,
                        courses = currentTableCourses,
                        showNonCurrentWeek = showNonCurrentWeek,
                        showTimeLine = showTimeline,
                        showDate = showDate,
                        showPeriodTime = showPeriodTime,
                        currentWeek = pageIndex + 1,
                        activePeriodIndex = if (pageIndex + 1 == currentWeek) activePeriodIndex else -1,
                        onCourseClick = { course ->
                            viewModel.updateUiState {
                                it.copy(
                                    clickedCourse = course,
                                    showDetailSheet = true
                                )
                            }
                        },
                        onEmptySlotClick = { day, period ->
                            viewModel.updateUiState {
                                it.copy(
                                    showEditDialog = true,
                                    editingCourseId = 0L,
                                    initialDay = day,
                                    initialStartPeriod = period
                                )
                            }
                        }
                    )
                }
            }
            viewModel.pendingImport?.let { data ->
                TableConfigDialog(
                    initialMetadata = data.metadata,
                    maxPeriodInUse = data.courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0,
                    onDismiss = { viewModel.cancelImport() },
                    onConfirm = { finalMetadata -> viewModel.confirmImport(finalMetadata) }
                )
            }

            TimetableOverlayHost(
                windowSizeClass = windowSizeClass,
                state = uiState,
                onStateChange = { viewModel.updateUiState(it) },
                onImportRequest = onImportRequest,
                currentWeek = currentWeek,
                currentTableCourses = currentTableCourses,
                allTables = allTables,
                currentTable = currentTable,
                maxPeriodInUse = viewModel.currentMaxPeriodInUse.collectAsState().value,
                onSaveCourse = viewModel::saveCourse,
                onDeleteCourse = viewModel::removeCourse,
                onCreateTable = viewModel::createTable,
                onUpdateTable = viewModel::updateTable,
                onDeleteTable = viewModel::deleteTable,
                onSwitchTable = viewModel::switchTable
            )
        }
    }
}
