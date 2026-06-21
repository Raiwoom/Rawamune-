pluginManagement {
    repositories {
        google()        // Официальный репозиторий Google для загрузки Android Build Tools
        mavenCentral()  // Глобальный репозиторий для загрузки библиотек шифрования
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()        // Разрешаем скачивание Nearby Connections API и Jetpack Compose
        mavenCentral()  // Разрешаем скачивание WebRTC, SQLCipher и BouncyCastle
    }
}

// Имя нашего мессенджера
rootProject.name = "HardcoreP2PMessenger"

// Подключаем приложение
include(":app")

