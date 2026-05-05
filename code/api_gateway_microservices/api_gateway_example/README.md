# API Gateway Example

This project demonstrates a simple API Gateway implementation that routes requests to multiple microservices.

## What is an API Gateway?

An API Gateway is a server that acts as an entry point for all client requests. It sits between clients and microservices, handling cross-cutting concerns like routing, authentication, rate limiting, and logging.

## Architecture

```
Client Request
    ↓
[API Gateway]
    ↓
├── [User Service] (localhost:8081)
├── [Order Service] (localhost:8082)
└── [Product Service] (localhost:8083)
```

## Key Features Demonstrated

### **1. Service Registration**
- Microservices register with the gateway
- Gateway maintains a service registry

### **2. Request Routing**
- Routes requests based on URL path
- `/api/users/*` → User Service
- `/api/orders/*` → Order Service
- `/api/products/*` → Product Service

### **3. HTTP Method Support**
- GET, POST, PUT, DELETE methods
- Proper request forwarding

### **4. Request Headers**
- Adds gateway-specific headers
- Request ID generation
- Forwarded headers

## Build and Run

### 1. Build the project
```bash
mvn clean compile
```

### 2. Run the demo
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.gateway.ApiGatewayDemo"
```

## What You'll Learn

### **Demo 1: Simple Routing**
- Routes requests to appropriate services based on path
- Shows how gateway determines destination service

### **Demo 2: HTTP Methods**
- Demonstrates different HTTP methods (GET, POST, PUT, DELETE)
- Shows proper request forwarding with method preservation

### **Demo 3: Multi-Service Routing**
- Multiple requests to different services
- Shows load distribution across services

## Key Files

- `ApiGateway.java` - Main gateway implementation with routing logic
- `Microservice.java` - Represents a backend microservice
- `Request.java` - Request model
- `Response.java` - Response model
- `ApiGatewayDemo.java` - Demonstration class

## Expected Output

```
🚀 API Gateway Demo

📝 Registered service: User Service at /api/users
📝 Registered service: Order Service at /api/orders
📝 Registered service: Product Service at /api/products

📊 API Gateway Status:
==================================================
Registered Services: 3
  - User Service (/api/users)
  - Order Service (/api/orders)
  - Product Service (/api/products)
==================================================

📚 DEMO 1: Simple Routing
==================================================

📨 Incoming Request: GET /api/users/123
🔄 Routing to User Service: http://localhost:8081/api/users/123
```

## Real-World API Gateway Features

This demo shows basic routing. Production API Gateways also include:

### **Authentication & Authorization**
- JWT validation
- OAuth integration
- Role-based access control

### **Rate Limiting**
- Per-client rate limits
- Per-endpoint rate limits
- Token bucket algorithm

### **Load Balancing**
- Round-robin
- Least connections
- Health checks

### **Caching**
- Response caching
- Cache invalidation
- TTL management

### **API Composition**
- Combine multiple service responses
- Parallel service calls
- Response transformation

### **Monitoring & Logging**
- Request logging
- Metrics collection
- Distributed tracing

## Popular API Gateway Solutions

### **Open Source**
- **Kong** - Plugin-based, high performance
- **NGINX** - Lightweight, widely used
- **Traefik** - Cloud-native, auto-discovery

### **Cloud Services**
- **AWS API Gateway** - Managed, AWS integration
- **Google Cloud Endpoints** - GCP integration
- **Azure API Management** - Azure integration

### **Framework-Specific**
- **Spring Cloud Gateway** - Java/Spring
- **Express Gateway** - Node.js
- **Zuul** - Netflix (deprecated, use Spring Cloud Gateway)

## Next Steps

1. Implement actual microservices with HTTP servers
2. Add authentication middleware
3. Implement rate limiting
4. Add caching layer
5. Implement API composition pattern

## Troubleshooting

### Connection Refused Errors
The demo simulates routing logic. In a real implementation, ensure microservices are running on the specified ports.

### Maven Build Issues
```bash
mvn clean install
```
**Solution**: Ensure Maven is properly configured and Java 11+ is installed
