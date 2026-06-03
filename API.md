# DYNX Memory Java - Public API Specification

## Creating Memory Instance

```java
Memory memory = new Memory();
```

---

## Opening Processes

### Open By PID

```java
memory.openProcess(1234);
```

### Open By Name

```java
memory.openProcess("notepad.exe");
```

---

## Closing Process

```java
memory.closeProcess();
```

---

## Reading Memory

### Byte

```java
byte value = memory.readByte(address);
```

### Short

```java
short value = memory.readShort(address);
```

### Integer

```java
int value = memory.readInt(address);
```

### Long

```java
long value = memory.readLong(address);
```

### Float

```java
float value = memory.readFloat(address);
```

### Double

```java
double value = memory.readDouble(address);
```

### String

```java
String value = memory.readString(address, length);
```

---

## Writing Memory

### Byte

```java
memory.writeByte(address, value);
```

### Integer

```java
memory.writeInt(address, value);
```

### Float

```java
memory.writeFloat(address, value);
```

### Double

```java
memory.writeDouble(address, value);
```

### String

```java
memory.writeString(address, value);
```

---

## Module Operations

```java
Module module =
memory.getModule("Game.dll");
```

```java
long base =
module.getBaseAddress();
```

---

## Address Resolver

```java
long address =
memory.resolve(
"Game.dll+123456");
```

Pointer chain:

```java
long address =
memory.resolve(
"Game.dll+123456,10,20,30");
```

---

## AoB Scanner

```java
List<Long> results =
memory.scan(
"89 AB ?? FF");
```

---

## Memory Protection

```java
memory.changeProtection(
address,
MemoryProtection.READ_WRITE
);
```

---

## Process Information

```java
boolean is64 =
memory.is64Bit();
```

```java
int pid =
memory.getPid();
```

---

## Cleanup

```java
memory.close();
```

---

## Fluent API

```java
Memory.attach("game.exe")
.readInt(address);
```

---

## Future APIs

```java
memory.watch(address);

memory.snapshot();

memory.diff(snapshotA,
snapshotB);
```
