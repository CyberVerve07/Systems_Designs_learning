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
        // Safe hash modulo guaranteeing non-negative shard index [0, totalShards - 1]
        // In production, use consistent hashing or MurmurHash
        return Math.floorMod(userId, totalShards);
    }
    
    @Override
    public String getStrategyName() {
        return "Hash-Based Sharding";
    }
}
