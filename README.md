# Claude Code Usage Plugin

An IntelliJ IDEA plugin that displays live Claude Code usage statistics directly in your IDE's status bar.

## Features

- **Real-time Usage Tracking**: Display current session token usage and limits
- **Status Bar Integration**: Seamlessly integrated into IntelliJ's status bar
- **Model Information**: Shows current AI model and configuration details
- **Reset Timers**: Track when usage limits will reset
- **Configurable Updates**: Adjustable refresh intervals for usage data

## Installation

### From JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to `File` → `Settings` → `Plugins`
3. Search for "Claude Code Usage"
4. Click `Install` and restart the IDE

### Manual Installation
1. Download the latest release from [Releases](../../releases)
2. Open IntelliJ IDEA
3. Go to `File` → `Settings` → `Plugins`
4. Click the gear icon and select `Install Plugin from Disk...`
5. Select the downloaded `.zip` file
6. Restart the IDE

## Requirements

- IntelliJ IDEA 2024.1+ (Community or Ultimate Edition)
- Claude Code CLI installed and configured
- Java 17+

## Usage

Once installed, the plugin automatically displays Claude Code usage information in the status bar. The widget shows:

- Current token usage vs. limits
- Active AI model
- Time until usage reset
- Session statistics

Click on the status bar widget for detailed information and configuration options.

## Configuration

Access plugin settings through:
- `File` → `Settings` → `Tools` → `Claude Code Usage`

Available options:
- Refresh interval for usage data
- Display format preferences
- Notification settings

## Development

### Prerequisites

- JDK 17+
- Gradle 8.13+
- IntelliJ IDEA 2024.1+ (for development)

### Building

```bash
# Build the plugin
./gradlew build

# Run IntelliJ with the plugin loaded
./gradlew runIde

# Build distribution ZIP
./gradlew buildPlugin
```

### Project Structure

```
src/main/
├── kotlin/io/ckmk/intellijccusage/
│   ├── model/           # Data models for ccusage information
│   ├── notification/    # Notification system for user alerts
│   ├── service/         # Core services for usage tracking
│   ├── settings/        # Plugin configuration
│   └── ui/             # Status bar widgets and UI components
└── resources/
    └── META-INF/
        ├── plugin.xml   # Plugin configuration
        └── pluginIcon.svg
```

### Key Components

- **CcUsageService**: Core service for fetching usage data from ccusage CLI
- **CcUsageStatusBarWidgetFactory**: Creates and manages status bar widget
- **CcUsageSettings**: Handles plugin configuration and preferences
- **CcUsageData**: Data model for ccusage information
- **CcUsageNotifications**: Notification system for usage alerts

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Technical Details

- **Plugin ID**: `io.ckmk.intellij-ccusage`
- **Target Platform**: IntelliJ Platform 2024.1+
- **Language**: Kotlin
- **Build Tool**: Gradle with IntelliJ Platform Gradle Plugin
- **Dependencies**: Gson for JSON parsing

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2025 Kelvin Cheong

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

## Support

- Report issues: [GitHub Issues](../../issues)
- Documentation: [Wiki](../../wiki)
- Claude Code CLI: [Official Documentation](https://docs.anthropic.com/en/docs/claude-code)

## Changelog

### 1.0-SNAPSHOT
- Initial release
- Status bar widget with basic usage display
- Configuration options for refresh intervals
- Integration with ccusage library