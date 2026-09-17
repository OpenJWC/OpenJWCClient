import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "org.openjwc.client"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.openjwc.client"
        minSdk = 26
        targetSdk = 37
        versionCode = 38
        versionName = "2.0b"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("openjwc.keyStore")
            if (!keystorePath.isNullOrBlank()) {
                val keystoreFile = file(keystorePath)
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = localProperties.getProperty("openjwc.storePassword")
                    keyAlias = localProperties.getProperty("openjwc.keyAlias")
                    keyPassword = localProperties.getProperty("openjwc.keyPassword")
                }
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.java.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    testOptions {
        unitTests {
            // 让 android.util.Log 等桩方法返回默认值，便于在 JVM 单测里覆盖走日志的代码路径
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.java.get()))
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi")
    }
}

configurations.all {
    exclude(group = "androidx.navigationevent", module = "navigationevent-compose")
}

dependencies {
    implementation(libs.androidx.foundation)
    implementation(libs.animation)
    implementation(libs.androidx.appcompat)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation.layout)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material3.adaptive.navigation)
    implementation(libs.materialKolor)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.markdown)
    implementation(libs.compose.markdown.code)
    implementation(libs.compose.markdown.coil3)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.monet.compat)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.jsoup)
    implementation(libs.quickjs.android)
    implementation(libs.androidx.security.crypto)
    debugImplementation(libs.glance.appwidget.preview)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
