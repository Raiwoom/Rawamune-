package p2p.messenger.hardcore.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import p2p.messenger.hardcore.models.Message

@Dao
interface P2PMessageDao {
    // Сохранение входящих/исходящих сообщений, опросов, викторин и медиа
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: Message): Long

    // Подсчет непрочитанных сообщений для конкретного чата (status < 3 — не прочитано)
    @Query("SELECT COUNT(*) FROM messages WHERE contactId = :chatId AND status < 3")
    fun getUnreadCountForChat(chatId: String): Int

    // Мгновенный сброс счетчика-соты при открытии чата пользователем
    @Query("UPDATE messages SET status = 3 WHERE contactId = :chatId AND status < 3")
    fun markChatAsRead(chatId: String)

    // Запрос всей истории сообщений для вывода в капсулы чата (с сортировкой по времени)
    @Query("SELECT * FROM messages WHERE contactId = :chatId ORDER BY timestamp ASC")
    fun getMessageHistory(chatId: String): List<Message>
}
