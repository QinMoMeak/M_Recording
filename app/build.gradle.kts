import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

fun localOrProject(name: String, default: String = ""): String {
    val fromProject = (project.findProperty(name) as String?)?.trim()
    val fromLocal = localProps.getProperty(name)?.trim()
    val fromEnv = System.getenv(name)?.trim()
    return listOf(fromProject, fromLocal, fromEnv, default.trim())
        .firstOrNull { it != null && it.isNotBlank() }
        .orEmpty()
}

val aliyunAppKey = localOrProject("ALIYUN_APP_KEY", "YOUR_APP_KEY")
val aliyunAccessKeyId = localOrProject("ALIYUN_ACCESS_KEY_ID")
val aliyunAccessKeySecret = localOrProject("ALIYUN_ACCESS_KEY_SECRET")
val aliyunToken = localOrProject("ALIYUN_TOKEN")
val ossEndpoint = localOrProject("OSS_ENDPOINT")
val ossBucket = localOrProject("OSS_BUCKET")
val ossObjectPrefix = localOrProject("OSS_OBJECT_PREFIX", "m_recording/")
val ossPublicRead = localOrProject("OSS_PUBLIC_READ", "true")
val ossSignExpireSeconds = localOrProject("OSS_SIGN_EXPIRE_SECONDS", "3600")
val fileTransRegionId = localOrProject(
    "FILETRANS_REGION_ID",
    when {
        ossEndpoint.contains("beijing") -> "cn-beijing"
        ossEndpoint.contains("shenzhen") -> "cn-shenzhen"
        else -> "cn-shanghai"
    }
)
val fileTransDomain = localOrProject("FILETRANS_DOMAIN", "filetrans.$fileTransRegionId.aliyuncs.com")
val aiEndpoint = localOrProject("AI_ENDPOINT", "https://apis.iflow.cn/v1/chat/completions")
val aiApiKey = localOrProject("AI_API_KEY")
val aiModel = localOrProject("AI_MODEL", "qwen3-max")
val siliconFlowApiKey = localOrProject("SILICONFLOW_API_KEY")

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

fun parseVersionName(versionName: String): Triple<Int, Int, Int> {
    val parts = versionName.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 1
    return Triple(major, minor, patch)
}

val appVersionName = versionProps.getProperty("VERSION_NAME")?.trim()
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.1"
val appVersionCode = versionProps.getProperty("VERSION_CODE")?.trim()
    ?.toIntOrNull()
    ?: 1

android {
    namespace = "com.qinmomeak.recording"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.qinmomeak.recording"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ALIYUN_APP_KEY", "\"$aliyunAppKey\"")
        buildConfigField("String", "ALIYUN_ACCESS_KEY_ID", "\"$aliyunAccessKeyId\"")
        buildConfigField("String", "ALIYUN_ACCESS_KEY_SECRET", "\"$aliyunAccessKeySecret\"")
        buildConfigField("String", "ALIYUN_TOKEN", "\"$aliyunToken\"")
        buildConfigField("String", "OSS_ENDPOINT", "\"$ossEndpoint\"")
        buildConfigField("String", "OSS_BUCKET", "\"$ossBucket\"")
        buildConfigField("String", "OSS_OBJECT_PREFIX", "\"$ossObjectPrefix\"")
        buildConfigField("String", "OSS_PUBLIC_READ", "\"$ossPublicRead\"")
        buildConfigField("String", "OSS_SIGN_EXPIRE_SECONDS", "\"$ossSignExpireSeconds\"")
        buildConfigField("String", "FILETRANS_REGION_ID", "\"$fileTransRegionId\"")
        buildConfigField("String", "FILETRANS_DOMAIN", "\"$fileTransDomain\"")
        buildConfigField("String", "AI_ENDPOINT", "\"$aiEndpoint\"")
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")
        buildConfigField("String", "AI_MODEL", "\"$aiModel\"")
        buildConfigField("String", "SILICONFLOW_API_KEY", "\"$siliconFlowApiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(
                listOf(
                    "src/main/java/com/qinmomeak/recording",
                    "src/main/java/com/qinmomeak/recording/data"
                )
            )
        }
    }
}

tasks.register("bumpPatchVersion") {
    group = "versioning"
    description = "Increment patch version (0.0.1) for next build after assembleDebug."
    doLast {
        val currentVersionName = versionProps.getProperty("VERSION_NAME")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "1.0.1"
        val currentVersionCode = versionProps.getProperty("VERSION_CODE")?.trim()
            ?.toIntOrNull()
            ?: 1
        val (major, minor, patch) = parseVersionName(currentVersionName)
        val nextVersionName = "$major.$minor.${patch + 1}"
        val nextVersionCode = currentVersionCode + 1

        versionProps.setProperty("VERSION_NAME", nextVersionName)
        versionProps.setProperty("VERSION_CODE", nextVersionCode.toString())
        versionPropsFile.outputStream().use { out ->
            versionProps.store(out, "Auto-generated app version. Used by app/build.gradle.kts.")
        }
        println("Version bumped for next build -> versionName=$nextVersionName, versionCode=$nextVersionCode")
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("bumpPatchVersion")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(files("libs/nuisdk-release.aar"))
}
