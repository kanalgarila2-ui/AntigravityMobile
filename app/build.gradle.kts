plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Один ключ подписи для debug и release: APK обновляются поверх друг друга.
// Путь и пароли берутся из переменных окружения (в CI — из GitHub Secrets).
// Без AG_KEYSTORE_PATH сборка подписывается стандартным debug-ключом машины,
// и тогда поставить поверх уже установленной сборки не получится.
val agKeystorePath: String? = System.getenv("AG_KEYSTORE_PATH")

android {
    namespace = "ru.nanodev.antigravity"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.nanodev.antigravity"
        minSdk = 30
        targetSdk = 34
        // Растёт от сборки к сборке (CI подставляет номер запуска), иначе обновление не встанет
        versionCode = (System.getenv("AG_VERSION_CODE") ?: "2").toInt()
        versionName = "0.2.0"
    }

    if (agKeystorePath != null) {
        signingConfigs {
            create("shared") {
                storeFile = file(agKeystorePath)
                storePassword = System.getenv("AG_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AG_KEY_ALIAS")
                keyPassword = System.getenv("AG_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (agKeystorePath != null) signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Без applicationIdSuffix: debug и release — один и тот же пакет
            if (agKeystorePath != null) signingConfig = signingConfigs.getByName("shared")
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Хранение API-ключа в зашифрованных настройках
    implementation("androidx.security:security-crypto:1.0.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}
