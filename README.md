# System Design Learning Journey

Welcome to my System Design learning repository! This repository documents my daily learnings in System Design with comprehensive theory notes and practical code implementations.

## 📚 Topics Covered

### **Day 1: Servers & Load Balancers**
- **Theory**: `theory/load_balancer/load_balancer.md`
- **Code**: `code/load_balancer/`
- **Key Concepts**:
  - What is a Server and Load Balancer
  - Traffic distribution algorithms (Round Robin, Least Connections, IP Hash)
  - High availability and reliability
  - Horizontal scaling

### **Day 2: Caching Strategies**
- **Theory**: `theory/caching/caching_strategies.md`
- **Code**: `code/caching/redis_cache_example/`
- **Key Concepts**:
  - Client-side, Server-side, and CDN caching
  - Caching strategies (Cache-Aside, Write-Through, Write-Behind)
  - Cache eviction policies (LRU, LFU, FIFO, TTL)
  - Redis implementation with Java
  - Performance optimization (50-100x speedup)

### **Day 3: Database Design & Sharding**
- **Theory**: `theory/database_design/database_design.md`
- **Code**: `code/database_design/sharding_example/`
- **Key Concepts**:
  - SQL vs NoSQL databases
  - ACID vs BASE consistency models
  - Database scaling (Vertical vs Horizontal)
  - Replication strategies (Master-Slave, Master-Master, Leaderless)
  - Sharding strategies (Hash-based, Range-based)
  - Indexing and normalization

### **Day 4: API Gateway & Microservices**
- **Theory**: `theory/api_gateway_microservices/api_gateway_microservices.md`
- **Code**: `code/api_gateway_microservices/api_gateway_example/`
- **Key Concepts**:
  - Monolithic vs Microservices architecture
  - API Gateway responsibilities (routing, load balancing, authentication)
  - Service discovery patterns
  - Microservices communication (Synchronous vs Asynchronous)
  - Database per service pattern
  - Distributed transactions (Saga pattern)

### **Day 5: Authentication & Authorization**
- **Theory**: `theory/authentication_authorization/authentication_authorization.md`
- **Code**: `code/authentication_authorization/jwt_example/`
- **Key Concepts**:
  - Authentication vs Authorization
  - Authentication methods (Password, MFA, API Keys, OAuth 2.0)
  - JWT (JSON Web Tokens) implementation
  - Authorization models (RBAC, ABAC, ACL)
  - Password hashing with bcrypt
  - Security best practices

### **Day 6: Rate Limiting & Throttling**
- **Theory**: `theory/rate_limiter/rate_limiter.md`
- **Code**: `code/rate_limiter/rate_limiter_example/`
- **Key Concepts**:
  - Purpose of Rate Limiting (Abuse prevention, cost control, resource management)
  - Placements (Client-side, Server-side, API Gateway)
  - Algorithms (Token Bucket, Leaky Bucket, Sliding Window Counter, Sliding Window Log)
  - Concurrency challenges in distributed rate limiters (Race conditions, Redis Lua scripting)

### **Day 7: Message Queues & Event-Driven Architecture**
- **Theory**: `theory/message_queues/message_queues_deep_dive.md`
- **Code**: `code/message_queues/message_broker_example/`
- **Key Concepts**:
  - Asynchronous patterns, non-blocking flow control, and backpressure
  - Messaging models: Point-to-Point (Queues) vs. Publish/Subscribe (Topics)
  - Kafka-style hybrid Consumer Groups load balancing
  - Reliability semantics: ACKs, NACKs, and dead-letter queues (DLQ)
  - Scheduled reaper threads for reclaiming in-flight timeouts

### **Day 8: Distributed Unique ID Generator (Snowflake)**
- **Theory**: `theory/distributed_id_generator/distributed_id_generator.md`
- **Code**: *N/A (Theoretical Session)*
- **Key Concepts**:
  - Auto-increment limitations in sharded databases
  - Comparison of alternatives (UUID, Multi-Master replication, Ticket Server)
  - Twitter Snowflake 64-bit partition mathematics
  - NTP clock drift detection & Zookeeper dynamic Node ID allocation

## 🚀 Getting Started

