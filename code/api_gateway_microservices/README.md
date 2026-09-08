# API Gateway & Microservices Code Examples

This directory contains practical implementations of API Gateway and Microservices patterns discussed in the theory section.

## Files Structure

- `api_gateway_example/` - API Gateway implementation in Java, demonstrating:
  - Dynamic service registration
  - URL prefix and regex path routing
  - Request/Response translation and header decoration (`X-Gateway-Request-ID`, `X-Forwarded-For`)
  - Multi-service dispatch simulation (User, Order, and Product services)

## Setup Instructions

### Prerequisites
- Java 11+
- Maven
- (Optional) Docker for containerization

## Running Examples

Each subdirectory contains a README with specific instructions for that implementation.

## Key Concepts Demonstrated

1. **API Gateway Routing**: Route requests to appropriate microservices
2. **Service Discovery**: Dynamic service registration and discovery
3. **API Aggregation**: Combine multiple service responses
4. **Load Balancing**: Distribute traffic across service instances
5. **Microservices Communication**: Synchronous (HTTP) and Asynchronous (Message Queue)
