package com.systemdesign.auth;

import java.util.HashSet;
import java.util.Set;

/**
 * Authentication Demo - Demonstrates JWT authentication and authorization
 */
public class AuthDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Authentication & Authorization Demo\n");
        
        AuthService authService = AuthService.getInstance();
        
        // Demo 1: User Login
        demonstrateLogin(authService);
        
        // Demo 2: Token Validation
        demonstrateTokenValidation(authService);
        
        // Demo 3: Role-Based Access Control
        demonstrateRBAC(authService);
        
        // Demo 4: Password Hashing
        demonstratePasswordHashing();
        
        // Demo 5: User Registration
        demonstrateRegistration(authService);
        
        System.out.println("\n✅ Authentication Demo completed!");
    }
    
    /**
     * Demo 1: User Login
     */
    private static void demonstrateLogin(AuthService authService) {
        System.out.println("📚 DEMO 1: User Login");
        System.out.println("=".repeat(50));
        
        // Successful login
        String token1 = authService.login("admin", "password123");
        System.out.println("Admin token: " + (token1 != null ? "Generated successfully" : "Failed"));
        
        // Failed login (wrong password)
        String token2 = authService.login("admin", "wrongpassword");
        System.out.println("Wrong password login: " + (token2 != null ? "Generated successfully" : "Failed as expected"));
        
        // Failed login (user not found)
        String token3 = authService.login("nonexistent", "password123");
        System.out.println("Non-existent user login: " + (token3 != null ? "Generated successfully" : "Failed as expected"));
    }
    
    /**
     * Demo 2: Token Validation
     */
    private static void demonstrateTokenValidation(AuthService authService) {
        System.out.println("\n📚 DEMO 2: Token Validation");
        System.out.println("=".repeat(50));
        
        // Generate valid token
        String validToken = authService.login("john", "password123");
        
        // Validate valid token
        System.out.println("\nValidating valid token:");
        User user = authService.validateToken(validToken);
        if (user != null) {
            System.out.println("User: " + user);
            System.out.println("Username from token: " + JwtUtil.extractUsername(validToken));
            System.out.println("User ID from token: " + JwtUtil.extractUserId(validToken));
            System.out.println("Roles from token: " + JwtUtil.extractRoles(validToken));
        }
        
        // Validate invalid token
        System.out.println("\nValidating invalid token:");
        User invalidUser = authService.validateToken("invalid.token.here");
        System.out.println("Result: " + (invalidUser != null ? "Valid" : "Invalid as expected"));
    }
    
    /**
     * Demo 3: Role-Based Access Control
     */
    private static void demonstrateRBAC(AuthService authService) {
        System.out.println("\n📚 DEMO 3: Role-Based Access Control");
        System.out.println("=".repeat(50));
        
        // Admin token
        String adminToken = authService.login("admin", "password123");
        
        // Regular user token
        String userToken = authService.login("john", "password123");
        
        System.out.println("\nChecking ADMIN role:");
        System.out.println("Admin has ADMIN role: " + authService.hasRole(adminToken, "ADMIN"));
        System.out.println("John has ADMIN role: " + authService.hasRole(userToken, "ADMIN"));
        
        System.out.println("\nChecking USER role:");
        System.out.println("Admin has USER role: " + authService.hasRole(adminToken, "USER"));
        System.out.println("John has USER role: " + authService.hasRole(userToken, "USER"));
        
        System.out.println("\nAccess Control Example:");
        System.out.println("Admin can delete users: " + authService.hasRole(adminToken, "ADMIN"));
        System.out.println("John can delete users: " + authService.hasRole(userToken, "ADMIN"));
    }
    
    /**
     * Demo 4: Password Hashing
     */
    private static void demonstratePasswordHashing() {
        System.out.println("\n📚 DEMO 4: Password Hashing");
        System.out.println("=".repeat(50));
        
        String plainPassword = "mySecurePassword123";
        
        System.out.println("Plain password: " + plainPassword);
        
        // Hash password
        String hashedPassword = JwtUtil.hashPassword(plainPassword);
        System.out.println("Hashed password: " + hashedPassword);
        
        // Verify correct password
        boolean isCorrect = JwtUtil.verifyPassword(plainPassword, hashedPassword);
        System.out.println("Verify correct password: " + isCorrect);
        
        // Verify wrong password
        boolean isWrong = JwtUtil.verifyPassword("wrongPassword", hashedPassword);
        System.out.println("Verify wrong password: " + isWrong);
        
        // Hash same password again (should be different due to salt)
        String hashedPassword2 = JwtUtil.hashPassword(plainPassword);
        System.out.println("Hash same password again: " + hashedPassword2);
        System.out.println("Hashes are different (due to salt): " + !hashedPassword.equals(hashedPassword2));
        System.out.println("Both verify correctly: " + 
            (JwtUtil.verifyPassword(plainPassword, hashedPassword) && 
             JwtUtil.verifyPassword(plainPassword, hashedPassword2)));
    }
    
    /**
     * Demo 5: User Registration
     */
    private static void demonstrateRegistration(AuthService authService) {
        System.out.println("\n📚 DEMO 5: User Registration");
        System.out.println("=".repeat(50));
        
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        
        // Register new user
        boolean registered = authService.register("alice", "alice@example.com", "newPassword123", roles);
        System.out.println("Register new user 'alice': " + (registered ? "Success" : "Failed"));
        
        // Try to register same user again
        boolean duplicate = authService.register("alice", "alice2@example.com", "anotherPassword", roles);
        System.out.println("Register duplicate 'alice': " + (duplicate ? "Success" : "Failed as expected"));
        
        // Login with new user
        String token = authService.login("alice", "newPassword123");
        System.out.println("Login with new user: " + (token != null ? "Success" : "Failed"));
    }
}
