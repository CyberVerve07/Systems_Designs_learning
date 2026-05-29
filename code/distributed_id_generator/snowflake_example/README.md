# Twitter Snowflake ID Generator Example

This is a complete, production-grade, thread-safe Java implementation of the **Twitter Snowflake Algorithm**. It generates time-sortable, 64-bit unique identifiers without relying on external network dependencies.

## 📁 Project Structure

* `pom.xml`: Maven configuration.
* `src/main/java/com/systemdesign/idgenerator/SnowflakeIdGenerator.java`: The core logic implementing dynamic bit shifts, synchronization, and clock drift protection.
* `src/main/java/com/systemdesign/idgenerator/SnowflakeDemo.java`: Interactive test suite running highly concurrent generations across 4 parallel threads, checking for duplicates, sorting order, and displaying bit-wise breakdowns of generated IDs.

## 🚀 How to Run the Demo

Open a terminal at this directory and execute the following:

### 1. Compile the Code
```bash
mvn clean compile
```

### 2. Run the Demo
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.idgenerator.SnowflakeDemo"
```

## 🔍 What the Demo Validates

1. **Bitwise Extraction:** Parses the 64-bit decimal ID back into its original binary segments, displaying:
   * Positive Sign Bit (0)
   * Offset Milliseconds since custom epoch
   * Assigned Datacenter ID
   * Assigned Worker Node ID
   * Millisecond-specific Sequence count
2. **Concurrency Verification:** Spawns **4 parallel worker threads** generating **10,000 IDs each** simultaneously (40,000 IDs total) and asserts that zero duplicate collisions occurred.
3. **Time-Ordering Order:** Validates that IDs are perfectly sequential/chronological ($ID_A < ID_B$) across the runtime window.
