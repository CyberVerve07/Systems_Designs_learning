# Day 4: API Gateway & Microservices

Welcome to Day 4 of our System Design journey! Today we're exploring **API Gateway & Microservices** - the backbone of modern distributed systems.

## What is Microservices Architecture?

**Microservices Architecture** is an approach where a single application is built as a suite of small services, each running in its own process and communicating with lightweight mechanisms (usually HTTP APIs).

**Real-world Analogy:** Think about a shopping mall.
- **Monolithic Architecture** = One giant store that sells everything (clothes, electronics, food, pharmacy)
- **Microservices Architecture** = Multiple specialized stores (clothing store, electronics store, food court, pharmacy) - each store operates independently

---

## Monolithic vs Microservices

### **Monolithic Architecture**

**Characteristics:**
- Single codebase
- Single database
- All functionality in one application
- Deployed as a single unit

**Pros:**
- Simple to develop initially
- Easy to test (end-to-end)
- No network latency
- Simple deployment

**Cons:**
- Difficult to scale (must scale entire app)
- Single point of failure
- Technology lock-in (one language/framework)
- Hard to maintain as codebase grows
- Slow development cycles

**Example:**
```
[Monolithic App]
├── User Module
├── Order Module
├── Payment Module
├── Product Module
└── Notification Module
```

---

### **Microservices Architecture**

**Characteristics:**
- Multiple independent services
- Each service has its own database
- Services communicate via APIs
- Deployed independently

