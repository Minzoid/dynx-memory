# DYNX Memory Java

A production-grade Java library for interacting with Windows process memory. Inspired by `Memory.cs`.

## Features
- **Clean Architecture:** Built on Java 21, JNA, and SOLID principles.
- **Process Management:** Open/close processes by PID or Name. Architecture detection (x64 / x86).
- **Module System:** Module enumeration, caching, and base address retrieval.
- **Pointer Resolution:** Multi-level pointer chain parsing (`"Game.dll+1234,10,20"`).
- **AoB Scanner:** High-performance parallel pattern matching with wildcard (`??`) support.
- **Memory Protection:** Safe modification of memory protection flags via `VirtualProtectEx`.

## Requirements
- Windows 10/11
- Java 21+

## Quick Start
```java
// Open by process name
Memory memory = new Memory();
memory.openProcess("notepad.exe");

// Read a value
int value = memory.readInt(0x7FF000001234L);

// Write a value
memory.writeInt(0x7FF000001234L, 42);

// Resolve a pointer chain
long addr = memory.resolve("Game.dll+ABCD,10,20");

// AoB scan
List<Long> hits = memory.scan("89 AB ?? FF");

// Always clean up
memory.close();
```

## Fluent API
```java
int hp = Memory.attach("Game.exe").readInt(address);
```

## Building
Use Gradle to build the library:
```sh
./gradlew clean build
```

## License
MIT License
