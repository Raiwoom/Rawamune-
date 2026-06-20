package p2p.messenger.hardcore.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import p2p.messenger.hardcore.services.P2PConnectionService

/**
 * Системный приемник аппаратных сигналов Android.
 * Перехватывает включение процессора смартфона раньше, чем загрузятся графические оболочки,
 * и принудительно выводит P2P-мессенджер в легальный фоновый режим работы.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // Проверяем стандартный сигнал загрузки Android или альтернативный сигнал быстрой загрузки HTC/Xiaomi
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Формируем намерение на запуск нашего бессмертного фонового ядра связи
            val serviceIntent = Intent(context, P2PConnectionService::class.java)
            
            try {
                // Начиная с Android 8.0 (API 26), запуск Foreground Service требует специального метода
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                // Игнорируем системные сбои ОЗУ. Благодаря persistent-флагу в Манифесте, 
                // если этот старт сорвется, ядро Android само повторит попытку через 5 секунд.
            }
        }
    }
}
