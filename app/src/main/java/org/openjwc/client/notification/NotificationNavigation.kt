package org.openjwc.client.notification

/**
 * 通知点击后的跳转目标（放在通知的 contentIntent 里）。
 * 由 `MainActivity` 读取并交给 `NavContainer` 消费。
 */
object NotificationNavigation {
    const val EXTRA_DESTINATION = "extra_destination"

    /** 打开某条资讯详情（配合 [NewsNotificationContract.EXTRA_NEWS_ID]）。 */
    const val DEST_NEWS_DETAIL = "news_detail"

    /** 打开资讯页。 */
    const val DEST_NEWS = "news"

    /** 打开课程表页。 */
    const val DEST_TIMETABLE = "timetable"
}
