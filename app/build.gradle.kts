plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.taraz.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.taraz.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

// Vazirmatn is bundled into the APK at build time, so the app does not depend on
// an online font service at runtime. The upstream project is licensed under OFL.
val downloadVazirmatnFonts = tasks.register("downloadVazirmatnFonts") {
    outputs.files(
        file("src/main/res/font/vazirmatn_regular.ttf"),
        file("src/main/res/font/vazirmatn_medium.ttf"),
        file("src/main/res/font/vazirmatn_bold.ttf")
    )
    doLast {
        val fontDir = file("src/main/res/font")
        fontDir.mkdirs()
        val fonts = mapOf(
            "vazirmatn_regular.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Regular.ttf",
            "vazirmatn_medium.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Medium.ttf",
            "vazirmatn_bold.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Bold.ttf"
        )
        fonts.forEach { (name, url) ->
            val target = file("src/main/res/font/$name")
            java.net.URI(url).toURL().openStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
    }
}

tasks.named("preBuild") { dependsOn(downloadVazirmatnFonts) }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    androidTestImplementation(bom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
