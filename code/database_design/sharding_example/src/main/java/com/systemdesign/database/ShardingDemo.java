package com.systemdesign.database;

import java.util.List;

/**
 * Sharding Demo - Demonstrates different sharding strategies
 */
public class ShardingDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Database Sharding Demo\n");
        
        // Demo 1: Hash-based Sharding
        demonstrateHashBasedSharding();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Demo 2: Range-based Sharding
        demonstrateRangeBasedSharding();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Demo 3: Performance Comparison
        demonstratePerformanceComparison();
    }
    
    /**
     * Demo 1: Hash-based Sharding
     */
    private static void demonstrateHashBasedSharding() {
        System.out.println("📚 DEMO 1: Hash-Based Sharding");
        System.out.println("Strategy: Uses hash function (userId % numShards) to distribute data\n");
        
        ShardingManager manager = new ShardingManager(3, new HashBasedSharding());
        
        // Insert users
        System.out.println("Inserting users...");
        for (int i = 1; i <= 10; i++) {
            User user = new User(i, "User " + i, "user" + i + "@example.com", 20 + i);
            manager.insertUser(user);
        }
        
        // Show distribution
        manager.printShardStatistics();
        
        // Query specific users
        System.out.println("Querying users:");
        User user1 = manager.getUserById(1);
        System.out.println("User 1: " + user1);
        
        User user5 = manager.getUserById(5);
        System.out.println("User 5: " + user5);
        
        // Update user
        System.out.println("\nUpdating user 5...");
        user5.setName("User 5 Updated");
        user5.setAge(30);
        manager.updateUser(user5);
        
        User updatedUser = manager.getUserById(5);
        System.out.println("Updated User 5: " + updatedUser);
        
        manager.close();
    }
    
    /**
     * Demo 2: Range-based Sharding
     */
    private static void demonstrateRangeBasedSharding() {
        System.out.println("📚 DEMO 2: Range-Based Sharding");
        System.out.println("Strategy: Splits data based on ID ranges (100 users per shard)\n");
        
        ShardingManager manager = new ShardingManager(3, new RangeBasedSharding(100));
        
        // Insert users with IDs spread across ranges
        System.out.println("Inserting users across different ranges...");
        
        // Users 1-50 → Shard 0
        for (int i = 1; i <= 50; i++) {
            User user = new User(i, "Range1_User " + i, "user" + i + "@example.com", 20 + i);
            manager.insertUser(user);
        }
        
        // Users 101-150 → Shard 1
        for (int i = 101; i <= 150; i++) {
            User user = new User(i, "Range2_User " + i, "user" + i + "@example.com", 20 + (i % 30));
            manager.insertUser(user);
        }
        
        // Users 201-250 → Shard 2
        for (int i = 201; i <= 250; i++) {
            User user = new User(i, "Range3_User " + i, "user" + i + "@example.com", 20 + (i % 30));
            manager.insertUser(user);
        }
        
        // Show distribution
        manager.printShardStatistics();
        
        // Query users from different ranges
        System.out.println("Querying users from different ranges:");
        User user1 = manager.getUserById(25);  // Shard 0
        System.out.println("User 25 (Shard 0): " + user1);
        
        User user2 = manager.getUserById(125); // Shard 1
        System.out.println("User 125 (Shard 1): " + user2);
        
        User user3 = manager.getUserById(225); // Shard 2
        System.out.println("User 225 (Shard 2): " + user3);
        
        manager.close();
    }
    
    /**
     * Demo 3: Performance Comparison
     */
    private static void demonstratePerformanceComparison() {
        System.out.println("⚡ DEMO 3: Performance Comparison");
        System.out.println("Comparing sharded vs non-sharded performance\n");
        
        // Sharded approach
        System.out.println("Sharded Database (3 shards):");
        ShardingManager shardedManager = new ShardingManager(3, new HashBasedSharding());
        
        long shardedInsertStart = System.currentTimeMillis();
        for (int i = 1; i <= 1000; i++) {
            User user = new User(i, "User " + i, "user" + i + "@example.com", 20 + (i % 50));
            shardedManager.insertUser(user);
        }
        long shardedInsertEnd = System.currentTimeMillis();
        
        long shardedReadStart = System.currentTimeMillis();
        for (int i = 1; i <= 100; i++) {
            shardedManager.getUserById(i);
        }
        long shardedReadEnd = System.currentTimeMillis();
        
        System.out.println("Insert 1000 users: " + (shardedInsertEnd - shardedInsertStart) + "ms");
        System.out.println("Read 100 users: " + (shardedReadEnd - shardedReadStart) + "ms");
        
        shardedManager.printShardStatistics();
        shardedManager.close();
        
        System.out.println("\n💡 Key Insights:");
        System.out.println("- Sharding distributes load across multiple databases");
        System.out.println("- Each shard handles only a subset of data");
        System.out.println("- Can scale horizontally by adding more shards");
        System.out.println("- Trade-off: Cross-shard queries are expensive");
    }
}
