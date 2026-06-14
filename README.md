[![](https://jitpack.io/v/Minzoid/memory.svg)](https://jitpack.io/#Minzoid/memory)
![Platform](https://img.shields.io/badge/platform-windows-blue.svg)
![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

# Memory Java

A high-performance, production-grade Java library for interacting with Windows process memory. Inspired by the popular `Memory.cs` library, it leverages **Java 21**, **JNA (Java Native Access)**, and **SOLID design principles** to provide a clean, developer-friendly API with zero native compilation dependencies.

---

## Table of Contents
1. [Key Features](#key-features)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Quick Start & Fluent API](#quick-start--fluent-api)
5. [Core Memory Operations](#core-memory-operations)
   - [Reading Memory](#reading-memory)
   - [Writing Memory](#writing-memory)
6. [Advanced Concepts](#advanced-concepts)
   - [Resolving Pointer Chains](#resolving-pointer-chains)
   - [Memory Protection Modification](#memory-protection-modification)
7. [AoB Scanning & Code Patching (AoB Replacement)](#aob-scanning--code-patching-aob-replacement)
   - [What is AoB Scanning?](#what-is-aob-scanning)
   - [How to Scan for a Pattern](#how-to-scan-for-a-pattern)
   - [How to Replace/Patch AoB Code (Step-by-Step)](#how-to-replacepatch-aob-code-step-by-step)
8. [Thread Safety & Internal Architecture](#thread-safety--internal-architecture)
9. [Zero-Config JVM Native Access Opt-In](#zero-config-jvm-native-access-opt-in)
10. [Building & Testing](#building--testing)
11. [License](#license)

---

## Key Features

- **Process Management**: Open and attach to processes via Numeric PID or Executable Name (case-insensitive).
- **Module System**: Dynamic module enumeration, caching, and base address lookup.
- **Pointer Resolution**: Multi-level pointer chain parsing (e.g., `"Game.dll+ABCD,10,20"`).
- **Parallel AoB Scanner**: High-performance signature scanning leveraging the JVM `ForkJoinPool` for parallel scanning of committed memory regions with full wildcard support (`??` or `?`).
- **Memory Protection**: Abstraction over `VirtualProtectEx` to safely modify and restore memory page permissions.
- **Fluent API**: Builder-style method chains for single-line operations.
- **Zero-Config Warning Suppression**: Automatic JVM warning suppression for JNA native memory access on Java 17–21.

---

## Requirements

- **Operating System**: Windows 10 or Windows 11 (supports both x64 and x86/WOW64 target processes).
- **Java**: JDK 21+ (Runs on Java 17+ if built accordingly; uses Java 21 features natively).

---

## Installation

Add the following JitPack repository and dependency configuration to your build system:

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Minzoid:memory:1.0.0")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Minzoid:memory:1.0.0'
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Minzoid</groupId>
    <artifactId>memory</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Quick Start & Fluent API

The entry point of the library is the `com.minzoid.memory.Memory` class. It implements `AutoCloseable`, making it easy to manage process handles using a `try-with-resources` block.

```java
import com.minzoid.memory.Memory;

public class Main {
    public static void main(String[] args) {
        // Open by process name (automatically closes resources on exit)
        try (Memory memory = new Memory()) {
            memory.openProcess("notepad.exe");
            
            // Check process architecture
            if (memory.is64Bit()) {
                System.out.println("Running inside a 64-bit target process!");
            }
        }
    }
}
```

### Fluent API Usage
For single-line read/write tasks, use the fluent API to attach, perform operations, and automatically clean up resources:

```java
// Read an integer in a single statement
int hp = Memory.attach("Game.exe").readInt(0x7FF000001234L);

// Write an integer in a single statement 
Memory.attach("Game.exe").writeInt(0x7FF000001234L, 9999);
```

---

## Core Memory Operations

### Reading Memory

`Memory` offers highly optimized methods to read all standard primitives, UTF-8/UTF-16/ASCII strings, and raw byte arrays:

```java
byte   b      = memory.readByte(address);
short  s      = memory.readShort(address);
int    i      = memory.readInt(address);
long   l      = memory.readLong(address);
float  f      = memory.readFloat(address);
double d      = memory.readDouble(address);

// Read a UTF-8 string (stripping null terminators)
String name   = memory.readString(address, 32); 

// Read a string with custom charset (e.g. UTF-16LE)
String wide   = memory.readString(address, 64, StandardCharsets.UTF_16LE);

// Read a raw chunk of memory
byte[] buffer = memory.readBytes(address, 256);
```

### Writing Memory

Write operations automatically write values in little-endian format matching Win32 standards:

```java
memory.writeByte(address, (byte) 1);
memory.writeShort(address, (short) 123);
memory.writeInt(address, 1337);
memory.writeLong(address, 9876543210L);
memory.writeFloat(address, 3.14f);
memory.writeDouble(address, 2.71828);

// Write a UTF-8 string
memory.writeString(address, "Memory");

// Write a raw byte array
memory.writeBytes(address, new byte[] { 0x90, 0x90, 0x90 });
```

---

## Advanced Concepts

### Resolving Pointer Chains

In most modern Windows applications, memory addresses shift dynamically across application restarts due to ASLR (Address Space Layout Randomization). To counter this, memory hacks trace static "Pointer Chains" consisting of a module base address, a base offset, and multiple dereferenced offsets.

`AddressResolver` parses and resolves pointer chains recursively.

#### Syntax Formats
- **Raw Base address**: `"7FF000001234"` or `"0x7FF000001234"`
- **Module Offset**: `"Game.dll+0xABCD"` (looks up `Game.dll` base, adds `0xABCD`)
- **Pointer Chain**: `"Game.dll+ABCD,10,20,30"`
  1. Resolves `Game.dll` base and adds base offset `ABCD`.
  2. Dereferences the pointer value at that address (reads 8 bytes for x64 processes).
  3. Adds offset `10` to that value.
  4. Dereferences the pointer value at the new address.
  5. Adds offset `20`.
  6. Dereferences, adds offset `30`, and returns the final target address without dereferencing it.

```java
// Resolve a deep pointer chain to find the actual health address
long hpAddress = memory.resolve("Game.dll+0xABCD,0x10,0x20,0x30");

// Read/write the resolved address
int currentHp = memory.readInt(hpAddress);
memory.writeInt(hpAddress, 9999);
```

### Memory Protection Modification

Target memory pages are often write-protected or execute-only. Trying to write to these pages directly will throw a `MemoryWriteException`. You must temporarily adjust the memory page protection flags using `VirtualProtectEx` through `MemoryProtection`.

```java
import com.minzoid.memory.protection.MemoryProtection;

// Change protection of 4 bytes at target address to READ_WRITE
MemoryProtection oldProtect = memory.changeProtection(address, 4, MemoryProtection.READ_WRITE);

try {
    memory.writeInt(address, 9999);
} finally {
    // Crucial: Restore original protection (e.g. EXECUTE_READ) to avoid instability or anti-cheat detection
    memory.changeProtection(address, 4, oldProtect);
}
```

---

## AoB Scanning & Code Patching (AoB Replacement)

### What is AoB Scanning?

**AoB (Array of Bytes)** Scanning searches through the target process's virtual memory space for specific signature patterns of machine code bytes. This is extremely useful for:
1. Finding function entry points or data structures when static offsets change due to game patches.
2. Locating instructions to overwrite (patch/hook) with modified logic (e.g., bypass restrictions, bypass functions, modify instructions).

### How to Scan for a Pattern

The scanner supports space-separated hex byte patterns and `??` wildcards representing unknown/varying bytes:

```java
// Scan the entire readable process space in parallel
List<Long> matches = memory.scan("48 89 5C 24 ?? 57 48 83 EC ?? 49 8B");

if (!matches.isEmpty()) {
    long firstMatch = matches.get(0);
    System.out.printf("Signature found at address: 0x%X\n", firstMatch);
}
```

### How to Replace/Patch AoB Code (Step-by-Step)

To replace an AoB pattern (e.g. disabling an instruction by replacing it with `NOP` instructions (`0x90`)), follow this step-by-step code workflow:

```java
import com.minzoid.memory.Memory;
import com.minzoid.memory.protection.MemoryProtection;
import java.util.List;

public class CodePatcher {
    public static void main(String[] args) {
        try (Memory memory = Memory.attach("target_game.exe")) {
            // Step 1: Scan for the instructions we want to modify.
            // Example pattern of instructions that reduce player ammo: "FF 8E 94 01 00 00" (dec [esi+0x194])
            String ammoPattern = "FF 8E 94 01 00 00"; 
            List<Long> addresses = memory.scan(ammoPattern);
            
            if (addresses.isEmpty()) {
                System.err.println("Could not find the target code signature.");
                return;
            }
            
            long patchAddress = addresses.get(0);
            System.out.printf("Found target instruction at: 0x%X\n", patchAddress);
            
            // Step 2: Define your replacement bytes. 
            // We replace 6 bytes of 'dec' with 6 NOPs (0x90) to cancel out the ammunition reduction.
            byte[] patchBytes = { 0x90, 0x90, 0x90, 0x90, 0x90, 0x90 };
            
            // Step 3: Change protection flags to allow writing/executing (needed if it's in the .text section)
            MemoryProtection oldProtect = memory.changeProtection(
                patchAddress, 
                patchBytes.length, 
                MemoryProtection.EXECUTE_READ_WRITE
            );
            
            try {
                // Step 4: Write the replacement bytes to patch the code.
                memory.writeBytes(patchAddress, patchBytes);
                System.out.println("Code successfully patched (No-Recoil/Infinite Ammo applied).");
                
            } finally {
                // Step 5: Restore the original memory protection flags.
                memory.changeProtection(patchAddress, patchBytes.length, oldProtect);
                System.out.println("Original memory protection restored.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Thread Safety & Internal Architecture

Memory Java is designed for highly concurrent usage:
- **Shared Resources**: Subsystems share a thread-safe `ReentrantReadWriteLock` to isolate read operations (e.g., reading/scanning memory, getting modules) and write operations (e.g., opening/closing processes).
- **Module Caching**: Thread-safe caching via `ConcurrentHashMap` avoids excessive module enumeration calls to the Windows API.
- **Parallel Scanning**: The `AoBScanner` uses a split-and-conquer ForkJoin framework. Large memory pages are divided into batches, processed concurrently, and matches are aggregated into a thread-safe `CopyOnWriteArrayList`.

---

## Zero-Config JVM Native Access Opt-In

On newer Java versions (Java 17+ and especially Java 22+), the JVM warns about or restricts restricted native methods (JNA load operations) unless the `--enable-native-access` command-line flag is passed.

Memory Java bypasses this friction:
- For **Java 22+**, the project packages the `Enable-Native-Access: ALL-UNNAMED` attribute directly in the JAR manifest.
- For **Java 17–21**, a custom static initializer utilizes a temporary stderr filtering stream to load JNA classes. This interceptor catches and discards the startup warnings silently, providing a **frictionless dependency** for your downstream library users.

---

## Building & Testing

We use Gradle to build the project. The build outputs both standard JARs and a shadowed "fat" JAR containing dependencies.

### Clean and build:
```sh
./gradlew clean build
```

### Run tests:
```sh
./gradlew test
```

### View test coverage:
We use JaCoCo for code coverage. Run tests first, then view the report:
```sh
./gradlew jacocoTestReport
# The report is generated at build/reports/jacoco/test/html/index.html
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

