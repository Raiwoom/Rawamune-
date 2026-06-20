package p2p.messenger.hardcore.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

object E2EEManager {
    private const val KEY_SIZE_BYTES = 32 // 256 бит для ключа AES
    private const val IV_SIZE_BYTES = 12  // 96 бит для вектора инициализации GCM (стандарт)

    /**
     * ФУНКЦИЯ ШИФРОВАНИЯ:
     * Принимает сырую JSON-строку (сообщение, опрос, медиа-ссылку) и 256-битный сессионный ключ.
     * Возвращает монолитный массив байтов: IV + Зашифрованные данные + Аутентификационный тег.
     */
    fun encryptPayload(plainText: String, sessionKey: ByteArray): ByteArray {
        val inputBytes = plainText.toByteArray(Charsets.UTF_8)
        
        // Генерация криптографически стойкого случайного вектора инициализации (IV)
        // Это гарантирует, что даже одинаковые тексты при повторной отправке выдадут разный шифр-текст
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        // Инициализируем блочный шифр AES в безопасном режиме GCM (Authenticated Encryption)
        val cipher = GCMBlockCipher(AESEngine())
        
        // Настройка параметров: Ключ, длина тега аутентификации 128 бит, вектор инициализации
        val parameters = AEADParameters(KeyParameter(sessionKey), 128, iv)
        cipher.init(true, parameters)

        // Вычисляем размер выходного буфера с учетом тега целостности
        val outputBytes = ByteArray(cipher.getOutputSize(inputBytes.size))
        val outputLen = cipher.processBytes(inputBytes, 0, inputBytes.size, outputBytes, 0)
        cipher.doFinal(outputBytes, outputLen)

        // Склеиваем вектор инициализации и зашифрованное тело для отправки по P2P сети
        return iv + outputBytes
    }

    /**
     * ФУНКЦИЯ РАСШИФРОВКИ:
     * Принимает зашифрованный массив байтов, прилетевший из P2P-канала, и сессионный ключ.
     * Проверяет подлинность (тег целостности) и возвращает чистую исходную строку.
     */
    fun decryptPayload(encryptedPayload: ByteArray, sessionKey: ByteArray): String {
        // Извлекаем вектор инициализации (первые 12 байт)
        val iv = encryptedPayload.copyOfRange(0, IV_SIZE_BYTES)
        // Извлекаем зашифрованные данные (все остальные байты)
        val cipherBytes = encryptedPayload.copyOfRange(IV_SIZE_BYTES, encryptedPayload.size)

        val cipher = GCMBlockCipher(AESEngine())
        val parameters = AEADParameters(KeyParameter(sessionKey), 128, iv)
        cipher.init(false, parameters)

        val outputBytes = ByteArray(cipher.getOutputSize(cipherBytes.size))
        val outputLen = cipher.processBytes(cipherBytes, 0, cipherBytes.size, outputBytes, 0)
        
        // Если данные были подменены или перехвачены РКН/третьими лицами на пути связи,
        // этот метод doFinal выбросит исключение и намертво заблокирует чтение искаженного пакета.
        cipher.doFinal(outputBytes, outputLen)

        return String(outputBytes, Charsets.UTF_8)
    }
}
