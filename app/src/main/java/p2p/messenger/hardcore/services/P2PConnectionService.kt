package p2p.messenger.hardcore.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class P2PConnectionService : Service() {

    override fun onCreate() {
        super.onCreate()
        // Мгновенно переводим сервис в легальный статус Foreground при создании.
        // Использование ID 1337 и типов phoneCall/connectedDevice дает высший сетевой приоритет.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1337, 
                createPersistentNotification(), 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(1337, createPersistentNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // РЕЖИМ START_STICKY: Если система Android критически перегрузит ОЗУ и убьет сервис,
        // она обязана будет автоматически перезапустить его, как только освободится память.
        return START_STICKY 
    }

    /**
     * Создание постоянного системного уведомления для легализации фонового процесса.
     * Мы выставляем минимальную важность IMPORTANCE_MIN, чтобы иконка не мешала пользователю,
     * но правила Android (API 26+) были полностью соблюдены.
     */
    private fun createPersistentNotification(): Notification {
        val channelId = "p2p_core_immortality"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "P2P Ядро Связи", 
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Круглосуточный фоновый поток обхода NAT и ТСПУ"
                setSound(null, null) // Полная тишина для сервисного уведомления
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Защищенная P2P Сеть")
            .setContentText("Сквозное шифрование и обход ограничений активны")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Легальная системная иконка компаса/сети
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true) // Флаг запрещает пользователю случайно закрыть мессенджер свайпом
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Защита от случайного закрытия: если сервис уничтожен, посылаем интент на самовоскрешение
        val restartIntent = Intent(applicationContext, P2PConnectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onDestroy()
    }
}

