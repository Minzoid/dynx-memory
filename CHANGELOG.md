# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Initial Release

### Added
- **Core Memory API:** Open by PID/Name, close process, architecture detection.
- **Read/Write Operations:** Support for Byte, Short, Int, Long, Float, Double, String, and raw byte arrays.
- **Module Management:** Enumerate modules, cache base addresses, and perform case-insensitive lookups.
- **Pointer Resolution:** Dynamic multi-level pointer resolution (e.g., `Game.dll+ABCD,10,20`).
- **AoB Scanner:** Parallel Array-of-Bytes pattern matching with wildcard (`??`) support.
- **Memory Protection:** Abstraction over `VirtualProtectEx` using `MemoryProtection` enum.
- **Thread Safety:** Full concurrency support via `ReentrantReadWriteLock` and `ConcurrentHashMap`.
- **Testing:** Comprehensive unit test suite using JUnit 5.
- **CI/CD:** GitHub Actions workflows for building, testing, and Maven package deployment.

