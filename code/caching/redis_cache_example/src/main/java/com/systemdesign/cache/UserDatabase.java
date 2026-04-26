package com.systemdesign.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simulated Database - In real world, this would be a real database connection
 */
public class UserDatabase {
    private static UserDatabase instance;
    private Map<Integer, User> users;
    
    private UserDatabase() {
        users = new HashMap<>();
        // Initialize with some sample data
        initializeSampleData();
    }
    
    public static synchronized UserDatabase getInstance() {
        if (instance == null) {
            instance = new UserDatabase();
        }
        return instance;
    }
    
    private void initializeSampleData() {
        users.put(1, new User(1, "John Doe", "john@example.com", 25));
        users.put(2, new User(2, "Jane Smith", "jane@example.com", 30));
        users.put(3, new User(3, "Bob Johnson", "bob@example.com", 35));
        users.put(4, new User(4, "Alice Brown", "alice@example.com", 28));
        users.put(5, new User(5, "Charlie Wilson", "charlie@example.com", 32));
    }
    
    /**
     * Simulate slow database read
     */
    public User getUserById(int id) {
        System.out.println("📡 DATABASE: Reading user " + id + " from database...");
        
        // Simulate database latency
        try {
            TimeUnit.MILLISECONDS.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        User user = users.get(id);
        if (user != null) {
            System.out.println("✅ DATABASE: Found user " + id);
        } else {
            System.out.println("❌ DATABASE: User " + id + " not found");
        }
        
        return user;
    }
    
    /**
     * Simulate database write
     */
    public void saveUser(User user) {
        System.out.println("💾 DATABASE: Saving user " + user.getId() + " to database...");
        
        // Simulate database write latency
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        users.put(user.getId(), user);
        System.out.println("✅ DATABASE: User " + user.getId() + " saved successfully");
    }
    
    /**
     * Simulate database update
     */
    public void updateUser(User user) {
        System.out.println("🔄 DATABASE: Updating user " + user.getId() + " in database...");
        
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        users.put(user.getId(), user);
        System.out.println("✅ DATABASE: User " + user.getId() + " updated successfully");
    }
    
    /**
     * Simulate database delete
     */
    public void deleteUser(int id) {
        System.out.println("🗑️ DATABASE: Deleting user " + id + " from database...");
        
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        users.remove(id);
        System.out.println("✅ DATABASE: User " + id + " deleted successfully");
    }
    
    public void clearAllUsers() {
        users.clear();
        System.out.println("🗑️ DATABASE: All users cleared");
    }
}
