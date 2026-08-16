package org.openjwc.client.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Article
import androidx.compose.material.icons.twotone.CalendarToday
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.HourglassEmpty
import androidx.compose.material.icons.twotone.Label
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.viewmodels.NewsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewedNoticesScreen(navigator: Navigator, newsViewModel: NewsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val reviewedData by newsViewModel.reviewedNoticesData.collectAsState()
    LaunchedEffect(Unit) { newsViewModel.fetchReviewedNotices() }
    val reviews = reviewedData?.notices.orEmpty()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.upload_results)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (reviews.isEmpty()) {
            Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.TwoTone.Article, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_upload_records), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.review_result_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(reviews, key = { it.id }) { r ->
                    Spacer(Modifier.height(10.dp))
                    ReviewCard(r)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(r: org.openjwc.client.net.models.ReviewedNotice) {
    val icon: ImageVector
    val bgColor: Color
    val statusText: String
    when (r.status) {
        "approved" -> { icon = Icons.TwoTone.CheckCircle; bgColor = Color(0xFF2E7D32); statusText = stringResource(R.string.approved) }
        "rejected" -> { icon = Icons.TwoTone.Error; bgColor = Color(0xFFC62828); statusText = stringResource(R.string.rejected) }
        else -> { icon = Icons.TwoTone.HourglassEmpty; bgColor = Color(0xFFE65100); statusText = stringResource(R.string.pending) }
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(0.dp)) {
            // Top colored status bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(bgColor)
            )

            Column(Modifier.padding(16.dp)) {
                // Status badge + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bgColor.copy(alpha = 0.12f)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, Modifier.size(16.dp), tint = bgColor)
                            Spacer(Modifier.width(4.dp))
                            Text(statusText, style = MaterialTheme.typography.labelSmall, color = bgColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Title
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(10.dp))

                // Metadata row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.TwoTone.Label, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(r.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.TwoTone.CalendarToday, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(r.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }

                // Review reason
                if (r.review != null && r.review.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor.copy(alpha = 0.08f)
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            Icon(Icons.TwoTone.Article, null, Modifier.size(16.dp), tint = bgColor.copy(alpha = 0.6f))
                            Spacer(Modifier.width(8.dp))
                            Text(r.review, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
