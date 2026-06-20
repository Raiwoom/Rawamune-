pluginManagement {
    repositories {
        // Использовать строго локальный кэш плагинов для обхода сетевых ошибок
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Использовать строго локальный кэш библиотек для защиты от блокировок серверов
        mavenLocal()
    }
}

// Задаем имя нашего неуязвимого мессенджера
rootProject.name = "HardcoreP2PMessenger"

// Подключаем главный модуль приложения, где лежат манифест, криптография и интерфейс
include(":app")
