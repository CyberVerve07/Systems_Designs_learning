# Day 6: Rate Limiting & Throttling (Theory)

Rate limiting is one of the most fundamental concepts in System Design. It is used to control the rate of traffic sent by a client or user to a server. If a client exceeds the defined limit, further requests are blocked or throttled (usually returning an `HTTP 429 Too Many Requests` status code).

---

## 1. Why Do We Need Rate Limiting?

1.  **Prevent Abuse / DoS & DDoS Attacks**: Malicious actors can flood your server with requests to bring it down. A rate limiter drops these requests at the entry point.
2.  **Reduce Cost**: If your servers call expensive third-party APIs (e.g., OpenAI, Twilio, Stripe) or run heavy database queries, rate limiting prevents runaway costs from runaway loops or abuse.
3.  **Prevent Resource Starvation**: It ensures that a single high-volume user (or a buggy client loop) doesn't consume all the server resources, leaving other users with slow response times.
4.  **Manage Server Load**: It helps in traffic shaping, ensuring the servers operate under their safe capacity limits.

---

## 2. Where to Place the Rate Limiter?

```
Client-Side Option (Not secure, can be easily bypassed)
[ Client App ] --(Bypassed Limiter)--> [ API Gateway / Middleware ] ---> [ Backend Servers ]
                                              ^
                                     Best Place to Rate Limit
```

*   **Client-side**: Easy to implement but **unsafe**. Attackers can easily bypass it by making raw HTTP requests directly to the API.
*   **Server-side / Middleware**: Great for application-specific limits.
*   **API Gateway**: **The Industry Standard**. Modern API Gateways (like Kong, AWS API Gateway, Nginx) have built-in, highly optimized rate-limiting plugins that block bad traffic before it even hits your application servers.

---

## 3. Core Rate Limiting Algorithms

Understanding these five algorithms is highly critical for system design interviews and real-world architectures.

### A. Token Bucket Algorithm (Most Popular)
*Used by: Stripe, Amazon Web Services (AWS)*

*   **How it works**: 
    1. A bucket has a maximum capacity of $N$ tokens.
    2. Tokens are added to the bucket at a constant rate (e.g., $r$ tokens per second). Once the bucket is full, excess tokens overflow and are discarded.
    3. Every incoming request consumes exactly **one token** (or more depending on request size).
    4. If the bucket has enough tokens, the request is processed, and tokens are decremented.
    5. If the bucket is empty, the request is dropped (`HTTP 429`).

```
  Tokens drop in at rate 'r'
       ↓  ↓  ↓
    |o o o o o|  ← Max Capacity 'N' (Tokens represent capacity)
    |o o o o  |
    +---------+
         ↓
  Request arrives -> Consumes 1 Token -> Request Allowed!
  If no tokens -> Request Blocked (429)!
```

*   **Pros**: 
    * Highly memory efficient.
    * Allows **bursts of traffic** (if a bucket is full, it can process $N$ requests instantly).
*   **Cons**: 
    * Configuring parameters ($N$ and $r$) requires careful tuning.

---

### B. Leaky Bucket Algorithm
*Used for: Traffic shaping (smooth constant rate)*

*   **How it works**:
    1. Requests are added to a queue (the bucket) of a fixed size.
    2. The queue processes requests at a **constant, smooth rate** (leaks at a fixed speed).
    3. If a request arrives and the queue is full, the request is immediately dropped (spills over).

```
   Incoming Requests (Burst traffic)
       ↓  ↓  ↓
    | █ █ █ █ |  ← Fixed capacity queue
    | █ █ █   |
    +----.----+
         |  ← Leaks requests at a constant, smooth rate
         ↓
    [ Processed Requests ]
```

*   **Pros**:
    * Guarantees a stable, predictable load on the downstream system (removes traffic bursts).
*   **Cons**:
    * A sudden burst of legitimate requests can be delayed in the queue or dropped, resulting in high latency for normal users.

