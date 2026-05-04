package com.systemdesign.database;

/**
 * Interface for different sharding strategies
 */
public interface ShardingStrategy {
    /**
     * Determine which shard should store the given user ID
     */
    int getShardId(int userId, int totalShards);
    
    /**
     * Get the name of this sharding strategy
     */
    String getStrategyName();
}
