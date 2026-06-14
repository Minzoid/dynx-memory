# Memory Java - Technology Stack

## Programming Language

Java 21 LTS

Reason:

* Modern language features
* Long-term support
* High performance

---

## Build System

Gradle

Reason:

* Fast builds
* Flexible configuration
* Wide adoption

---

## Native Access

JNA (Java Native Access)

Reason:

* Direct access to Windows APIs
* No custom JNI code required
* Easier maintenance

Dependency:

com.sun.jna:jna

---

## Windows APIs

Kernel32.dll

Functions:

* OpenProcess
* CloseHandle
* ReadProcessMemory
* WriteProcessMemory
* VirtualProtectEx
* VirtualQueryEx
* IsWow64Process
* GetSystemInfo

---

## Concurrency

Java ExecutorService

Features:

* Thread pools
* Parallel scanning
* Future support

---

## Collections

Java Concurrent Package

Classes:

* ConcurrentHashMap
* ConcurrentLinkedQueue
* CopyOnWriteArrayList

---

## Testing

JUnit 5

Purpose:

* Unit testing
* Integration testing

---

## Logging

SLF4J

Implementation:

* Logback

---

## CI/CD

GitHub Actions

Pipelines:

* Build
* Test
* Publish

---

## Package Distribution

Maven Central

Alternative:

* GitHub Packages
* JitPack

---

## Documentation

Markdown

Tools:

* MkDocs
* GitHub Wiki

---

## Versioning

Semantic Versioning

Example:

1.0.0
1.1.0
2.0.0

---

## Minimum Requirements

Operating System:

* Windows 10
* Windows 11

Java:

* Java 17+

Architecture:

* x64
* x86

Memory:

* 512 MB minimum

Storage:

* 50 MB

