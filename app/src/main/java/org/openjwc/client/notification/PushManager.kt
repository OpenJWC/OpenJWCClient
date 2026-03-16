package org.openjwc.client.notification

import android.content.Context
import android.util.Log
import cn.jpush.android.api.JPushInterface
import cn.jpush.android.ups.JPushUPSManager

enum class Status { Successful, Failed }

object PushManager {
    private const val TAG = "PushManager"

    /**
     * 初始化极光推送
     * 注意：必须在用户点击隐私协议“同意”后调用
     */
    fun init(
        context: Context,
        appId: String,
        debugMode: Boolean = false,
        onGetStatus: (Status) -> Unit
    ) {
        Log.d(TAG, "Initializing JPush...")
        JPushInterface.setDebugMode(debugMode)
        JPushUPSManager.registerToken(
            context, appId, null, ""
        ) {
            if (it.returnCode == 0) {
                Log.d("UPS", "Register result: $it")
                onGetStatus(Status.Successful)
            }
            else {
                Log.e("UPS", "Register failed: $it")
                onGetStatus(Status.Failed)
            }
        }


    }

    /**
     * 获取设备的 Registration ID
     * 用于后端定向推送。如果初始化未完成，可能返回空。
     */
    fun getRegistrationId(context: Context): String? {
        val rid = JPushInterface.getRegistrationID(context)
        if (rid.isNullOrEmpty()) {
            Log.w(TAG, "Registration ID is empty. Is JPush initialized?")
        }
        return rid
    }

    /**
     * 停止推送服务（例如用户在设置中关闭了通知）
     */
    fun stopPush(context: Context, onGetStatus: (Status) -> Unit) {
        JPushUPSManager.turnOffPush(context) {
            if (it.returnCode == 0) {
                Log.d(TAG, "Push stopped")
                onGetStatus(Status.Successful)
            } else {
                Log.e(TAG, "Failed to stop push: $it")
                onGetStatus(Status.Failed)
            }
        }
    }

    /**
     * 恢复推送服务
     */
    fun resumePush(context: Context, onGetStatus: (Status) -> Unit) {
        JPushUPSManager.turnOnPush(context) {
            if (it.returnCode == 0) {
                Log.d(TAG, "Push resumed")
                onGetStatus(Status.Successful)
            } else {
                Log.e(TAG, "Failed to resume push: $it")
                onGetStatus(Status.Failed)
            }
        }
    }

    /**
     * 绑定别名（通常建议用学号或经过哈希的设备唯一标识）
     * 绑定后，后端可以直接根据 alias 发消息
     */
    fun setAlias(context: Context, alias: String) {
        // sequence 用于标记操作，可以在回调中识别
        val sequence = System.currentTimeMillis().toInt()
        JPushInterface.setAlias(context, sequence, alias)
        Log.d(TAG, "Setting alias: $alias")
    }
}