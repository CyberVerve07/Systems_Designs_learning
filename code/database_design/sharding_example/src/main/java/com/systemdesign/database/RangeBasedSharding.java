package com.systemdesign.database;

/**
 * Range-based Sharding Strategy
 * Splits data based on ID ranges
 * Pros: Simple to understand, easy to query ranges
 * Cons: Uneven distribution, hotspots possible
 */
public class RangeBasedSharding implements ShardingStrategy {
    private final int usersPerShard;
    
    public RangeBasedSharding(int usersPerShard) {
        this.usersPerShard = usersPerShard;
    }
    
    @Override
    public int getShardId(int userId, int totalShards) {
        // Calculate shard based on user ID range
        // Example: if usersPerShard = 100, then:
        // Users 1-100 → Shard 0
        // Users 101-200 → Shard 1
        // Users 201-300 → Shard 2
        int shardId = (userId - 1) / usersPerShard;
        return Math.min(shardId, totalShards - 1);
    }
    
    @Override
    public String getStrategyName() {
        return "Range-Based Sharding";
    }
}
