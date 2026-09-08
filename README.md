# 🚀 System Design Learning Journey & Production Architecture

Welcome to the **System Design Learning Journey**! This repository documents real-world system design principles, comprehensive theoretical notes, working Java code implementations, end-to-end architecture case studies, and a 100-question production interview guide.

---

## 🎯 Special Highlights

- 🛍️ **[100 Production E-Commerce System Design Interview Questions (3+ YOE)](theory/interview_prep/ecommerce_100_system_design_questions.md)**: Real-world failure scenarios, API latency spikes, Kafka consumer crashes, inventory overselling prevention, payment idempotency, and distributed transactions.
- 🐦 **[Twitter / X Complete System Design & Architecture](theory/twitter_x_system_design/README.md)**: End-to-end production architecture featuring HD visual architecture diagrams in [`theory/twitter_x_system_design/assets/`](theory/twitter_x_system_design/assets/).

---

## 📚 Topics Covered

### **Day 1: Servers & Load Balancers**
- **Theory**: [`theory/load_balancer/load_balancer.md`](theory/load_balancer/load_balancer.md)
- **Code**: [`code/load_balancer/`](code/load_balancer/)
- **Key Concepts**:
  - What is a Server and Load Balancer
  - Traffic distribution algorithms (Round Robin, Least Connections, IP Hash)
  - High availability, health checks, and failover
  - Horizontal scaling

### **Day 2: Caching Strategies**
- **Theory**: [`theory/caching/caching_strategies.md`](theory/caching/caching_strategies.md)
- **Code**: [`code/caching/redis_cache_example/`](code/caching/redis_cache_example/)
- **Key Concepts**:
  - Client-side, Server-side, and CDN caching
  - Caching strategies (Cache-Aside, Write-Through, Write-Behind)
  - Cache eviction policies (LRU, LFU, FIFO, TTL)
  - Redis implementation with Java
  - Performance optimization (50-100x speedup)

### **Day 3: Database Design & Sharding**
- **Theory**: [`theory/database_design/database_design.md`](theory/database_design/database_design.md)
- **Code**: [`code/database_design/sharding_example/`](code/database_design/sharding_example/)
- **Key Concepts**:
  - SQL vs NoSQL databases
  - ACID vs BASE consistency models
  - Database scaling (Vertical vs Horizontal)
  - Replication strategies (Master-Slave, Master-Master, Leaderless)
  - Sharding strategies (Hash-based, Range-based)
  - Indexing and normalization

### **Day 4: API Gateway & Microservices**
- **Theory**: [`theory/api_gateway_microservices/api_gateway_microservices.md`](theory/api_gateway_microservices/api_gateway_microservices.md)
- **Code**: [`code/api_gateway_microservices/api_gateway_example/`](code/api_gateway_microservices/api_gateway_example/)
- **Key Concepts**:
  - Monolithic vs Microservices architecture
  - API Gateway responsibilities (routing, load balancing, authentication)
  - Service discovery patterns
  - Microservices communication (Synchronous vs Asynchronous)
  - Database per service pattern
  - Distributed transactions (Saga pattern)

### **Day 5: Authentication & Authorization**
- **Theory**: [`theory/authentication_authorization/authentication_authorization.md`](theory/authentication_authorization/authentication_authorization.md)
- **Code**: [`code/authentication_authorization/jwt_example/`](code/authentication_authorization/jwt_example/)
- **Key Concepts**:
  - Authentication vs Authorization
  - Authentication methods (Password, MFA, API Keys, OAuth 2.0)
  - JWT (JSON Web Tokens) implementation
  - Authorization models (RBAC, ABAC, ACL)
  - Password hashing with bcrypt
  - Security best practices

### **Day 6: Rate Limiting & Throttling**
- **Theory**: [`theory/rate_limiter/rate_limiter.md`](theory/rate_limiter/rate_limiter.md)
- **Code**: [`code/rate_limiter/rate_limiter_example/`](code/rate_limiter/rate_limiter_example/)
- **Key Concepts**:
  - Purpose of Rate Limiting (Abuse prevention, cost control, resource management)
  - Placements (Client-side, Server-side, API Gateway)
  - Algorithms (Token Bucket, Leaky Bucket, Sliding Window Counter, Sliding Window Log)
  - Concurrency challenges in distributed rate limiters (Race conditions, Redis Lua scripting)

