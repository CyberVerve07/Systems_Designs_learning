# Redis Cache Example

This project demonstrates practical implementation of caching strategies using Redis and Java.

## Prerequisites

1. **Java 11+** installed
2. **Maven** for dependency management
3. **Redis Server** running on localhost:6379

## Setup Redis

### Option 1: Docker (Recommended)
```bash
docker run -d -p 6379:6379 --name redis-cache redis:latest
```

### Option 2: Local Installation
Download and install Redis from [redis.io](https://redis.io/download)

## Build and Run

### 1. Build the project
```bash
mvn clean compile
```

### 2. Run the demo
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.cache.CacheDemo"
```

## What You'll Learn

### 1. **Cache-Aside Pattern**
- First request: Cache miss → Database → Cache
- Second request: Cache hit → Direct from cache
- Performance improvement: ~1000x faster

### 2. **Write-Through Pattern**
- Update database and cache simultaneously
- Ensures cache and database are always in sync
- Slightly slower writes but guaranteed consistency

### 3. **Write-Behind Pattern**
- Update cache immediately, database asynchronously
- Very fast writes
- Risk of data loss if system fails

### 4. **Cache Invalidation**
- Remove outdated data from cache
- Force fresh data on next request

## Key Files

- `CacheDemo.java` - Main demonstration class
- `RedisCacheService.java` - Cache service with different patterns
- `UserDatabase.java` - Simulated database with delays
- `User.java` - Data model

## Expected Output

```
🚀 Starting Cache Demo...

📚 DEMO 1: Cache-Aside Pattern
First request (cache miss):
📡 DATABASE: Reading user 1 from database...
✅ DATABASE: Found user 1
💾 CACHE: User 1 cached successfully
Result: User{id=1, name='John Doe', email='john@example.com', age=25}
Time taken: 1005ms

Second request (cache hit):
🎯 CACHE HIT: User 1 found in cache
Result: User{id=1, name='John Doe', email='john@example.com', age=25}
Time taken: 15ms
```

## Performance Metrics

- **Database Read**: ~1000ms (simulated)
- **Cache Read**: ~10-20ms
- **Speedup**: 50-100x faster for cache hits

## Troubleshooting

### Redis Connection Error
```
redis.clients.jedis.exceptions.JedisConnectionException: Connection refused
```
**Solution**: Make sure Redis is running on localhost:6379

### Maven Build Issues
```bash
mvn clean install
```
**Solution**: Ensure Maven is properly configured and Java 11+ is installed

## Next Steps

1. Try modifying TTL (time-to-live) values
2. Implement different eviction policies
3. Add monitoring and metrics
4. Experiment with different data structures in Redis

## Real-World Applications

- **Session Storage**: User login tokens
- **Product Catalog**: E-commerce product details
- **Leaderboards**: Gaming scores
- **Real-time Analytics**: Live metrics and dashboards
