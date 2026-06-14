# Memory Java - Design Document

## Architecture

The project follows a layered architecture.

+-------------------------+
| Public API |
+-------------------------+
| Core Services |
+-------------------------+
| Native Layer |
+-------------------------+
| Windows APIs |
+-------------------------+

---

## Package Structure

com.minzoid.memory

├── Memory
├── process
├── modules
├── scanner
├── pointer
├── protection
├── nativeapi
├── structures
├── exceptions
└── utils

---

## Main Components

### Memory

Primary entry point.

Responsibilities:

* Attach process
* Read memory
* Write memory
* Manage resources

---

### ProcessManager

Responsibilities:

* Open process
* Close process
* Validate process state
* Detect architecture

---

### ModuleManager

Responsibilities:

* Enumerate modules
* Cache module data
* Resolve module addresses

---

### AddressResolver

Responsibilities:

* Parse address expressions
* Resolve pointer chains
* Handle module offsets

---

### MemoryReader

Responsibilities:

* Typed memory reads
* Buffer management

---

### MemoryWriter

Responsibilities:

* Typed memory writes
* Validation

---

### AoBScanner

Responsibilities:

* Region enumeration
* Pattern matching
* Parallel scanning

---

### PatternMatcher

Responsibilities:

* Wildcard support
* Byte comparison
* Match detection

---

### ProtectionManager

Responsibilities:

* Memory protection changes
* Permission verification

---

## Data Flow

Application

↓

Memory API

↓

Core Service

↓

Native Layer

↓

Kernel32

↓

Target Process

---

## Error Handling

Custom Exceptions:

* ProcessNotFoundException
* AccessDeniedException
* InvalidAddressException
* MemoryReadException
* MemoryWriteException
* ScanException

---

## Thread Safety

Shared resources:

* Process handles
* Module cache

Protection:

* ReadWriteLock
* ConcurrentHashMap

---

## Logging

SLF4J

Levels:

* INFO
* WARN
* ERROR
* DEBUG

