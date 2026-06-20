package p2p.messenger.hardcore.crypto

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import p2p.messenger.hardcore.database.P2PMessageDao
import p2p.messenger.hardcore.database.BlacklistDao
import p2p.messenger.hardcore.models.Contact
import p2p.messenger.hardcore.models.GroupChat
import p2p.messenger.hardcore.models.P2PChannel
import p2p.messenger.hardcore.models.Message
import p2p.messenger.hardcore.models.MyProfile
import p2p.messenger.hardcore.models.VmessProxyConfig

@Database(
    entities = [
        MyProfile::class,
        Contact::class,
        GroupChat::class,
        P2PChannel::class,
        Message::class,
        VmessProxyConfig::class
    ], 
    version = 1, 
    exportSchema = false
)
abstract class SecureDatabase : RoomDatabase() {
    
    // Подключаем ранее спроектированные интерфейсы запросов (DAO)
    abstract fun p2pMessageDao(): P2PMessageDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        @Volatile
        private var INSTANCE: SecureDatabase? = null

        /**
         * Инициализация зашифрованной базы данных.
         * Метод принимает ваш строгий пароль, конвертирует его в хэш-ключ
         * и разворачивает зашифрованный контейнер SQLCipher.
         */
        fun getInstance(context: Context, userPassword: String): SecureDatabase {
            return INSTANCE ?: synchronized(this) {
                // Инициализируем нативные C++ библиотеки SQLCipher в памяти Android
                SQLiteDatabase.loadLibs(context)

                // Превращаем сложный пароль (буквы + цифры) в байтовый ключ для AES-256
                val passphrase = SQLiteDatabase.getBytes(userPassword.toCharArray())
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureDatabase::class.java,
                    "hardcore_secure_p2p.db"
                )
                .openHelperFactory(factory) // Передаем фабрику шифрования в ядро Room
                .fallbackToDestructiveMigration() // Защита от сбоев при обновлении структуры таблиц
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}

