package p2p.messenger.hardcore.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import p2p.messenger.hardcore.models.Contact
import p2p.messenger.hardcore.models.GroupChat
import p2p.messenger.hardcore.models.P2PChannel

@Dao
interface BlacklistDao {
    // Получение списков забаненных сущностей для трех табов в настройках
    @Query("SELECT * FROM contacts WHERE isBlocked = 1")
    fun getBlockedContacts(): List<Contact>

    @Query("SELECT * FROM groups WHERE isBlocked = 1")
    fun getBlockedGroups(): List<GroupChat>

    @Query("SELECT * FROM channels WHERE isBlocked = 1")
    fun getBlockedChannels(): List<P2PChannel>

    // Методы для быстрой разблокировки прямо по нажатию кнопки в настройках
    @Query("UPDATE contacts SET isBlocked = 0 WHERE contactId = :id")
    fun unblockContact(id: String)

    @Query("UPDATE groups SET isBlocked = 0 WHERE groupId = :id")
    fun unblockGroup(id: String)

    @Query("UPDATE channels SET isBlocked = 0 WHERE channelId = :id")
    fun unblockChannel(id: String)
}

