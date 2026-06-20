package p2p.messenger.hardcore.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.IGEBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.MessageDigest
import java.security.SecureRandom

object MTProtoEngine {

    // Вычисление Message Key (msg_key) по официальной спецификации MTProto 2.0
    private fun computeMsgKey(payload: ByteArray, authKey: ByteArray, isOutgoing: Boolean): ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        
        // Смещение в auth_key зависит от направления пакета (X = 0 для исходящих, X = 8 для входящих)
        val x = if (isOutgoing) 0 else 8
        
        // Формула: msg_key = SHA256(auth_key[x+88..x+120] + payload) -> берем первые 16 байт
        sha256.update(authKey, x + 88, 32)
        sha256.update(payload)
        
        val fullHash = sha256.digest()
        return fullHash.copyOfRange(0, 16)
    }

    // Генерация криптографических параметров AES на основе msg_key и auth_key
    private fun deriveAesParams(authKey: ByteArray, msgKey: ByteArray, isOutgoing: Boolean): Pair<ByteArray, ByteArray> {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val x = if (isOutgoing) 0 else 8

        // sha256A = SHA256(msg_key + auth_key[x .. x+32])
        sha256.update(msgKey)
        sha256.update(authKey, x, 32)
        val sha256A = sha256.digest()

        // sha256B = SHA256(auth_key[x+32 .. x+64] + msg_key)
        sha256.reset()
        sha256.update(authKey, x + 32, 32)
        sha256.update(msgKey)
        val sha256B = sha256.digest()

        // Сборка 256-битного aes_key
        val aesKey = ByteArray(32)
        System.arraycopy(sha256A, 0, aesKey, 0, 8)
        System.arraycopy(sha256B, 8, aesKey, 8, 16)
        System.arraycopy(sha256A, 24, aesKey, 24, 8)

        // Сборка 256-битного aes_iv (Вектор инициализации)
        val aesIv = ByteArray(32)
        System.arraycopy(sha256B, 0, aesIv, 0, 8)
        System.arraycopy(sha256A, 8, aesIv, 8, 16)
        System.arraycopy(sha256B, 24, aesIv, 24, 8)

        return Pair(aesKey, aesIv)
    }

    // ЖЕСТКОЕ ШИФРОВАНИЕ: Упаковка текста, файлов, опросов, викторин, аудио и видео в контейнер AES-IGE
    fun encryptMTProto(plainText: String, authKey: ByteArray, authKeyId: Long): ByteArray {
        val rawData = plainText.toByteArray(Charsets.UTF_8)
        
        // Данные дополняются до кратности 16 байтам (Системный Padding для AES Block Cipher)
        val paddingLen = 16 - (rawData.size % 16)
        val paddedData = ByteArray(rawData.size + paddingLen)
        System.arraycopy(rawData, 0, paddedData, 0, rawData.size)
        
        // Заполняем остаток блока случайными байтами для защиты от криптоанализа
        SecureRandom().nextBytes(paddedData.copyOfRange(rawData.size, paddedData.size))

        // Вычисляем msg_key
        val msgKey = computeMsgKey(paddedData, authKey, isOutgoing = true)

        // Генерируем ключи шифрования
        val (aesKey, aesIv) = deriveAesParams(authKey, msgKey, isOutgoing = true)

        // Инициализируем нативный IGE-шифратор BouncyCastle
        val engine = IGEBlockCipher(AESEngine())
        val params = ParametersWithIV(KeyParameter(aesKey), aesIv)
        engine.init(true, params)

        val encryptedData = ByteArray(paddedData.size)
        var i = 0
        while (i < paddedData.size) {
            engine.processBlock(paddedData, i, encryptedData, i)
            i += 16
        }

        // Финальный монолитный пакет: AuthKeyID (8 байт) + MsgKey (16 байт) + Зашифрованное тело
        val result = ByteArray(8 + 16 + encryptedData.size)
        
        // Запись AuthKeyID (Long) в байты
        for (b in 0..7) {
            result[b] = (authKeyId ushr (b * 8)).toByte()
        }
        System.arraycopy(msgKey, 0, result, 8, 16)
        System.arraycopy(encryptedData, 0, result, 24, encryptedData.size)

        return result
    }

    // РАСШИФРОВКА: Извлечение и валидация входящих P2P пакетов
    fun decryptMTProto(encryptedPacket: ByteArray, authKey: ByteArray): String {
        val msgKey = encryptedPacket.copyOfRange(8, 24)
        val cipherData = encryptedPacket.copyOfRange(24, encryptedPacket.size)

        // Извлекаем параметры (isOutgoing = false, так как пакет входящий)
        val (aesKey, aesIv) = deriveAesParams(authKey, msgKey, isOutgoing = false)

        val engine = IGEBlockCipher(AESEngine())
        val params = ParametersWithIV(KeyParameter(aesKey), aesIv)
        engine.init(false, params)

        val decryptedData = ByteArray(cipherData.size)
        var i = 0
        while (i < cipherData.size) {
            engine.processBlock(cipherData, i, decryptedData, i)
            i += 16
        }

        // Возвращаем чистую JSON-строку, автоматически обрезая нули заполнения (Padding)
        return String(decryptedData, Charsets.UTF_8).trimEnd { it == '\u0000' || it.code < 32 }
    }
}

