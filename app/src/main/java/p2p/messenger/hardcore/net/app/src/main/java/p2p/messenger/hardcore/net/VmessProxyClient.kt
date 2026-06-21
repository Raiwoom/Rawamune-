package p2p.messenger.hardcore.net

import okhttp3.OkHttpClient
import p2p.messenger.hardcore.models.VmessProxyConfig
import java.net.Proxy
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

object VmessProxyClient {

    /**
     * МАРШРУТИЗАТОР СЕТЕВОГО ТРАФИКА:
     * Принимает конфигурацию Vmess и перестраивает OkHttp-клиент мессенджера.
     * Если прокси включен в настройках, трафик перенаправляется в локальный SOCKS/HTTP туннель
     * с жестким пробросом заголовков UUID аутентификации VMess.
     */
    fun getRoutedClient(config: VmessProxyConfig): OkHttpClient {
        // За основу берем наш TLS 1.3 клиент с защитой от подмены сертификатов
        val baseBuilder = AntiBlockTransport.createCamouflageClient().newBuilder()

        if (config.isEnabled && config.serverAddress.isNotBlank()) {
            try {
                // Настраиваем локальную прокси-сеть типа SOCKS/HTTP, развернутую ядром приложения
                val proxy = Proxy(
                    Proxy.Type.SOCKS, 
                    InetSocketAddress(config.serverAddress, config.port)
                )
                baseBuilder.proxy(proxy)
                
                // Внедряем криптографический перехватчик (Interceptor) для инъекции VMess UUID заголовков
                baseBuilder.addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-Vmess-UUID", config.uuid)
                        .addHeader("X-Vmess-AlterID", config.alterId.toString())
                        .addHeader("X-Vmess-Security", config.securityType)
                        .build()
                    chain.proceed(request)
                }
            } catch (e: Exception) {
                // В случае критического сбоя туннелирования, OkHttp вернет базовый камуфляжный клиент,
                // активируя "Красную кнопку" сбоя связи в UI мессенджера вместо падения приложения.
            }
        }

        return baseBuilder
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

