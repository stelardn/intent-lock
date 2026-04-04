plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Base64

val dotEnvFile = rootProject.file(".env")
val dotEnvValues =
    if (dotEnvFile.isFile) {
        dotEnvFile.readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    null
                } else {
                    val separatorIndex = trimmed.indexOf('=')
                    if (separatorIndex <= 0) {
                        null
                    } else {
                        val key = trimmed.substring(0, separatorIndex).trim()
                        val value = trimmed.substring(separatorIndex + 1).trim().removeSurrounding("\"")
                        key to value
                    }
                }
            }
            .toMap()
    } else {
        emptyMap()
    }

fun optionalSigningConfig(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: dotEnvValues[name]?.takeIf { it.isNotBlank() }

fun resolveReleaseStoreFile(): String? {
    optionalSigningConfig("INTENTLOCK_RELEASE_STORE_FILE")?.let { return it }

    val keystoreBase64 = optionalSigningConfig("INTENTLOCK_RELEASE_KEYSTORE_BASE64") ?: return null
    val generatedKeystore = rootProject.layout.buildDirectory.file("signing/intentlock-release.keystore").get().asFile
    generatedKeystore.parentFile.mkdirs()
    generatedKeystore.writeBytes(Base64.getDecoder().decode(keystoreBase64))
    return generatedKeystore.absolutePath
}

val releaseStoreFile = resolveReleaseStoreFile()
val releaseStorePassword = optionalSigningConfig("INTENTLOCK_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = optionalSigningConfig("INTENTLOCK_RELEASE_KEY_ALIAS")
val releaseKeyPassword = optionalSigningConfig("INTENTLOCK_RELEASE_KEY_PASSWORD")
val hasReleaseSigning =
    listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

android {
    namespace = "com.larissa.socialcontrol"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.larissa.socialcontrol"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
