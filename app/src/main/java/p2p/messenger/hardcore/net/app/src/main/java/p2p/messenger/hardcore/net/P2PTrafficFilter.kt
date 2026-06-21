package p2p.messenger.hardcore.services

import p2p.messenger.hardcore.models.Contact
import p2p.messenger.hardcore.models.GroupChat
import p2p.messenger.hardcore.models.P2PChannel

object P2PTrafficFilter {

    /**
     * Жесткий фильтр для обычных приватных диалогов один на один.
     * Если контакт заблокирован пользователем, метод возвращает false,
     * и фоновое ядро связи полностью стирает пакет из ОЗУ.
     */
    fun shouldProcessContactMessage(sender: Contact): Boolean {
        return !sender.isBlocked
    }

    /**
     * Жесткий фильтр для группового P2P-трафика.
     * Защищает от приема сообщений, медиафайлов или викторин из групп,
     * которые пользователь занес в Черный Список в настройках.
     */
    fun shouldProcessGroupMessage(group: GroupChat): Boolean {
        return !group.isBlocked
    }

    /**
     * Жесткий фильтр для децентрализованных P2P-каналов.
     * Полностью блокирует обновление ленты постов и скачивание файлов
     * из забаненных идентификаторов каналов.
     */
    fun shouldProcessChannelPost(channel: P2PChannel): Boolean {
        return !channel.isBlocked
    }

    /**
     * Логика формирования пакета децентрализованной жалобы (Report).
     * Поскольку сервера нет, жалоба подписывается вашим крипто-ключом и транслируется
     * в P2P-сеть, чтобы скомпрометированный ID получил метку "Scam" у других узлов.
     */
    fun generateReportPayload(targetContactId: String, reason: String): String {
        val timestamp = System.currentTimeMillis()
        return """
            {
                "type": "DECENTRALIZED_REPORT",
                "target_id": "$targetContactId",
                "reason": "$reason",
                "timestamp": $timestamp
            }
        """.trimIndent()
    }
}
