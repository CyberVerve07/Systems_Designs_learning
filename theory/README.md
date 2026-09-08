# 📚 System Design Concepts & Theory Notes

This directory contains comprehensive theoretical notes, deep-dives, architecture diagrams, and interview preparation materials covering foundational and advanced distributed system design.

---

## 📑 Contents & Roadmaps

### 🏢 Real-World Architecture Case Studies
- **[Twitter / X Distributed Architecture](./twitter_x_system_design/README.md)**: End-to-end multi-region design covering ingestion, hybrid fan-out, microservices mesh, Kafka event streaming, and polyglot persistence. Includes HD architecture diagrams in [`./twitter_x_system_design/assets/`](./twitter_x_system_design/assets/).

### 🎯 Interview Preparation Masterclass
- **[100 Production E-Commerce System Design Interview Questions (3+ YOE)](./interview_prep/ecommerce_100_system_design_questions.md)**:
  - 100 in-depth production failure scenarios, latency spikes, Kafka consumer crashes, inventory overselling, payment idempotency, and distributed transactions.

---

## 🛠️ Topic-by-Topic Learning Notes

| Day | Topic | Theory Document | Practical Code |
|:---:|:---|:---|:---|
| **01** | **Servers & Load Balancers** | [`theory/load_balancer/load_balancer.md`](./load_balancer/load_balancer.md) | [`code/load_balancer/`](../code/load_balancer/) |
| **02** | **Caching Strategies** | [`theory/caching/caching_strategies.md`](./caching/caching_strategies.md) | [`code/caching/redis_cache_example/`](../code/caching/redis_cache_example/) |
| **03** | **Database Design & Sharding** | [`theory/database_design/database_design.md`](./database_design/database_design.md) | [`code/database_design/sharding_example/`](../code/database_design/sharding_example/) |
| **04** | **API Gateway & Microservices** | [`theory/api_gateway_microservices/api_gateway_microservices.md`](./api_gateway_microservices/api_gateway_microservices.md) | [`code/api_gateway_microservices/api_gateway_example/`](../code/api_gateway_microservices/api_gateway_example/) |
| **05** | **Authentication & Authorization** | [`theory/authentication_authorization/authentication_authorization.md`](./authentication_authorization/authentication_authorization.md) | [`code/authentication_authorization/jwt_example/`](../code/authentication_authorization/jwt_example/) |
| **06** | **Rate Limiting & Throttling** | [`theory/rate_limiter/rate_limiter.md`](./rate_limiter/rate_limiter.md) | [`code/rate_limiter/rate_limiter_example/`](../code/rate_limiter/rate_limiter_example/) |
| **07** | **Message Queues & Event-Driven Architecture** | [`theory/message_queues/message_queues_deep_dive.md`](./message_queues/message_queues_deep_dive.md) | [`code/message_queues/message_broker_example/`](../code/message_queues/message_broker_example/) |
| **08** | **Distributed Unique ID Generator (Snowflake)** | [`theory/distributed_id_generator/distributed_id_generator.md`](./distributed_id_generator/distributed_id_generator.md) | [`code/distributed_id_generator/snowflake_example/`](../code/distributed_id_generator/snowflake_example/) |
| **09** | **Apache Kafka & Event Streaming** | [`theory/kafka/apache_kafka_basics.md`](./kafka/apache_kafka_basics.md) | [`code/kafka/basic_example/`](../code/kafka/basic_example/) |
| **10** | **Notification System Architecture** | [`theory/notification_system/notification_system.md`](./notification_system/notification_system.md) | [`code/notification_system/`](../code/notification_system/) |
