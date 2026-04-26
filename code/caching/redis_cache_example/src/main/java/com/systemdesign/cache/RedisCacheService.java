package com.systemdesign.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * Redis Cache Service - Implements various caching patterns
 */
public class RedisCacheService {
    private static RedisCacheService instance;
    private JedisPool jedisPool;
    private ObjectMapper objectMapper;
    
    private RedisCacheService() {
        // Configure Redis connection pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        
        // Connect to Redis (localhost:6379)
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        this.objectMapper = new ObjectMapper();
    }
    
    public static synchronized RedisCacheService getInstance() {
        if (instance == null) {
            instance = new RedisCacheService();
        }
        return instance;
    }
    
    /**
     * Cache-Aside Pattern: Get user with caching
     */
    public User getUserWithCache(int userId) {
        String cacheKey = "user:" + userId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 1. Try to get from cache first
            String cachedUser = jedis.get(cacheKey);
            
            if (cachedUser != null) {
                System.out.println("🎯 CACHE HIT: User " + userId + " found in cache");
                return objectMapper.readValue(cachedUser, User.class);
            }
            
            // 2. Cache miss - get from database
            System.out.println("❌ CACHE MISS: User " + userId + " not in cache, fetching from database");
            User user = UserDatabase.getInstance().getUserById(userId);
            
            if (user != null) {
                // 3. Store in cache for future requests
                String userJson = objectMapper.writeValueAsString(user);
                jedis.setex(cacheKey, 300, userJson); // Cache for 5 minutes (300 seconds)
                System.out.println("💾 CACHE: User " + userId + " cached successfully");
            }
            
            return user;
            
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Write-Through Pattern: Update user and cache immediately
     */
    public void updateUserWithCache(User user) {
        String cacheKey = "user:" + user.getId();
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 1. Update database first
            UserDatabase.getInstance().updateUser(user);
            
            // 2. Update cache immediately
            String userJson = objectMapper.writeValueAsString(user);
            jedis.setex(cacheKey, 300, userJson);
            System.out.println("💾 CACHE: User " + user.getId() + " updated in cache");
            
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        }
    }
    
    /**
     * Write-Behind Pattern: Update cache first, database later (async)
     */
    public void updateUserAsync(User user) {
        String cacheKey = "user:" + user.getId();
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 1. Update cache immediately
            String userJson = objectMapper.writeValueAsString(user);
            jedis.setex(cacheKey, 300, userJson);
            System.out.println("💾 CACHE: User " + user.getId() + " updated in cache (async write)");
            
            // 2. Schedule database update asynchronously
            new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2); // Simulate async delay
                    UserDatabase.getInstance().updateUser(user);
                    System.out.println("🔄 DATABASE: Async update completed for user " + user.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        }
    }
    
    /**
     * Cache Invalidation: Remove user from cache
     */
    public void invalidateUserCache(int userId) {
        String cacheKey = "user:" + userId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            Long result = jedis.del(cacheKey);
            if (result > 0) {
                System.out.println("🗑️ CACHE: User " + userId + " removed from cache");
            } else {
                System.out.println("ℹ️ CACHE: User " + userId + " was not in cache");
            }
        }
    }
    
    /**
     * Clear all cache (for testing)
     */
    public void clearAllCache() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
            System.out.println("🗑️ CACHE: All cache cleared");
        }
    }
    
    /**
     * Get cache statistics
     */
    public void getCacheStats() {
        try (Jedis jedis = jedisPool.getResource()) {
            String info = jedis.info("memory");
            System.out.println("📊 CACHE STATS:");
            System.out.println(info);
        }
    }
    
    /**
     * Check if user exists in cache
     */
    public boolean isUserCached(int userId) {
        String cacheKey = "user:" + userId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(cacheKey);
        }
    }
    
    /**
     * Close Redis connection pool
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            System.out.println("🔌 Redis connection pool closed");
        }
    }
}
