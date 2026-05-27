# Day 6: Rate Limiting & Throttling Example

This project implements and demonstrates two major rate-limiting algorithms, along with a distributed Redis-backed rate-limiting framework in **Java**.

---

## 🚀 Features Implemented

1.  **Token Bucket Algorithm (`TokenBucketRateLimiter`)**:
    *   Uses a **Lazy Refill** mechanism (calculates token refills dynamically on-the-fly when a request arrives, avoiding heavy CPU thread pools).
    *   Fully thread-safe and concurrent.
    *   Allows traffic bursts up to the defined bucket capacity.
2.  **Sliding Window Counter (`SlidingWindowCounterRateLimiter`)**:
    *   Splits time into fixed windows and keeps track of adjacent window counters.
    *   Uses mathematical estimation to achieve $99\%+$ accuracy of a sliding window without high memory footprint.
    *   Avoids the boundary spike issue seen in traditional Fixed Window counters.
3.  **Distributed Rate Limiter (`RedisRateLimiter`)**:
    *   Uses **Jedis** (Redis Client) to perform atomic operations using Redis' `INCR` and `EXPIRE`.
    *   Implements clean fallback logic: if Redis is offline/unreachable on `localhost:6379`, it automatically falls back to an in-memory simulation so the demo doesn't crash!

---

## 🛠️ How to Run the Demo

### Step 1: Clone & Compile
Make sure you are in this directory:
```bash
cd code/rate_limiter/rate_limiter_example
```

Compile the project using Maven:
```bash
mvn clean compile
```

### Step 2: (Optional) Run Redis using Docker
If you want to test the *real* distributed Redis-backed rate limiter, spin up a local Redis container:
```bash
docker run -d -p 6379:6379 redis:latest
```
*(If you do not run Redis, the demo will still run perfectly by falling back to our in-memory sliding window limiter!)*

### Step 3: Run the Java Demo
Run the compiled `RateLimiterDemo` class:
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.ratelimiter.RateLimiterDemo"
```

---

## 📊 Understanding the Output

When you run the demo, you will see three simulations:

### 1. Token Bucket Burst Testing
*   Initial capacity: **5 tokens**, Refill rate: **2 tokens/sec**.
*   We send **10 requests** in rapid succession (100ms interval).
*   **Result**: The first **5 requests** are allowed instantly (`200 OK`). Once tokens are empty, the remaining requests are blocked (`429 Too Many Requests`).
*   After a **2-second delay**, we send another burst. You will observe that the bucket has accumulated a few more tokens, which allow subsequent requests!

### 2. Sliding Window Counter Testing
*   Limit: **5 requests per 2-second window**.
*   We send **10 requests** at 150ms intervals.
*   **Result**: You will see that requests are smoothly allowed or throttled based on the estimated overlap rate in the sliding window.

### 3. Redis Rate Limiter Testing
*   Attempts to connect to Redis on `localhost:6379`.
*   If connected: Demonstrates distributed atomic count tracking.
*   If offline: Cleanly notifies you and falls back to in-memory sliding window simulation to ensure a smooth, crash-free execution.
