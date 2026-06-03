# Java Distributed Notification System Simulation

This project simulates a distributed notification system featuring a multithreaded worker pool and an in-memory rate limiter using the Token Bucket algorithm.

## Prerequisites
- **Java JDK 17+** (tested on Java 26)

## Project Structure
- `model/NotificationEvent.java`: Payload structure containing event details and channel type (EMAIL, SMS, PUSH).
- `limiter/RateLimiter.java`: Thread-safe Token Bucket rate limiter per user.
- `worker/NotificationWorker.java`: Runnables pulling events from the queue, verifying rate limits, and dispatching to senders.
- `sender/`: Senders representing SMTP (Email), SMS gateways, and Firebase (Push Notifications) with simulated network latencies.
- `NotificationApp.java`: Main application running the simulation.

## How to Compile & Run

### Using Raw Java Commands
Navigate to this folder in your terminal and execute:

```powershell
# Compile the classes
javac -d bin src/main/java/com/systemdesign/notification/model/*.java src/main/java/com/systemdesign/notification/limiter/*.java src/main/java/com/systemdesign/notification/sender/*.java src/main/java/com/systemdesign/notification/worker/*.java src/main/java/com/systemdesign/notification/*.java

# Run the simulation
java -cp bin com.systemdesign.notification.NotificationApp
```

### Expected Output
You should see:
1. Three workers start up concurrently.
2. The producer publishes 5 events for Alice and 2 events for Bob in quick succession.
3. The Rate Limiter blocks Alice's 4th and 5th messages (limit is 3 messages per 5 seconds).
4. Bob's 2 messages go through successfully.
5. The system sleeps for 6 seconds, resetting the token buckets.
6. Alice sends another message, which succeeds because the bucket has refilled.
