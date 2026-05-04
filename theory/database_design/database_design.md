# Day 3: Database Design & Scaling

Welcome to Day 3 of our System Design journey! Today we're diving deep into **Database Design** - the foundation of any scalable system.

## What is Database Design?

**Database Design** is the process of structuring data in a way that optimizes storage, retrieval, and management. It's about choosing the right database type, schema, and architecture for your application's needs.

**Real-world Analogy:** Think about organizing a library.
- **SQL Database** = Organized shelves with books categorized by author, genre, ISBN (structured)
- **NoSQL Database** = A giant pile of books where you can find any book by any attribute (flexible)

---

## SQL vs NoSQL: The Fundamental Choice

### **SQL (Relational Databases)**

**Examples:** MySQL, PostgreSQL, Oracle, SQL Server

**Characteristics:**
- **Structured Data**: Data stored in tables with rows and columns
- **Fixed Schema**: Must define structure before inserting data
- **ACID Compliance**: Atomicity, Consistency, Isolation, Durability
- **Relationships**: Foreign keys, joins between tables
- **Transactions**: All-or-nothing operations

**Best For:**
- Financial applications (banking, payments)
- E-commerce (orders, inventory)
- CRM systems
- Applications with complex relationships

**Example Schema:**
```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    total DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### **NoSQL (Non-Relational Databases)**

**Examples:** MongoDB, Cassandra, DynamoDB, Redis

**Types of NoSQL:**

1. **Document Stores** (MongoDB, CouchDB)
   - Store data as JSON/BSON documents
   - Flexible schema
   - Example: User profiles, product catalogs

2. **Key-Value Stores** (Redis, DynamoDB)
   - Simple key-value pairs
   - Fast lookups
   - Example: Caching, session storage

3. **Column-Family Stores** (Cassandra, HBase)
   - Wide-column storage
   - High write throughput
   - Example: Time-series data, IoT

4. **Graph Databases** (Neo4j)
   - Store relationships as edges
   - Example: Social networks, recommendation engines

**Characteristics:**
- **Flexible Schema**: Can change structure on the fly
- **Horizontal Scalability**: Easy to distribute across servers
- **BASE Model**: Basically Available, Soft state, Eventual consistency
- **No Joins**: Data denormalized, stored together

**Best For:**
- Social media feeds
- Real-time analytics
- Content management systems
- Applications with evolving data structures

**Example Document (MongoDB):**
```json
{
    "_id": "user123",
    "name": "John Doe",
    "email": "john@example.com",
    "orders": [
        {"id": 1, "total": 99.99},
        {"id": 2, "total": 149.99}
    ],
    "preferences": {
        "notifications": true,
        "theme": "dark"
    }
}
```

---

## ACID vs BASE: Consistency Models

### **ACID (SQL Databases)**

**A - Atomicity**: All operations in a transaction succeed or all fail
**C - Consistency**: Database remains in valid state after transaction
**I - Isolation**: Transactions don't interfere with each other
**D - Durability**: Once committed, data persists even if system crashes

**Example:** Bank transfer - Money debited from Account A and credited to Account B must happen together or not at all.

### **BASE (NoSQL Databases)**

**B - Basically Available**: System always responds (might have stale data)
**A - Soft State**: Data can change over time
**E - Eventual Consistency**: System becomes consistent eventually

**Example:** Social media likes - When you like a post, it might not show immediately to all users, but will eventually.

---

## Database Scaling Strategies

### **Vertical Scaling (Scale Up)**

**What it is:** Adding more resources to a single server (more CPU, RAM, storage)

**Pros:**
- Simple to implement
- No code changes needed
- Maintains ACID properties

**Cons:**
- Expensive (high-end servers cost a lot)
- Single point of failure
- Physical limits (can't add infinite RAM)

**When to use:** Small to medium applications, early stage startups

---

### **Horizontal Scaling (Scale Out)**

**What it is:** Adding more servers to distribute the load

**Pros:**
- Cost-effective (use commodity hardware)
- No single point of failure
- Virtually unlimited scaling

**Cons:**
- Complex to implement
- Data consistency challenges
- Requires sharding or replication

**When to use:** Large-scale applications, high traffic systems

---

## Database Replication

### **What is Replication?**

Copying data from one database server to another for:
- **High Availability**: If primary fails, secondary takes over
- **Read Scalability**: Distribute read operations across replicas
- **Disaster Recovery**: Backup in different geographic locations

### **Replication Types:**

#### **1. Master-Slave (Primary-Replica)**
```
[Primary] → Writes
    ↓
[Replica 1] → Reads
[Replica 2] → Reads
[Replica 3] → Reads
```
- **How it works:** All writes go to primary, data replicated to replicas
- **Pros:** Simple, read scalability
- **Cons:** Single write bottleneck, replication lag

#### **2. Master-Master (Multi-Master)**
```
[Primary 1] ↔ [Primary 2]
    ↓           ↓
