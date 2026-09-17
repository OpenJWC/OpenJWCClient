package org.openjwc.client.net.models

/** 网络代理配置（目前仅更新检查使用）。 */
sealed class Proxy {
    class NoProxy : Proxy()
    data class HttpProxy(val host: String, val port: Int) : Proxy()
    data class SocksProxy(val host: String, val port: Int) : Proxy()
}
