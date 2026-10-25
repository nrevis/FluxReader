import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.fluxreader.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.fluxreader.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 20121
        versionName = "0.2.0-alpha.104"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets.getByName("main").jniLibs.directories.add(layout.buildDirectory.dir("generated/speech/jniLibs").get().asFile.path)

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Alpha sideloads use the same local certificate as previous debug builds so an
            // installed test copy can be upgraded in-place, while the APK itself is non-debuggable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Official Apache-2.0 runtime, pinned by version AND digest. Models are downloaded
// by the user separately; neither voices nor espeak/piper-phonemize enter the APK.
val prepareSpeechRuntime = tasks.register("prepareSpeechRuntime") {
    val nativeRoot = layout.buildDirectory.dir("generated/speech")
    outputs.dir(nativeRoot)
    inputs.property("runtimeVersion", "2.0.0")
    doLast {
        val archive = rootProject.file(".gradle/flux-speech/runtime-v2.0.0.tar.bz2")
        val expected = "578650d0221255566bd7c88bd3a0deeb974b8562a332a6a875b35bd00b7e0f5d"
        fun digest(file: File): String {
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) }
            }
            return hash.digest().joinToString("") { "%02x".format(it) }
        }
        if (!archive.isFile || digest(archive) != expected) {
            archive.parentFile.mkdirs()
            val part = File(archive.path + ".part")
            try {
                URI("https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.0/sherpa-onnx-v2.0.0-android.tar.bz2")
                    .toURL().openConnection().apply { connectTimeout = 30000; readTimeout = 30000 }
                    .getInputStream().use { input -> part.outputStream().use { input.copyTo(it) } }
                check(digest(part) == expected) { "Speech runtime checksum mismatch" }
                Files.move(part.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally { part.delete() }
        }
        copy {
            from(tarTree(resources.bzip2(archive)))
            include("jniLibs/*/libsherpa-onnx-jni.so", "jniLibs/*/libonnxruntime.so")
            into(nativeRoot)
        }
    }
}
tasks.named("preBuild").configure { dependsOn(prepareSpeechRuntime) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("androidx.pdf:pdf-core:1.0.0-beta01")
    implementation("androidx.pdf:pdf-document-service:1.0.0-beta01")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    implementation("org.apache.commons:commons-compress:1.28.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation("junit:junit:4.13.2")
}
