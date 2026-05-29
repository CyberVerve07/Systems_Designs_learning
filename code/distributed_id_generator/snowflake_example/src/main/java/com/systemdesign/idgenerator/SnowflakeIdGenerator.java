package com.systemdesign.idgenerator;

/**
 * Thread-safe implementation of Twitter Snowflake Algorithm in Java.
 * Generates unique, time-sortable 64-bit IDs.
 */
public class SnowflakeIdGenerator {

    // Custom Epoch (May 1, 2026 00:00:00 UTC) in milliseconds
    private static final long CUSTOM_EPOCH = 1777593600000L;

    // Bit lengths for segments
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // Maximum values calculated using bit shifts
    private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS); // 31
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);         // 31
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);           // 4095

    // Bit shift offsets
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 17
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * Instantiates Snowflake Generator with specific datacenter and worker ID.
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter ID must be between 0 and %d", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        System.out.printf("[INFO] Initialized Snowflake Generator. Datacenter ID: %d, Worker ID: %d%n", datacenterId, workerId);
    }

    /**
     * Thread-safe ID generation.
     */
    public synchronized long nextId() {
        long currentTimestamp = getSystemTimeMillis();

        // 1. Clock Drift Detection (If current time is less than last recorded time)
        if (currentTimestamp < lastTimestamp) {
            long driftMillis = lastTimestamp - currentTimestamp;
            System.err.printf("[WARN] Clock drift detected! System time moved backwards by %d ms.%n", driftMillis);

            // If drift is small, wait for the clock to catch up
            if (driftMillis <= 10) {
                try {
                    Thread.sleep(driftMillis + 1);
                    currentTimestamp = getSystemTimeMillis();
                    if (currentTimestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock drift persisted. Unable to generate ID. Last: %d, Current: %d", lastTimestamp, currentTimestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for clock to catch up", e);
                }
            } else {
                // Hard drift: fail-fast to prevent ID collisions
                throw new RuntimeException(String.format("Clock moved backwards by too much (%d ms). ID generation rejected to avoid collision.", driftMillis));
            }
        }

        // 2. If generated in same millisecond, increment sequence counter
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            
            // Sequence overflow (exceeded 4095 IDs in this millisecond)
            if (sequence == 0) {
                // Block/Wait till next millisecond
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // Clock moved forward: reset sequence for the new millisecond
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // 3. Bit Shift & Assemble 64-bit ID
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_LEFT_SHIFT) // Shift timestamp left by 22 bits
                | (datacenterId << DATACENTER_ID_SHIFT)                    // Shift datacenter left by 17 bits
                | (workerId << WORKER_ID_SHIFT)                            // Shift worker left by 12 bits
                | sequence;                                                // Combine sequence (lower 12 bits)
    }

    /**
     * Helper method to wait till the next millisecond.
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = getSystemTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = getSystemTimeMillis();
        }
        return timestamp;
    }

    protected long getSystemTimeMillis() {
        return System.currentTimeMillis();
    }
}
