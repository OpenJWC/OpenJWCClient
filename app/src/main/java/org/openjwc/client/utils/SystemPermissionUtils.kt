package org.openjwc.client.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** 运行时通知权限（Android 13+）。低于该版本始终视为已授权。 */
fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/** 系统层面是否允许本应用发送通知（包含用户在系统设置里手动关闭的情况）。 */
fun areNotificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } catch (_: Exception) {
        false
    }
}

fun openNotificationSettings(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    return startActivitySafely(context, intent, fallbackIntent)
}

fun openBatteryOptimizationSettings(context: Context): Boolean {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    return startActivitySafely(context, requestIntent, listIntent, detailsIntent)
}

/** 打开电池优化列表，用户可在此把应用改回“优化”以关闭豁免。 */
fun openBatteryOptimizationList(context: Context): Boolean {
    val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    return startActivitySafely(context, listIntent, detailsIntent)
}

/**
 * 依次尝试候选 Intent。Android 11+ 的包可见性会让 [Intent.resolveActivity] 对系统设置
 * 返回 null，因此这里不预检，直接尝试启动并捕获异常。
 */
fun startActivitySafely(context: Context, vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            // 尝试下一个候选 Intent
        }
    }
    return false
}

/**
 * 自启动（后台运行）权限是各厂商自定义的，没有统一 API 可读取状态，
 * 这里按机型逐个尝试打开对应的自启动管理页，最后回退到应用详情页。
 */
private val AUTO_START_COMPONENTS = listOf(
    // Xiaomi / Redmi / POCO
    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    // Huawei / Honor
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
    // OPPO / realme / OnePlus
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
    ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
    // vivo / iQOO
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
    // Meizu
    ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
    // Letv
    ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
)

fun openAutoStartSettings(context: Context): Boolean {
    for (component in AUTO_START_COMPONENTS) {
        try {
            context.startActivity(Intent().setComponent(component))
            return true
        } catch (_: Exception) {
            // 当前机型不适用，尝试下一个
        }
    }
    return startActivitySafely(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )
}