---

### C. Fixed Window Counter
*   **How it works**:
    1. Divide time into fixed-size windows (e.g., 1 minute).
    2. Maintain a counter for each user for the current window.
    3. If the counter is less than the limit, allow the request and increment the counter.
    4. If the window resets (e.g., next minute starts), reset the counter to 0.

*   **The Critical Flaw (Spike at Window Boundaries)**:
    * Suppose the limit is 100 requests/minute.
    * An attacker sends 100 requests between `10:00:59` and `10:01:00` (end of window 1).
    * The attacker sends another 100 requests between `10:01:00` and `10:01:01` (start of window 2).
    * **Result**: The attacker successfully bypassed the limit by sending **200 requests within a 2-second window** across the boundary!

```
 Window 1 (10:00 - 10:01)         Window 2 (10:01 - 10:02)
 |-----------------------██████|██████-----------------------|
                      100 Requests  100 Requests
                      (Last 1 sec)  (First 1 sec)
                      
          Result: 200 Requests in 2 seconds! (Limit Bypassed)
```

---

### D. Sliding Window Log
*   **How it works**:
    1. Keep a sorted log of request timestamps for each user (e.g., in a Redis sorted set).
    2. When a new request arrives, remove all timestamps older than the sliding window threshold (e.g., `currentTime - 1 minute`).
    3. Count the remaining elements in the log.
    4. If the log size is below the limit, allow the request and append the current timestamp to the log.
    5. If the log size exceeds the limit, drop the request.

*   **Pros**:
    * 100% accurate. Avoids the boundary spike issue entirely.
*   **Cons**:
    * **High Memory Consumption**: You must store a timestamp for *every single request* made by every user. If a user makes 50,000 requests, you store 50,000 timestamps in memory.

---

### E. Sliding Window Counter (Best of both worlds)
*   **How it works**:
    * Combines Fixed Window Counter and Sliding Window Log without high memory usage.
    * Imagine a request arrives at 30% into the current window.
    * We calculate the request count in the rolling window using the formula:
      $$\text{Estimated Requests} = (\text{Count in Previous Window} \times (1 - \text{Percentage overlap})) + \text{Count in Current Window}$$
    * If the estimated requests are within the limit, process it.

*   **Pros**:
    * Extremely low memory footprint (only requires 2 counters per user).
    * Solves the boundary spike issue smoothly with minor mathematical approximation (99%+ accuracy).

---

## 4. Distributed Rate Limiting: Core Challenges

When you scale your application horizontally to multiple servers, rate limiting becomes much harder.

### Challenge 1: Race Conditions
In a high-concurrency system, two threads might read a user's token count simultaneously, see that there is 1 token left, allow both requests, and decrement the count to -1.

*   **Solution**: Use **Redis with Lua Scripting** or **Redis Locks**. Lua scripts run atomically inside Redis, ensuring that reading, checking, and updating the counter/tokens happens as a single indivisible transaction.

### Challenge 2: Synchronization
If you use in-memory rate limiting, Server A doesn't know about requests handled by Server B. If a client targets Server B next, their limit is reset.

*   **Solution**: Use a centralized store like **Redis** to keep track of user limits, or use sticky sessions at the load balancer level (not recommended for REST APIs).

---

## 5. Summary: Which Algorithm to Choose?

| Algorithm | Memory Usage | Handles Bursts? | Accuracy / Edge Cases | Best Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Token Bucket** | Low | **Yes** | High | Standard APIs (Stripe, GitHub) |
| **Leaky Bucket** | Medium | No | High | Traffic Shaping / Background Jobs |
| **Fixed Window** | Low | Yes | **Poor** (Boundary Spike) | Simple, non-critical limits |
| **Sliding Log** | **High** | Yes | Perfect | High-security, low-volume APIs |
| **Sliding Counter**| Low | Yes | Good (Approximated) | High-scale, memory-sensitive systems |
