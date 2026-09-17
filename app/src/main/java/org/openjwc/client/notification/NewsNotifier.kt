package org.openjwc.client.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.openjwc.client.MainActivity
import org.openjwc.client.R
import org.openjwc.client.net.models.FetchedNotice

object NewsNotifier {

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NewsNotificationContract.CHANNEL_ID,
            context.getString(R.string.notification_channel_news),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_news_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun postNewNews(context: Context, notices: List<FetchedNotice>) {
        if (notices.isEmpty()) return
        if (!canPostNotifications(context)) return

        ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 单条 → 直达该资讯详情；多条 → 打开资讯页
            if (notices.size == 1) {
                putExtra(NotificationNavigation.EXTRA_DESTINATION, NotificationNavigation.DEST_NEWS_DETAIL)
                putExtra(NewsNotificationContract.EXTRA_NEWS_ID, notices.first().id)
            } else {
                putExtra(NotificationNavigation.EXTRA_DESTINATION, NotificationNavigation.DEST_NEWS)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NewsNotificationContract.SUMMARY_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (notices.size == 1) {
            val notice = notices.first()
            buildSingleNotification(context, notice, contentPendingIntent)
        } else {
            buildSummaryNotification(context, notices, contentPendingIntent)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(NewsNotificationContract.SUMMARY_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission state may change between check and notify.
        }
    }

    private fun buildSingleNotification(
        context: Context,
        notice: FetchedNotice,
        contentIntent: PendingIntent
    ) = NotificationCompat.Builder(context, NewsNotificationContract.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(notice.title)
        .setContentText(notice.label)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notice.title))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    private fun buildSummaryNotification(
        context: Context,
        notices: List<FetchedNotice>,
        contentIntent: PendingIntent
    ): android.app.Notification {
        val style = NotificationCompat.InboxStyle()
        notices.take(MAX_INBOX_LINES).forEach { notice ->
            style.addLine(notice.title)
        }
        if (notices.size > MAX_INBOX_LINES) {
            style.setSummaryText(context.getString(R.string.news_notification_more, notices.size - MAX_INBOX_LINES))
        }

        return NotificationCompat.Builder(context, NewsNotificationContract.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.news_notification_summary_title))
            .setContentText(context.getString(R.string.news_notification_count, notices.size))
            .setStyle(style)
            .setNumber(notices.size)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private const val MAX_INBOX_LINES = 5
}
