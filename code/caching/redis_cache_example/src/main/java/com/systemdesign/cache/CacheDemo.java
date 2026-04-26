package com.systemdesign.cache;

import java.util.concurrent.TimeUnit;

/**
 * Cache Demo - Shows different caching patterns in action
 */
public class CacheDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Cache Demo...\n");
        
        RedisCacheService cacheService = RedisCacheService.getInstance();
        
        try {
            // Demo 1: Cache-Aside Pattern
            demonstrateCacheAside(cacheService);
            
            TimeUnit.SECONDS.sleep(2);
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // Demo 2: Cache Hit Performance
            demonstrateCacheHitPerformance(cacheService);
            
            TimeUnit.SECONDS.sleep(2);
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // Demo 3: Write-Through Pattern
            demonstrateWriteThrough(cacheService);
            
            TimeUnit.SECONDS.sleep(2);
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // Demo 4: Cache Invalidation
            demonstrateCacheInvalidation(cacheService);
            
            TimeUnit.SECONDS.sleep(2);
            System.out.println("\n" + "=".repeat(50) + "\n");
            
            // Demo 5: Write-Behind Pattern
            demonstrateWriteBehind(cacheService);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cacheService.close();
        }
        
        System.out.println("\n✅ Cache Demo completed!");
    }
    
    /**
     * Demo 1: Cache-Aside Pattern
     */
    private static void demonstrateCacheAside(RedisCacheService cacheService) {
        System.out.println("📚 DEMO 1: Cache-Aside Pattern");
        System.out.println("First request (cache miss):");
        
        long startTime = System.currentTimeMillis();
        User user1 = cacheService.getUserWithCache(1);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Result: " + user1);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        
        System.out.println("\nSecond request (cache hit):");
        startTime = System.currentTimeMillis();
        User user2 = cacheService.getUserWithCache(1);
        endTime = System.currentTimeMillis();
        
        System.out.println("Result: " + user2);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
    }
    
    /**
     * Demo 2: Cache Hit Performance Comparison
     */
    private static void demonstrateCacheHitPerformance(RedisCacheService cacheService) {
        System.out.println("⚡ DEMO 2: Performance Comparison");
        
        // Test database performance (no cache)
        System.out.println("Database-only performance:");
        long dbStartTime = System.currentTimeMillis();
        User dbUser = UserDatabase.getInstance().getUserById(2);
        long dbEndTime = System.currentTimeMillis();
        System.out.println("Database time: " + (dbEndTime - dbStartTime) + "ms");
        
        // Test cache performance
        System.out.println("\nCache performance:");
        long cacheStartTime = System.currentTimeMillis();
        User cacheUser = cacheService.getUserWithCache(2);
        long cacheEndTime = System.currentTimeMillis();
        System.out.println("Cache time: " + (cacheEndTime - cacheStartTime) + "ms");
        
        long speedup = (dbEndTime - dbStartTime) / (cacheEndTime - cacheStartTime + 1);
        System.out.println("🚀 Speedup: " + speedup + "x faster");
    }
    
    /**
     * Demo 3: Write-Through Pattern
     */
    private static void demonstrateWriteThrough(RedisCacheService cacheService) {
        System.out.println("💾 DEMO 3: Write-Through Pattern");
        
        User user = new User(3, "Bob Johnson Updated", "bob.updated@example.com", 36);
        System.out.println("Updating user: " + user);
        
        cacheService.updateUserWithCache(user);
        
        // Verify the update
        User updatedUser = cacheService.getUserWithCache(3);
        System.out.println("Updated user from cache: " + updatedUser);
    }
    
    /**
     * Demo 4: Cache Invalidation
     */
    private static void demonstrateCacheInvalidation(RedisCacheService cacheService) {
        System.out.println("🗑️ DEMO 4: Cache Invalidation");
        
        // First, cache a user
        System.out.println("Caching user 4...");
        cacheService.getUserWithCache(4);
        System.out.println("User 4 cached: " + cacheService.isUserCached(4));
        
        // Invalidate cache
        System.out.println("\nInvalidating cache for user 4...");
        cacheService.invalidateUserCache(4);
        System.out.println("User 4 still cached: " + cacheService.isUserCached(4));
        
        // Next request will be cache miss
        System.out.println("\nRequesting user 4 again (should be cache miss):");
        cacheService.getUserWithCache(4);
    }
    
    /**
     * Demo 5: Write-Behind Pattern
     */
    private static void demonstrateWriteBehind(RedisCacheService cacheService) {
        System.out.println("🔄 DEMO 5: Write-Behind (Async) Pattern");
        
        User user = new User(5, "Charlie Wilson Updated", "charlie.updated@example.com", 33);
        System.out.println("Updating user asynchronously: " + user);
        
        cacheService.updateUserAsync(user);
        
        // Immediately check cache (should be updated)
        System.out.println("Checking cache immediately after async update:");
        User cachedUser = cacheService.getUserWithCache(5);
        System.out.println("User from cache: " + cachedUser);
        
        System.out.println("\n⏳ Waiting for async database update...");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
