// Standalone build (not part of the Android project): compiles the app's own calculation engine
// sources unchanged, so the server and the app can never disagree.
plugins {
    kotlin("jvm") version "2.2.10"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    testImplementation(kotlin("test"))
}

val engineFiles = listOf(
    "CalculationEngine", "DimensionTables", "GCodeSafetyAudit", "HeadingProgression", "IsoThreadClass", "PokaYokeEngine",
    "ResearchedMaterials", "SelectionFilter", "SoftAnnealEngine", "SpcStats", "TorqueCalculator", "FastenerData"
)

sourceSets.main {
    kotlin.srcDir("../app/src/main/java")
    kotlin.include(engineFiles.map { "com/example/data/$it.kt" } + listOf("com/example/server/**", "androidx/**"))
}

// Bytecode for Java 17 (the Docker runtime); builds on any JDK 17+
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

application { mainClass.set("com.example.server.MainKt") }
