# Database Sharding Example

This project demonstrates practical implementation of database sharding strategies using Java and H2 in-memory database.

## What is Sharding?

Sharding is a database scaling technique that splits a large database into smaller, more manageable pieces called **shards**. Each shard contains a subset of the data and runs on a separate server.

## Sharding Strategies Demonstrated

### 1. **Hash-Based Sharding**
- Uses hash function: `hash(userId) % numShards`
- **Pros**: Even distribution, no hotspots
- **Cons**: Rebalancing difficult when adding shards
- **Best for**: Uniformly distributed data

### 2. **Range-Based Sharding**
- Splits data based on ID ranges
- **Example**: Users 1-100 → Shard 0, Users 101-200 → Shard 1
- **Pros**: Simple to understand, easy range queries
- **Cons**: Uneven distribution, hotspots possible
- **Best for**: Sequential data, time-series data

## Build and Run

### 1. Build the project
```bash
mvn clean compile
```

### 2. Run the demo
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.database.ShardingDemo"
```

## What You'll Learn

### **Demo 1: Hash-Based Sharding**
- Insert 10 users across 3 shards
- See how hash function distributes data
- Query specific users from correct shard
- Update and delete operations

### **Demo 2: Range-Based Sharding**
- Insert users across different ID ranges
- Understand range-based distribution
- Query users from different ranges
- Compare with hash-based approach

### **Demo 3: Performance Comparison**
- Insert 1000 users using sharding
- Measure read/write performance
- Understand scalability benefits

## Key Files

- `ShardingManager.java` - Manages multiple shards and routes requests
- `DatabaseShard.java` - Represents a single database shard
- `HashBasedSharding.java` - Hash-based sharding strategy
- `RangeBasedSharding.java` - Range-based sharding strategy
- `ShardingDemo.java` - Main demonstration class

## Expected Output

```
🚀 Database Sharding Demo

📚 DEMO 1: Hash-Based Sharding
Strategy: Uses hash function (userId % numShards) to distribute data

🚀 Sharding Manager initialized with 3 shards
📊 Strategy: Hash-Based Sharding

Inserting users...
💾 Shard 1: User 1 inserted
💾 Shard 2: User 2 inserted
💾 Shard 0: User 3 inserted
...

📊 Shard Distribution Statistics:
==================================================
Shard 0: 3 users (30.0%)
Shard 1: 4 users (40.0%)
Shard 2: 3 users (30.0%)
==================================================
Total Users: 10
```

## Sharding Challenges

### **1. Rebalancing**
When adding/removing shards, data must be redistributed.
**Solution**: Consistent hashing (minimizes data movement)

### **2. Cross-Shard Queries**
Queries that need data from multiple shards.
**Solution**: Application-level joins, denormalization

### **3. Data Consistency**
Keeping data consistent across shards.
**Solution**: Two-phase commit, eventual consistency

### **4. Complex Transactions**
ACID transactions across shards are difficult.
**Solution**: Saga pattern, distributed transactions

## Real-World Examples

### **Instagram**
- User data sharded by user ID
- Posts sharded by post ID
- Uses hash-based sharding for even distribution

### **Twitter**
- Tweets sharded by tweet ID
- User timelines sharded by user ID
- Uses consistent hashing for rebalancing

### **Uber**
- Trip data sharded by city
- User profiles sharded by user ID
- Uses range-based sharding for geographic data

## Next Steps

1. Implement consistent hashing for dynamic shard addition
2. Add cross-shard query support
3. Implement distributed transactions
4. Add monitoring and metrics for each shard

## Troubleshooting

### Maven Build Issues
```bash
mvn clean install
```
**Solution**: Ensure Maven is properly configured and Java 11+ is installed

### Database Connection Issues
**Solution**: H2 is in-memory, no external database needed. If issues persist, check Java version compatibility.
