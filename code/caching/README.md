# Caching Code Examples

This directory contains practical implementations of caching strategies discussed in the theory section.

## Files Structure

- `redis_cache_example/` - Redis implementation with Java
- `simple_in_memory_cache/` - Basic in-memory cache implementation
- `cache_patterns/` - Different caching patterns (Cache-Aside, Write-Through, etc.)
- `performance_comparison/` - Performance benchmarks

## Setup Instructions

### Redis Setup
1. Install Redis locally or use Docker:
   ```bash
   docker run -d -p 6379:6379 redis:latest
   ```

2. Add Redis dependency to your project (Maven):
   ```xml
   <dependency>
       <groupId>redis.clients</groupId>
       <artifactId>jedis</artifactId>
       <version>5.0.0</version>
   </dependency>
   ```

## Running Examples

Each subdirectory contains a README with specific instructions for that implementation.
