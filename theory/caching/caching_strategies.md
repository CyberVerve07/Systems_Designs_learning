# Day 2: Caching Strategies

Welcome to Day 2 of our System Design journey! Today we're diving deep into **Caching** - one of the most powerful techniques for building high-performance systems.

## What is Caching?

In simple terms, **Caching** is the process of storing frequently accessed data in a temporary storage location (cache) so that future requests for that data can be served faster.

**Real-world Analogy:** Think about your kitchen vs. a supermarket.
- **Supermarket** = Main Database (slow, far away, has everything)
- **Kitchen Refrigerator** = Cache (fast, nearby, has frequently used items)
- When you need milk, you first check your refrigerator. If it's there, you grab it quickly. If not, you go to the supermarket.

## Why Do We Need Caching?

### The Performance Problem
- **Database queries are expensive**: Reading from disk is 100,000x slower than reading from RAM
- **Network latency**: Remote API calls take time (hundreds of ms)
- **Computation costs**: Complex calculations take CPU time

### The Solution: Caching
1. **Reduced Latency**: Data served from cache is much faster
2. **Reduced Load**: Fewer requests hit your database/backend services
3. **Improved Scalability**: Handle more users with same infrastructure
4. **Better User Experience**: Faster response times

---

## Types of Caching

### 1. **Client-Side Caching**
- **Browser Cache**: Stores static assets (CSS, JS, images) in user's browser
- **App Cache**: Mobile apps cache data locally
- **Pros**: Reduces network requests, works offline
- **Cons**: Limited storage, cache invalidation is tricky

### 2. **Server-Side Caching**
- **In-Memory Cache**: Data stored in RAM (Redis, Memcached)
- **Database Cache**: Query result caching
- **CDN Cache**: Geographic distribution of static content

### 3. **CDN (Content Delivery Network)**
- **What it is**: Network of servers distributed globally
- **How it works**: Routes users to nearest server
- **Example**: User in India gets content from Mumbai server, not US server

---

## Popular Caching Technologies

### **Redis (Remote Dictionary Server)**
- **Type**: In-memory data structure store
- **Features**: Key-value store, supports multiple data types (strings, lists, sets, hashes)
- **Persistence**: Can save data to disk (optional)
- **Use Cases**: Session storage, leaderboards, real-time analytics

### **Memcached**
- **Type**: In-memory key-value store
- **Features**: Simple, lightweight, multi-threaded
- **Limitations**: Only strings, no persistence
- **Use Cases**: Simple caching, database query results

### **CDN Providers**
- **Cloudflare**: Security + CDN
- **AWS CloudFront**: AWS ecosystem integration
- **Fastly**: Real-time CDN with edge computing

---

## Caching Strategies

### 1. **Cache-Aside (Lazy Loading)**
```
1. Application checks cache first
2. If cache miss: query database
3. Store result in cache
4. Return data to user
```
**Pros**: Simple to implement, cache only contains needed data
**Cons**: First request is always slow (cache miss)

### 2. **Write-Through Cache**
```
1. Write to cache first
2. Write to database immediately
3. Return success
```
**Pros**: Cache and database always in sync
**Cons**: Higher write latency

### 3. **Write-Behind (Write-Back)**
```
1. Write to cache immediately
2. Write to database asynchronously (batch)
3. Return success
```
**Pros**: Very fast writes, reduces database load
**Cons**: Risk of data loss if cache fails

### 4. **Read-Through Cache**
```
1. Application always reads from cache
2. Cache manages database queries on miss
3. Application code is simpler
```
**Pros**: Simplified application code
**Cons**: Less control over caching logic

---

## Cache Eviction Policies

When cache is full, which data to remove?

### **LRU (Least Recently Used)**
- Removes items that haven't been accessed for longest time
- **Example**: Browser cache

### **LFU (Least Frequently Used)**
- Removes items accessed least frequently
- **Good for**: Data with predictable access patterns

### **FIFO (First In, First Out)**
- Removes oldest items first
- **Simple but not always optimal**

### **TTL (Time To Live)**
- Items expire after set time
- **Example**: Session data expires after 30 minutes

---

## Common Caching Patterns

### **Multi-Level Caching**
```
Browser Cache → CDN Cache → Application Cache → Database
```
Each level serves as cache for the next level.

### **Cache Warming**
- Pre-populate cache with frequently accessed data
- **Example**: Load popular products into cache during startup

### **Cache Invalidation**
The hardest problem in computer science!

**Strategies:**
1. **Time-based**: Cache expires after X time
2. **Event-based**: Clear cache when data changes
3. **Manual**: Admin manually clears cache

---

## When NOT to Use Cache

1. **Frequently changing data**: Stock prices, real-time scores
2. **Critical data**: Bank transactions, medical records
3. **Large datasets**: Cache size limitations
4. **Write-heavy workloads**: Cache invalidation overhead

---

## Real-World Examples

### **Facebook**
- **Edge Caching**: Static content served from CDN
- **Database Caching**: User profiles, friend lists
- **Session Caching**: Login tokens, user preferences

### **Netflix**
- **Video Streaming**: Cached at CDN edge locations
- **Metadata**: Movie information cached globally
- **User Profiles**: Personalized recommendations

### **Amazon**
- **Product Catalog**: Product details cached
- **Search Results**: Popular searches cached
- **User Sessions**: Shopping cart data cached

---

## Key Metrics to Monitor

1. **Cache Hit Ratio**: % of requests served from cache
   - **Good**: 90%+ for read-heavy workloads
2. **Cache Miss Ratio**: % of requests requiring backend fetch
3. **Cache Latency**: Time to read from cache
4. **Eviction Rate**: How often items are removed from cache

---

## Summary

Caching is fundamental to building scalable systems:
- **Reduces latency** by serving data from faster storage
- **Decreases load** on backend systems
- **Improves user experience** with faster response times
- **Enables scalability** to handle more traffic

**Remember**: Caching adds complexity, so start simple and add caching where it provides the most benefit!

---

## Next Steps

In our next lesson, we'll implement caching with practical code examples using Redis and explore real-world caching scenarios.
