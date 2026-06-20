plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "p2p.messenger.hardcore"
    compileSdk = 34

    defaultConfig {
        applicationId = "p2p.messenger.hardcore"
        minSdk = 26 // Требуется для легальных каналов уведомлений и Java 8+ API
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Включаем ProGuard/R8 обфускацию, чтобы Android не мог декомпилировать код
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Ядро P2P интернет-связи и жесткий обход NAT
    implementation("org.webrtc:google-webrtc:1.0.32006")

    // Оффлайн Mesh-связь по воздуху без интернета (Google Nearby)
    implementation("com.google.android.gms:play-services-nearby:19.0.1")

    // Jetpack Compose для футуристичного неонового UI, гексагональных сот и карандашных линий
    val composeVersion = "1.6.1"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Локальная Room DB + Жесткое AES-256 шифрование базы на лету (SQLCipher)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    // Криптография для MTProto 2.0, Диффи-Хеллмана и стеганографии
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // Сетевой клиент для Vmess-прокси и обфусцированных TLS 1.3 каналов
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
