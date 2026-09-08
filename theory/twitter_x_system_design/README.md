# 🐦 Twitter / X — Complete System Design & Architecture

> A production-grade, end-to-end system design of Twitter/X covering every layer:
> Clients → Edge → Microservices → Event Streaming → Storage → Platform Ops.

---

## 🎨 Visual System Architecture Diagrams

### 1. Overall Twitter / X Architecture Overview
![Twitter System Architecture](./assets/twitter_overall_architecture.jpg)

### 2. Tweet Ingestion & Hybrid Fan-out Architecture
![Twitter Fan-out Architecture](./assets/twitter_fanout_flow.jpg)

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
   - [Post a Tweet](#81-post-a-tweet-flow)
   - [Timeline / Feed Generation](#82-timeline--feed-generation-flow)
   - [Follow a User](#83-follow-a-user-flow)
   - [Search a Tweet](#84-search-a-tweet-flow)
9. [Data Modeling](#9-data-modeling)
10. [Scalability & Fault Tolerance](#10-scalability--fault-tolerance)
11. [Numbers to Know (Back-of-Envelope)](#11-numbers-to-know-back-of-envelope)

---

## 1. High-Level Architecture Overview

The entire system is split into **6 horizontal layers**, each responsible for a specific concern:

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

### Full Architecture Diagram

```mermaid
graph TB
    subgraph CLIENTS["👥 Clients"]
        WEB[Web App]
        MOB[iOS / Android]
        API_PUB[Public API]
        CDN_CLIENT[CDN / Edge]
        TV[Smart TV / Embeds]
        BOTS[3rd-party Bots]
    end

    subgraph EDGE["🌐 Edge & API Layer — Multi-Region Anycast"]
        LB[Load Balancer]
        APIGW[API Gateway<br/>AuthN/Z · Rate Limit]
        WS[WebSocket / Push]
        WAF[WAF + DDoS + Rate Limiter<br/>token-bucket · per-user-quotas]
    end

    subgraph MICRO["⚙️ Core Microservices — gRPC + Service Mesh"]
        UA[User & Auth]
        TS[Tweet Service]
        SG[Social Graph]
        TL[Timeline / Feed]
        NOTIF[Notifications]
        DM[DM Service]
        MEDIA[Media Service]
        SEARCH[Search & Trends]
        ADS[Ads & Ranking]
        SM[Service Mesh<br/>Envoy/Istio<br/>mTLS · retries · circuit breaking]
        MT[Media Transcoding<br/>FFMPEG / DASH builder]
    end

    subgraph STREAM["📡 Event Streaming Backbone — partitioned · exactly-once"]
        KAFKA[Kafka<br/>tweet / follow events]
        FAN[Fan-out Workers]
        FLINK[Stream Processing<br/>Flink]
        CDC[CDC / Debezium + Outbox]
        DLQ[DLQ + Retry Topics]
        SCHEMA[Schema Registry<br/>Avro]
    end

    subgraph STORAGE["🗄️ Storage & Caching — Sharded · Replicated"]
        ES[Elasticsearch<br/>Search Index]
        CASS[Cassandra<br/>Tweets · Timelines]
        PG[PostgreSQL<br/>Users · Graph]
        S3[S3 Object Store<br/>photos / Video]
        LAKE[Data Lake<br/>Analytics / ML]
        GRAPH[Graph DB<br/>FlockDB-like<br/>Follows / Blocks]
        VEC[Vector Store<br/>Embeddings / ANN]
        FS[Feature Store<br/>Online · offline party]
    end

    subgraph OPS["🛠️ Platform & Ops — SRE · SLOs"]
        K8S[Kubernetes]
        OBS[Observability]
        ML[ML Ranking]
        TS_OPS[Trust & Safety]
        CICD[CI/CD · Canary]
        VAULT[Vault & Secrets]
    end

    CLIENTS --> LB
    LB --> APIGW
    APIGW --> WS
    APIGW --> WAF
    WAF --> MICRO
    APIGW --> MICRO
    MICRO --> STREAM
    STREAM --> STORAGE
    STORAGE --> OPS
    MICRO --> OPS
```

---

## 2. Clients Layer

All traffic originates from one of these 5 client types:

```mermaid
graph LR
    subgraph "👥 Client Types"
        W[🌐 Web App<br/>React SPA]
        M[📱 iOS / Android<br/>Native Apps]
        P[🔌 Public API<br/>OAuth 2.0]
        C[📺 CDN / Edge<br/>Static Assets]
        T[📡 Smart TV / Embeds]
        B[🤖 3rd-party Bots<br/>Twitter API v2]
    end

    W & M & P & C & T & B -->|HTTPS / WSS| EDGE[Edge Layer]
```

| Client | Protocol | Auth |
|--------|----------|------|
| Web App | HTTPS + WebSocket | Session Cookie / JWT |
| Mobile | HTTPS + HTTP/2 | OAuth 2.0 Bearer Token |
| Public API | HTTPS | OAuth 2.0 / API Key |
| Smart TV | HTTPS | Device Code Flow |
| Bots | HTTPS | App-only Bearer |

---

## 3. Edge & API Layer

This layer handles **all incoming traffic** before it reaches any microservice.

```mermaid
graph TD
    CLIENT[Client Request] --> LB

    subgraph EDGE_DETAIL["🌐 Edge & API Layer"]
        LB[⚖️ Load Balancer<br/>Anycast · GeoDNS · L4/L7]
        LB --> APIGW[🔑 API Gateway<br/>· AuthN / AuthZ<br/>· Rate Limiting<br/>· Request Routing<br/>· SSL Termination]
        APIGW --> WS[📡 WebSocket / Push Server<br/>· Real-time tweets<br/>· Notification push<br/>· Presence signals]
        APIGW --> WAF[🛡️ WAF + DDoS + Rate Limiter<br/>· Token bucket per user<br/>· IP-based quotas<br/>· Bot fingerprinting]
    end

    WAF --> MICRO[Core Microservices]
    WS --> MICRO
```

### Key Responsibilities

| Component | Responsibility |
|-----------|---------------|
| **Load Balancer** | Anycast routing, health checks, geographic failover |
| **API Gateway** | AuthN/Z, rate limiting, request routing, response caching |
| **WebSocket Server** | Persistent connections for real-time push (tweets, notifications) |
| **WAF / DDoS Limiter** | Block malicious traffic, token-bucket rate limiting, bot detection |

---

## 4. Core Microservices

All services communicate via **gRPC** over a **Service Mesh (Envoy/Istio)** with mTLS.

```mermaid
graph TD
    GW[API Gateway] --> SM_MESH

    subgraph SERVICES["⚙️ Core Microservices — gRPC"]
        SM_MESH[🕸️ Service Mesh<br/>Envoy / Istio<br/>mTLS · retries · circuit breaking]

        SM_MESH --> UA[👤 User & Auth<br/>· Registration / Login<br/>· JWT issuance<br/>· Profile management]
        SM_MESH --> TS[🐦 Tweet Service<br/>· Create / Delete tweets<br/>· Retweet / Quote<br/>· Like / Bookmark]
        SM_MESH --> SG[🔗 Social Graph<br/>· Follow / Unfollow<br/>· Block / Mute<br/>· Followers list]
        SM_MESH --> TL[📰 Timeline / Feed<br/>· Home timeline<br/>· Fan-out on write<br/>· Feed ranking]
        SM_MESH --> NOTIF[🔔 Notifications<br/>· Like / Retweet alerts<br/>· Mention alerts<br/>· DM notifications]
        SM_MESH --> DM[💬 DM Service<br/>· Encrypted DMs<br/>· Group chats]
        SM_MESH --> MEDIA[🖼️ Media Service<br/>· Upload orchestration<br/>· CDN URL generation]
        SM_MESH --> SEARCH[🔍 Search & Trends<br/>· Full-text search<br/>· Hashtag trending<br/>· Autocomplete]
        SM_MESH --> ADS[📢 Ads & Ranking<br/>· Promoted tweets<br/>· CTR prediction<br/>· Auction engine]
        SM_MESH --> MT[🎬 Media Transcoding<br/>· FFMPEG pipeline<br/>· HLS / DASH output<br/>· Thumbnail generation]
    end
```

### Service Communication Pattern

```mermaid
sequenceDiagram
    participant Client
    participant API_GW as API Gateway
    participant SM as Service Mesh (Envoy)
    participant Tweet as Tweet Service
    participant SG as Social Graph
    participant TL as Timeline Service

    Client->>API_GW: POST /tweet {text, media}
    API_GW->>SM: gRPC route → Tweet Service
    SM->>Tweet: CreateTweet(req)
    Tweet->>SG: GetFollowers(userId)
    SG-->>Tweet: [followerId list]
    Tweet->>TL: FanoutTweet(tweetId, followers)
    TL-->>Tweet: ACK
    Tweet-->>API_GW: TweetResponse{id, url}
    API_GW-->>Client: 201 Created
```

---

## 5. Event Streaming Backbone

The backbone is **Apache Kafka** — all state changes produce events, consumed asynchronously.

```mermaid
graph LR
    subgraph PRODUCERS["📤 Event Producers"]
        TS2[Tweet Service]
        SG2[Social Graph]
        UA2[User & Auth]
        MEDIA2[Media Service]
    end

    subgraph KAFKA_CLUSTER["📡 Kafka Cluster — Partitioned · Exactly-Once"]
        K_TWEET[Topic: tweet.created<br/>tweet.deleted<br/>tweet.liked]
        K_FOLLOW[Topic: user.followed<br/>user.unfollowed]
        K_MEDIA[Topic: media.uploaded<br/>media.transcoded]
        DLQ2[DLQ + Retry Topics]
        CDC2[CDC / Debezium + Outbox<br/>DB → Kafka change capture]
        SCHEMA2[Schema Registry<br/>Avro schemas]
    end

    subgraph CONSUMERS["📥 Event Consumers"]
        FAN2[Fan-out Workers<br/>Write to follower timelines]
        FLINK2[Flink Stream Processor<br/>· Trending detection<br/>· Analytics aggregation<br/>· Anomaly detection]
        SEARCH2[Search Indexer<br/>→ Elasticsearch]
        NOTIF2[Notification Sender]
        ML2[ML Feature Pipeline]
    end

    PRODUCERS --> KAFKA_CLUSTER
    KAFKA_CLUSTER --> CONSUMERS
```

### Why Kafka?

| Feature | Benefit for Twitter |
|---------|-------------------|
| **Partitioned Topics** | Scale horizontally — each partition handled by one consumer |
| **Exactly-Once Semantics** | No duplicate tweets in timelines |
| **Log Retention** | Replay events for ML training, debugging |
| **Consumer Groups** | Fan-out workers, Flink, Search indexer all consume same topic independently |
| **CDC + Outbox** | No dual-write problems — DB is source of truth |

---

## 6. Storage & Caching Layer

Different data needs → different databases (polyglot persistence).

```mermaid
graph TD
    subgraph STORAGE_DETAIL["🗄️ Storage Layer — Polyglot Persistence"]
        subgraph HOT["🔥 Hot / Operational Data"]
            CASS2[🟠 Cassandra<br/>Tweets · Timelines<br/>Wide-column · time-series<br/>Sharded by userId + time]
            PG2[🔵 PostgreSQL<br/>Users · Profiles · Graph<br/>ACID transactions<br/>Read replicas]
            REDIS[🔴 Redis Cluster<br/>Timeline cache<br/>Session tokens<br/>Rate limit counters<br/>Hot tweet cache]
        end

        subgraph SEARCH_STORE["🔍 Search & Discovery"]
            ES2[🟡 Elasticsearch<br/>Full-text tweet index<br/>Hashtag index<br/>Geo search]
            VEC2[🟣 Vector Store<br/>Tweet embeddings<br/>ANN similarity search<br/>For recommendations]
        end

        subgraph GRAPH_STORE["🔗 Graph Data"]
            GRAPH2[🔵 Graph DB<br/>FlockDB-like<br/>Follows / Blocks / Mutes<br/>Sharded adjacency lists]
        end

        subgraph COLD["❄️ Cold / Analytical Data"]
            S3_2[☁️ S3 Object Store<br/>Photos / Videos / GIFs<br/>CDN-backed]
            LAKE2[📊 Data Lake<br/>Analytics / ML training<br/>Parquet / Delta Lake]
            FS2[🧠 Feature Store<br/>Online: Redis<br/>Offline: Hive/Spark]
        end
    end
```

### Storage Decision Matrix

| Data Type | Database | Reason |
|-----------|----------|--------|
| Tweets | **Cassandra** | High write throughput, time-series, append-only |
| User Profiles | **PostgreSQL** | Relational, ACID, low volume |
| Timelines | **Cassandra + Redis** | Pre-computed fan-out cached in Redis |
| Follow Graph | **Graph DB (FlockDB)** | Adjacency list traversal at scale |
| Search Index | **Elasticsearch** | Inverted index, full-text, faceted search |
| Media Files | **S3 + CDN** | Object storage, geographically distributed |
| ML Features | **Feature Store** | Online serving + offline training |
| Embeddings | **Vector Store (FAISS)** | ANN search for recommendations |

---

### Caching Strategy

```mermaid
graph LR
    CLIENT2[Client] --> CACHE{Redis Cache Hit?}
    CACHE -->|HIT| RETURN[Return Cached Timeline]
    CACHE -->|MISS| CASS3[Read from Cassandra]
    CASS3 --> POPULATE[Populate Redis Cache<br/>TTL: 5 minutes]
    POPULATE --> RETURN
```

---

## 7. Platform & Ops (SRE)

```mermaid
graph TD
    subgraph OPS_DETAIL["🛠️ Platform & Ops — SRE · SLOs"]
        K8S2[☸️ Kubernetes<br/>· Container orchestration<br/>· Auto-scaling HPA/VPA<br/>· Multi-cluster federation]
        OBS2[📊 Observability<br/>· Metrics: Prometheus + Grafana<br/>· Traces: Jaeger / Zipkin<br/>· Logs: ELK Stack<br/>· Alerts: PagerDuty]
        ML2_OPS[🧠 ML Ranking<br/>· Timeline ranking model<br/>· Ads CTR prediction<br/>· Spam detection]
        TS2_OPS[🛡️ Trust & Safety<br/>· CSAM detection<br/>· Hate speech classifier<br/>· Spam filter<br/>· Account suspension]
        CICD2[🚀 CI/CD · Canary<br/>· Feature flags<br/>· Blue/Green deploys<br/>· Automated rollback]
        VAULT2[🔐 Vault & Secrets<br/>· API key rotation<br/>· DB credential injection<br/>· Encryption key mgmt]
    end

    K8S2 --- OBS2
    OBS2 --- ML2_OPS
    ML2_OPS --- TS2_OPS
    TS2_OPS --- CICD2
    CICD2 --- VAULT2
```

---

## 8. Key Flows — Request Journeys

### 8.1 Post a Tweet Flow

```mermaid
sequenceDiagram
    participant User
    participant APIGW2 as API Gateway
    participant TS3 as Tweet Service
    participant KAFKA3 as Kafka
    participant FAN3 as Fan-out Workers
    participant CASS4 as Cassandra
    participant REDIS3 as Redis
    participant NOTIF3 as Notifications

    User->>APIGW2: POST /2/tweets {text}
    APIGW2->>TS3: gRPC CreateTweet
    TS3->>CASS4: INSERT tweet row
    TS3->>KAFKA3: Produce event → tweet.created
    KAFKA3->>FAN3: Consume tweet.created
    FAN3->>CASS4: Read followers of author
    FAN3->>REDIS3: Push tweetId into follower timeline caches
    FAN3->>NOTIF3: Trigger mention notifications
    TS3-->>APIGW2: TweetId + URL
    APIGW2-->>User: 201 Created {tweetId, url}
```

> **Fan-out on Write** strategy: Pre-compute timelines for all followers so reads are O(1).  
> Exception: **Celebrity users (>1M followers)** → Fan-out on Read to avoid hot partitions.

---

### 8.2 Timeline / Feed Generation Flow

```mermaid
flowchart TD
    A[User opens Home Timeline] --> B{Cache Hit in Redis?}
    B -->|Yes| C[Return cached timeline\nfrom Redis sorted set]
    B -->|No| D[Read from Cassandra\ntimeline table]
    D --> E[Ranking Service\nML score each tweet]
    E --> F[Inject Ads\nfrom Ads Service]
    F --> G[Populate Redis Cache\nTTL: 5 min]
    G --> C
    C --> H[Stream to client\nvia HTTP/2 or WebSocket]
```

---

### 8.3 Follow a User Flow

```mermaid
sequenceDiagram
    participant User
    participant SG3 as Social Graph Service
    participant GRAPH3 as Graph DB
    participant KAFKA4 as Kafka
    participant FAN4 as Fan-out Workers
    participant TL3 as Timeline Service

    User->>SG3: POST /follow {targetUserId}
    SG3->>GRAPH3: Add edge User→Target
    SG3->>KAFKA4: Produce → user.followed
    KAFKA4->>FAN4: Consume user.followed
    FAN4->>TL3: Back-fill recent tweets of Target into User's timeline
    TL3-->>User: Timeline updated (next refresh)
    SG3-->>User: 200 OK
```

---

### 8.4 Search a Tweet Flow

```mermaid
sequenceDiagram
    participant User
    participant APIGW3 as API Gateway
    participant SRCH as Search Service
    participant ES3 as Elasticsearch
    participant CASS5 as Cassandra

    User->>APIGW3: GET /search?q=bitcoin
    APIGW3->>SRCH: gRPC Search(query)
    SRCH->>ES3: Full-text query\n+ trend boost scoring
    ES3-->>SRCH: [tweetId list + scores]
    SRCH->>CASS5: Hydrate tweet objects by ID
    CASS5-->>SRCH: [tweet payloads]
    SRCH-->>APIGW3: Ranked results
    APIGW3-->>User: 200 OK [tweets]
```

---

## 9. Data Modeling

### Tweet Table (Cassandra)

```sql
TABLE tweets (
  tweet_id      TIMEUUID,       -- sorted by time globally
  author_id     UUID,
  text          TEXT,
  media_ids     LIST<UUID>,
  reply_to_id   UUID,
  like_count    COUNTER,
  retweet_count COUNTER,
  created_at    TIMESTAMP,
  PRIMARY KEY ((author_id), created_at, tweet_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

### Timeline Table (Cassandra — Fan-out materialized view)

```sql
TABLE user_timeline (
  user_id       UUID,           -- the owner of the timeline
  tweet_id      TIMEUUID,       -- sorted newest-first
  author_id     UUID,
  score         FLOAT,          -- ML ranking score
  PRIMARY KEY ((user_id), score, tweet_id)
) WITH CLUSTERING ORDER BY (score DESC, tweet_id DESC);
```

### Social Graph (Graph DB)

```
NODES:  User {userId, username, verified}
EDGES:  FOLLOWS  (User → User)  {since: timestamp}
        BLOCKS   (User → User)
        MUTES    (User → User)
```

### Entity Relationship (High Level)

```mermaid
erDiagram
    USER {
        uuid user_id PK
        string username
        string email
        string password_hash
        bool verified
        timestamp created_at
    }
    TWEET {
        uuid tweet_id PK
        uuid author_id FK
        text content
        uuid reply_to FK
        int like_count
        int retweet_count
        timestamp created_at
    }
    FOLLOW {
        uuid follower_id FK
        uuid following_id FK
        timestamp created_at
    }
    MEDIA {
        uuid media_id PK
        uuid tweet_id FK
        string s3_url
        string type
    }
    LIKE {
        uuid user_id FK
        uuid tweet_id FK
        timestamp created_at
    }

    USER ||--o{ TWEET : "authors"
    USER ||--o{ FOLLOW : "follows"
    TWEET ||--o{ MEDIA : "has"
    USER ||--o{ LIKE : "likes"
    TWEET ||--o{ LIKE : "receives"
    TWEET ||--o{ TWEET : "reply_to"
```

---

## 10. Scalability & Fault Tolerance

```mermaid
graph TD
    subgraph SCALE["📈 Scalability Strategies"]
        HS[Horizontal Sharding<br/>Cassandra: userId hash shard<br/>Kafka: partitioned by userId]
        REPLICA[Read Replicas<br/>PostgreSQL read replicas<br/>Elasticsearch replica shards]
        CACHE[Aggressive Caching<br/>Redis for timelines<br/>CDN for media]
        FANOUT[Hybrid Fan-out<br/>Write for normal users<br/>Read for celebrities]
        ASYNC[Async Processing<br/>Kafka decouples write path<br/>from fan-out latency]
    end

    subgraph FAULT["🛡️ Fault Tolerance"]
        CB[Circuit Breakers<br/>Envoy auto-trips on errors]
        RETRY[Retry + DLQ<br/>Failed events go to DLQ<br/>Retried with backoff]
        MULTI[Multi-Region Active-Active<br/>GeoDNS routes to nearest<br/>Cross-region replication]
        CANARY[Canary Deployments<br/>5% → 25% → 100% rollout<br/>Auto-rollback on SLO breach]
    end
```

### SLOs (Service Level Objectives)

| Metric | Target |
|--------|--------|
| Timeline load p99 latency | < 200ms |
| Tweet post latency p99 | < 500ms |
| Search latency p99 | < 300ms |
| Availability | 99.99% (52 min/year downtime) |
| Fan-out lag | < 5 seconds for 99% of users |

---

## 11. Numbers to Know (Back-of-Envelope)

| Metric | Estimate |
|--------|----------|
| **DAU** | ~250 million |
| **Tweets per day** | ~500 million |
| **Tweets per second (avg)** | ~5,800 TPS |
| **Tweets per second (peak)** | ~150,000 TPS |
| **Timeline reads per second** | ~300,000 RPS |
| **Avg followers per user** | ~200 |
| **Fan-out writes per tweet** | 200 × 5,800 = ~1.1M writes/sec |
| **Media storage growth** | ~100 TB/day |
| **Kafka events per day** | ~10 billion |

### Storage Estimation

```
Tweets per day:    500M × 300 bytes      = 150 GB/day
Media per day:     ~100 TB/day (photos/video)
Timeline cache:    ~1 KB per user × 250M = 250 GB (Redis)
Total 5-year data: ~200 PB
```

---

## 🗺️ Full System Map (Summary)

```mermaid
flowchart TB
    CL["👥 Clients\nWeb · Mobile · API · TV · Bots"]
    EDGE2["🌐 Edge Layer\nLB → API GW → WAF → WebSocket"]
    MICRO2["⚙️ Microservices\nUser · Tweet · Graph · Timeline · Search · Notif · DM · Ads · Media"]
    KAFKA5["📡 Kafka Backbone\nExactly-once · Partitioned"]
    WORKERS["🔧 Workers\nFan-out · Flink · Indexer · Transcoding"]
    DB["🗄️ Storage\nCassandra · PostgreSQL · Redis · ES · S3 · GraphDB · VectorDB"]
    OPS2["🛠️ Ops\nK8s · Observability · ML · Trust & Safety · CI/CD · Vault"]

    CL --> EDGE2
    EDGE2 --> MICRO2
    MICRO2 --> KAFKA5
    KAFKA5 --> WORKERS
    WORKERS --> DB
    DB --> OPS2
    MICRO2 --> DB
    OPS2 -.->|monitors| MICRO2
    OPS2 -.->|monitors| KAFKA5
    OPS2 -.->|monitors| DB
```

---

> 📌 **Key Takeaway**: Twitter/X is fundamentally a **fan-out problem** at scale.
> The entire architecture — Kafka, Cassandra, Redis, hybrid fan-out — exists to solve
> the challenge of delivering 1 tweet to potentially millions of followers in near-real-time,
> while maintaining sub-200ms read latency for home timelines.
