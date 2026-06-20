package p2p.messenger.hardcore.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1. МОДЕЛЬ НАШЕГО СОБСТВЕННОГО ПРОФИЛЯ
 * Хранит ФИО пользователя, пути к двум аватаркам и кастомный ID,
 * который начинается строго со строки "•User ID: "
 */
@Entity(tableName = "my_profile")
data class MyProfile(
    @PrimaryKey val id: Int = 1,
    var firstName: String = "",
    var lastName: String = "",
    var customUserId: String = "", // Пример: "•User ID: core_hacker"
    var avatarPath1: String? = null,
    var avatarPath2: String? = null
)

/**
 * 2. МОДЕЛЬ ДРУГИХ КОНТАКТОВ В СПИСКЕ
 * Поддерживает имя, фамилию, флаги "Избранное", "Заметки / Сам себе",
 * локальный Черный список, кастомные фоны и время последнего визита.
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val contactId: String, // Публичный крипто-ключ ID собеседника
    val firstName: String,
    val lastName: String,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val isSelf: Boolean = false,       // Если true — это чат для заметок самому себе
    val backgroundThemeUri: String? = null,
    val lastSeenTimestamp: Long = 0,
    val isOnline: Boolean = false
)

/**
 * 3. МОДЕЛЬ P2P ГРУППОВЫХ ЧАТОВ
 * Текстовый идентификатор группы создается пользователем
 * и должен начинаться строго со строки "•Group ID: " на английском.
 */
@Entity(tableName = "groups")
data class GroupChat(
    @PrimaryKey val groupId: String,   // Пример: "•Group ID: secure_mesh_team"
    var groupName: String,
    val memberIdsJson: String,         // Список крипто-ID участников в формате JSON-массива
    val creatorId: String,
    val isBlocked: Boolean = false     // Флаг блокировки группы в Черном списке
)

/**
 * 4. МОДЕЛЬ P2P КАНАЛОВ
 * Пользовательский текстовый идентификатор канала
 * должен начинаться строго со строки "•Chanell ID: "
 */
@Entity(tableName = "channels")
data class P2PChannel(
    @PrimaryKey val channelId: String, // Пример: "•Chanell ID: underground_news"
    var channelName: String,
    var channelAvatarPath1: String? = null,
    var channelAvatarPath2: String? = null,
    val creatorCryptoId: String,
    val isSubscribed: Boolean = true,
    val isBlocked: Boolean = false     // Флаг блокировки канала в Черном списке
)

/**
 * 5. УНИВЕРСАЛЬНАЯ МОДЕЛЬ ДЛЯ СООБЩЕНИЙ, ОПРОСОВ И ВИКТОРИН
 * Поддерживает таймер автоматического удаления (TTL) после прочтения.
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,             // ID чата, группы или канала, куда идет сообщение
    val text: String,                  // Контент (текст, Base64 медиафайла или JSON опроса)
    val timestamp: Long,               // Время отправки
    val status: Int,                   // 0 - часики, 1 - отправлено, 2 - доставлено, 3 - прочитано
    val ttlSeconds: Long = 0,          // Таймер самоликвидации в секундах
    val isOutgoing: Boolean = true     // true = отправили мы, false = входящее сообщение
)
