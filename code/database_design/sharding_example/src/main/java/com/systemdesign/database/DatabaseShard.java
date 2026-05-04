package com.systemdesign.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single database shard
 */
public class DatabaseShard {
    private final int shardId;
    private final String connectionString;
    private Connection connection;
    
    public DatabaseShard(int shardId, String connectionString) {
        this.shardId = shardId;
        this.connectionString = connectionString;
        initializeShard();
    }
    
    private void initializeShard() {
        try {
            // Create in-memory H2 database for this shard
            connection = DriverManager.getConnection(connectionString);
            
            // Create users table
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "email VARCHAR(100), " +
                    "age INT)";
            
            Statement stmt = connection.createStatement();
            stmt.execute(createTableSQL);
            stmt.close();
            
            System.out.println("✅ Shard " + shardId + " initialized successfully");
            
        } catch (SQLException e) {
            System.err.println("❌ Error initializing shard " + shardId + ": " + e.getMessage());
        }
    }
    
    /**
     * Insert user into this shard
     */
    public void insertUser(User user) {
        try {
            String insertSQL = "INSERT INTO users (id, name, email, age) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(insertSQL);
            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getEmail());
            pstmt.setInt(4, user.getAge());
            pstmt.executeUpdate();
            pstmt.close();
            
            System.out.println("💾 Shard " + shardId + ": User " + user.getId() + " inserted");
            
        } catch (SQLException e) {
            System.err.println("❌ Error inserting user in shard " + shardId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get user by ID from this shard
     */
    public User getUserById(int userId) {
        try {
            String selectSQL = "SELECT * FROM users WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(selectSQL);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("age")
                );
                rs.close();
                pstmt.close();
                System.out.println("🎯 Shard " + shardId + ": User " + userId + " found");
                return user;
            }
            
            rs.close();
            pstmt.close();
            System.out.println("❌ Shard " + shardId + ": User " + userId + " not found");
            return null;
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting user from shard " + shardId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all users from this shard
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            String selectSQL = "SELECT * FROM users";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(selectSQL);
            
            while (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("age")
                );
                users.add(user);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting users from shard " + shardId + ": " + e.getMessage());
        }
        
        return users;
    }
    
    /**
     * Update user in this shard
     */
    public void updateUser(User user) {
        try {
            String updateSQL = "UPDATE users SET name = ?, email = ?, age = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(updateSQL);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setInt(3, user.getAge());
            pstmt.setInt(4, user.getId());
            pstmt.executeUpdate();
            pstmt.close();
            
            System.out.println("🔄 Shard " + shardId + ": User " + user.getId() + " updated");
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating user in shard " + shardId + ": " + e.getMessage());
        }
    }
    
    /**
     * Delete user from this shard
     */
    public void deleteUser(int userId) {
        try {
            String deleteSQL = "DELETE FROM users WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            pstmt.close();
            
            System.out.println("🗑️ Shard " + shardId + ": User " + userId + " deleted");
            
        } catch (SQLException e) {
            System.err.println("❌ Error deleting user from shard " + shardId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get user count in this shard
     */
    public int getUserCount() {
        try {
            String countSQL = "SELECT COUNT(*) FROM users";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(countSQL);
            
            if (rs.next()) {
                int count = rs.getInt(1);
                rs.close();
                stmt.close();
                return count;
            }
            
            rs.close();
            stmt.close();
            return 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error counting users in shard " + shardId + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Close shard connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Shard " + shardId + " connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing shard " + shardId + ": " + e.getMessage());
        }
    }
    
    public int getShardId() {
        return shardId;
    }
}
