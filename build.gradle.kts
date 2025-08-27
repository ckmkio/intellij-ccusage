plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.1.0"
  id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.ckmk"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  // JSON parsing for ccusage data
  implementation("com.google.code.gson:gson:2.10.1")
}

intellij {
  version.set("2025.2")
  type.set("IC")
  
  plugins.set(listOf(
    // Add plugin dependencies if needed
  ))
}

tasks {
  patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("252.*")
    
    changeNotes.set("""
      Initial version of Claude Code Usage plugin.
      - Display live ccusage data in status bar
      - Show token usage, reset times, and AI model info
      - Configurable refresh intervals
    """.trimIndent())
    
    // License information
    pluginDescription.set("""
      An IntelliJ IDEA plugin that displays live Claude Code usage statistics directly in your IDE's status bar.
      
      Licensed under the MIT License.
    """.trimIndent())
  }
  
  // Disable buildSearchableOptions to avoid conflicts when IntelliJ is running
  buildSearchableOptions {
    enabled = false
  }
  
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
  }
}
