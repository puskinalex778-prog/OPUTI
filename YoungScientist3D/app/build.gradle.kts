plugins {
    id("com.android.application")
}

android {
    namespace = "ru.younglab.lab3d"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.younglab.lab3d"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Для публикации подпишите своим ключом (см. README).
            // Чтобы просто установить на свой телефон, подойдёт debug-ключ:
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // не сжимать js-файлы, чтобы WebView быстрее их читал
        noCompress.addAll(listOf("js", "css", "html"))
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