### Prerequisites
- Java 11+
- Maven
- (Optional) Docker for Redis and MongoDB

### Running Code Examples

Each topic has its own code directory with a README containing specific instructions. General steps:

```bash
# Navigate to the code directory
cd code/[topic_name]/[example_name]

# Build the project
mvn clean compile

# Run the demo
mvn exec:java -Dexec.mainClass="com.systemdesign.[package].DemoClass"
```

### External Dependencies

Some examples require external services:

**Redis (for Caching examples):**
```bash
docker run -d -p 6379:6379 redis:latest
```

**MongoDB (for NoSQL examples):**
```bash
docker run -d -p 27017:27017 mongo:latest
```

## 📁 Repository Structure

```
Systems_Designs_learning/
├── theory/                          # Theory notes
│   ├── load_balancer/
│   ├── caching/
│   ├── database_design/
│   ├── api_gateway_microservices/
│   ├── authentication_authorization/
│   ├── rate_limiter/
│   ├── message_queues/
│   └── distributed_id_generator/
├── code/                            # Practical implementations
│   ├── load_balancer/
│   ├── caching/
│   │   └── redis_cache_example/
│   ├── database_design/
│   │   └── sharding_example/
│   ├── api_gateway_microservices/
│   │   └── api_gateway_example/
│   ├── authentication_authorization/
│   │   └── jwt_example/
│   ├── rate_limiter/
│   │   └── rate_limiter_example/
│   ├── message_queues/
│   │   └── message_broker_example/
│   └── distributed_id_generator/
│       └── snowflake_example/
└── README.md                        # This file
```

## 🎯 Learning Path

1. **Day 1**: Understand basic infrastructure (Servers, Load Balancers)
2. **Day 2**: Learn performance optimization (Caching)
3. **Day 3**: Master data storage (Database Design, Sharding)
4. **Day 4**: Build distributed systems (API Gateway, Microservices)
5. **Day 5**: Secure your systems (Authentication, Authorization)
6. **Day 6**: Control API traffic (Rate Limiting & Throttling)
7. **Day 7**: Decouple components (Message Queues & Event-Driven Architecture)
8. **Day 8**: Design highly scalable unique IDs (Distributed Unique ID Generator)

## 🛠️ Technologies Used

- **Java 11+** - Primary programming language
- **Maven** - Dependency management
- **Redis** - Caching
- **H2 Database** - In-memory database for demos
- **JWT** - Token-based authentication
- **BCrypt** - Password hashing

## 📖 Key Takeaways

### System Design Fundamentals
- **Scalability**: Horizontal vs Vertical scaling
- **Availability**: Replication and fault tolerance
- **Performance**: Caching and indexing
- **Security**: Authentication and authorization
- **Reliability**: Load balancing and service discovery

### Best Practices
- Always use HTTPS in production
- Never store plain text passwords
- Implement proper error handling
- Use appropriate caching strategies
- Design for failure (fault tolerance)
- Monitor and log everything

## 🔗 Resources

- **System Design Primer**: https://github.com/donnemartin/system-design-primer
- **High Scalability**: http://highscalability.com/
- **Engineering Blogs**: Netflix Tech Blog, Uber Engineering, AWS Architecture Blog

## 📝 Progress

- ✅ Day 1: Load Balancer
- ✅ Day 2: Caching
- ✅ Day 3: Database Design & Sharding
- ✅ Day 4: API Gateway & Microservices
- ✅ Day 5: Authentication & Authorization
- ✅ Day 6: Rate Limiting & Throttling
- ✅ Day 7: Message Queues & Event-Driven Architecture
- ✅ Day 8: Distributed Unique ID Generator (Snowflake)

## 🎓 Next Topics to Cover

- Notification Systems (Using Webhooks, Push, SMS, Email routing)
- Content Delivery Networks (CDN) & Edge Caching
- Distributed Unique ID Generator (Twitter Snowflake, etc.)
- Distributed Systems (CAP Theorem, Consistency Models, Consensus)
- Monitoring & Observability
- Real-world System Design (URL Shortener, Twitter Timeline, etc.)

## 📧 Contact

Feel free to reach out for discussions or collaborations on System Design topics!

---

**Happy Learning! 🚀**