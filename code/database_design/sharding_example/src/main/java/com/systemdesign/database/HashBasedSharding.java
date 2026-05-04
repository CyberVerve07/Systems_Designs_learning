package com.systemdesign.database;

/**
 * Hash-based Sharding Strategy
 * Uses hash function to determine shard
 * Pros: Even distribution, no hotspots
 * Cons: Rebalancing difficult when adding shards
 */
public class HashBasedSharding implements ShardingStrategy {
    
    @Override
    public int getShardId(int userId, int totalShards) {
        // Simple hash function: userId % totalShards
        // In production, use better hash like MurmurHash or consistent hashing
        return Math.abs(userId % totalShards);
    }
    
    @Override
    public String getStrategyName() {
        return "Hash-Based Sharding";
    }
}
