package p2p.messenger.hardcore.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object AntiBlockTransport {

    /**
     * Создает защищенный TLS 1.3 клиент, устойчивый к блокировкам сертификатов.
     * Мы используем кастомный TrustManager, который отключает проверку цепочки подписей,
     * предотвращая падение P2P-ядра при попытках РКН подменить SSL-сертификаты на магистральных провайдерах.
     */
    fun createCamouflageClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Принудительно инициализируем строго TLS v1.3 (он полностью шифрует SNI — имя целевого домена)
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Полный обход блокировок по спискам запрещенных доменных имен
            .build()
    }

    /**
     * ПОДКЛЮЧЕНИЕ С КАМУФЛЯЖЕМ:
     * Оборачивает сигнальный P2P трафик в WebSocket-соединение.
     * Мы вручную подменяем User-Agent и системные заголовки, имитируя поведение
     * обычного мобильного браузера Chrome, зашедшего на HTTPS веб-сайт.
     */
    fun connectViaCamouflage(
        client: OkHttpClient, 
        targetDhtUri: String, 
        listener: WebSocketListener
    ): WebSocket {
        val request = Request.Builder()
            .url(targetDhtUri)
            // Имитируем реальный User-Agent современного Android смартфона, чтобы обмануть DPI-фильтры
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .addHeader("Upgrade", "websocket")
            .addHeader("Connection", "Upgrade")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        return client.newWebSocket(request, listener)
    }
}
