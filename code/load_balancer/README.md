# Load Balancer Code Examples

This directory contains practical examples and simulations for Day 1: Servers and Load Balancers.

## Directory Structure

- `basic_example/` - Minimal standalone Java console demonstration.
- `client/` - Spring Boot 3 client application simulating requests to a load-balanced server endpoint.

## Running Examples

### 1. Basic Example

Compile and run the standalone Java entry point:
```bash
cd code/load_balancer/basic_example
javac Main.java
java systemdesign.load_balancer.Main
```

### 2. Spring Boot Client

The Spring Boot client runs on port `8080` by default and provides a `/send-request` endpoint to simulate traffic forwarding to a designated load balancer URL.

```bash
cd code/load_balancer/client
mvn clean spring-boot:run
```

Once running, simulate dispatching a request:
```bash
curl "http://localhost:8080/send-request?lbUrl=http://localhost:8080"
```
