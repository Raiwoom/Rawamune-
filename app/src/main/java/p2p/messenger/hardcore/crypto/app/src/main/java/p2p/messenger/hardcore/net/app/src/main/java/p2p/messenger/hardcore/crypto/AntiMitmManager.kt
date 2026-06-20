package p2p.messenger.hardcore.crypto

import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.MessageDigest
import java.security.SecureRandom

object AntiMitmManager {

    // 1. Генерация временных ключей X25519 для безопасного рукопожатия
    fun generateEcdhKeyPair(): Pair<X25519PrivateKeyParameters, X25519PublicKeyParameters> {
        val generator = X25519KeyPairGenerator()
        generator.init(org.bouncycastle.crypto.KeyGenerationParameters(SecureRandom(), 256))
        val keyPair = generator.generateKeyPair()
        return Pair(keyPair.private as X25519PrivateKeyParameters, keyPair.public as X25519PublicKeyParameters)
    }

    // 2. Вычисление общего секретного ключа (Auth Key) без передачи его в сеть
    fun calculateSharedSecret(ourPrivate: X25519PrivateKeyParameters, theirPublic: X25519PublicKeyParameters): ByteArray {
        val sharedSecret = ByteArray(32)
        ourPrivate.generateSecret(theirPublic, sharedSecret, 0)
        return sharedSecret // Этот ключ передается в MTProto 2.0 для AES-IGE шифрования
    }

    // 3. Создание 4-значного визуального "Секретного кода" для защиты от MITM
    fun generateFingerprintCode(sharedSecret: ByteArray): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(sharedSecret)
        
        // Берем первые 4 байта хэша и превращаем их в простые цифры для сверки глазами в чате
        val code = StringBuilder()
        for (i in 0..3) {
            val num = (hash[i].toInt() and 0xFF) % 10
            code.append(num)
        }
        return code.toString() // Пример вывода: "4729"
    }
}
