package com.systemdesign.idgenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demo class to validate and visualize Twitter Snowflake ID generation.
 * Tests thread safety, collision-free uniqueness, and chronological sorting.
 */
public class SnowflakeDemo {
    private static final Logger log = LoggerFactory.getLogger(SnowflakeDemo.class);

    public static void main(String[] args) {
        log.info("=== Starting Twitter Snowflake ID Generator Demo ===");

        // Initialize generator for Datacenter 1, Worker 5
        long datacenterId = 1;
        long workerId = 5;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(datacenterId, workerId);

        // -------------------------------------------------------------
        // Test 1: Single ID Bitwise Visualization
        // -------------------------------------------------------------
        log.info("\n--- TEST 1: Bitwise Visualization ---");
        long sampleId = generator.nextId();
        log.info("Generated Decimal ID: {}", sampleId);
        log.info("Binary Representation: {}", Long.toBinaryString(sampleId));
        visualizeBits(sampleId);

        // -------------------------------------------------------------
        // Test 2: High Concurrency & Uniqueness Test
        // -------------------------------------------------------------
        log.info("\n--- TEST 2: High Concurrency Uniqueness Test ---");
        int threadCount = 4;
        int idsPerThread = 10000;
        int totalExpectedIds = threadCount * idsPerThread;

        // Thread-safe Set to record all generated IDs
        Set<Long> uniqueIds = ConcurrentHashMap.newKeySet();
        List<Long> orderedList = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                log.info("Thread {} started generating {} IDs...", threadId, idsPerThread);
                for (int j = 0; j < idsPerThread; j++) {
                    long id = generator.nextId();
                    uniqueIds.add(id);
                    orderedList.add(id);
                }
                log.info("Thread {} completed.", threadId);
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.error("Executor service did not terminate in time!");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for threads to finish", e);
            Thread.currentThread().interrupt();
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        log.info("\n--- CONCURRENCY TEST RESULTS ---");
        log.info("Total IDs expected: {}", totalExpectedIds);
        log.info("Total Unique IDs captured: {}", uniqueIds.size());
        log.info("Execution Time: {} ms", durationMs);
        log.info("Average Speed: {} IDs/millisecond", (double) totalExpectedIds / durationMs);

        // Check for duplicates
        if (uniqueIds.size() == totalExpectedIds) {
            log.info("SUCCESS: Zero ID collisions detected! 100% Uniqueness Verified.");
        } else {
            log.error("FAILURE: Duplicate IDs generated! Collisions count: {}", totalExpectedIds - uniqueIds.size());
        }

        // -------------------------------------------------------------
        // Test 3: Chronological (Time-Sortable) Test
        // -------------------------------------------------------------
        log.info("\n--- TEST 3: Chronological Sorting Test ---");
        boolean isSorted = true;
        for (int i = 0; i < orderedList.size() - 1; i++) {
            if (orderedList.get(i) > orderedList.get(i + 1)) {
                isSorted = false;
                break;
            }
        }

        if (isSorted) {
            log.info("SUCCESS: All generated IDs are chronological (Time-Sortable).");
            log.info("First ID: {}", orderedList.get(0));
            log.info("Last ID : {}", orderedList.get(orderedList.size() - 1));
        } else {
            log.error("FAILURE: IDs are not chronological!");
        }

        log.info("\n=== Snowflake Demo Completed ===");
    }

    /**
     * Helper to show the bit representation alignment.
     */
    private static void visualizeBits(long id) {
        String binary = String.format("%64s", Long.toBinaryString(id)).replace(' ', '0');
        
        // Split by Snowflake segments
        // Bit 0: Sign bit (1 bit)
        // Bit 1-41: Timestamp (41 bits)
        // Bit 42-46: Datacenter (5 bits)
        // Bit 47-51: Worker (5 bits)
        // Bit 52-63: Sequence (12 bits)
        String sign = binary.substring(0, 1);
        String timestamp = binary.substring(1, 42);
        String datacenter = binary.substring(42, 47);
        String worker = binary.substring(47, 52);
        String sequence = binary.substring(52, 64);

        log.info("Bit-Partitioned View:");
        log.info("[Sign: {}] [Timestamp (41b): {}] [Datacenter (5b): {}] [Worker (5b): {}] [Sequence (12b): {}]",
                sign, timestamp, datacenter, worker, sequence);
        
        // Show decimal parses
        long parsedTimestamp = Long.parseLong(timestamp, 2);
        long parsedDatacenter = Long.parseLong(datacenter, 2);
        long parsedWorker = Long.parseLong(worker, 2);
        long parsedSequence = Long.parseLong(sequence, 2);

        log.info("Parsed Decimals from Bits:");
        log.info("-> Milliseconds since custom epoch: {} ms", parsedTimestamp);
        log.info("-> Datacenter ID: {}", parsedDatacenter);
        log.info("-> Worker ID    : {}", parsedWorker);
        log.info("-> Sequence ID  : {}", parsedSequence);
    }
}