**Pros:**
- Independent scaling (scale only what needs it)
- Fault isolation (one service failure doesn't crash everything)
- Technology diversity (use best tool for each service)
- Faster development cycles
- Easier to maintain and understand

**Cons:**
- Complex to set up initially
- Network latency between services
- Distributed system challenges (consistency, transactions)
- More infrastructure to manage
- Monitoring and debugging complexity

**Example:**
```
[API Gateway]
    ↓
├── [User Service] → [User DB]
├── [Order Service] → [Order DB]
├── [Payment Service] → [Payment DB]
├── [Product Service] → [Product DB]
└── [Notification Service] → [Notification DB]
```

---

## What is an API Gateway?

An **API Gateway** is a server that acts as an entry point for all client requests. It sits between clients and microservices, handling cross-cutting concerns.

**Real-world Analogy:** Think about a hotel reception desk.
- **Guests (Clients)** don't go directly to housekeeping, restaurant, or spa
- **Reception Desk (API Gateway)** routes requests to the right department
- Reception handles check-in, billing, and other common tasks

---

## API Gateway Responsibilities

### **1. Request Routing**
Routes incoming requests to the appropriate microservice.

**Example:**
```
/api/users → User Service
/api/orders → Order Service
/api/products → Product Service
```

### **2. Load Balancing**
Distributes traffic across multiple instances of a service.

### **3. Authentication & Authorization**
Validates user identity before forwarding requests to services.

### **4. Rate Limiting**
Prevents abuse by limiting request rates per client.

### **5. Caching**
Caches responses to reduce load on backend services.

### **6. Request/Response Transformation**
Converts between different protocols or data formats.

### **7. API Composition**
Combines responses from multiple services into a single response.

### **8. Logging & Monitoring**
Logs all requests for debugging and analytics.

---

## API Gateway Patterns

### **1. Routing Pattern**
Simple routing based on URL path or HTTP method.

**Example:**
```
GET /api/users/123 → User Service
POST /api/orders → Order Service
GET /api/products → Product Service
```

### **2. Aggregation Pattern**
Combines multiple service calls into a single response.

**Example:**
```
Client Request: GET /api/orders/123
Gateway:
  → Call Order Service (get order details)
  → Call User Service (get customer info)
  → Call Product Service (get product details)
  → Combine and return to client
```

### **3. Offloading Pattern**
Moves cross-cutting concerns from services to gateway.

**Example:**
- SSL/TLS termination
- Compression
- Logging
- Monitoring

---

## Popular API Gateway Solutions

### **1. Kong**
- Open-source, plugin-based
- High performance
- Rich plugin ecosystem

### **2. NGINX**
- High-performance web server
- Can act as API gateway
- Lightweight

### **3. AWS API Gateway**
- Managed service
- Integrates with AWS ecosystem
- Pay-as-you-go pricing

### **4. Spring Cloud Gateway**
- Java-based
- Built for Spring Boot microservices
- Reactive programming model

### **5. Ambassador**
- Kubernetes-native
- Built on Envoy proxy
- DevOps-friendly

---

## Microservices Communication Patterns

### **1. Synchronous Communication (HTTP/REST)**
Services communicate via HTTP requests.

**Pros:**
- Simple to implement
- Request-response pattern
- Easy to debug

**Cons:**
- Tight coupling
- Blocking operations
- Network latency

**Example:**
```java
// Order Service calling User Service
User user = restTemplate.getForObject(
    "http://user-service/users/" + userId, 
    User.class
);
```

### **2. Asynchronous Communication (Message Queues)**
Services communicate via message queues.

**Pros:**
- Loose coupling
- Non-blocking
- Better for high throughput
- Fault tolerance

**Cons:**
- More complex
- Eventual consistency
- Harder to debug

**Example:**
```java
// Order Service publishes event
kafkaTemplate.send("order-events", orderEvent);
// Notification Service consumes event
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderEvent event) {
    // Send notification
}
```

---

## Service Discovery

### **The Problem**
In microservices, services have dynamic IP addresses. How do services find each other?

### **Solution: Service Discovery**

#### **Client-Side Discovery**
Client queries service registry to get service location.

**Example:**
```
Client → Service Registry → "Where is User Service?"
Registry → "User Service is at 192.168.1.100:8080"
Client → User Service (192.168.1.100:8080)
```

**Tools:** Eureka, Consul, Zookeeper

#### **Server-Side Discovery**
Client makes request to load balancer, which routes to appropriate service.

**Example:**
```
Client → Load Balancer → Routes to available service instance
```

**Tools:** AWS ALB, NGINX, Envoy

---

## Data Management in Microservices

### **Database per Service Pattern**
Each microservice has its own database.

**Pros:**
- Loose coupling
- Independent scaling
- Technology diversity

**Cons:**
- Cross-service transactions difficult
- Data duplication
- Complex queries

### **Shared Database Pattern**
Multiple services share the same database.

**Pros:**
- Simple transactions
- No data duplication

**Cons:**
- Tight coupling
- Harder to scale independently
- Technology lock-in

**Recommendation:** Use database per service for true microservices.

---

## Handling Distributed Transactions

### **The Problem**
ACID transactions don't work well across multiple services.

### **Solutions:**

#### **1. Two-Phase Commit (2PC)**
- Coordinator asks all participants to prepare
- If all agree, coordinator asks to commit
- If any fails, coordinator asks to rollback

**Pros:** Strong consistency
**Cons:** Slow, blocking, single point of failure

#### **2. Saga Pattern**
Break transaction into sequence of local transactions with compensating actions.

**Example:**
```
Order Saga:
1. Create Order (Order Service)
2. Reserve Inventory (Inventory Service)
3. Process Payment (Payment Service)
4. Confirm Order (Order Service)

If any step fails, execute compensating actions:
- Cancel payment
- Release inventory
- Cancel order
```

**Pros:** Scalable, fault-tolerant
**Cons:** Eventual consistency, complex

---

## Microservices Deployment

### **Containerization**
Package each service in a container (Docker).

**Benefits:**
- Consistent environments
- Easy deployment
- Resource isolation

### **Orchestration**
Manage containers at scale (Kubernetes).

**Benefits:**
- Auto-scaling
- Self-healing
- Load balancing
- Service discovery

---

## Monitoring & Observability

### **1. Logging**
- Centralized logging (ELK Stack, Splunk)
- Structured logs with correlation IDs
- Log aggregation across services

### **2. Metrics**
- Service performance metrics
- Business metrics
- Resource utilization
- Tools: Prometheus, Grafana, Datadog

### **3. Tracing**
- Distributed tracing (Jaeger, Zipkin)
- Track requests across services
- Identify performance bottlenecks

---

## Real-World Examples

### **Netflix**
- Hundreds of microservices
- API Gateway for routing
- Chaos engineering for fault tolerance
- Custom service discovery (Eureka)

### **Amazon**
- Thousands of microservices
- API Gateway (AWS)
- Event-driven architecture
- Database per service

### **Uber**
- Microservices for different domains
- API Gateway for routing
- Real-time communication
- Distributed tracing

---

## When to Use Microservices

### **Use Microservices When:**
- Large, complex application
- Multiple teams working on different features
- Need independent scaling
- Need technology diversity
- Fast development cycles required

### **Stick with Monolith When:**
- Small team
- Simple application
- Just starting out
- Don't need independent scaling
- Want to avoid complexity

---

## Common Pitfalls

### **1. Over-engineering**
Don't break into microservices too early. Start with monolith, split when needed.

### **2. Distributed Monolith**
If services are tightly coupled, you haven't gained microservices benefits.

### **3. Ignoring Data Consistency**
Plan for eventual consistency and implement proper patterns.

### **4. Poor Monitoring**
Without proper monitoring, debugging distributed systems is a nightmare.

### **5. Network Latency**
Don't make too many service calls in a single request.

---

## Summary

API Gateway and Microservices are fundamental to modern system design:

**Key Takeaways:**
1. **Microservices** break applications into small, independent services
2. **API Gateway** acts as entry point, handling cross-cutting concerns
3. **Service Discovery** enables services to find each other dynamically
4. **Database per Service** ensures loose coupling
5. **Saga Pattern** handles distributed transactions
6. **Monitoring** is critical for distributed systems

**Remember:** Microservices add complexity - use them when the benefits outweigh the costs!

---

## Next Steps

In our next lesson, we'll implement Authentication & Authorization patterns with practical code examples including JWT, OAuth, and security best practices.
