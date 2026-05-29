import java.util.Properties

// ── Sprint 4: Lê a chave do Gemini do local.properties ──────────────────────
// ── Sprint 3: Lê também a chave do Unsplash ──────────────────────────────────
// Feito aqui, fora do bloco android {}, para que o import de Properties
// seja resolvido corretamente pelo Kotlin DSL em todas as versões do Gradle.
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}
val geminiApiKey: String    = localProps.getProperty("GEMINI_API_KEY", "")
val unsplashAccessKey: String = localProps.getProperty("UNSPLASH_ACCESS_KEY", "")
// ────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.inventaai"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.inventaai"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Sprint 4: chave Gemini
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Sprint 3: chave Unsplash
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"$unsplashAccessKey\"")
    }

    buildFeatures {
        buildConfig = true   // necessário para expor as chaves via BuildConfig
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Sprint 3: Retrofit (cliente HTTP tipado para Unsplash API) ───────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ── Sprint 3: Glide (carregamento e cache de imagens) ───────────────────
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}