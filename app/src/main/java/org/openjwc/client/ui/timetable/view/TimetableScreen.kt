package org.openjwc.client.ui.timetable.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.timetable.edit.EmptyGuidePlaceholder
import org.openjwc.client.ui.timetable.edit.tables.TableConfigDialog
import org.openjwc.client.ui.timetable.view.components.TimetableOverlayHost
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

    val isReady by viewModel.isReady.collectAsState()
    // 数据就绪后再推迟一帧渲染重网格，让加载动画先显示，避免阻塞切换 Tab 的那一帧
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(isReady) {
        if (isReady) {
            withFrameNanos { }
            showContent = true
        }
    }

    // 稳定的回调，避免父级重组时网格整棵子树级联重组
    val onCourseClick = remember {
        { course: Course ->
            viewModel.updateUiState {
                it.copy(clickedCourse = course, showDetailSheet = true)
            }
        }
    }
    val onEmptySlotClick = remember {
        { day: java.time.DayOfWeek, period: Int ->
            viewModel.updateUiState {
                it.copy(
                    showEditDialog = true,
                    editingCourseId = 0L,
                    initialDay = day,
                    initialStartPeriod = period
                )
            }
        }
    }

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

    Box(
        modifier = Modifier.padding(contentPadding)
    ) {
        if (!isReady || !showContent) {
            // 加载动画占位
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        } else {
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
                beyondViewportPageCount = 0
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
                    onCourseClick = onCourseClick,
                    onEmptySlotClick = onEmptySlotClick
                )
            }
        }
        viewModel.pendingImport?.let { data ->
            TableConfigDialog(
                initialMetadata = data.metadata,
                maxPeriodInUse = data.courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0,
                onDismiss = { viewModel.cancelImport() },
                onConfirm = { finalMetadata ->
                    viewModel.confirmImport(finalMetadata)
                }
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
