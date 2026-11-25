import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.compose)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { targetCompatibility = JavaVersion.VERSION_17; sourceCompatibility = JavaVersion.VERSION_17 }

// The calculation engine and the platform-free screens are compiled straight from the Android app's sources,
// so both apps always run the same numbers. Files that need Android (camera, AI, storage, video) are left out.
val syncShared by tasks.registering(Sync::class) {
  from("../app/src/main/java") {
    include("com/aistudio/aerofast/trmst/data/*.kt")
    exclude(
      "**/data/AiProviders.kt", "**/data/AppStateStore.kt", "**/data/CustomMaterialStore.kt",
      "**/data/GeminiAiService.kt", "**/data/MediaAi.kt", "**/data/OfflineKnowledge.kt", "**/data/AndroidPrefs.kt", "**/data/QuestionParser.kt"
    )
    include(
      "com/aistudio/aerofast/trmst/ui/theme/Color.kt",
      "com/aistudio/aerofast/trmst/ui/theme/ThemeColors.kt",
      "com/aistudio/aerofast/trmst/ui/platform/PlatformActions.kt",
      "com/aistudio/aerofast/trmst/ui/screens/QualityScreen.kt"
    )
  }
  into(layout.buildDirectory.dir("generated/shared"))
}

sourceSets { main { kotlin.srcDir(syncShared) } }

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation(compose.materialIconsExtended)
  implementation(libs.kotlinx.coroutines.swing)
  testImplementation(libs.junit)
}

compose.desktop {
  application {
    mainClass = "com.aistudio.aerofast.desktop.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Msi, TargetFormat.Exe)
      packageName = "AeroFast Pro"
      packageVersion = "1.0.0"
      description = "Fastener engineering: sizing, heading, tooling, heat treatment and quality"
      vendor = "Parandama Pathipati"
      copyright = "© 2026 Parandama Pathipati"
      modules("java.prefs", "java.datatransfer")
      windows {
        iconFile.set(project.file("icons/aerofast.ico"))
        menuGroup = "AeroFast Pro"
        shortcut = true
        menu = true
        dirChooser = true
        perUserInstall = true
        upgradeUuid = "6f4d3c52-8f0e-4a7e-9b55-2f6a1d9e7c31"
      }
      linux { iconFile.set(project.file("icons/aerofast.png")) }
    }
  }
}
