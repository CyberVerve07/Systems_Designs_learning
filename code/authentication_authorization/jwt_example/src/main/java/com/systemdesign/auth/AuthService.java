package com.systemdesign.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication Service - Manages user authentication and token generation
 */
public class AuthService {
    private static AuthService instance;
    private ConcurrentHashMap<String, User> users;
    private ConcurrentHashMap<String, String> tokens; // token -> username mapping
    
    private AuthService() {
        this.users = new ConcurrentHashMap<>();
        this.tokens = new ConcurrentHashMap<>();
        initializeSampleUsers();
    }
    
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    private void initializeSampleUsers() {
        // Create sample users with hashed passwords
        Set<String> adminRoles = new HashSet<>();
        adminRoles.add("ADMIN");
        adminRoles.add("USER");
        
        Set<String> userRoles = new HashSet<>();
        userRoles.add("USER");
        
        // Password is "password123" for all users
        String hashedPassword = JwtUtil.hashPassword("password123");
        
        users.put("admin", new User(1, "admin", "admin@example.com", hashedPassword, adminRoles));
        users.put("john", new User(2, "john", "john@example.com", hashedPassword, userRoles));
        users.put("jane", new User(3, "jane", "jane@example.com", hashedPassword, userRoles));
        
        System.out.println("📝 Initialized sample users: admin, john, jane");
        System.out.println("💡 Default password for all users: password123");
    }
    
    /**
     * Login user and generate JWT token
     */
    public String login(String username, String password) {
        System.out.println("\n🔐 Login attempt for user: " + username);
        
        User user = users.get(username);
        
        if (user == null) {
            System.out.println("❌ User not found: " + username);
            return null;
        }
        
        if (!JwtUtil.verifyPassword(password, user.getPasswordHash())) {
            System.out.println("❌ Invalid password for user: " + username);
            return null;
        }
        
        // Generate JWT token
        String token = JwtUtil.generateToken(user);
        tokens.put(token, username);
        
        System.out.println("✅ Login successful for user: " + username);
        System.out.println("🎫 Generated JWT token: " + token.substring(0, 20) + "...");
        
        return token;
    }
    
    /**
     * Validate token and return user
     */
    public User validateToken(String token) {
        if (!JwtUtil.validateToken(token)) {
            System.out.println("❌ Invalid token");
            return null;
        }
        
        if (JwtUtil.isTokenExpired(token)) {
            System.out.println("❌ Token expired");
            return null;
        }
        
        String username = JwtUtil.extractUsername(token);
        User user = users.get(username);
        
        if (user != null) {
            System.out.println("✅ Token valid for user: " + username);
        }
        
        return user;
    }
    
    /**
     * Logout user (invalidate token)
     */
    public void logout(String token) {
        String username = tokens.remove(token);
        if (username != null) {
            System.out.println("🚪 Logged out user: " + username);
        } else {
            System.out.println("⚠️ Token not found in active sessions");
        }
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(String token, String role) {
        User user = validateToken(token);
        if (user == null) {
            return false;
        }
        
        boolean hasRole = user.getRoles().contains(role);
        System.out.println("🔍 User " + user.getUsername() + " has role " + role + ": " + hasRole);
        
        return hasRole;
    }
    
    /**
     * Get user by username
     */
    public User getUser(String username) {
        return users.get(username);
    }
    
    /**
     * Register new user
     */
    public boolean register(String username, String email, String password, Set<String> roles) {
        if (users.containsKey(username)) {
            System.out.println("❌ User already exists: " + username);
            return false;
        }
        
        String hashedPassword = JwtUtil.hashPassword(password);
        int userId = users.size() + 1;
        
        User user = new User(userId, username, email, hashedPassword, roles);
        users.put(username, user);
        
        System.out.println("✅ Registered new user: " + username);
        return true;
    }
    
    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return tokens.size();
    }
}
