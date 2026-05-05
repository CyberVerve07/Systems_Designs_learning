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
│   └── authentication_authorization/
├── code/                            # Practical implementations
│   ├── load_balancer/
│   ├── caching/
│   │   └── redis_cache_example/
│   ├── database_design/
│   │   └── sharding_example/
│   ├── api_gateway_microservices/
│   │   └── api_gateway_example/
│   └── authentication_authorization/
│       └── jwt_example/
└── README.md                        # This file
```

## 🎯 Learning Path

1. **Day 1**: Understand basic infrastructure (Servers, Load Balancers)
2. **Day 2**: Learn performance optimization (Caching)
3. **Day 3**: Master data storage (Database Design, Sharding)
4. **Day 4**: Build distributed systems (API Gateway, Microservices)
5. **Day 5**: Secure your systems (Authentication, Authorization)

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

## 🎓 Next Topics to Cover

- Message Queues & Async Architecture
- Notification Systems
- Rate Limiting & Throttling
- Content Delivery Networks (CDN)
- Distributed Systems (CAP Theorem, Consistency Models)
- Monitoring & Observability
- Real-world System Design (URL Shortener, Twitter Timeline, etc.)

## 📧 Contact

Feel free to reach out for discussions or collaborations on System Design topics!

---

**Happy Learning! 🚀**