[Replica 1]  [Replica 2]
```
- **How it works:** Multiple servers can accept writes
- **Pros:** Write scalability, high availability
- **Cons:** Conflict resolution complexity

#### **3. Leaderless (Cassandra, DynamoDB)**
```
[Node 1] ↔ [Node 2] ↔ [Node 3]
```
- **How it works:** Any node can accept reads/writes
- **Pros:** No single point of failure
- **Cons:** Eventual consistency, complex coordination

---

## Database Sharding

### **What is Sharding?**

Breaking a large database into smaller, more manageable pieces called **shards**. Each shard contains a subset of the data.

**Analogy:** Splitting a large book into multiple volumes. Each volume has different chapters, but together they make the complete book.

### **Sharding Strategies:**

#### **1. Horizontal Sharding (Range-based)**
```
Users 1-1000    → Shard 1
Users 1001-2000 → Shard 2
Users 2001-3000 → Shard 3
```
- **How it works:** Data split by range (e.g., user ID ranges)
- **Pros:** Simple to understand
- **Cons:** Uneven distribution (hotspots)

#### **2. Vertical Sharding (Feature-based)**
```
User Profiles → Shard 1
User Orders   → Shard 2
User Payments → Shard 3
```
- **How it works:** Different features on different shards
- **Pros:** Easy to implement, isolates hot features
- **Cons:** Cross-shard queries difficult

#### **3. Hash-based Sharding**
```
hash(user_id) % number_of_shards = shard_number
```
- **How it works:** Apply hash function to determine shard
- **Pros:** Even distribution, no hotspots
- **Cons:** Rebalancing difficult when adding shards

#### **4. Directory-based Sharding**
```
[Lookup Service] → Maps user_id to shard
```
- **How it works:** Central service tracks which data is on which shard
- **Pros:** Flexible, easy to rebalance
- **Cons:** Additional lookup service overhead

---

## Sharding Challenges

### **1. Rebalancing**
When adding/removing shards, data must be redistributed.
**Solution:** Consistent hashing (minimizes data movement)

### **2. Cross-Shard Queries**
Queries that need data from multiple shards.
**Solution:** Application-level joins, denormalization

### **3. Data Consistency**
Keeping data consistent across shards.
**Solution:** Two-phase commit, eventual consistency

### **4. Complex Transactions**
ACID transactions across shards are difficult.
**Solution:** Saga pattern, distributed transactions

---

## Database Indexing

### **What is an Index?**

An index is a data structure that improves the speed of data retrieval operations on a database table.

**Analogy:** Index at the back of a book - you can quickly find pages where a topic appears instead of reading the entire book.

### **Types of Indexes:**

#### **1. B-Tree Index (Most Common)**
- Balanced tree structure
- Good for equality and range queries
- Used by: MySQL, PostgreSQL

#### **2. Hash Index**
- Hash table structure
- Only for equality queries
- Used by: Redis, Memcached

#### **3. Full-Text Index**
- For searching text content
- Used by: Elasticsearch, MongoDB

#### **4. Composite Index**
- Index on multiple columns
- Example: `(last_name, first_name)`

### **Index Trade-offs:**
- **Pros:** Faster reads
- **Cons:** Slower writes (indexes must be updated), more storage

---

## Database Normalization vs Denormalization

### **Normalization**
**Goal:** Eliminate data redundancy, ensure data integrity

**Rules:**
- **1NF:** No repeating groups
- **2NF:** No partial dependencies
- **3NF:** No transitive dependencies

**Example:**
```sql
-- Normalized
users (id, name, email)
orders (id, user_id, total, date)
```

**Pros:** Less storage, data consistency
**Cons:** More joins, slower reads

---

### **Denormalization**
**Goal:** Improve read performance by adding redundancy

**Example:**
```sql
-- Denormalized
users (id, name, email, last_order_date, total_spent)
```

**Pros:** Faster reads, fewer joins
**Cons:** More storage, data inconsistency risk

**When to use:** Read-heavy workloads, NoSQL databases

---

## Choosing the Right Database

### **Decision Framework:**

| Factor | Choose SQL | Choose NoSQL |
|--------|-----------|--------------|
| **Data Structure** | Fixed, structured | Flexible, evolving |
| **Scale** | Vertical scaling | Horizontal scaling |
| **Consistency** | Strong consistency required | Eventual consistency OK |
| **Relationships** | Complex relationships | Simple relationships |
| **Queries** | Complex joins | Simple lookups |
| **Schema Changes** | Difficult | Easy |

### **Real-World Examples:**

**Instagram:**
- PostgreSQL for user data (relationships important)
- Cassandra for feed (high write throughput)
- Redis for caching (fast access)

**Uber:**
- PostgreSQL for financial transactions (ACID required)
- MongoDB for trip data (flexible schema)
- Redis for real-time tracking

**Netflix:**
- Cassandra for video metadata (high availability)
- Elasticsearch for search (full-text search)
- DynamoDB for user profiles (scalability)

---

## Database Performance Optimization

### **1. Query Optimization**
- Use indexes effectively
- Avoid SELECT *
- Use WHERE clauses to filter data
- Optimize JOIN operations

### **2. Connection Pooling**
- Reuse database connections
- Reduce connection overhead
- Example: HikariCP (Java)

### **3. Read Replicas**
- Offload read queries to replicas
- Reduce load on primary

### **4. Partitioning**
- Split large tables into smaller partitions
- Improve query performance

### **5. Caching**
- Cache frequently accessed data
- Reduce database load

---

## Summary

Database design is critical for system scalability:

**Key Takeaways:**
1. **SQL vs NoSQL**: Choose based on data structure and consistency requirements
2. **Scaling**: Vertical for simplicity, Horizontal for scale
3. **Replication**: High availability and read scalability
4. **Sharding**: Distribute data across multiple servers
5. **Indexing**: Speed up reads at the cost of writes
6. **Normalization vs Denormalization**: Trade-off between storage and performance

**Remember:** There's no "perfect" database - choose based on your specific requirements!

---

## Next Steps

In our next lesson, we'll implement database design patterns with practical code examples including sharding strategies and performance optimization techniques.
