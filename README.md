<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f0c29,50:302b63,100:24243e&height=200&section=header&text=Systems%20Design%20Learning&fontSize=42&fontColor=ffffff&fontAlignY=38&desc=Production%20Architecture%20%E2%80%A2%20AI-Era%20Distributed%20Systems%20%E2%80%A2%20Open%20Source&descAlignY=58&descSize=16&animation=fadeIn" />

<br/>

[![GitHub Stars](https://img.shields.io/github/stars/CyberVerve07/Systems_Designs_learning?style=for-the-badge&logo=github&color=0f0c29&labelColor=302b63&label=⭐%20Stars)](https://github.com/CyberVerve07/Systems_Designs_learning/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/CyberVerve07/Systems_Designs_learning?style=for-the-badge&logo=git&color=0f0c29&labelColor=302b63&label=🍴%20Forks)](https://github.com/CyberVerve07/Systems_Designs_learning/forks)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen?style=for-the-badge&logo=github&color=0f0c29&labelColor=1a6b3c)](https://github.com/CyberVerve07/Systems_Designs_learning/pulls)
[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&color=0f0c29&labelColor=b07219)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge&logo=opensourceinitiative&color=0f0c29&labelColor=f7c948&logoColor=000)](LICENSE)

<br/>

[![CI Status](https://img.shields.io/github/actions/workflow/status/CyberVerve07/Systems_Designs_learning/ci.yml?branch=main&style=for-the-badge&logo=githubactions&label=CI%20Build&color=0f0c29&labelColor=24243e)](https://github.com/CyberVerve07/Systems_Designs_learning/actions)

</div>

---

## ⚡ Why System Design in the Age of AI?

<div align="center">

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                                                                                ║
║   "Every AI model that speaks to billions relies on a distributed system        ║
║    built by engineers who understood scalability before they wrote a            ║
║    single line of machine learning code."                                       ║
║                                                                                ║
╚══════════════════════════════════════════════════════════════════════════════════╝
```

</div>

We are living through the most consequential shift in computing history. AI is no longer a research
curiosity — it is the backbone of products used by billions daily. But here is what gets overlooked:

> **The intelligence of an AI system is only as reliable as the distributed infrastructure
> that serves it.**

ChatGPT, Gemini, Claude, Midjourney — every one of these systems is, at its core, a **System Design problem**:

- **How do you serve 100 million requests per day** to an LLM inference endpoint without it collapsing?
- **How does a vector database** retrieve semantically similar embeddings in under 10ms across billions of records?
- **How do you stream AI-generated tokens** to a user's screen in real-time without buffering?
- **How do RAG pipelines** orchestrate document retrieval, model inference, and result caching at scale?

**System Design is the engineering layer that makes AI real.** An AI engineer who cannot design
scalable systems will build impressive demos — an AI engineer who *can* will build products that last.

This repository exists to build that foundation — from first principles to production-grade architecture,
documented publicly for every engineer on this journey.

---

## 🎯 What This Repository Is

<table>
<tr>
<td width="50%" valign="top">

### 📖 For Learners
A **structured, day-by-day curriculum** covering distributed systems from the ground up.
Every topic includes:
- 📄 **In-depth theory notes** with ASCII architecture diagrams
- ☕ **Working Java implementations** with Maven
- 🔬 **Real-world analogies** connecting theory to production systems like Netflix, Uber, and Instagram

</td>
<td width="50%" valign="top">

### 🧑‍💻 For Interview Candidates
A **production-grade interview preparation resource** featuring:
- 💥 Real failure scenarios (Kafka consumer crashes, DB deadlocks, cache stampedes)
- 🧮 Back-of-envelope calculations for every major system
- 🏗️ End-to-end architecture case studies (Twitter/X, Instagram Engagement Engine)

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🤝 For Contributors
A **100% open-source community project** where you can:
- Add new case studies and architecture deep-dives
- Submit interview scenarios and trade-off analyses
- Contribute Java simulation modules

</td>
<td width="50%" valign="top">

### 🤖 For AI Engineers
Foundational distributed systems knowledge that directly applies to:
- LLM inference serving architecture
- Vector database and embedding pipeline design
- Real-time streaming and event-driven AI pipelines
- Multi-tenant AI platform scalability

</td>
</tr>
</table>

---

## 🌟 Special Highlights

<div align="center">

| 🏆 Resource | Description | Link |
|:---:|:---|:---:|
| 🐦 **Twitter / X Architecture** | End-to-end production design — Multi-region, Kafka fan-out, Hybrid timeline, Cassandra, Elasticsearch. Includes HD visual diagrams. | [→ Open](theory/twitter_x_system_design/README.md) |
| ❤️ **Social Engagement Engine** | Like/Dislike/Comments at 1M+ writes/sec — Sharded counters, Write-behind batching, Cursor pagination, Anti-brigading. Instagram + YouTube + Twitter. | [→ Open](theory/social_engagement_service/README.md) |
| 🛍️ **100 E-Commerce Design Questions** | Production failure scenarios (3+ YOE level) — Kafka crashes, inventory overselling, payment idempotency, distributed transactions. | [→ Open](theory/interview_prep/ecommerce_100_system_design_questions.md) |
| 🎥 **Real-Time Conferencing** | WebRTC + SFU architecture at scale (Zoom/Google Meet style) — Mediasoup orchestration, TURN/STUN, MCU vs SFU tradeoffs. | [→ Open](theory/real_time_conferencing/real_time_conferencing_system_design.md) |

</div>

---

## 📚 Learning Curriculum

<details>
<summary><b>📦 Day 01 — Servers & Load Balancers</b></summary>

**Theory:** [`theory/load_balancer/load_balancer.md`](theory/load_balancer/load_balancer.md) · **Code:** [`code/load_balancer/`](code/load_balancer/)

| Concept | Detail |
|---|---|
| Traffic distribution algorithms | Round Robin, Least Connections, IP Hash, Weighted |
| Health Checks & Failover | Active vs Passive health probing, circuit-open behavior |
| Layer 4 vs Layer 7 LB | TCP passthrough vs HTTP-aware routing |
| Horizontal Scaling | Stateless service design, sticky sessions pitfall |

</details>

<details>
<summary><b>🗄️ Day 02 — Caching Strategies</b></summary>

**Theory:** [`theory/caching/caching_strategies.md`](theory/caching/caching_strategies.md) · **Code:** [`code/caching/redis_cache_example/`](code/caching/redis_cache_example/)

| Concept | Detail |
|---|---|
| Caching Topologies | Client-side, CDN, Server-side, Distributed |
| Write Strategies | Cache-Aside, Write-Through, Write-Behind, Refresh-Ahead |
| Eviction Policies | LRU, LFU, FIFO, TTL-based expiry |
| Failure Patterns | Cache Stampede, Thundering Herd, Cache Poisoning |

</details>

<details>
<summary><b>🏛️ Day 03 — Database Design & Sharding</b></summary>

**Theory:** [`theory/database_design/database_design.md`](theory/database_design/database_design.md) · **Code:** [`code/database_design/sharding_example/`](code/database_design/sharding_example/)

| Concept | Detail |
|---|---|
| SQL vs NoSQL | ACID vs BASE — when to pick which |
| Replication | Leader-Follower, Multi-Leader, Leaderless (Dynamo-style) |
| Sharding Strategies | Hash-based, Range-based, Directory-based |
| Hot Partition Problem | Consistent Hashing, Virtual Nodes |

</details>

<details>
<summary><b>🌐 Day 04 — API Gateway & Microservices</b></summary>

**Theory:** [`theory/api_gateway_microservices/api_gateway_microservices.md`](theory/api_gateway_microservices/api_gateway_microservices.md) · **Code:** [`code/api_gateway_microservices/api_gateway_example/`](code/api_gateway_microservices/api_gateway_example/)

| Concept | Detail |
|---|---|
| Gateway Responsibilities | Routing, Auth, Rate-Limiting, SSL termination |
| Service Mesh | Istio-style sidecar proxies, mTLS between services |
| Communication | Synchronous (gRPC, REST) vs Asynchronous (Events) |
| Data Isolation | Database-per-service pattern, eventual consistency |

</details>

<details>
<summary><b>🔐 Day 05 — Authentication & Authorization</b></summary>

**Theory:** [`theory/authentication_authorization/authentication_authorization.md`](theory/authentication_authorization/authentication_authorization.md) · **Code:** [`code/authentication_authorization/jwt_example/`](code/authentication_authorization/jwt_example/)

| Concept | Detail |
|---|---|
| Auth Mechanisms | Password, MFA, OAuth 2.0, API Keys, SAML |
| JWT Deep-Dive | Header.Payload.Signature, RS256 vs HS256, Refresh rotation |
| Authorization Models | RBAC, ABAC, ACL — trade-offs at scale |
| Security | bcrypt hashing, Token revocation at scale |

</details>

<details>
<summary><b>🚦 Day 06 — Rate Limiting & Throttling</b></summary>

**Theory:** [`theory/rate_limiter/rate_limiter.md`](theory/rate_limiter/rate_limiter.md) · **Code:** [`code/rate_limiter/rate_limiter_example/`](code/rate_limiter/rate_limiter_example/)

| Concept | Detail |
|---|---|
| Algorithms | Token Bucket, Leaky Bucket, Sliding Window Counter/Log |
| Distributed Rate Limiting | Redis Lua scripting, race condition prevention |
| Placement | Client-side, Gateway, Backend service-level |
| Edge Cases | Burst allowance, per-user vs global throttles |

</details>

<details>
<summary><b>📨 Day 07 — Message Queues & Event-Driven Architecture</b></summary>

**Theory:** [`theory/message_queues/message_queues_deep_dive.md`](theory/message_queues/message_queues_deep_dive.md) · **Code:** [`code/message_queues/message_broker_example/`](code/message_queues/message_broker_example/)

| Concept | Detail |
|---|---|
| Messaging Models | Point-to-Point (Queue) vs Pub/Sub (Topic/Fan-out) |
| Reliability | ACK/NACK, At-least-once, Exactly-once, Dead Letter Queue |
| Backpressure | Consumer lag detection, flow control, rate-matching |
| Kafka Consumer Groups | Partition assignment, rebalancing, offset management |

</details>

<details>
<summary><b>❄️ Day 08 — Distributed Unique ID Generator (Snowflake)</b></summary>

**Theory:** [`theory/distributed_id_generator/distributed_id_generator.md`](theory/distributed_id_generator/distributed_id_generator.md) · **Code:** [`code/distributed_id_generator/snowflake_example/`](code/distributed_id_generator/snowflake_example/)

| Concept | Detail |
|---|---|
| Why Not Auto-Increment | Sharding incompatibility, ordering failure |
| Twitter Snowflake | 64-bit = 41ms + 10 node + 12 sequence |
| Clock Drift Protection | NTP skew detection, monotonic clock fallback |
| Alternatives | UUID v4, ULID, MongoDB ObjectId |

</details>

<details>
<summary><b>🌊 Day 09 — Apache Kafka & Event Streaming</b></summary>

**Theory:** [`theory/kafka/apache_kafka_basics.md`](theory/kafka/apache_kafka_basics.md) · **Code:** [`code/kafka/basic_example/`](code/kafka/basic_example/)

| Concept | Detail |
|---|---|
| Core Architecture | Brokers, Topics, Partitions, Replication Factor |
| Producer Semantics | Partition Keys, ordering guarantees, compression |
| Consumer Mechanics | Offset commit, group rebalancing, lag monitoring |
| Local Dev | Docker Compose Kafka + Zookeeper setup |

</details>

<details>
<summary><b>🔔 Day 10 — Notification System Architecture</b></summary>

**Theory:** [`theory/notification_system/notification_system.md`](theory/notification_system/notification_system.md) · **Code:** [`code/notification_system/`](code/notification_system/)

| Concept | Detail |
|---|---|
| Multi-Channel Pipeline | SMS, Push (APNs/FCM), Email, In-App — unified fanout |
| Priority Queuing | Critical alerts vs promotional messages routing |
| Deduplication | Idempotency keys, sliding window dedup store |
| User Preferences | Per-channel opt-out, timezone-aware delivery |

</details>

<details>
<summary><b>🏗️ Day 11 — Microservices Patterns (CQRS, Saga, Outbox, Circuit Breaker)</b></summary>

**Theory:** [`theory/microservices_patterns/`](theory/microservices_patterns/) · **Code:** [`code/microservices_patterns/`](code/microservices_patterns/)

| Pattern | Purpose |
|---|---|
| **CQRS** | Separate read/write models — optimized query projections |
| **Saga** | Distributed transactions without 2PC — Choreography vs Orchestration |
| **Outbox** | Atomically publish events alongside DB writes (CDC via Debezium) |
| **Circuit Breaker** | Fail-fast against degraded downstream services |
| **Event-Driven Architecture** | Decoupled domain events, eventual consistency |

</details>

<details>
<summary><b>📹 Day 12 — Real-Time Video Conferencing (WebRTC + SFU)</b></summary>

**Theory:** [`theory/real_time_conferencing/real_time_conferencing_system_design.md`](theory/real_time_conferencing/real_time_conferencing_system_design.md) · **Code:** [`code/real_time_conferencing/`](code/real_time_conferencing/)

| Concept | Detail |
|---|---|
| WebRTC Mechanics | ICE/STUN/TURN negotiation, SDP offer-answer exchange |
| MCU vs SFU | Trade-offs — server compute vs client bandwidth |
| Mediasoup SFU | Room/Router/Transport/Producer/Consumer orchestration |
| Signaling Service | WebSocket-based room management and peer coordination |

</details>

---

## 🗂️ Repository Structure

```
Systems_Designs_learning/
│
├── 📂 theory/                              ← Architecture notes & case studies
│   ├── load_balancer/
│   ├── caching/
│   ├── database_design/
│   ├── api_gateway_microservices/
│   ├── authentication_authorization/
│   ├── rate_limiter/
│   ├── message_queues/
│   ├── distributed_id_generator/
│   ├── kafka/
│   ├── notification_system/
│   ├── microservices_patterns/            ← CQRS, Saga, Outbox, EDA, Circuit Breaker
│   ├── real_time_conferencing/            ← WebRTC + SFU system design
│   ├── twitter_x_system_design/           ← Case Study: Twitter/X ⭐
│   │   ├── assets/                        ← HD Architecture diagrams
│   │   └── README.md
│   ├── social_engagement_service/         ← Case Study: Likes/Comments/Dislikes ⭐
│   │   └── README.md
│   └── interview_prep/                    ← 100-question production interview bank
│       └── ecommerce_100_system_design_questions.md
│
├── 📂 code/                               ← Working Java implementations (Maven)
│   ├── load_balancer/
│   ├── caching/redis_cache_example/
│   ├── database_design/sharding_example/
│   ├── api_gateway_microservices/
│   ├── authentication_authorization/jwt_example/
│   ├── rate_limiter/
│   ├── message_queues/
│   ├── distributed_id_generator/snowflake_example/
│   ├── kafka/basic_example/
│   ├── notification_system/
│   ├── microservices_patterns/
│   └── real_time_conferencing/
│
├── .github/
│   ├── workflows/ci.yml                   ← GitHub Actions CI (Build & Verify)
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
│
├── CONTRIBUTING.md
├── LICENSE
└── README.md                              ← You are here
```

---

## 🚀 Getting Started

### Prerequisites

```bash
# Required
Java 17+     →  https://adoptium.net/
Maven 3.8+   →  https://maven.apache.org/
Docker       →  https://www.docker.com/   (for Redis, Kafka)
```

### Run Any Code Example

```bash
# 1. Clone the repository
git clone https://github.com/CyberVerve07/Systems_Designs_learning.git
cd Systems_Designs_learning

# 2. Navigate to any topic
cd code/rate_limiter/rate_limiter_example

# 3. Build and run
mvn clean compile
mvn exec:java -Dexec.mainClass="com.systemdesign.ratelimiter.RateLimiterDemo"
```

### Start External Services

```bash
# Redis (for Caching & Rate Limiting)
docker run -d -p 6379:6379 --name redis redis:latest

# Kafka + Zookeeper (for Event Streaming examples)
cd code/kafka/basic_example
docker-compose up -d
```

---

## ✅ Progress Tracker

| # | Topic | Theory | Code | Case Study |
|:---:|:---|:---:|:---:|:---:|
| 01 | Servers & Load Balancers | ✅ | ✅ | — |
| 02 | Caching Strategies & Redis | ✅ | ✅ | — |
| 03 | Database Design & Sharding | ✅ | ✅ | — |
| 04 | API Gateway & Microservices | ✅ | ✅ | — |
| 05 | Authentication & Authorization (JWT) | ✅ | ✅ | — |
| 06 | Rate Limiting & Throttling | ✅ | ✅ | — |
| 07 | Message Queues & Event-Driven Arch | ✅ | ✅ | — |
| 08 | Distributed Unique ID (Snowflake) | ✅ | ✅ | — |
| 09 | Apache Kafka & Event Streaming | ✅ | ✅ | — |
| 10 | Notification System Architecture | ✅ | ✅ | — |
| 11 | Microservices Patterns (CQRS, Saga…) | ✅ | ✅ | — |
| 12 | Real-Time Video Conferencing (WebRTC) | ✅ | ✅ | — |
| — | **Case Study: Twitter / X Architecture** | ✅ | — | ✅ |
| — | **Case Study: Social Engagement Engine** | ✅ | — | ✅ |
| — | **100 Production Interview Questions** | ✅ | — | ✅ |

---

## 🤝 Contributing

Contributions are warmly welcomed from engineers at every level.

```
📌 Ways to Contribute
├── 📄 Add a system design case study (e.g., Uber Surge Pricing, Netflix Recommendation)
├── ☕ Contribute a Java simulation module (e.g., Consistent Hashing ring, Vector Clocks)
├── ❓ Add production interview scenarios to the question bank
├── 🔧 Fix issues, improve explanations, update dependencies
└── 🎨 Add architecture diagrams (Mermaid.js preferred)
```

**To get started:**
1. Read the **[Contribution Guidelines](CONTRIBUTING.md)**
2. Check open **[Issues](https://github.com/CyberVerve07/Systems_Designs_learning/issues)**
3. Fork → Branch → Commit → Pull Request

---

## 📖 Recommended Reading

| Resource | Why It Matters |
|:---|:---|
| [System Design Primer](https://github.com/donnemartin/system-design-primer) | The canonical open-source reference — covers every major concept |
| **Designing Data-Intensive Applications** — Martin Kleppmann | The definitive textbook on distributed systems (read this cover to cover) |
| [Netflix Tech Blog](https://netflixtechblog.com/) | Real production decisions at billion-user scale |
| [Uber Engineering Blog](https://www.uber.com/en-US/blog/engineering/) | Microservices, geospatial indexing, real-time systems |
| [AWS Architecture Blog](https://aws.amazon.com/blogs/architecture/) | Cloud-native patterns and reference architectures |
| [High Scalability](http://highscalability.com/) | Architecture breakdowns of real systems |

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:24243e,50:302b63,100:0f0c29&height=120&section=footer&animation=fadeIn" />

<br/>

**Built in public · Open source · Contributions welcome**

*If this helped you, consider giving it a ⭐ — it helps others discover the resource.*

[![GitHub](https://img.shields.io/badge/GitHub-CyberVerve07-181717?style=for-the-badge&logo=github)](https://github.com/CyberVerve07)

</div>