### **Day 7: Message Queues & Event-Driven Architecture**
- **Theory**: [`theory/message_queues/message_queues_deep_dive.md`](theory/message_queues/message_queues_deep_dive.md)
- **Code**: [`code/message_queues/message_broker_example/`](code/message_queues/message_broker_example/)
- **Key Concepts**:
  - Asynchronous patterns, non-blocking flow control, and backpressure
  - Messaging models: Point-to-Point (Queues) vs. Publish/Subscribe (Topics)
  - Kafka-style hybrid Consumer Groups load balancing
  - Reliability semantics: ACKs, NACKs, and dead-letter queues (DLQ)
  - Scheduled reaper threads for reclaiming in-flight timeouts

### **Day 8: Distributed Unique ID Generator (Snowflake)**
- **Theory**: [`theory/distributed_id_generator/distributed_id_generator.md`](theory/distributed_id_generator/distributed_id_generator.md)
- **Code**: [`code/distributed_id_generator/snowflake_example/`](code/distributed_id_generator/snowflake_example/)
- **Key Concepts**:
  - Auto-increment limitations in sharded databases
  - Comparison of alternatives (UUID, Multi-Master replication, Ticket Server)
  - Twitter Snowflake 64-bit partition mathematics
  - NTP clock drift detection & dynamic Node ID allocation

### **Day 9: Apache Kafka & Event Streaming**
- **Theory**: [`theory/kafka/apache_kafka_basics.md`](theory/kafka/apache_kafka_basics.md)
- **Code**: [`code/kafka/basic_example/`](code/kafka/basic_example/)
- **Key Concepts**:
  - Core architecture: Brokers, Topics, Partitions, and Consumer Groups
  - Producers with keys, partition hashing, and ordering guarantees
  - Consumer offset management and rebalancing mechanics
  - Docker Compose Kafka & Zookeeper orchestration

### **Day 10: Notification System Architecture**
- **Theory**: [`theory/notification_system/notification_system.md`](theory/notification_system/notification_system.md)
- **Code**: [`code/notification_system/`](code/notification_system/)
- **Key Concepts**:
  - Multi-channel notification pipeline (SMS, Push, Email, In-App)
  - Priority queueing and worker rate limiting
  - User notification settings and deduplication
  - Visual architecture flow diagram

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (or Java 11+)
- **Maven 3.8+**
- **Docker & Docker Compose** (for Redis, Kafka, Zookeeper)

### Running Code Examples

Each topic has its own code directory with a README containing specific instructions:

```bash
# Navigate to the code directory
cd code/[topic_name]/[example_name]

# Build the project
mvn clean compile

# Run the demo
mvn exec:java -Dexec.mainClass="com.systemdesign.[package].DemoClass"
```

### External Services (Docker)

**Redis (for Caching and Rate Limiting):**
```bash
docker run -d -p 6379:6379 --name redis redis:latest
```

**Kafka & Zookeeper:**
```bash
cd code/kafka/basic_example
docker-compose up -d
```

---

## 📁 Repository Structure

```
Systems_Designs_learning/
├── theory/                                    # Theoretical notes and case studies
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
│   ├── twitter_x_system_design/               # Case Study: Twitter/X architecture
│   │   ├── assets/                            # HD Architecture diagrams
│   │   └── README.md
│   └── interview_prep/                        # Interview Question Bank
│       └── ecommerce_100_system_design_questions.md
├── code/                                      # Practical implementations
│   ├── load_balancer/
│   ├── caching/
│   ├── database_design/
│   ├── api_gateway_microservices/
│   ├── authentication_authorization/
│   ├── rate_limiter/
│   ├── message_queues/
│   ├── distributed_id_generator/
│   ├── kafka/
│   └── notification_system/
├── .gitignore                                 # Clean ignore rules (excludes build target/)
└── README.md                                  # Repository overview
```

---

## 📝 Progress Checklist

- [x] Day 1: Servers & Load Balancer
- [x] Day 2: Caching Strategies & Redis
- [x] Day 3: Database Design & Sharding
- [x] Day 4: API Gateway & Microservices
- [x] Day 5: Authentication & Authorization (JWT)
- [x] Day 6: Rate Limiting & Throttling
- [x] Day 7: Message Queues & Event-Driven Architecture
- [x] Day 8: Distributed Unique ID Generator (Snowflake)
- [x] Day 9: Apache Kafka & Event Streaming
- [x] Day 10: Notification System Architecture
- [x] Case Study: Twitter / X Scalable Architecture
- [x] Interview Series: 100 Production E-Commerce System Design Questions (3+ YOE)

---

## 🔗 Recommended Resources

- **System Design Primer**: https://github.com/donnemartin/system-design-primer
- **Designing Data-Intensive Applications** by Martin Kleppmann
- **Engineering Blogs**: Netflix Tech Blog, Uber Engineering, AWS Architecture Blog