plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")

}

android {
    namespace = "com.example.vpn_serve"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "29.0.13599879"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.example.vpn_serve"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
//    dependencies {
//        implementation("com.github.schwabe:ics-openvpn:icsopenvpn-0.7.33")
//    }
    buildFeatures {
        aidl = true  // This enables AIDL processing and stub generation
    }

}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
// Latest
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
// Update ExoPlayer

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":vpnLib"))

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.work:work-runtime") {
        version {
            strictly("2.7.0")
        }
    }
}

flutter {
    source = "../.."
}
