package com.systemdesign.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sharding Manager - Manages multiple database shards and routes requests
 */
public class ShardingManager {
    private final List<DatabaseShard> shards;
    private final ShardingStrategy shardingStrategy;
    private final Map<Integer, DatabaseShard> shardMap;
    
    public ShardingManager(int numShards, ShardingStrategy strategy) {
        this.shardingStrategy = strategy;
        this.shards = new ArrayList<>();
        this.shardMap = new HashMap<>();
        
        // Initialize shards
        for (int i = 0; i < numShards; i++) {
            String connectionString = "jdbc:h2:mem:shard_" + i + ";DB_CLOSE_DELAY=-1";
            DatabaseShard shard = new DatabaseShard(i, connectionString);
            shards.add(shard);
            shardMap.put(i, shard);
        }
        
        System.out.println("🚀 Sharding Manager initialized with " + numShards + " shards");
        System.out.println("📊 Strategy: " + strategy.getStrategyName());
        System.out.println();
    }
    
    /**
     * Insert user - routes to appropriate shard based on sharding strategy
     */
    public void insertUser(User user) {
        int shardId = shardingStrategy.getShardId(user.getId(), shards.size());
        DatabaseShard shard = shardMap.get(shardId);
        shard.insertUser(user);
    }
    
    /**
     * Get user by ID - routes to appropriate shard
     */
    public User getUserById(int userId) {
        int shardId = shardingStrategy.getShardId(userId, shards.size());
        DatabaseShard shard = shardMap.get(shardId);
        return shard.getUserById(userId);
    }
    
    /**
     * Update user - routes to appropriate shard
     */
    public void updateUser(User user) {
        int shardId = shardingStrategy.getShardId(user.getId(), shards.size());
        DatabaseShard shard = shardMap.get(shardId);
        shard.updateUser(user);
    }
    
    /**
     * Delete user - routes to appropriate shard
     */
    public void deleteUser(int userId) {
        int shardId = shardingStrategy.getShardId(userId, shards.size());
        DatabaseShard shard = shardMap.get(shardId);
        shard.deleteUser(userId);
    }
    
    /**
     * Get all users from all shards (expensive operation)
     */
    public List<User> getAllUsers() {
        List<User> allUsers = new ArrayList<>();
        for (DatabaseShard shard : shards) {
            allUsers.addAll(shard.getAllUsers());
        }
        return allUsers;
    }
    
    /**
     * Get distribution of users across shards
     */
    public Map<Integer, Integer> getShardDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();
        for (DatabaseShard shard : shards) {
            distribution.put(shard.getShardId(), shard.getUserCount());
        }
        return distribution;
    }
    
    /**
     * Print shard distribution statistics
     */
    public void printShardStatistics() {
        System.out.println("\n📊 Shard Distribution Statistics:");
        System.out.println("=".repeat(50));
        
        Map<Integer, Integer> distribution = getShardDistribution();
        int totalUsers = distribution.values().stream().mapToInt(Integer::intValue).sum();
        
        for (int i = 0; i < shards.size(); i++) {
            int count = distribution.getOrDefault(i, 0);
            double percentage = totalUsers > 0 ? (count * 100.0 / totalUsers) : 0;
            System.out.printf("Shard %d: %d users (%.1f%%)\n", i, count, percentage);
        }
        
        System.out.println("=".repeat(50));
        System.out.printf("Total Users: %d\n", totalUsers);
        System.out.println();
    }
    
    /**
     * Close all shard connections
     */
    public void close() {
        for (DatabaseShard shard : shards) {
            shard.close();
        }
        System.out.println("🔌 All shard connections closed");
    }
    
    public int getNumberOfShards() {
        return shards.size();
    }
}
