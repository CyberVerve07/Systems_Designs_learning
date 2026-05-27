package com.systemdesign.ratelimiter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Distributed Rate Limiter using Redis.
 * Implements a thread-safe Fixed Window Counter utilizing Redis' atomic 'INCR' and 'EXPIRE' commands.
 * Safely falls back to local simulation if Redis is not running.
 */
public class RedisRateLimiter implements RateLimiter {
    
    private final int limit;
    private final int windowSeconds;
    private JedisPool jedisPool;
    private boolean isRedisAvailable = true;
    private RateLimiter fallbackLimiter;

    public RedisRateLimiter(int limit, int windowSeconds, String host, int port) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            this.jedisPool = new JedisPool(poolConfig, host, port);
            
            // Ping to verify connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                System.out.println("✅ Successfully connected to Redis for Rate Limiting!");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Redis is not running or unreachable at " + host + ":" + port);
            System.out.println("   Falling back to an in-memory Sliding Window Counter for demonstration.");
            this.isRedisAvailable = false;
            this.fallbackLimiter = new SlidingWindowCounterRateLimiter(limit, windowSeconds * 1000L);
        }
    }

    @Override
    public boolean allowRequest(String clientId) {
        if (!isRedisAvailable) {
            return fallbackLimiter.allowRequest(clientId);
        }

        String key = "ratelimit:" + clientId + ":" + (System.currentTimeMillis() / (windowSeconds * 1000L));
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Using a pipeline or a simple atomic Transaction / INCR
            long currentCount = jedis.incr(key);
            
            if (currentCount == 1) {
                // Set TTL when key is first created (atomic block window)
                jedis.expire(key, windowSeconds);
            }
            
            return currentCount <= limit;
        } catch (JedisConnectionException e) {
            System.out.println("⚠️ Lost connection to Redis! Falling back to local simulation.");
            this.isRedisAvailable = false;
            this.fallbackLimiter = new SlidingWindowCounterRateLimiter(limit, windowSeconds * 1000L);
            return fallbackLimiter.allowRequest(clientId);
        }
    }

    @Override
    public String getStrategyName() {
        return isRedisAvailable ? "Distributed Fixed Window Counter (Redis-backed)" : "Redis (Fallen Back to In-Memory)";
    }
}
