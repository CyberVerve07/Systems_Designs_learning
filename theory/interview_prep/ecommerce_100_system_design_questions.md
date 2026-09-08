# 🛍️ 100 Production System Design & Resilience Interview Questions (E-Commerce — 3+ YOE)

> **Level**: Mid-to-Senior Software Engineer / Tech Lead (3+ Years Experience)  
> **Focus**: Real-World Production Failures, Scalability Bottlenecks, Distributed Systems Mechanics, High-Throughput E-Commerce Architecture (Amazon, Flipkart, Shopify, Uber Eats).

---

## 📑 Table of Contents

1. [API Performance, Latency & Degradation (Q1 – Q10)](#1-api-performance-latency--degradation-q1--q10)
2. [Kafka & Event-Driven Failures in E-Commerce (Q11 – Q20)](#2-kafka--event-driven-failures-in-e-commerce-q11--q20)
3. [Inventory Management, Overselling & Concurrency (Q21 – Q30)](#3-inventory-management-overselling--concurrency-q21--q30)
4. [Order Processing & Distributed Transactions (Q31 – Q40)](#4-order-processing--distributed-transactions-q31--q40)
5. [Payment Gateway, Double Charge & Financial Consistency (Q41 – Q50)](#5-payment-gateway-double-charge--financial-consistency-q41--q50)
6. [Caching Strategies, Pitfalls & Invalidation (Q51 – Q60)](#6-caching-strategies-pitfalls--invalidation-q51--q60)
7. [Database Scaling, Sharding & Data Modeling (Q61 – Q70)](#7-database-scaling-sharding--data-modeling-q61--q70)
8. [Cart, Checkout & Session Management (Q71 – Q80)](#8-cart-checkout--session-management-q71--q80)
9. [Product Catalog, Search & Recommendations (Q81 – Q90)](#9-product-catalog-search--recommendations-q81--q90)
10. [System Resilience, Chaos Engineering & Observability (Q91 – Q100)](#10-system-resilience-chaos-engineering--observability-q91--q100)

---

## 1. API Performance, Latency & Degradation (Q1 – Q10)

### Q1: An API in the checkout path suddenly degrades from 80ms to 4500ms p99 latency under normal traffic. How do you troubleshoot and isolate the root cause?
- **Interviewer Intent**: Tests systematic incident debugging skills rather than random guessing.
- **Production Diagnosis Steps**:
  1. **Check RED Metrics** (Rate, Errors, Duration) on API Gateway / Ingress: Is it affecting all endpoints or specifically `/checkout`?
  2. **Distributed Tracing (OpenTelemetry / Jaeger / Zipkin)**: Inspect trace flame graphs for outlier requests. Check which span takes 90%+ of the execution time (Downstream HTTP call? Database query? Redis lock? Thread pool queue wait?).
  3. **JVM / Runtime Telemetry**: Check JVM Garbage Collection pauses (`G1GC/ZGC` Stop-The-World duration), CPU throttling (Kubernetes CFS quota), and thread pool saturation.
  4. **Database & Connection Pools**: Inspect HikariCP `ActiveConnections` vs `PendingThreads`. If `PendingThreads > 0`, requests are waiting for a DB connection because of unindexed queries or connection leaks.
  5. **Downstream Dependencies**: Check third-party external calls (fraud check, address validation, tax calculation) for latency spikes.
- **Immediate Mitigation**: Apply Circuit Breaker fallback on non-critical spans, increase pod replicas if CPU bound, or scale connection pool temporarily if database headroom permits.

### Q2: Average latency is 50ms, but p99 latency is 3.5 seconds. What does this indicate and how do you fix it?
- **Root Cause**: Averages hide tail-end pain. A high p99 with low average indicates **concurrency queueing, sporadic blocking, or uneven data skew**.
- **Common Triggers**:
  - **HikariCP / Thread Pool Queueing**: When 99% of requests get a worker instantly, but 1% queue up behind a slow query.
  - **Garbage Collection Spikes**: Major GC pauses stopping application threads periodically.
  - **Lock Contention**: Hot-row locking (e.g. updating a single merchant or inventory counter).
  - **Cache Misses for Cold Products**: Fast for cached items (1ms), slow for cold items needing full DB join + disk read (3s).
- **Remediation**: Tune GC (switch to ZGC or Shenandoah), use asynchronous non-blocking I/O (Netty/WebFlux), implement single-flight request coalescing for cache misses, and optimize the slowest 1% database queries.

### Q3: A downstream partner API (e.g., Shipping Carrier or Tax calculation) is slow. How do you prevent it from crashing your checkout flow?
- **Architecture Solution**:
  1. **Circuit Breaker (Resilience4j / Envoy)**: Set failure rate threshold (e.g. 50% slow/error calls over 20 requests) to trip into `OPEN` state, skipping the downstream call for 30 seconds.
  2. **Strict Timeouts & Deadline Propagation**: Set timeout to 800ms instead of default 30s. Never allow unbounded socket read timeouts.
  3. **Fallback Strategy**:
     - *Shipping API slow*: Fall back to estimated cached flat rates based on zipcode/region.
     - *Tax API slow*: Fall back to a local pre-computed tax rate table. Reconcile differences asynchronously during post-processing.
  4. **Bulkhead Pattern**: Allocate a dedicated, bounded thread pool (e.g. max 20 threads) for the third-party client so third-party slowness cannot exhaust the main checkout thread pool.

### Q4: What is Connection Pool Exhaustion and how do you prevent thread starvation?
- **Mechanism**: When request threads hold database connections while doing slow non-database tasks (e.g. making an HTTP call or waiting for an external lock) or queries take too long, the pool runs out of available connections. Subsequent incoming threads block until `connectionTimeout` is reached, leading to cascading HTTP 500/504 errors.
- **Prevention Rules**:
  - **Keep Transactions Tiny**: Never invoke network/HTTP calls inside `@Transactional` blocks.
  - **Proper Sizing (HikariCP Formula)**: `PoolSize = (CoreCount * 2) + EffectiveSpindleCount`. Oversized pools increase context switching and lock contention on the database engine.
  - **Strict `connectionTimeout`**: Default to 1500–2500ms. Fail fast instead of queuing hundreds of requests.
  - **Leak Detection**: Enable `leakDetectionThreshold = 2000ms` in HikariCP to log stack traces of threads holding connections for longer than expected.

### Q5: How do you handle tail latency amplification in a Scatter-Gather / Fan-out microservice call?
- **Problem**: When a Search or Product Details API fans out calls to 10 microservices in parallel, the total response time is bounded by the *slowest* service: $P(\text{System Slow}) = 1 - (1 - p)^N$. If 1 call has a 1% chance of being slow, a 10-service fan-out has a ~10% chance of being slow.
- **Mitigation Strategies**:
  - **Hedged Requests**: If no response is received within p95 latency (e.g. 80ms), fire a duplicate request to a different replica; take the fastest response and cancel the other.
  - **Degraded Fallback**: Mark non-essential services (recommendations, badges, sponsored ads) as optional. If they don't reply within 120ms, drop them and return the core product payload.
  - **Tie-breaker cancellation**: Propagate context cancellation tokens immediately when one side responds or client disconnects.

### Q6: What happens when an API experiences "Thread Pool Starvation" and how does reactive/non-blocking I/O solve it?
- **Problem**: In traditional thread-per-request models (Tomcat / Spring MVC), each incoming HTTP request ties up 1 OS thread. If 200 requests are blocked waiting for I/O (e.g. waiting for DB or Redis responses), all 200 threads sleep. Any 201st request is queued or rejected with HTTP 503.
- **Solution (Reactive / Async I/O)**: Frameworks like Spring WebFlux / Netty / Vert.x use an **Event Loop with epoll/kqueue**. A tiny pool of threads (equal to CPU cores) handles thousands of concurrent connections. While waiting for database network sockets to respond, the thread registers a callback and immediately processes other incoming requests.

### Q7: How do you implement End-to-End Deadline / Timeout Propagation across microservice hops?
- **Scenario**: Gateway receives request with 2.0s client timeout. Gateway calls Service A (takes 1.2s), Service A calls Service B (takes 0.7s), Service B calls Service C.
- **Failure without propagation**: Service C starts expensive computation even though the client already closed the connection 500ms ago.
- **Implementation**:
  - Transmit an `X-Request-Deadline: <epoch_ms>` or `grpc-timeout` header across all downstream gRPC/HTTP calls.
  - Each microservice computes `RemainingTime = Deadline - CurrentTime`.
  - If `RemainingTime <= 0`, abort processing immediately and return HTTP 504 / gRPC `DEADLINE_EXCEEDED` without hitting downstream services or databases.

### Q8: What is HTTP/2 multiplexing and why does it matter for E-Commerce Product Listing Pages (PLP)?
- **Details**: HTTP/1.1 requires a separate TCP connection per concurrent request (browsers limit to 6 connections per domain) and suffers from Head-of-Line (HoL) blocking at the application level.
- **HTTP/2 Benefit**: Allows thousands of concurrent requests over a single TCP connection using binary framing into independent streams. A Product Listing page loading 50 product thumbnails, prices, and reviews simultaneously avoids connection handshake overhead, reduces SSL/TLS negotiation, and cuts page load times by 40–60%.

### Q9: How do you gracefully degrade an E-Commerce API when CPU reaches 95% during a Big Sale?
- **Multi-Level Degradation Strategy**:
  1. **Level 1 (Disable Async Logging / Auditing verbose levels)**: Switch loggers from `DEBUG`/`INFO` to `WARN`/`ERROR`.
  2. **Level 2 (Drop Non-Essential UI widgets)**: Return empty responses for personalized recommendations, "frequently bought together", and live inventory visitor counters ("15 people viewing this").
  3. **Level 3 (Stale / Aggressive Cache Serving)**: Serve expired cache entries (`stale-while-revalidate`) without querying the primary DB.
  4. **Level 4 (Admission Control & Virtual Waiting Room)**: Reject incoming non-critical traffic at the API Gateway with HTTP 429 / queue tokens (Cloudflare Waiting Room / Redis Token Bucket) to protect active checkout sessions.

### Q10: How do you debug database slow queries blocking checkout threads during peak traffic?
- **Steps**:
  1. Query active locks and running queries:
     ```sql
     -- PostgreSQL
     SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state 
     FROM pg_stat_activity 
     WHERE state != 'idle' ORDER BY duration DESC;
     ```
  2. Look for `Lock:tuple` or `Lock:transactionid` wait events indicating row-level lock contention.
  3. Inspect `EXPLAIN (ANALYZE, BUFFERS)` for sequential table scans (`Seq Scan`) instead of index scans.
  4. Kill stuck rogue queries using `SELECT pg_cancel_backend(pid);` or terminate with `pg_terminate_backend(pid);`.
  5. Check `statement_timeout` settings (enforce 3s max on OLTP transactions to prevent unbounded locks).

---

## 2. Kafka & Event-Driven Failures in E-Commerce (Q11 – Q20)

### Q11: What happens if a Kafka Consumer crashes or dies midway through processing an order event?
- **Failure Flow**:
  1. If `enable.auto.commit=true`: The offset might have been committed before processing completed $\rightarrow$ **Message Loss**!
  2. If `enable.auto.commit=false` (manual commit): The consumer dies before calling `commitSync()` / `commitAsync()`.
  3. The Kafka Coordinator detects missed heartbeats (`session.timeout.ms` expires, e.g. 10s).
  4. Kafka triggers a **Consumer Group Rebalance**.
  5. The partition is assigned to a healthy consumer replica.
  6. The new consumer reads from the **last committed offset** and re-processes the event $\rightarrow$ **Duplicate Processing Risk**!
- **Mandatory Production Pattern**: Consumers must be **idempotent**. Use unique `orderId` deduplication in DB or Redis before processing side effects.

### Q12: What is a Consumer Group "Rebalance Storm" and how do you stop it from freezing order processing?
- **Cause**: A slow consumer takes longer to process a batch than `max.poll.interval.ms` (e.g. default 300s). The broker thinks the consumer died, kicks it out, and triggers a rebalance. All consumers stop processing ("Stop-the-World"). During rebalancing, message queues pile up; when consumers resume, the huge batch causes another timeout, triggering an infinite rebalance loop!
- **Mitigation**:
  - Switch assignment protocol to `CooperativeStickyAssignor` (incremental cooperative rebalancing instead of eager stop-the-world).
  - Decrease `max.poll.records` (e.g. from 500 to 50) so batches complete well within `max.poll.interval.ms`.
  - Offload heavy operations (e.g. PDF invoice generation or image processing) to an internal thread pool or separate background workers so the Kafka poll loop is never blocked.

### Q13: What is a "Poison Pill" message in Kafka and how do you handle it with Dead Letter Queues (DLQ)?
- **Problem**: A malformed or corrupted JSON payload (or a business logic bug) causes an unhandled exception every time a consumer reads it. The consumer crashes, restarts, reads the exact same offset, and crashes again in an infinite loop.
- **Production DLQ Pattern**:
  ```
  [Order Topic] ──► [Consumer: try/catch] ──(Success)──► [Commit Offset]
                           │
                    (On Error / Max Retries)
                           ▼
                  [Produce to DLQ Topic] ──► [Commit Original Offset]
                           │
                  [Alert SRE / Fix & Replay]
  ```
  - Catch deserialization and fatal processing exceptions.
  - Implement a retry topic with exponential backoff (e.g. `order.retry.10s`, `order.retry.1m`).
  - If retries exceed maximum threshold (e.g. 3 attempts), produce the event to `order.dlq` containing error stack trace headers and **commit the original topic offset** to allow the partition to advance.

### Q14: How do you guarantee strictly ordered message processing in Kafka for an e-commerce order?
- **Context**: Events for an order must process in sequence: `OrderCreated` $\rightarrow$ `PaymentCaptured` $\rightarrow$ `OrderPacked` $\rightarrow$ `OrderShipped`.
- **Kafka Guarantee Rules**:
  1. **Partition Key**: Set the Kafka message key to `order_id` (or `account_id`). All events with the same key are guaranteed to land on the same partition.
  2. **Single Consumer per Partition**: Within a consumer group, only one consumer instance reads a given partition at any time, ensuring sequential consumption.
  3. **Producer Settings**: Set `max.in.flight.requests.per.connection=1` (or $\le 5$ with `enable.idempotence=true`). Otherwise, if request 1 fails and retries after request 2 succeeds, order is inverted!

### Q15: How do you handle growing Consumer Lag during high-traffic flash sales?
- **Diagnostics**: Check `kafka-consumer-groups.sh --describe --group order-fulfillment-group` to identify whether lag is distributed across all partitions or isolated to specific partitions (hot partitions due to bad keys).
- **Resolution Strategy**:
  1. **Scale Out**: Add more consumer pods up to the number of partitions. (Note: having more consumer instances than partitions leaves surplus instances idle).
  2. **Increase Partition Count**: If consumers = partitions already, increase topic partitions dynamically and scale consumers.
  3. **In-Memory Worker Dispatcher**: The single partition consumer thread fetches messages, groups them by sub-key (`order_id`), and delegates to an internal bounded thread pool while tracking in-flight offsets.
  4. **Batch DB Inserts**: Batch 50 DB writes into a single `INSERT INTO ... VALUES (), (), ()` statement instead of 50 individual roundtrips.

### Q16: What is the Dual-Write Problem and how does the Transactional Outbox Pattern solve it?
- **The Problem**: A service updates the SQL database and then produces an event to Kafka:
  ```java
  orderRepository.save(order); // Step 1
  kafkaTemplate.send("orders", order); // Step 2
  ```
  If the application crashes or network fails between Step 1 and Step 2, DB has the order, but Kafka never gets the event! If you swap the order, Kafka gets the event, but DB write might roll back.
- **Solution — Transactional Outbox Pattern**:
  1. Within the **same DB transaction**, write the order to the `orders` table AND write the event payload to an `outbox` table.
  2. A Change Data Capture (CDC) tool like **Debezium** tailing the Postgres WAL (Write-Ahead Log) reads the outbox table and streams events into Kafka with at-least-once reliability.
  3. Zero dual-write discrepancies.

### Q17: What causes `CommitFailedException` in Kafka and how do you fix it?
- **Root Cause**: The time between two successive `poll()` calls exceeded `max.poll.interval.ms`. The coordinator marked the consumer dead and reassigned its partitions to another node. When the original thread finishes its lengthy processing and attempts `consumer.commitSync()`, the coordinator rejects it with `CommitFailedException`.
- **Fix**:
  - Do not do heavy processing on the main poll thread.
  - Decrease `max.poll.records`.
  - Increase `max.poll.interval.ms` if lengthy batches are unavoidable.

### Q18: What is the difference between "At-Least-Once", "At-Most-Once", and "Exactly-Once" (EOS) processing in Kafka?
- **At-Most-Once**: Commit offsets *before* processing message. If processing fails, message is lost forever. Unacceptable in e-commerce.
- **At-Least-Once**: Process message *first*, then commit offset. If crash happens during processing, message is re-delivered on restart. (Standard industry choice when paired with idempotent consumers).
- **Exactly-Once (EOS)**: Uses Kafka transactional API (`sendOffsetsToTransaction` + `initTransactions`). Typically used in Kafka Streams or Kafka-to-Kafka topologies where read, process, and write happen exclusively within Kafka brokers.

### Q19: How do you handle schema evolution and backward compatibility in Kafka event streams?
- **Solution**: Use **Schema Registry** (Confluent / Apicurio) with **Avro** or **Protobuf**:
  - Producers fetch schemas and register new versions.
  - Set compatibility mode to `BACKWARD` or `FULL` in production.
  - **Rules**: Never delete a mandatory field; assign default values to newly added fields; never change field data types. Consumers using older schemas can still read payloads serialized with newer schemas.

### Q20: How do you prevent downstream database exhaustion from high-throughput Kafka consumers?
- **Mitigation**:
  - **Pause / Resume API**: When downstream database connection pool latency climbs above 200ms or queue fills up, call `consumer.pause(partitions)`. Keep invoking `poll()` to maintain heartbeats, but fetch 0 records. Once DB recovers, invoke `consumer.resume(partitions)`.
  - **Rate Limiting with Resilience4j**: Throttle consumer execution rate to match the maximum sustainable database write throughput.

---

## 3. Inventory Management, Overselling & Concurrency (Q21 – Q30)

### Q21: 1,000 units of an iPhone are released. 200,000 users click "Buy Now" at the same second. How do you prevent overselling without crashing the database?
- **Naive (Broken) Approach**:
  ```sql
  SELECT stock FROM inventory WHERE product_id = 1; -- Returns 10
  -- If stock > 0
  UPDATE inventory SET stock = stock - 1 WHERE product_id = 1;
  ```
  Race condition! Thousands of threads read `stock = 10` simultaneously and decrement, resulting in negative stock (-5000).
- **Production Multi-Tier Solution**:
  1. **Layer 1 — Redis Atomic Decrement (`DECR` / Lua Script)**:
     - Store inventory in Redis: `SET product:101:stock 1000`.
     - Execute an atomic Lua script:
       ```lua
       local stock = redis.call('get', KEYS[1])
       if tonumber(stock) > 0 then
           redis.call('decr', KEYS[1])
           return 1
       else
           return 0
       end
       ```
     - 199,000 requests are rejected in-memory in Redis sub-millisecond without touching the database!
  2. **Layer 2 — Async DB Sync**: The 1,000 successful decrements produce events into a Kafka `order-validation` topic, which asynchronously writes orders into the relational database.
  3. **Layer 3 — DB Safety Check**: Atomic database decrement with positive constraint:
     ```sql
     UPDATE inventory SET stock = stock - 1 WHERE product_id = 101 AND stock > 0;
     ```

### Q22: Optimistic Locking vs. Pessimistic Locking: Which one should you use for high-traffic inventory updates?
- **Pessimistic Locking (`SELECT FOR UPDATE`)**:
  - Acquires an exclusive lock on the row until the transaction commits.
  - *Flash Sale behavior*: Thousands of transactions block waiting for the single row lock. Threads queue up, database connection pool exhausts, and the DB crashes.
- **Optimistic Locking (`version` column)**:
  - `UPDATE inventory SET stock = stock - 1, version = version + 1 WHERE product_id = 101 AND version = 5;`
  - *Flash Sale behavior*: 1 request succeeds; 999 requests fail due to version mismatch and must retry, causing massive CPU thrashing on the DB.
- **Verdict for Flash Sales**: Neither! Use **In-Memory Atomic Counters (Redis)** or **Bucket Sharding** at the caching layer; push accepted reservations asynchronously to the DB.

### Q23: What is "Inventory Sharding / Bucketing" and how does it solve DB hot-row lock contention?
- **Architecture**:
  - Instead of having 1 row for Product 101 with `stock = 1000`, divide the stock across $N$ virtual buckets (e.g. 10 rows):
    - `(product_id=101, bucket=0, stock=100)`
    - `(product_id=101, bucket=1, stock=100)` ...
    - `(product_id=101, bucket=9, stock=100)`
  - When an order arrives, select a bucket randomly (`hash(user_id) % 10`) and decrement that specific bucket.
  - Row lock contention on the database decreases by **10x**!

### Q24: How does an Inventory Reservation (Hold) pattern work, and how do you handle abandonment after 10 minutes?
- **Lifecycle**:
  1. User enters checkout $\rightarrow$ Reserve 1 item: `available_stock - 1`, `reserved_stock + 1`.
  2. Set a timer/TTL for 10 minutes.
  3. **Case A (Payment Success)**: Convert reservation to permanent deduction: `reserved_stock - 1`, generate order.
  4. **Case B (User drops off / Payment Timeout)**: Reclaim stock: `reserved_stock - 1`, `available_stock + 1`.
- **Implementation for Release**:
  - Use **Redis Key Expiration Notification (`keyspace notifications`)** or **Scheduled Delayed Message Queue** (RabbitMQ Dead-Letter Exchange / Kafka delayed topic / Redisson DelayedQueue).
  - When the 10-minute timer expires without a `payment_confirmed` flag, the reaper worker automatically reclaims the stock.

### Q25: How does the Redis Redlock algorithm work, and what are its criticisms in distributed systems?
- **How Redlock Works**:
  - Client attempts to acquire a lock across $N$ independent Redis masters (e.g. 5 nodes) sequentially using `SET resource_name my_random_value NX PX 30000`.
  - The lock is considered acquired if the client obtains locks in at least $(N/2 + 1)$ masters within a timeout shorter than lock validity time.
- **Criticisms (Martin Kleppmann)**:
  - Vulnerable to **GC Pauses and Clock Drift**: A client acquires the lock, enters a long Stop-The-World GC pause, the lock expires in Redis, another client acquires it, and both clients now think they hold the lock!
- **Production Defense**: Always pair distributed locks with a **Fencing Token** (monotonically incrementing counter checked by the storage layer).

### Q26: How do you prevent negative inventory anomalies when a database network partition occurs?
- **CAP Theorem Trade-Off**: For inventory during checkout, choose **Consistency over Availability (CP)**.
- Enforce strict database column constraints:
  ```sql
  ALTER TABLE inventory ADD CONSTRAINT check_stock_positive CHECK (stock >= 0);
  ```
- Any transaction that would push stock below 0 is immediately aborted and rolled back by the database engine, regardless of application logic bugs.

### Q27: How do you handle Multi-Warehouse Inventory Allocation in real-time?
- **Flow**:
  1. Order contains items A, B, and C destined for Zipcode 94105.
  2. Warehouse Service evaluates:
     - Availability of items across Regional Distribution Centers (RDCs).
     - Minimizing split shipments (shipping from 1 warehouse vs 2).
     - Lowest shipping cost / fastest delivery SLA.
  3. Solve as a **Knapsack / Constraint Satisfaction Problem**:
     - Lock inventory tentatively across the chosen warehouse.
     - If single warehouse cannot satisfy, split into 2 fulfillment orders (`FulfillmentOrder_1`, `FulfillmentOrder_2`).

### Q28: How do you design an Event-Sourced Inventory System?
- **Concept**: Instead of storing mutable `current_stock = 45`, store immutable append-only ledger events:
  - `ItemReceived {sku: 101, qty: +100}`
  - `ItemReserved {sku: 101, qty: -1}`
  - `ItemDamaged {sku: 101, qty: -2}`
- **Read Model**: Maintain a materialized snapshot table updated via Kafka event stream. Full auditability of every single stock adjustment over time.

### Q29: What is "Phantom Inventory" and how do reconciliation pipelines detect it?
- **Definition**: Discrepancy between digital system inventory (e.g. shows 3 units in stock) and physical shelf inventory (0 units due to theft, misplacement, or unrecorded damage).
- **Mitigation**:
  - Run nightly reconciliation jobs comparing warehouse WMS physical cycle count scans against OLTP database tables.
  - Emit alert if order cancellations due to "Item Not Found in Warehouse" exceed 1% threshold for a SKU. Automatically mark SKU as out-of-stock.

### Q30: How do you handle stock re-allocation when a canceled order has backordered waitlist customers?
- **Pattern**:
  1. Order cancellation event `OrderCanceledEvent` emitted to Kafka.
  2. Inventory Service reclaims stock.
  3. Do NOT immediately return stock to general public pool.
  4. Query priority waitlist table (`SELECT * FROM backorders WHERE sku = 101 ORDER BY priority ASC LIMIT 1`).
  5. Automatically assign reservation to the first waitlisted customer and trigger a notification with a 2-hour payment link.

---

## 4. Order Processing & Distributed Transactions (Q31 – Q40)

### Q31: Why does Two-Phase Commit (2PC) fail in a distributed microservices e-commerce system?
- **Reasons**:
  1. **Blocking Protocol**: In the prepare phase, resources (DB rows, locks) are held until the coordinator issues commit/abort. If the coordinator or network fails, resources stay locked indefinitely.
  2. **Latency & Throughput Collapse**: Network round-trips multiply across $N$ microservices. Total latency is the sum of the slowest nodes.
  3. **Single Point of Failure**: Dependency on an external distributed transaction manager (XA transactions).
- **Alternative**: Use the **Saga Pattern** with eventual consistency.

### Q32: Orchestration-based Saga vs. Choreography-based Saga: Which one should you choose for Order Fulfillment?
- **Choreography**:
  - Services listen to Kafka events and publish new events without a central coordinator (e.g., Order Service emits `OrderCreated` $\rightarrow$ Payment Service listens, charges card, emits `PaymentSuccess` $\rightarrow$ Inventory Service listens, reserves stock...).
  - *Drawback*: Cyclic dependencies, hard to trace workflow state, nightmare to manage when workflows exceed 4-5 steps.
- **Orchestration (Recommended for E-Commerce Checkout)**:
  - A central **Order Saga Orchestrator** (e.g., Temporal.io, Camunda, or custom state machine) explicitly calls each service and manages state transitions and compensation workflows.
  - *Benefit*: Centralized visibility, easy rollback handling, clear timeouts.

### Q33: How do Compensating Transactions work when payment succeeds but inventory reservation fails?
- **Saga Flow**:
  1. Step 1: Create Pending Order (Order Service) $\rightarrow$ OK.
  2. Step 2: Deduct Loyalty Points (Points Service) $\rightarrow$ OK.
  3. Step 3: Charge Payment (Stripe/Payment Service) $\rightarrow$ OK.
  4. Step 4: Reserve Inventory (Inventory Service) $\rightarrow$ **FAIL (Out of Stock)!**
- **Trigger Compensation in Reverse Order**:
  - Compensate Step 3: Refund payment via Stripe API (`refundTransactionId`).
  - Compensate Step 2: Credit back loyalty points.
  - Compensate Step 1: Update order status to `CANCELLED_OUT_OF_STOCK`.

### Q34: How do you design an Idempotent Order Placement API to prevent duplicate orders on double click?
- **Pattern**:
  1. Client requests an **Idempotency Key** (UUIDv4) when opening the checkout page.
  2. Client submits `POST /orders` with header `Idempotency-Key: 7b8c9d...`.
  3. API Gateway / Order Service executes in Redis:
     ```
     SET order:idempotency:<key> "PROCESSING" NX EX 120
     ```
     - If returns `false` (key exists): Return `409 Conflict` or poll for the existing order result.
     - If returns `true`: Proceed with order placement.
  4. Once order is saved, update Redis with `orderId` and response payload:
     ```
     SET order:idempotency:<key> '{"orderId":"ORD-12345","status":"CREATED"}' EX 86400
     ```
  5. Any retried request with the same key gets the cached response instantly without creating another order.

### Q35: How do you model an Order State Machine to handle concurrent lifecycle events?
- **Finite State Machine (FSM)**:
  - States: `CREATED` $\rightarrow$ `PAID` $\rightarrow$ `PROCESSING` $\rightarrow$ `SHIPPED` $\rightarrow$ `DELIVERED`.
  - Terminal States: `CANCELLED`, `REFUNDED`.
- **Concurrency Rule**: Never update status without verifying previous state:
  ```sql
  UPDATE orders 
  SET status = 'PAID', updated_at = NOW() 
  WHERE order_id = 'ORD-101' AND status = 'CREATED';
  ```
  If affected rows = 0, another process already transitioned the order or it was cancelled. Reject illegal transitions (e.g. cannot transition `SHIPPED` to `CANCELLED` directly).

### Q36: How do you handle asynchronous fraud checks that take 3 to 10 seconds without blocking user checkout?
- **Flow**:
  1. Allow checkout to complete with status `ORDER_PLACED_PENDING_VERIFICATION`.
  2. Return immediate HTTP 201 with Order Confirmation screen to the customer.
  3. Asynchronously publish `OrderSubmittedForReview` event to Kafka.
  4. Fraud Detection Service (ML model + external third-party rules) evaluates the order:
     - *Passed*: Emits `FraudCheckPassed` $\rightarrow$ Order transitions to `READY_FOR_FULFILLMENT`.
     - *Flagged*: Emits `FraudCheckFailed` $\rightarrow$ Suspends payment capture and routes to manual reviewer queue.

### Q37: How do you handle split shipments across different third-party marketplace sellers?
- **Data Model**:
  - Parent: `Order` (stores overall total, customer info, payment details).
  - Child: `SubOrder` or `FulfillmentPackage` (stores seller_id, items, separate tracking number, individual status).
- **Payment & Escrow**: The customer's credit card is charged once for the parent order. Marketplace escrow splits payouts to individual merchant sub-accounts upon individual delivery confirmation.

### Q38: What happens if an order cancellation arrives while the warehouse is physically packing the box?
- **Race Condition Resolution**:
  - Order status is `PICKED`.
  - When user clicks "Cancel", Order Service checks with Warehouse Management System (WMS).
  - If WMS state is already `PACKED_AND_LABELED` or `LOADED_ON_TRUCK`:
    - Reject cancellation with message: *"Your order is already packed and on its way. You can initiate a free return once delivered."*
  - If WMS state is `ASSIGNED_TO_PICKER`:
    - WMS sends cancel signal to the picker's RF scanner gun; items are returned to bin. Cancellation succeeds.

### Q39: How do you design an Order History search query for millions of users with diverse filters?
- **Problem**: Querying `orders` table with complex filters (date range, order status, item keyword search, price range) will crush an OLTP relational database.
- **Architecture**:
  - OLTP (Postgres/MySQL) handles fast write operations (`INSERT INTO orders`).
  - Stream changes via Debezium CDC $\rightarrow$ Kafka $\rightarrow$ **Elasticsearch / OpenSearch**.
  - Customer Order History queries hit Elasticsearch index sharded by `user_id`, delivering 30ms search results across millions of past records.

### Q40: How do you rebuild Order state if database corruption occurs?
- **Event Sourcing & Log Replay**:
  - If the primary read database experiences corruption or unrecoverable disk failure, restore the latest clean database backup.
  - Identify the timestamp of the backup.
  - Replay the immutable Kafka `order-events` topic from the corresponding offset timestamp forward to reconstruct exact current state for all active orders.

---

## 5. Payment Gateway, Double Charge & Financial Consistency (Q41 – Q50)

### Q41: Payment Gateway times out with HTTP 504. The user was deducted money, but your backend received an error. How do you resolve this?
- **Never guess or assume failed**:
  1. **Do not mark order CANCELLED immediately**: Set order status to `PAYMENT_PENDING_VERIFICATION`.
  2. **Active Inquiry (Status Polling)**: The Payment Service invokes the Gateway's inquiry API with the original transaction reference: `GET /v1/payments/{payment_reference_id}`.
  3. **Handle Gateway Response**:
     - *If Gateway shows SUCCESS*: Update order to `PAID`, kick off fulfillment.
     - *If Gateway shows NOT_FOUND / FAILED*: Trigger auto-void or mark failed.
  4. **Fallback Webhook**: If inquiry also fails, wait for the asynchronous payment webhook from Stripe/PayPal.
  5. **Nightly Reconciliation**: Flag any transactions unresolved after 2 hours for automated reverse settlement.

### Q42: How do you guarantee that a user is never double-charged if they rapidly tap "Pay" twice?
- **Defensive Multi-Layer Barrier**:
  1. **Frontend**: Disable the submit button immediately upon first click; render spinner.
  2. **Idempotency Key at Gateway**: Generate a unique key per checkout attempt.
  3. **Distributed Lock on User Account / Cart**:
     ```
     SET lock:payment:cart_101 "locked" NX EX 30
     ```
     Any simultaneous request fails to acquire the lock and is rejected.
  4. **Pass Idempotency Key to Third-Party Gateway**: Pass the key directly to Stripe/Adyen in the `Idempotency-Key` HTTP header. The payment processor's own engine will deduplicate and ensure only one card charge is authorized.

### Q43: A payment webhook arrives before the checkout API call returns to the client. How do you prevent race conditions?
- **Scenario**: User completes 3D-Secure in bank window. The bank immediately notifies your webhook endpoint (`PAYMENT_SUCCESS`), while the user's browser redirect request is still in transit.
- **Handling**:
  - Webhook worker attempts an atomic update:
    ```sql
    UPDATE orders 
    SET payment_status = 'PAID', updated_at = NOW() 
    WHERE order_id = 'ORD-101' AND payment_status != 'PAID';
    ```
  - If browser redirect hits server afterwards: Server queries DB, sees `payment_status == 'PAID'`, and immediately displays the success page to the user without attempting to re-charge.

### Q44: What is Dual-Entry Bookkeeping and why is it mandatory for e-commerce ledgers?
- **Single-Entry (Wrong)**: Storing a single column `user_balance = 500`. Prone to arithmetic bugs, concurrency race conditions, and un-auditable losses.
- **Dual-Entry (Production Standard)**: Every financial event creates at least two ledger entries: a **Debit** and an equal and opposite **Credit**.
  $$\sum \text{Debits} = \sum \text{Credits}$$
- **Example: Customer purchases $100 product**:
  - Credit: `Accounts Receivable (Payment Gateway)`: +$100
  - Debit: `Merchant Payable`: +$85
  - Debit: `Platform Commission Revenue`: +$15
  - Ledger balance is always zero-sum. Any corruption or missing transaction causes an immediate checksum alert.

### Q45: What happens when an automated refund fails because the customer's card has expired or the bank account is closed?
- **Failure Flow**: Payment gateway returns `REFUND_FAILED: CARD_EXPIRED` or `ACCOUNT_CLOSED`.
- **Handling**:
  - Mark refund state in internal ledger as `REFUND_REQUIRES_MANUAL_INTERVENTION`.
  - Emit event to Customer Service / Finance dashboard.
  - Trigger email to user prompting them to provide an alternative payout method (Store Credit / ACH transfer / Bank Wire).
  - Automatically credit store balance as default fallback if user opted in.

### Q46: How do you design an End-of-Day (EOD) Payment Reconciliation Pipeline?
- **Pipeline Architecture**:
  1. Daily at 02:00 AM, fetch Settlement Report (CSV/JSON) via SFTP/API from Payment Providers (Stripe, PayPal, Adyen).
  2. Parse gateway settlement rows (`TransactionID, Amount, Fee, Currency, Status`).
  3. Stream through an ETL worker (Spark / Batch SQL job) matching Gateway `TransactionID` against Internal Database Ledger entries.
  4. Identify discrepancies:
     - *Type 1 (Amount Mismatch)*: Currency conversion or fee difference.
     - *Type 2 (Missing in DB)*: Payment charged on gateway but order missing internally.
     - *Type 3 (Missing in Gateway)*: Order marked paid internally, but no settlement found at bank.
  5. Generate exception report for automated finance team review.

### Q47: How does PCI-DSS compliance impact system design for storing payment credentials?
- **Golden Rule**: **Never touch, store, or transmit raw credit card numbers (PAN), CVVs, or expiration dates on your servers.**
- **Tokenization Flow**:
  - Host payment input fields inside an **iframe** or **SDK provided by the payment processor** (e.g. Stripe Elements).
  - Card details go directly from the customer's browser to Stripe's PCI-certified servers.
  - Stripe returns a non-sensitive token (`tok_1N4m2...`).
  - Your backend only receives and stores the token and metadata (card brand, last 4 digits). Reduces PCI audit scope from Level 1 SAQ-D to lightweight SAQ-A.

### Q48: How do you handle Dynamic Currency Conversion and exchange rate volatility at checkout?
- **Mechanism**:
  - User browses in EUR, but merchant settles in USD.
  - Caching exchange rates: Fetch from FX provider (e.g. Bloomberg/OANDA), store in Redis with short TTL (e.g. 15 minutes).
  - **Rate Lock Guarantee**: When user enters checkout, lock the exchange rate for 15 minutes by storing `fx_rate` in the checkout session.
  - Prevent losses due to sudden intraday currency fluctuations.

### Q49: How do you handle partial captures and split authorizations for backordered items?
- **Scenario**: User orders 2 items ($100 total). Item 1 ships now ($60); Item 2 ships in 3 weeks ($40).
- **Credit Card Rules**: Authorizations typically expire in 7 days. You cannot charge for an item before it ships (FTC regulations).
- **Implementation**:
  - Perform an initial **Auth** for $100.
  - **Capture** $60 immediately when Item 1 ships.
  - When Item 2 is ready in 3 weeks: Use the customer's saved payment token to execute a **new authorization and capture** for $40. If auth fails, notify user to update payment method before shipping.

### Q50: How do you protect against "Carding Attacks" (automated bot testing stolen credit cards)?
- **Attack Pattern**: Fraudsters use bots to run thousands of small $1.00 purchases across e-commerce checkouts to test validity of stolen card lists.
- **Defense Mechanism**:
  - Rate limit checkout attempts per IP, Device Fingerprint, and User ID at API Gateway.
  - Integrate CAPTCHA (Cloudflare Turnstile) dynamically when payment failures from the same session exceed 2 attempts.
  - Deploy Machine Learning velocity checks (e.g. Stripe Radar / Sift Science): Flag sessions attempting multiple distinct card numbers within minutes.

---

## 6. Caching Strategies, Pitfalls & Invalidation (Q51 – Q60)

### Q51: What is "Cache Penetration" and how do you protect an e-commerce catalog using Bloom Filters?
- **The Threat**: Attackers send millions of requests for non-existent product IDs (e.g., `GET /products/-99999` or random UUIDs). Since the keys don't exist in Redis, every single request bypasses the cache and hammers the primary database, driving CPU to 100%.
- **Solutions**:
  1. **Cache Null Values**: If the DB returns empty, store a placeholder in Redis with a short TTL (e.g. `SET product:fake-id "" EX 60`).
  2. **Bloom Filter (Recommended)**:
     - Place a Bloom filter in front of Redis loaded with all valid `product_id`s.
     - If the Bloom filter says *"Definitely does not exist"*, reject the request immediately without checking Redis or DB!
     - Space-efficient: 10 million product IDs require only ~12MB of memory.

### Q52: What is a "Cache Stampede" (Cache Breakdown) and how does SingleFlight / Mutex solve it?
- **Problem**: A super-hot product (e.g. Black Friday deal) has its Redis cache key expire at 12:00:00. At 12:00:01, 10,000 concurrent requests find a cache miss and all simultaneously query the database to rebuild the cache, crushing the database.
- **Mitigations**:
  - **Distributed Mutex / Lock**: Only the first thread that experiences a cache miss acquires a Redis lock to query the DB and update the cache. All other 9,999 threads wait or sleep for 50ms and re-read from cache.
  - **SingleFlight (Golang / Java Caffeine)**: Request coalescing deduplicates concurrent in-flight requests for the same key.
  - **Probabilistic Early Expiration (XFetch Algorithm)**: Worker threads refresh the cache *before* it actually expires based on an asymmetric random probability distribution.

### Q53: What is "Cache Avalanche" and how do you prevent it?
- **Problem**: Hundreds of thousands of cache keys are set with the exact same TTL (e.g. 1 hour). When that hour elapses, all keys expire at the exact same second, causing system-wide database load spike.
- **Fix**:
  - **Add Random Jitter**:
    ```python
    ttl = base_ttl + random.randint(0, 300) # Base 1 hr + up to 5 min random jitter
    ```
  - Spread key expirations smoothly across time.

### Q54: What is the "Hot Key" problem in Redis and how do you scale a single viral product page?
- **Problem**: Redis partitions keys across cluster nodes using hash slots (`CRC16(key) % 16384`). A viral product key lives on **one single Redis node**. Even if you have a 50-node Redis cluster, that single node's CPU maxes out at 100% while 49 nodes sit idle.
- **Solutions**:
  1. **L1 Local Memory Cache (Two-Tier Caching)**: Use an in-process memory cache (Caffeine in Java, node-cache in Node) on every API Gateway/Service pod with a 5-second TTL. The 50,000 req/s are absorbed in application memory without hitting Redis.
  2. **Key Duplication with Random Suffixes**:
     - Replicate the hot key across shards: `product:101:copy_1`, `product:101:copy_2`, ... `product:101:copy_10`.
     - Client randomly reads from `product:101:copy_(random(1, 10))`. Load distributes across 10 different Redis shards!

### Q55: Cache-Aside vs. Write-Through vs. Write-Behind: Which one do you pick for Product Details vs. Order Creation?
- **Product Details (Read-Heavy 99:1)**:
  - Use **Cache-Aside**. App reads from cache; if miss, reads DB and populates cache. App invalidates/updates cache when admin updates product.
- **Order Creation (Write-Heavy, Critical Durability)**:
  - Do NOT use Write-Behind (Write-Behind writes to cache first and batches to DB later; risks data loss on cache crash).
  - Write directly to DB first; publish event to invalidate or update cache.

### Q56: What happens if a Redis Cluster node fails during a flash sale?
- **Failover Mechanism**:
  1. Cluster master stops responding to gossip pings.
  2. Remaining masters vote to declare node dead.
  3. A replica is promoted to master.
- **Risk 1 (Failover Lag / Connection Drop)**: During the 3–5 second election window, client requests to that shard throw `JedisConnectionException`. Application must have retry logic with exponential backoff.
- **Risk 2 (Data Loss)**: Redis master-replica replication is **asynchronous**. Writes committed to master just before failure that weren't yet replicated to the slave are lost forever!

### Q57: How do you handle the race condition where a slow DB query overwrites newer data in the cache?
- **Scenario**:
  1. Thread A reads DB (gets old Price = $10).
  2. Thread B updates DB (Price = $20) and evicts cache key.
  3. Thread A wakes up and writes its stale Price ($10) into cache!
- **Fix — Delayed Double Deletion**:
  - App updates DB $\rightarrow$ deletes cache key.
  - Sleep for 500ms (to allow any concurrent read transactions to finish).
  - Delete cache key a second time!
  - Alternatively, use Redis version comparison via Lua scripts.

### Q58: How do you design a Multi-Tier Caching Architecture for an E-Commerce Website?
```
[User Browser]
       │
       ▼
[Edge CDN (Cloudflare/Fastly)] ──► Caches static assets, product images, HTML skeletons (TTL: 1 day)
       │
       ▼
[API Gateway (Envoy/Kong)]    ──► Caches public category/navigation responses (TTL: 10 min)
       │
       ▼
[App L1 In-Memory (Caffeine)] ──► Hot product metadata, config flags (TTL: 5-10 sec)
       │
       ▼
[L2 Distributed Redis Cluster]──► Session data, cart, user profile, stock cache (TTL: hours)
       │
       ▼
[Primary Database (Postgres)]  ──► Source of Truth
```

### Q59: Which Redis memory eviction policy should you choose for e-commerce catalog vs. session storage?
- **Session / Cart Cache**: Use `noeviction` or `volatile-lru`. If Redis runs out of memory, throw errors rather than evicting an active customer's cart!
- **Catalog Browsing Cache**: Use `allkeys-lru` or `allkeys-lfu`. Least frequently used product records can be safely dropped; they will simply be re-fetched from the DB on demand.

### Q60: How do you invalidate CDN caches globally when a merchant changes a product price?
- **Strategies**:
  1. **Purge API with Cache-Tags (Surrogate Keys)**: Fastly / Cloudflare allow tagging responses with `product_101`. When price changes, invoke `CDN.purgeByTag("product_101")`. Completes in <150ms globally.
  2. **URL Versioning / Cache Busting**: For assets and images, append hash: `image.png?v=abc123`.

---

## 7. Database Scaling, Sharding & Data Modeling (Q61 – Q70)

### Q61: What is the ideal Sharding Key for an E-Commerce Orders database?
- **Option A: `order_id`**: Great for looking up an order by ID. Terrible for customer order history (`SELECT * FROM orders WHERE user_id = 123` must broadcast to all shards!).
- **Option B: `user_id` (Standard Choice)**:
  - All orders for a given user reside on the same shard.
  - User order history query is routed to a single shard.
  - *Trade-off*: Looking up an order by `order_id` (e.g. from customer support) requires a global secondary index lookup (`order_id -> user_id` mapping table or Elasticsearch).
- **Option C: `merchant_id`**: Good for B2B multi-tenant marketplaces (Shopify).

### Q62: What is Replication Lag and how do you prevent "Read-Your-Own-Writes" inconsistency?
- **Problem**: User places order on Primary DB. Browser immediately redirects to Order Confirmation screen and queries Read Replica. Read replica is lagging by 400ms. User sees *"You have no recent orders"* and panics, placing a second order!
- **Mitigation Techniques**:
  1. **Route Immediate Post-Write Reads to Primary**: If the user just performed a write operation, route their reads to the Primary DB for the next 5 seconds (track via cookie or JWT flag `just_wrote=true`).
  2. **Replication Checkpoint / GTID Tracking**: Return the transaction Global Transaction Identifier (GTID) in the API response. The read replica ensures it has caught up to that GTID before serving the query.

### Q63: Why does increasing the Database Connection Pool size often make database queries *slower*?
- **The Physics of Hardware**:
  - A database server has a fixed number of CPU cores (e.g. 16 cores) and disk spindles/NVMe channels.
  - When connection pool is set to 500, 500 OS threads compete for 16 cores.
  - The CPU spends more time performing **context switching, lock arbitration, and memory cache invalidation** than actually executing queries!
- **Little's Law**: `Throughput = Concurrency / Latency`. Keeping pool size small (e.g. 30–50 connections) keeps CPU utilization efficient and yields higher throughput with lower latency.

### Q64: How do you perform a Zero-Downtime Database Schema Migration on a table with 200M rows?
- **Dangerous**: `ALTER TABLE orders ADD COLUMN status_code VARCHAR(20) DEFAULT 'NEW';` locks the table for hours, causing a total site outage!
- **Expand / Contract (Parallel Run) Pattern**:
  1. **Phase 1**: Add the new column as `NULLABLE` without defaults (instant metadata update, no table lock).
  2. **Phase 2**: Deploy code that writes to *both* old and new columns.
  3. **Phase 3**: Run a background batch script migrating historical rows in small chunks (`WHERE id BETWEEN 1 AND 10000`).
  4. **Phase 4**: Switch read traffic to the new column.
  5. **Phase 5**: Remove writes to the old column and drop it.
- **Alternative Tools**: Use online schema migration utilities like **GitHub's `gh-ost`** or **Percona `pt-online-schema-change`**.

### Q65: How do you handle the "Celebrity / Super-Merchant" Hotspot in a Sharded Database?
- **Problem**: In a multi-tenant platform, 99.9% of merchants have 10 orders/day, but a merchant like "Gymshark" does 500,000 orders/hour. Storing Gymshark on a normal shard overwhelms that shard's disk and I/O.
- **Solution**:
  - **Dedicated Tenant Shards**: Flag large merchants in the routing directory. Route standard merchants to shared multi-tenant shards via consistent hashing, while VIP merchants are routed to dedicated, independently scaled database instances.

### Q66: How do you detect and resolve Database Deadlocks during concurrent transactions?
- **Detection**:
  - Engine automatically detects deadlocks (e.g. Postgres `deadlock_timeout`, MySQL InnoDB deadlock detector).
  - One transaction is chosen as the victim and aborted with error code `40P01` / `1213`.
- **Prevention Rules**:
  1. **Consistent Lock Acquisition Order**: Always lock rows in the exact same order across all code paths:
     ```sql
     -- Always sort IDs before locking!
     SELECT * FROM items WHERE id IN (2, 5, 8) ORDER BY id ASC FOR UPDATE;
     ```
  2. **Keep Transactions Short**: Acquire locks at the very end of the transaction immediately before commit.

### Q67: How do you design Cold Data Archiving for orders older than 2 years?
- **Architecture**:
  - 95% of customer queries target orders from the last 90 days.
  - A scheduled batch ETL job queries orders where `created_at < NOW() - INTERVAL '2 YEARS'` and status is terminal.
  - Export rows to compressed **Apache Parquet files on AWS S3 / Google Cloud Storage**.
  - Delete archived rows from OLTP database to free disk space and keep indexes compact in memory.
  - For historical analytics or rare customer inquiries, query S3 directly via **AWS Athena / Snowflake / BigQuery**.

### Q68: Soft Deletes vs. Hard Deletes: What are the pitfalls of `is_deleted = true`?
- **Pitfalls**:
  - `is_deleted` ruins unique indexes (e.g. unique constraint on `email` prevents user from signing up again after deleting account).
  - Queries must remember to append `WHERE is_deleted = false`, leading to accidental data leaks if a developer forgets.
  - Index bloat: Indexes must index both active and deleted rows.
- **Better Alternative**: Move deleted rows to an `orders_archive` / `users_deleted` table within a transaction, or use partial indexes:
  ```sql
  CREATE UNIQUE INDEX idx_users_email_active ON users (email) WHERE is_deleted = false;
  ```

### Q69: When should you pick PostgreSQL vs. MongoDB vs. Cassandra in an E-Commerce system?
- **PostgreSQL**: Users, Orders, Payments, Ledgers. (Requires ACID guarantees, foreign keys, and relational consistency).
- **Cassandra / ScyllaDB**: Time-series clicks, order tracking history, live event stream snapshots. (High append throughput, horizontal sharding, no transactions required).
- **MongoDB / DocumentDB**: Product Catalog with dynamic, polymorphic attributes (clothing has `size/color/fabric`, laptops have `RAM/CPU/ports`). Flexible schema avoids messy EAV (Entity-Attribute-Value) SQL tables.

### Q70: How do you prevent table locks when adding an index on a large table in production?
- In PostgreSQL: Always use `CONCURRENTLY`:
  ```sql
  CREATE INDEX CONCURRENTLY idx_orders_user_id ON orders (user_id);
  ```
  Standard `CREATE INDEX` takes a `SHARE` lock that blocks all incoming `INSERT`, `UPDATE`, and `DELETE` queries until the entire index build finishes!

---

## 8. Cart, Checkout & Session Management (Q71 – Q80)

### Q71: How do you handle Guest Cart to Logged-In User Cart Merge when items conflict?
- **Scenario**:
  - Guest Cart: 2 Laptops, 1 Shirt.
  - Logged-in Cart: 1 Laptop, 3 Books.
- **Merge Conflict Strategies**:
  1. **Quantity Summing with Max Stock Clamping**:
     - Laptops: `qty = min(2 + 1, max_allowed_per_user)` $\rightarrow$ 3 Laptops.
     - Shirt: 1 Shirt.
     - Books: 3 Books.
  2. **Timestamp Priority**: If cart allows only single quantity, take the most recently added item.
  3. **Atomic Execution**: Perform merge in a single Redis transaction / DB transaction and destroy the temporary guest session token.

### Q72: Where should you store Cart State: LocalStorage, Redis, or Relational Database?
| Storage Tier | Pros | Cons | Recommendation |
|---|---|---|---|
| **Client LocalStorage** | Zero server load | Disappears across devices; vulnerable to tampering | Good for offline drafts |
| **Relational DB** | Durable, easy joins | Every "Add to Cart" causes DB disk I/O; slow under high traffic | Bad for high scale |
| **Distributed Redis** | Sub-millisecond reads/writes, TTL support | Requires persistence configuration (AOF/RDB) | **Industry Standard** |
- **Hybrid Best Practice**: Store active cart in **Redis Hash** (`HSET cart:<user_id> sku_101 2`). Asynchronously sync to DB when user progresses to checkout or on periodic backup.

### Q73: What is Abandoned Cart handling and how do you track it without polling millions of carts?
- **Event-Driven Architecture**:
  - When user adds item or updates cart: Set Redis key with 2-hour TTL: `SET cart:abandoned:<user_id> "active" EX 7200`.
  - Also store timestamp in a Redis Sorted Set: `ZADD carts_pending_expiry <timestamp + 7200> <user_id>`.
  - A background worker polls the sorted set for entries with score $\le$ `CurrentTime`:
    ```redis
    ZRANGEBYSCORE carts_pending_expiry 0 <CurrentTime>
    ```
  - For expired entries: If no order was placed, emit `CartAbandonedEvent` to Kafka $\rightarrow$ Trigger marketing email with discount coupon.

### Q74: A product price changes while the item is sitting in a user's cart. How do you handle this at checkout?
- **Never blindly charge old price or silent increase**:
  1. Cart only stores `sku_id` and `quantity`, **never the canonical price**.
  2. When the checkout page loads, re-fetch live price from Catalog Service.
  3. If price changed:
     - Flag item on checkout screen: *"Item price changed from $50 to $55"*.
     - Require user to explicitly click "Acknowledge & Proceed" before payment can be authorized.

### Q75: How do you design a Coupon / Promo Code Engine that prevents a "Single-Use" coupon from being redeemed simultaneously?
- **The Vulnerability**: User shares a single-use coupon on Reddit. 5,000 users apply it at the exact same millisecond.
- **Atomic Concurrency Protection**:
  1. Track coupon usage in Redis with atomic decrement or atomic set:
     ```redis
     HSETNX coupon:WELCOME50:used <user_id> "1"
     ```
     If returns 0, the user already applied it.
  2. For coupons with a global usage limit (e.g. "First 100 users get 50% off"):
     ```lua
     local count = redis.call('incr', KEYS[1])
     if count <= 100 then
         return 1
     else
         redis.call('decr', KEYS[1])
         return 0
     end
     ```
  3. At database commit time: Enforce unique constraint `UNIQUE (coupon_id, user_id)`.

### Q76: What is a "Virtual Waiting Room" (Queue-it / Waiting Queue) and how does it protect checkout?
- **Concept**:
  - Flash sale capacity is 1,000 checkouts/second. 50,000 users attempt to enter checkout.
  - Intercept users at the Edge/API Gateway before they reach internal microservices.
  - Assign each user an encrypted **Queue Ticket** with position number stored in Redis.
  - Client polls queue status or receives updates via WebSocket: *"You are #450 in line. Estimated wait: 2 mins"*.
  - When capacity opens, server admits user with a signed JWT token valid for 10 minutes to complete checkout.

### Q77: JWT Stateless Tokens vs. Server-Side Redis Sessions: Which is better for E-Commerce?
- **JWT (Stateless)**:
  - *Pros*: No server lookup needed. Great for high-scale mobile API authentication.
  - *Cons*: Impossible to revoke instantly if account is compromised or user logs out (unless maintaining a centralized blacklist, which defeats the stateless benefit!).
- **Redis Sessions**:
  - *Pros*: Instant revocation, can inspect active sessions, easy to update permissions.
  - *Cons*: Extra network round-trip to Redis on every API call.
- **Hybrid Best Practice**: Use short-lived JWT access tokens (10 minutes) + server-side refresh tokens stored in Redis.

### Q78: How do you handle Cart Checkout Throttling when inventory verification is at max capacity?
- Use **Token Bucket Rate Limiting** at the checkout endpoint:
  - If token bucket for `/api/checkout` is empty, return HTTP `429 Too Many Requests` with a `Retry-After: 3` header.
  - Display friendly UI message: *"Traffic is high. We are reserving your place in line, retrying in 3 seconds..."*.

### Q79: How do you synchronize cart state across multiple devices in real-time?
- **Architecture**:
  - User adds an item to cart on Laptop. Mobile App should show the updated cart badge immediately.
  - When Cart Service processes update: Publishes event to Kafka `cart-updates` with key `user_id`.
  - A **WebSocket Gateway** cluster (connected to active mobile/web sessions) consumes the event.
  - Looks up user's active WebSocket connection in Redis session registry and pushes a lightweight event `{"action": "CART_SYNC", "item_count": 4}` directly to the client.

### Q80: How do you compute real-time shipping costs and taxes without adding 2 seconds of latency to checkout?
- **Strategies**:
  - Pre-calculate estimated shipping rates based on coarse user location (state/city) during cart view.
  - Cache third-party tax tables (Avalara / Vertex) locally in an in-memory cache / Redis instance.
  - Only call external live shipping carrier API when exact street address is submitted on the final review step.

---

## 9. Product Catalog, Search & Recommendations (Q81 – Q90)

### Q81: How do you synchronize Product Catalog data from PostgreSQL to Elasticsearch with zero data loss?
- **Anti-Pattern**: Updating PostgreSQL and then calling Elasticsearch REST API directly from the microservice. (Network failure causes index drift).
- **Production Solution — CDC + Outbox**:
  ```
  [Postgres DB] ──► [Write-Ahead Log (WAL)] ──► [Debezium CDC]
                                                      │
                                                      ▼
  [Elasticsearch Index] ◄── [ES Sink Consumer] ◄── [Kafka Topic]
  ```
  - Debezium reads every `INSERT/UPDATE/DELETE` from Postgres WAL.
  - Events stream to Kafka `product-cdc` topic.
  - Kafka consumer batches updates and executes Elasticsearch `_bulk` API.
  - Guaranteed eventual consistency and replayability.

### Q82: How do you solve the eventual consistency gap between Search results and the Product Details Page (PDP)?
- **Problem**: Elasticsearch shows product is "In Stock" with price $40. Customer clicks result; PDP queries Postgres and shows "Out of Stock" or price $50. Customer feels deceived.
- **Remediation**:
  - Display non-critical search listings with a disclaimer badge (*"Prices & availability subject to change"*).
  - The moment user clicks a product and lands on the PDP, always load real-time stock and price directly from the primary Cache/DB.
  - If a price drop or stock-out occurs, emit high-priority CDC event to refresh the Elasticsearch document within <1 second.

### Q83: How do you design multi-faceted filtering (Brand, Size, Color, Price Range) at scale?
- **Elasticsearch Aggregations**:
  - Use Elasticsearch `bool` query with `filter` context (filters are automatically cached in memory by Lucene, bypassing scoring overhead).
  - Execute `terms` aggregations on faceted fields:
    ```json
    {
      "query": { "bool": { "filter": [{ "term": { "category": "shoes" }}]}},
      "aggs": {
        "brands": { "terms": { "field": "brand.keyword" }},
        "sizes": { "terms": { "field": "available_sizes" }},
        "price_ranges": { "histogram": { "field": "price", "interval": 50 }}
      }
    }
    ```
  - Sub-50ms response across millions of SKUs.

### Q84: How do you implement a Real-Time Auto-Complete / Type-Ahead Search suggestion service?
- **Architecture Options**:
  1. **Trie Data Structure in Memory**: Fast prefix lookups $O(L)$, where $L$ is length of search prefix. Requires syncing trie across instances.
  2. **Elasticsearch Completion Suggester**: Uses an in-memory FST (Finite State Transducer) optimized for sub-millisecond prefix matching.
  3. **Redis Sorted Sets (ZSET)**:
     - Store prefixes with score = search popularity: `ZADD search:suggestions 1500 "iphone 15"`.
     - Query with `ZRANGEBYLEX` or `ZREVRANGEBYSCORE`.

### Q85: How do you handle Elasticsearch Circuit Breakers and Out-Of-Memory (OOM) errors during traffic spikes?
- **Root Cause**: Large aggregation queries (e.g. deep parent-child aggregations or sorting on un-indexed text fields) load massive amounts of data into JVM Heap `Fielddata`.
- **Protection**:
  - Enforce `indices.breaker.fielddata.limit: 40%` and `indices.breaker.total.use_real_memory: true`.
  - Prohibit `fielddata=true` on `text` fields; always aggregate on `keyword` fields (which use disk-backed doc values).
  - Enforce pagination limits (never allow `from + size > 10000`; use `search_after` for deep scrolling).

### Q86: How do you model complex Product Variants (Colors, Sizes, Bundles) in the database?
- **Document Model (E-Commerce Catalog)**:
  - `Product` (Parent): Title, Brand, Description, Base Category.
  - `SKU` (Variant / Child): SKU Code, Barcode, Specific Color, Specific Size, Price, Inventory ID, Images.
  - Storing as a nested JSON document in MongoDB/Postgres allows querying both parent details and checking specific SKU availability in a single read.

### Q87: How do you update 100,000 product prices during a midnight sale without overloading search and DB?
- **Execution Strategy**:
  1. **Pre-stage Changes**: Store upcoming promotional prices in a `scheduled_price_changes` table hours before the event.
  2. **Atomic Activation via Flag**: Instead of running 100,000 DB updates at 00:00, associate prices with a `sale_id`. At 00:00:00, flip `SET active_sale_id = "MIDNIGHT_2026"` in Redis.
  3. Catalog pricing service reads promotional price if `active_sale_id` matches.
  4. Search indices are pre-indexed on a **blue/green shadow index** (`products_v2`) and switched via an **Elasticsearch Index Alias** swap in 1 millisecond.

### Q88: How does an E-Commerce Recommendation Engine balance Real-Time Signals vs. Batch Collaborative Filtering?
- **Two-Tower Architecture**:
  - **Offline Batch (Heavy ML)**: Apache Spark / GPU cluster processes months of order history nightly to compute user/item embeddings via Matrix Factorization. Embeddings are stored in a **Vector Database (Milvus / Pinecone / pgvector)**.
  - **Near Real-Time (Streaming)**: Flink listens to `item_viewed` events on Kafka. Captures items viewed in the last 5 minutes, generates a real-time query vector, and performs Approximate Nearest Neighbor (ANN) search to deliver instant recommendations.

### Q89: How do you prevent "Index Bloat" when products are deleted or updated thousands of times per day?
- **Mechanism**: Lucene documents are immutable. An update is actually a `DELETE` (mark deleted) + `INSERT` of a new document.
- **Maintenance**:
  - Run scheduled **Force Merge** operations during low-traffic windows:
    ```http
    POST /products/_forcemerge?max_num_segments=1
    ```
  - Purges deleted documents from disk, releases memory, and improves query speeds.

### Q90: How do you handle catalog deduplication when multiple marketplace sellers list the exact same product?
- **Catalog Matching Pipeline**:
  - Compute standardized hashes based on universal identifiers: **UPC, EAN, ISBN, or MPN (Manufacturer Part Number)**.
  - If UPC matches: Merge under a single canonical Master Product ID.
  - Sellers attach as competing inventory/price offers under the same product listing (Buy Box model, similar to Amazon).

---

## 10. System Resilience, Chaos Engineering & Observability (Q91 – Q100)

### Q91: How do you diagnose a Memory Leak in a Java/Node.js e-commerce microservice under high load?
- **Diagnostic Runbook**:
  1. **Telemetry Check**: Memory graphs show a steady sawtooth pattern where GC fails to reclaim heap after each cycle, eventually causing `java.lang.OutOfMemoryError: Java heap space`.
  2. **Generate Heap Dump on OOM**:
     ```bash
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/oom.hprof
     ```
  3. **Analyze with Eclipse Memory Analyzer (MAT)**:
     - Check the **Leak Suspects Report**. Look for the largest retaining objects.
     - Common culprits: Unbounded in-memory collections (e.g. static HashMaps caching orders without eviction), unclosed database/network connections, thread locals not cleaned up in thread pool environments.
  4. **Immediate Mitigation**: Increase pod replicas and restart instances via Kubernetes liveness probes until code fix is deployed.

### Q92: What are the RED and USE observability metrics and why are they superior to basic CPU utilization alerts?
- **RED (Focus on Services/APIs)**:
  - **R**ate: Number of requests per second.
  - **E**rrors: Number of failed requests per second (HTTP 5xx).
  - **D**uration: Time each request takes (Latency distribution: p50, p95, p99).
- **USE (Focus on Infrastructure Resources)**:
  - **U**tilization: Percentage of time resource was busy (CPU, Disk I/O).
  - **S**aturation: Queue depth waiting for resource (Thread pool queue, DB run queue).
  - **E**rrors: Device/hardware error count.
- *Why CPU alerts fail*: A service can crash or return 100% errors while CPU is at 5% (e.g. thread deadlock or missing config). RED metrics reflect real customer pain immediately.

### Q93: How do you implement Distributed Tracing with OpenTelemetry across 25 microservices?
- **Mechanics**:
  - Ingress API Gateway generates a `TraceId` (unique per user request) and `SpanId` (unique per operation).
  - Propagate context downstream using the W3C TraceContext standard HTTP headers:
    ```http
    traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
    ```
  - Microservices extract `traceparent`, create a child span, and inject into outgoing gRPC/HTTP/Kafka headers.
  - Agents export spans asynchronously to **OpenTelemetry Collector** $\rightarrow$ visualized in **Jaeger / Grafana Tempo**.
  - Provides a complete end-to-end flamegraph showing every millisecond spent across all 25 services.

### Q94: What is the "Bulkhead Pattern" and how does it prevent cascading failures in checkout?
- **Origin**: Named after watertight compartments in ships. If one compartment punctures, water is contained and the ship stays afloat.
- **Software Implementation**:
  - Allocate isolated resources (thread pools, semaphores, connection pools) for each external dependency.
  - *Example*: Checkout Service has a pool of 100 threads.
    - Reserve 60 threads for core Order/Payment operations.
    - Reserve 20 threads for Inventory.
    - Reserve 10 threads for Third-party Shipping validation.
    - Reserve 10 threads for Email/SMS notification.
  - If the Shipping API stalls completely, only its 10 threads block; the remaining 90 threads continue processing orders unaffected.

### Q95: How do you design an API Rate Limiting strategy with differentiated tiers for an e-commerce platform?
- **Multi-Tier Rate Limiting Policy**:
  1. **Tier 1 (Public Endpoints - Search / Catalog)**: IP-based rate limiting (e.g. 100 requests/minute per IP).
  2. **Tier 2 (Sensitive Endpoints - Login / Register)**: Strict rate limiting + CAPTCHA trigger (e.g. 5 attempts/minute per IP/username to block brute-force attacks).
  3. **Tier 3 (Checkout / Buy Now)**: User-ID based rate limiting (e.g. 2 requests/second to prevent bot automation).
  4. **Tier 4 (Third-Party Merchant APIs)**: API-Key token bucket based on subscription tier (e.g. 5,000 req/min for Enterprise tier).
- Implemented using Redis sliding window log / token bucket algorithms at the API Gateway.

### Q96: Blue-Green vs. Canary Deployments: Which is best for an E-Commerce release during peak season?
- **Blue-Green**:
  - Two identical production environments (Blue = Active, Green = Idle). Deploy new version to Green, run smoke tests, switch router traffic instantly.
  - *Trade-off*: Requires 2x server infrastructure; abrupt 100% traffic shift can expose hidden bugs to all users immediately.
- **Canary (Recommended for Peak Season)**:
  - Route 2% of real user traffic to the new version.
  - Monitor RED metrics (p99 latency, 5xx error rate) for 15 minutes.
  - If error rate climbs >0.1%, automatically abort and route 100% back to baseline.
  - If healthy, incrementally scale traffic: 2% $\rightarrow$ 10% $\rightarrow$ 25% $\rightarrow$ 50% $\rightarrow$ 100%.

### Q97: What is Chaos Engineering and what experiments should you run before a Black Friday sale?
- **Principle**: Proactively injecting failures into a production/staging system to uncover systemic weaknesses before real disasters happen (Netflix Chaos Monkey).
- **Essential E-Commerce Chaos Experiments**:
  1. **Kill 1 of 3 Redis Cluster Masters**: Verify that automatic failover promotes a replica within 5 seconds without user cart data loss.
  2. **Inject 500ms Network Latency on Payment Gateway**: Verify that circuit breakers trip and timeouts propagate gracefully without thread pool exhaustion.
  3. **Kill 50% of Kafka Brokers**: Verify that producers and consumers re-connect without losing order events.
  4. **Simulate Database Primary Failover**: Verify that connection pools gracefully reconnect to the newly promoted primary.

### Q98: How do you design "Kill Switches" (Feature Flags) to dynamically sheds load under extreme traffic?
- **Implementation**:
  - Use a centralized feature flag system (LaunchDarkly / Unleash / Redis config).
  - Wrap non-essential services in conditional blocks:
    ```java
    if (featureFlags.isEnabled("ENABLE_RECOMMENDATIONS")) {
        return recommendationService.getPersonalizedItems(userId);
    } else {
        return Collections.emptyList(); // Instant no-op fallback
    }
    ```
  - Under sudden 5x traffic spikes, SREs flip flags via a dashboard to disable:
    - Personalized recommendations
    - Real-time product reviews & ratings
    - Animated UI banners
    - Address auto-completion
  - Recovers 30–40% of database and compute capacity instantly without a code deployment or restart.

### Q99: How do you set up an SLA / SLO / SLI framework for an E-Commerce Checkout service?
- **Definitions**:
  - **SLI (Service Level Indicator)**: The quantified metric (e.g. *Successful Checkout Ratio* over 5 minutes).
  - **SLO (Service Level Objective)**: The internal target (e.g. *99.95% of checkout requests must return HTTP 2xx within 1000ms over rolling 30 days*).
  - **SLA (Service Level Agreement)**: The contractual commitment to external customers/merchants with financial penalty penalties if breached (e.g. 99.9% uptime).
- **Error Budget**: An SLO of 99.9% allows 0.1% downtime (~43 minutes/month). If error budget is exhausted, all new feature releases are frozen; engineering shifts 100% focus to reliability and tech debt.

### Q100: What is your Disaster Recovery (DR) strategy for a multi-region e-commerce platform?
- **Architecture**:
  - **Active-Passive (Warm Standby)**: Primary region serves 100% traffic; secondary region replicates data. On failure, DNS switches to secondary (RTO: 10–30 min, RPO: seconds).
  - **Active-Active (Multi-Region)**: Both Region A (US-East) and Region B (US-West) serve live traffic.
- **Handling Data in Active-Active**:
  - Stateless services scale across both regions seamlessly.
  - **Data Partitioning**: User accounts and carts are pinned to their home region using **GeoDNS / Anycast IP**.
  - Cross-region asynchronous replication (PostgreSQL logical replication / CockroachDB / Cassandra multi-datacenter).
  - If Region A experiences a total cloud outage, Region B takes over traffic. RTO < 60 seconds.

---

> 💡 **Tip for Candidates**: When answering system design questions in an interview, structure your responses into:
> 1. **Clarifying Requirements & Constraints** (Read/Write ratios, latency SLOs, traffic volume).
> 2. **High-Level Design & Component Interaction**.
> 3. **Deep Dive into Failure Scenarios & Edge Cases** (Network partitions, race conditions, timeout cascades).
> 4. **Concrete Trade-Offs** (Why you picked Redis over Memcached, or Saga over 2PC).
