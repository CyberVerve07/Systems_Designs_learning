# Database Design Code Examples

This directory contains practical implementations of database design patterns discussed in the theory section.

## Files Structure

- `sharding_example/` - Database sharding implementation in Java with in-memory H2 databases, covering:
  - Hash-based sharding strategy
  - Range-based sharding strategy
  - Shard manager routing and distribution statistics

## Setup Instructions

### SQL Database (H2 - In-Memory for Demo)
No installation needed - uses H2 in-memory database for demonstrations.

### NoSQL Database (MongoDB)
1. Install MongoDB locally or use Docker:
   ```bash
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

2. Add MongoDB dependency to your project (Maven):
   ```xml
   <dependency>
       <groupId>org.mongodb</groupId>
       <artifactId>mongodb-driver-sync</artifactId>
       <version>4.10.0</version>
   </dependency>
   ```

## Running Examples

Each subdirectory contains a README with specific instructions for that implementation.

## Key Concepts Demonstrated

1. **Sharding Strategies**: Hash-based, Range-based, Directory-based
2. **Connection Pooling**: HikariCP for efficient connection management
3. **SQL vs NoSQL**: Same data modeled in both paradigms
4. **Indexing Performance**: Impact of indexes on query performance
