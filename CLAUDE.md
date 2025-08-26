# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin project named "Claude Code Usage" (intellij-ccusage) built with Kotlin and Gradle. The plugin displays live Claude Code usage statistics in the IDE status bar, showing session data, token usage, reset times, and AI model information using the ccusage library.

## Build System & Commands

### Core Commands
- `./gradlew build` - Build the plugin
- `./gradlew runIde` - Run IntelliJ IDEA with the plugin loaded for development/testing
- `./gradlew buildPlugin` - Build the plugin distribution ZIP
- `./gradlew publishPlugin` - Publish plugin to JetBrains Marketplace (requires authentication)

### Development Commands
- `./gradlew compileKotlin` - Compile Kotlin source files
- `./gradlew compileJava` - Compile Java source files (if any)
- `./gradlew processResources` - Process plugin resources

## Project Structure

- `src/main/kotlin/io/ckmk/intellijccusage/` - Main plugin source code
  - `model/` - Data models for ccusage information (CcUsageData)
  - `notification/` - Notification system for user alerts (CcUsageNotifications)
  - `service/` - Core services for usage tracking (CcUsageService)
  - `settings/` - Plugin configuration (CcUsageSettings)  
  - `ui/` - Status bar widgets and UI components (CcUsageStatusBarWidget, CcUsageStatusBarWidgetFactory)
- `src/main/resources/META-INF/plugin.xml` - Plugin configuration and metadata
- `src/main/resources/META-INF/pluginIcon.svg` - Plugin icon
- `build.gradle.kts` - Gradle build configuration with IntelliJ Platform plugin setup
- `gradle.properties` - Gradle build properties with optimizations enabled
- `README.md` - Project documentation and usage guide

## Technical Configuration

- **Language**: Kotlin with Java 17 compatibility
- **Target IDE**: IntelliJ IDEA Community Edition 2024.1+ 
- **Plugin Version**: 1.0-SNAPSHOT
- **Minimum Build**: 241
- **Maximum Build**: 251.*
- **Plugin ID**: `io.ckmk.intellij-ccusage`
- **Plugin Name**: "Claude Code Usage"
- **Gradle Version**: 8.13
- **Kotlin Version**: 2.1.0
- **IntelliJ Platform Plugin**: 1.17.4
- **Dependencies**: Gson 2.10.1 for JSON parsing

## Plugin Architecture

The plugin implements a status bar widget system with:
- **Status Bar Widget Factory**: Creates and manages the status bar display
- **Application Services**: CcUsageService for data fetching, CcUsageSettings for configuration
- **Data Models**: CcUsageData for structured usage information
- **Notification System**: CcUsageNotifications for balloon notifications and user alerts
- **Extension Points**: Properly configured in plugin.xml with service implementations

Key extension points:
- `statusBarWidgetFactory` - Status bar integration positioned before PositionPanel
- `applicationService` - Core usage tracking and settings services (CcUsageService, CcUsageSettings)
- `notificationGroup` - User notifications for usage alerts and updates

The plugin is designed to integrate with the Claude Code CLI's ccusage functionality to provide real-time usage monitoring directly within the IDE.

## Key Components

### Core Services
- **CcUsageService**: Main service that fetches usage data from the ccusage CLI and provides it to UI components
- **CcUsageSettings**: Persistent settings service for plugin configuration and user preferences

### UI Components  
- **CcUsageStatusBarWidgetFactory**: Factory that creates and manages status bar widgets
- **CcUsageStatusBarWidget**: The actual status bar widget that displays usage information

### Data & Notifications
- **CcUsageData**: Data model representing ccusage information structure
- **CcUsageNotifications**: Handles user notifications for usage alerts and system messages