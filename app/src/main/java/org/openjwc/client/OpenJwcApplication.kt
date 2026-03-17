package org.openjwc.client

import android.app.Application
import cn.jiguang.api.JCoreInterface
import cn.jiguang.api.utils.JCollectionAuth
import cn.jpush.android.api.JPushInterface


class OpenJwcApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val context = applicationContext

        JPushInterface.setDebugMode(true)
        // JCore 5.0.4之前版本需要显式设置false
        if (JCoreInterface.getJCoreSDKVersionInt() < 504) {
            JCollectionAuth.setAuth(context, false)
        } else {
            return
        }
        JPushInterface.init(context)
        JCollectionAuth.setAuth(context, true)
    }
}