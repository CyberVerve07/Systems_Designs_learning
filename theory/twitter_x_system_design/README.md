# 🐦 Twitter / X — System Design & Architecture

> Complete system design documentation with HD visual architecture diagrams, Mermaid charts, and in-depth explanations.

---

## 🎨 Visual System Architecture Diagrams

### 1. Overall Twitter / X Architecture Overview
![Twitter System Architecture](C:\Users\lenovo\.gemini\antigravity-ide\brain\9dba89a5-aed8-4d41-8b1b-cf9fcc38d3e5\twitter_overall_architecture_1787331358267.jpg)

### 2. Tweet Ingestion & Hybrid Fan-out Architecture
![Twitter Fan-out Architecture](C:\Users\lenovo\.gemini\antigravity-ide\brain\9dba89a5-aed8-4d41-8b1b-cf9fcc38d3e5\twitter_fanout_flow_1787331885536.jpg)

---

## 📋 Table of Contents

1. [High-Level Architecture Overview](#1-high-level-architecture-overview)
2. [Clients Layer](#2-clients-layer)
3. [Edge & API Layer](#3-edge--api-layer)
4. [Core Microservices](#4-core-microservices)
5. [Event Streaming Backbone](#5-event-streaming-backbone)
6. [Storage & Caching Layer](#6-storage--caching-layer)
7. [Platform & Ops (SRE)](#7-platform--ops-sre)
8. [Key Flows — Request Journeys](#8-key-flows--request-journeys)
9. [Data Modeling](#9-data-modeling)
10. [Scalability & Fault Tolerance](#10-scalability--fault-tolerance)
11. [Numbers to Know (Back-of-Envelope)](#11-numbers-to-know-back-of-envelope)

---

## 1. High-Level Architecture Overview

The system is organized into **6 core layers**:

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENTS                                │
│   Web App │ iOS/Android │ Public API │ Smart TV │ Bots      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│               EDGE & API LAYER (Multi-Region, Anycast)      │
│    Load Balancer → API Gateway → WebSocket/Push → WAF/DDoS  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│           CORE MICROSERVICES (gRPC + Service Mesh)          │
│  User&Auth │ Tweet │ Social Graph │ Timeline │ Notification  │
│  DM │ Media │ Search │ Ads&Ranking │ Service Mesh            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│          EVENT STREAMING BACKBONE (Kafka, exactly-once)     │
│  Kafka │ Fan-out Workers │ Stream Processing (Flink)         │
│  CDC/Debezium │ DLQ + Retry │ Schema Registry (Avro)         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│             STORAGE & CACHING (Sharded + Replicated)        │
│  Cassandra │ PostgreSQL │ Elasticsearch │ S3 │ Data Lake      │
│  Graph DB │ Vector Store │ Feature Store                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│             PLATFORM & OPS (SRE – SLOs)                     │
│  Kubernetes │ Observability │ ML Ranking │ Trust & Safety    │
│  CI/CD Canary │ Vault & Secrets                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Clients Layer

| Client | Protocol | Auth |
|--------|----------|------|
| Web App | HTTPS + WebSocket | Session Cookie / JWT |
| Mobile | HTTPS + HTTP/2 | OAuth 2.0 Bearer Token |
| Public API | HTTPS | OAuth 2.0 / API Key |
| Smart TV | HTTPS | Device Code Flow |
| Bots | HTTPS | App-only Bearer |

---

## 3. Edge & API Layer

```mermaid
graph TD
    CLIENT[Client Request] --> LB

    subgraph EDGE_DETAIL["🌐 Edge & API Layer"]
        LB[⚖️ Load Balancer<br/>Anycast · GeoDNS · L4/L7]
        LB --> APIGW[🔑 API Gateway<br/>· AuthN / AuthZ<br/>· Rate Limiting<br/>· Request Routing]
        APIGW --> WS[📡 WebSocket Server<br/>· Real-time tweets<br/>· Push notifications]
        APIGW --> WAF[🛡️ WAF + DDoS Limiter<br/>· Token bucket rate limiting<br/>· Bot fingerprinting]
    end

    WAF --> MICRO[Core Microservices]
    WS --> MICRO
```

---

## 4. Core Microservices

All services communicate via **gRPC** over a **Service Mesh (Envoy/Istio)** with mTLS.

```mermaid
graph TD
    GW[API Gateway] --> SM_MESH

    subgraph SERVICES["⚙️ Core Microservices — gRPC"]
        SM_MESH[🕸️ Service Mesh Envoy/Istio]

        SM_MESH --> UA[👤 User & Auth]
        SM_MESH --> TS[🐦 Tweet Service]
        SM_MESH --> SG[🔗 Social Graph]
        SM_MESH --> TL[📰 Timeline / Feed]
        SM_MESH --> NOTIF[🔔 Notifications]
        SM_MESH --> DM[💬 DM Service]
        SM_MESH --> MEDIA[🖼️ Media Service]
        SM_MESH --> SEARCH[🔍 Search & Trends]
        SM_MESH --> ADS[📢 Ads & Ranking]
    end
```

---

## 5. Event Streaming Backbone

```mermaid
graph LR
    subgraph PRODUCERS["📤 Event Producers"]
        TS2[Tweet Service]
        SG2[Social Graph]
    end

    subgraph KAFKA_CLUSTER["📡 Kafka Cluster"]
        K_TWEET[Topic: tweet.created]
        K_FOLLOW[Topic: user.followed]
    end

    subgraph CONSUMERS["📥 Event Consumers"]
        FAN2[Fan-out Workers]
        FLINK2[Flink Stream Processor]
        SEARCH2[Search Indexer]
        NOTIF2[Notifications]
    end

    PRODUCERS --> KAFKA_CLUSTER
    KAFKA_CLUSTER --> CONSUMERS
```

---

## 6. Storage & Caching Layer

| Data Type | Database | Reason |
|-----------|----------|--------|
| Tweets | **Cassandra** | High write throughput, time-series, append-only |
| User Profiles | **PostgreSQL** | Relational, ACID, low volume |
| Timelines | **Cassandra + Redis** | Pre-computed fan-out cached in Redis |
| Follow Graph | **Graph DB (FlockDB)** | Adjacency list traversal at scale |
| Search Index | **Elasticsearch** | Inverted index, full-text search |
| Media Files | **S3 + CDN** | Object storage, geographically distributed |

---

## 7. Key Flows

### Hybrid Fan-Out Strategy

- **Normal Users (<10k followers)**: **Fan-out on Write** — Tweet push karke sabhi followers ke Redis timeline cache mein likha jaata hai.
- **Celebrity Users (>1M followers)**: **Fan-out on Read** — Tweet directly Cassandra mein save hota hai, aur when follower app kholta hai tab timeline compute aur merge hoti hai.

```mermaid
sequenceDiagram
    participant User
    participant APIGW as API Gateway
    participant Tweet as Tweet Service
    participant Kafka as Kafka
    participant Fanout as Fan-out Workers
    participant Redis as Redis Cache
    participant Cass as Cassandra

    User->>APIGW: POST /tweet {text}
    APIGW->>Tweet: CreateTweet()
    Tweet->>Cass: Write Tweet Payload
    Tweet->>Kafka: Publish tweet.created
    Kafka->>Fanout: Consume Event
    Fanout->>Redis: Push to follower timelines (Fan-out on write)
    Tweet-->>User: 201 Created
```

---

## 8. Data Modeling (Cassandra)

```sql
TABLE tweets (
  tweet_id      TIMEUUID,
  author_id     UUID,
  text          TEXT,
  media_ids     LIST<UUID>,
  like_count    COUNTER,
  retweet_count COUNTER,
  created_at    TIMESTAMP,
  PRIMARY KEY ((author_id), created_at, tweet_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

---

## 9. Numbers to Know (Back-of-Envelope)

| Metric | Estimate |
|--------|----------|
| **DAU** | ~250 million |
| **Tweets / Day** | ~500 million (~5,800 avg TPS) |
| **Peak Write TPS** | ~150,000 TPS |
| **Timeline Reads** | ~300,000 RPS |
| **Daily Storage** | ~100 TB / day (media + logs) |

---

> 📌 **Summary**: Twitter/X Architecture is a classic example of **Event-Driven Microservices** with a **Hybrid Fan-out Pattern** to handle high write throughput and sub-200ms read latency for home timelines.
