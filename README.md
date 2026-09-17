# Fullbright

A lightweight Fullbright and Fog Remover mod for Minecraft.

**By Afkz studios / Mayank**

## Supported Versions

- Minecraft 1.21.1
- Minecraft 1.21.4
- Minecraft 1.21.6
- Minecraft 1.21.11
- Minecraft 26.1.2
- Minecraft 26.3

## Building

Build all versions:
```powershell
.\gradlew.bat clean build
```

Build a specific version:
```powershell
.\gradlew.bat :versions:1.21.1:build
.\gradlew.bat :versions:26.1.2:build
.\gradlew.bat :versions:26.3:build
```

Run development client:
```powershell
.\gradlew.bat :versions:1.21.1:runClient
.\gradlew.bat :versions:26.1.2:runClient
.\gradlew.bat :versions:26.3:runClient
```

## Features

- Fullbright lighting with adjustable intensity (0-15)
- Fog removal toggle
- Custom keybinds (configurable in-game)
- Per-version Mixin support for maximum compatibility

## Project Structure

```
Fullbright/
├── common/              # Shared code (main + client)
├── versioned/
│   ├── official/        # 26.1.x (unobfuscated mappings, GLFW)
│   ├── official-26.3/   # 26.3 (unobfuscated, SDL - no GLFW)
│   ├── official-1.21.11/
│   ├── official-pre1.21.11/
│   └── official-1.21.x/ # Per-version mixin variants
└── versions/            # Version-specific build subprojects
```

## License

MIT License - Copyright (c) 2026 Mayank
