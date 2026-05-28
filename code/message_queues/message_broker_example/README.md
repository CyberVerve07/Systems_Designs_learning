# Day 7: Custom Thread-Safe Message Broker Implementation

This directory contains a complete, production-grade, thread-safe, and high-concurrency **In-Memory Message Broker** built from scratch in Java. It showcases how message queues, topics, consumer groups, message retry resiliency, dead-letter queues (DLQ), and acknowledgment timeouts work behind the scenes in enterprise brokers like Apache Kafka and RabbitMQ.

## 🚀 Key Features Built From Scratch

1. **Dual Routing Models**:
   - **Queue Mode (Point-to-Point)**: Messages are dispatched to exactly one worker. Load is balanced across active consumers using a Round-Robin algorithm.
   - **Topic Mode (Publish/Subscribe)**: Messages are broadcasted to all registered consumer groups. Within each consumer group, a single worker receives the message.
2. **Reliable Processing (ACK/NACK)**:
   - Consumers must acknowledge success (`ack(true)`) or failure (`ack(false)`) to determine whether the broker should safely delete or re-enqueue the message.
3. **Resilient Retry Mechanics**:
   - Failed messages are re-queued to their active destinations up to a configured threshold.
4. **Dead Letter Queue (DLQ)**:
   - Malformed or corrupt messages that repeatedly fail are automatically intercepted and moved to the DLQ to avoid thread blockages or poison-pill infinite loops.
5. **Background Timeout Reaper**:
   - If a consumer freezes or drops connection while holding an in-flight message, the broker's daemon scheduled reaper automatically detects the timeout, reclaims ownership, and schedules a retry to another consumer.
6. **Thread-Safe & Non-Blocking Design**:
   - Fully synchronized with concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue`) and worker pools (`ExecutorService`), guaranteeing consistency even under heavy multi-threaded production loads.

---

## 📁 Project Structure

```
message_broker_example/
├── pom.xml                                 # Maven configuration
├── README.md                               # This guide
└── src/
    └── main/
        └── java/
            └── com/
                └── systemdesign/
                    └── messagequeue/
                        ├── Message.java             # Message wrapper (payload, timestamp, retries)
                        ├── Consumer.java            # Represents subscription endpoints
                        ├── MessageHandler.java      # Functional interface for listener callbacks
                        ├── Acknowledger.java        # Interface for signaling ACK/NACK
                        ├── InFlightMessage.java     # Tracks delivered but un-acknowledged messages
                        ├── MessageBroker.java       # The core routing and scheduling engine
                        └── MessageBrokerDemo.java   # Executable multi-scenario simulator
```

---

## 🛠️ How to Compile and Run

Make sure you have Java 11+ and Maven installed. Navigate to this directory in your terminal:

```bash
# 1. Compile the project
mvn clean compile

# 2. Run the interactive simulation demo
mvn exec:java -Dexec.mainClass="com.systemdesign.messagequeue.MessageBrokerDemo"
```

---

## 📊 Expected Output Walkthrough

When you run the demo, you will see a detailed log explaining the transitions of each message:

1. **Scenario 1 (Point-to-Point Queue)**:
   - 4 orders are published to `order-processing-queue`.
   - `OrderWorker-1` and `OrderWorker-2` split the workload, processing exactly 2 orders each.
2. **Scenario 2 (Pub/Sub with Consumer Groups)**:
   - 2 user signup events are published.
   - Both `EmailNotifierGroup` and `AnalyticsTrackerGroup` get separate copies of the events.
   - Within `EmailNotifierGroup`, the two active workers (`EmailWorker-1` and `EmailWorker-2`) load-balance the events.
   - Within `AnalyticsTrackerGroup`, the single worker receives all events.
3. **Scenario 3 (NACK & DLQ)**:
   - A malformed transaction is published, throwing NACKs.
   - You will see logs: `Retry: Re-queuing Message... Attempt: 1/2`, `Attempt: 2/2`.
   - After the second retry fails, the broker logs: `[DLQ] Message EXCEEDED max retries. Sent to DEAD LETTER QUEUE (DLQ)!!!`
4. **Scenario 4 (Timeout Reaper)**:
   - A slow item is sent. `InventoryWorker-1` receives it and hangs.
   - After 1000ms, the background Reaper logs: `[TIMEOUT] Message timed out at Consumer InventoryWorker-1! (No ACK/NACK received)`.
   - The message is reclaimed, and successfully handled by `InventoryWorker-2`!
