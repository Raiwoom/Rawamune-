package p2p.messenger.hardcore.crypto

import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom
import org.bouncycastle.util.encoders.Base58

object P2PAuth {

    /**
     * Валидация строгого пароля по вашему ТЗ.
     * Требования: длина от 4 символов, минимум 2 цифры, минимум 5 букв.
     * Примечание: из-за требований к количеству букв и цифр общая длина пароля станет минимум 7 символов.
     */
    fun verifyStrictPassword(pass: String): Boolean {
        if (pass.length < 4) return false
        val digits = pass.count { it.isDigit() }
        val letters = pass.count { it.isLetter() }
        return digits >= 2 && letters >= 5
    }

    /**
     * Детерминированная генерация пары ключей Ed25519 на основе Email и пароля.
     * Этот метод заменяет классическую регистрацию с токенами и SMS.
     * Одинаковая связка Почта + Пароль всегда математически выдаст один и тот же аккаунт на любом устройстве.
     */
    fun generateP2PKeysFromEmail(email: String, pass: String): Pair<ByteArray, ByteArray> {
        // Используем Email в качестве соли для PBKDF2 хэширования
        val salt = email.lowercase().trim().toByteArray(Charsets.UTF_8)
        
        // Настройка PBKDF2: 150 000 итераций для жесткой защиты от перебора паролей
        val spec: KeySpec = PBEKeySpec(pass.toCharArray(), salt, 150000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        
        // Получаем 256-битное "зерно" (Seed) для генератора ключей
        val seed = factory.generateSecret(spec).encoded

        // Инициализируем генератор ключей Ed25519 с полученным зерном
        val random = object : SecureRandom() {
            private var index = 0
            override fun nextBytes(bytes: ByteArray) {
                for (i in bytes.indices) {
                    if (index >= seed.size) index = 0
                    bytes[i] = seed[index++]
                }
            }
        }

        val kpGen = Ed25519KeyPairGenerator()
        kpGen.init(Ed25519KeyGenerationParameters(random))
        val keyPair = kpGen.generateKeyPair()

        val privateKey = (keyPair.private as Ed25519PrivateKeyParameters).encoded
        val publicKey = (keyPair.public as Ed25519PublicKeyParameters).encoded

        return Pair(privateKey, publicKey)
    }

    /**
     * Превращает публичный ключ в красивый и читаемый пользовательский ID.
     * Пример вывода: •User ID: p2p_5H7...39A
     */
    fun formatUserCryptoId(publicKey: ByteArray): String {
        val base58Key = Base58.encode(publicKey)
        // Обрезаем длинный ключ для удобства отображения, оставляя уникальный префикс и суффикс
        val shortId = base58Key.take(6) + "..." + base58Key.takeLast(4)
        return "•User ID: p2p_$shortId"
    }
}
