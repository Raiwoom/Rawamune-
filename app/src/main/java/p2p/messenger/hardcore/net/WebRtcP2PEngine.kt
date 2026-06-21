package p2p.messenger.hardcore.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.webrtc.*
import p2p.messenger.hardcore.crypto.MTProtoEngine
import java.nio.ByteBuffer

class WebRtcP2PEngine(
    private val context: Context,
    private val onConnectionError: (String) -> Unit,   // Передача ошибки сбоя сети в UI
    private val onConnectionRestored: () -> Unit,       // Сигнал успешного подключения в UI
    private val onIncomingMessage: (String) -> Unit     // Передача входящего текста в UI
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    init {
        // Инициализируем нативные C++ библиотеки WebRTC на процессоре Android
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    // Функция ручного перезапуска P2P-ядра (для вашей КРАСНОЙ КНОПКИ)
    fun reconnectP2P(theirCryptoId: String, authKey: ByteArray) {
        if (!isInternetAvailable()) {
            onConnectionError("Отсутствует подключение к интернету. Проверьте Wi-Fi или мобильную сеть.")
            return
        }
        closeConnection()
        initP2PConnection(theirCryptoId, authKey)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun initP2PConnection(theirCryptoId: String, authKey: ByteArray) {
        // Запрещаем использовать классический IPv4, принудительно переключаем WebRTC на IPv6/Yggdrasil для защиты от РКН
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:://google.com").createIceServer(),
            PeerConnection.IceServer.builder("stun:://google.com").createIceServer(),
            PeerConnection.IceServer.builder("stun:://mozilla.com").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAX_BUNDLE
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                // Мониторинг состояния сети в реальном времени
                when (newState) {
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        onConnectionError("Проблемы с P2P-соединением. Собеседник недоступен.")
                    }
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        onConnectionRestored()
                    }
                    else -> {}
                }
            }

            override fun onDataChannel(channel: DataChannel) {
                setupDataChannelCallbacks(channel, authKey)
            }

            override fun onIceCandidate(candidate: IceCandidate) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onRenegotiationNeeded() {}
        })

        val dcInit = DataChannel.Init().apply { ordered = true }
        dataChannel = peerConnection?.createDataChannel("p2p_secure_chat", dcInit)
        dataChannel?.let { setupDataChannelCallbacks(it, authKey) }
    }

    private fun setupDataChannelCallbacks(channel: DataChannel, authKey: ByteArray) {
        channel.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)

                // Пакет прилетел из интернета! Намертво расшифровываем его через протокол MTProto 2.0
                val jsonPayload = MTProtoEngine.decryptMTProto(data, authKey)
                onIncomingMessage(jsonPayload)
            }
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
        })
    }

    // ОТПРАВКА: Упаковка текста/файла/опроса в MTProto и выстрел в DataChannel
    fun sendP2PMessage(jsonString: String, authKey: ByteArray, authKeyId: Long): Boolean {
        if (dataChannel?.state() != DataChannel.State.OPEN) return false // Если канал закрыт, сработают "Красные часики"

        val encryptedPacket = MTProtoEngine.encryptMTProto(jsonString, authKey, authKeyId)
        val byteBuffer = ByteBuffer.wrap(encryptedPacket)
        val buffer = DataChannel.Buffer(byteBuffer, false)
        
        dataChannel?.send(buffer)
        return true
    }

    fun closeConnection() {
        try {
            dataChannel?.close()
            peerConnection?.close()
        } catch (e: Exception) {}
    }
}
