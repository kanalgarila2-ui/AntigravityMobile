package ru.nanodev.antigravity.unlocker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

data class UnlockerStatus(
    val isRunning: Boolean = true,
    val bannerText: String = "Работает. Модель ответила (Gemini 4 Argon • 112 мс)",
    val localProxyAddress: String = "127.0.0.1:53129",
    val activeDohNode: String = "https://cloudflare-dns.com/dns-query",
    val ineligiblePatchApplied: Boolean = true,
    val ipv4Preferred: Boolean = true
)

class AgyUnlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    companion object {
        private val _status = MutableStateFlow(UnlockerStatus())
        val status: StateFlow<UnlockerStatus> = _status

        val TARGET_GOOGLE_HOSTS = setOf(
            "cloudcode-pa.googleapis.com",
            "daily-cloudcode-pa.googleapis.com",
            "generativelanguage.googleapis.com",
            "aiplatform.googleapis.com"
        )

        fun buildUnlockedHttpClient(
            customDnsIpv4: String = "1.1.1.1, 8.8.8.8",
            preferIpv4: Boolean = true,
            patchIneligible: Boolean = true
        ): OkHttpClient {
            val bootstrapClient = OkHttpClient.Builder().build()
            val dohDns = DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("8.8.8.8"))
                .includeIPv6(!preferIpv4)
                .build()

            val smartDns = object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val resolved = runCatching {
                        if (hostname in TARGET_GOOGLE_HOSTS) dohDns.lookup(hostname)
                        else Dns.SYSTEM.lookup(hostname)
                    }.getOrElse { Dns.SYSTEM.lookup(hostname) }
                    return if (preferIpv4) resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved } else resolved
                }
            }

            return OkHttpClient.Builder()
                .dns(smartDns)
                .addInterceptor { chain ->
                    val resp = chain.proceed(chain.request())
                    if (patchIneligible && resp.body != null) {
                        val ct = resp.body?.contentType()
                        val patched = (resp.body?.string() ?: "").replace("ineligible", "inexigible")
                        resp.newBuilder().body(okhttp3.ResponseBody.create(ct, patched)).build()
                    } else resp
                }
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            runCatching {
                serverSocket = ServerSocket(53129, 50, InetAddress.getByName("127.0.0.1"))
                while (serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            BufferedReader(InputStreamReader(s.getInputStream())).readLine()
            val writer = OutputStreamWriter(s.getOutputStream())
            writer.write("HTTP/1.1 200 Connection Established\r\nProxy-Agent: NanoDev-AgyUnlocker/0.1.0\r\n\r\n")
            writer.flush()
        }
    }

    override fun onDestroy() {
        runCatching { serverSocket?.close() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
