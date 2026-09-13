import java.io.InputStream
import java.io.OutputStream
import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.readproplus"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.readproplus"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    packaging {
        resources {
            excludes += setOf("lib/x86/**", "lib/x86_64/**")
        }
        jniLibs {
            useLegacyPackaging = false
            // Sherpa-ONNX and the Kokoro Java binding both publish this
            // SONAME. Keep the Sherpa copy selected deterministically; it is
            // the runtime that owns the Piper JNI dependency.
            pickFirsts += setOf(
                "lib/arm64-v8a/libonnxruntime.so",
                "lib/armeabi-v7a/libonnxruntime.so",
                "lib/x86_64/libonnxruntime.so",
            )
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

tasks.register("verifyKokoroAssets") {
    val modelFile = file("src/main/assets/kokoro-v0_19.onnx")
    val voiceFile = file("src/main/assets/nicole_voice.bin")
    val vocabularyFile = file("src/main/assets/kokoro_vocab.json")
    val dictionaryFile = file("src/main/assets/cmudict.dict")

    inputs.files(modelFile, voiceFile, vocabularyFile, dictionaryFile)

    doLast {
        check(modelFile.length() > 10_000_000L) {
            "Missing Kokoro ONNX model at ${modelFile.path}."
        }
        check(voiceFile.length() >= 256L * 4L * 2L && voiceFile.length() % (256L * 4L) == 0L) {
            "Kokoro voice asset is missing or invalid: ${voiceFile.path}."
        }
        check(vocabularyFile.exists() && vocabularyFile.readText().contains("\"model\"")) {
            "Kokoro tokenizer asset is missing or invalid: ${vocabularyFile.path}."
        }
        check(dictionaryFile.length() > 1_000_000L) {
            "CMU pronunciation dictionary is missing or incomplete: ${dictionaryFile.path}."
        }
    }
}

tasks.register("downloadKokoroModel") {
    val modelFile = file("src/main/assets/kokoro-v0_19.onnx")
    outputs.file(modelFile)

    doLast {
        if (!modelFile.exists() || modelFile.length() < 10_000_000L) {
            println("Downloading Kokoro ONNX model (~300MB) to assets/kokoro-v0_19.onnx ...")
            val url = URL("https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/model.onnx")
            val conn = url.openConnection()
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            val input: InputStream = conn.getInputStream()
            val output: OutputStream = modelFile.outputStream()
            input.copyTo(output)
            output.close()
            input.close()
            println("Downloaded Kokoro model (${modelFile.length()} bytes)")
        } else {
            println("Kokoro model already present in assets (${modelFile.length()} bytes)")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("verifyKokoroAssets")
}



dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.pdfbox.android)
    implementation(libs.onnxruntime.android)
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.junrar)
    implementation(libs.libdjvu)
    implementation(libs.djvulibre)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
