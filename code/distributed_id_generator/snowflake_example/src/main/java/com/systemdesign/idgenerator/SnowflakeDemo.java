package com.systemdesign.idgenerator;

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

    public static void main(String[] args) {
        System.out.println("=== Starting Twitter Snowflake ID Generator Demo ===");

        // Initialize generator for Datacenter 1, Worker 5
        long datacenterId = 1;
        long workerId = 5;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(datacenterId, workerId);

        // -------------------------------------------------------------
        // Test 1: Single ID Bitwise Visualization
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 1: Bitwise Visualization ---");
        long sampleId = generator.nextId();
        System.out.printf("Generated Decimal ID: %d%n", sampleId);
        System.out.printf("Binary Representation: %s%n", Long.toBinaryString(sampleId));
        visualizeBits(sampleId);

        // -------------------------------------------------------------
        // Test 2: High Concurrency & Uniqueness Test
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 2: High Concurrency Uniqueness Test ---");
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
                System.out.printf("[INFO] Thread %d started generating %d IDs...%n", threadId, idsPerThread);
                for (int j = 0; j < idsPerThread; j++) {
                    long id = generator.nextId();
                    uniqueIds.add(id);
                    orderedList.add(id);
                }
                System.out.printf("[INFO] Thread %d completed.%n", threadId);
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("[ERROR] Executor service did not terminate in time!");
            }
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Interrupted while waiting for threads to finish");
            Thread.currentThread().interrupt();
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("\n--- CONCURRENCY TEST RESULTS ---");
        System.out.printf("Total IDs expected: %d%n", totalExpectedIds);
        System.out.printf("Total Unique IDs captured: %d%n", uniqueIds.size());
        System.out.printf("Execution Time: %d ms%n", durationMs);
        System.out.printf("Average Speed: %.2f IDs/millisecond%n", (double) totalExpectedIds / durationMs);

        // Check for duplicates
        if (uniqueIds.size() == totalExpectedIds) {
            System.out.println("SUCCESS: Zero ID collisions detected! 100% Uniqueness Verified.");
        } else {
            System.err.printf("FAILURE: Duplicate IDs generated! Collisions count: %d%n", totalExpectedIds - uniqueIds.size());
        }

        // -------------------------------------------------------------
        // Test 3: Chronological (Time-Sortable) Test
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 3: Chronological Sorting Test ---");
        List<Long> singleThreadList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            singleThreadList.add(generator.nextId());
        }

        boolean isSorted = true;
        for (int i = 0; i < singleThreadList.size() - 1; i++) {
            if (singleThreadList.get(i) >= singleThreadList.get(i + 1)) {
                isSorted = false;
                break;
            }
        }

        if (isSorted) {
            System.out.println("SUCCESS: All sequential generated IDs are strictly chronological (Time-Sortable).");
            System.out.printf("First ID: %d%n", singleThreadList.get(0));
            System.out.printf("Last ID : %d%n", singleThreadList.get(singleThreadList.size() - 1));
        } else {
            System.err.println("FAILURE: IDs are not strictly chronological!");
        }

        System.out.println("\n=== Snowflake Demo Completed ===");
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

        System.out.println("Bit-Partitioned View:");
        System.out.printf("[Sign: %s] [Timestamp (41b): %s] [Datacenter (5b): %s] [Worker (5b): %s] [Sequence (12b): %s]%n",
                sign, timestamp, datacenter, worker, sequence);
        
        // Show decimal parses
        long parsedTimestamp = Long.parseLong(timestamp, 2);
        long parsedDatacenter = Long.parseLong(datacenter, 2);
        long parsedWorker = Long.parseLong(worker, 2);
        long parsedSequence = Long.parseLong(sequence, 2);

        System.out.println("Parsed Decimals from Bits:");
        System.out.printf("-> Milliseconds since custom epoch: %d ms%n", parsedTimestamp);
        System.out.printf("-> Datacenter ID: %d%n", parsedDatacenter);
        System.out.printf("-> Worker ID    : %d%n", parsedWorker);
        System.out.printf("-> Sequence ID  : %d%n", parsedSequence);
    }
}
