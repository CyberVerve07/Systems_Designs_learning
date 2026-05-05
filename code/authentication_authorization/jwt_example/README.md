# JWT Authentication Example

This project demonstrates practical implementation of JWT (JSON Web Token) authentication and authorization using Java.

## What is JWT?

JSON Web Token (JWT) is a compact, URL-safe means of representing claims to be transferred between two parties. It's commonly used for authentication in stateless applications.

## JWT Structure

```
Header.Payload.Signature
```

### **Header**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### **Payload**
```json
{
  "sub": "username",
  "userId": 123,
  "email": "user@example.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1516239022,
  "exp": 1516242622
}
```

### **Signature**
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)

## Features Demonstrated

### **1. JWT Token Generation**
- Generate tokens with user claims
- Include user ID, username, email, roles
- Set expiration time

### **2. Token Validation**
- Validate token signature
- Check token expiration
- Extract user information from token

### **3. Password Hashing**
- Secure password hashing with bcrypt
- Salt generation for added security
- Password verification

### **4. Role-Based Access Control (RBAC)**
- Check user roles from token
- Authorization based on roles
- Admin vs User permissions

### **5. User Management**
- User registration
- Login/logout
- Session management

## Build and Run

### 1. Build the project
```bash
mvn clean compile
```

### 2. Run the demo
```bash
mvn exec:java -Dexec.mainClass="com.systemdesign.auth.AuthDemo"
```

## What You'll Learn

### **Demo 1: User Login**
- Successful login with correct credentials
- Failed login with wrong password
- Failed login with non-existent user

### **Demo 2: Token Validation**
- Validate valid JWT token
- Extract user information from token
- Handle invalid tokens

### **Demo 3: Role-Based Access Control**
- Check if user has specific roles
- Admin vs User permissions
- Access control examples

### **Demo 4: Password Hashing**
- Hash passwords with bcrypt
- Verify passwords
- Understand salt and hashing

### **Demo 5: User Registration**
- Register new users
- Handle duplicate registration
- Login with new user

## Key Files

- `JwtUtil.java` - JWT token generation and validation utilities
- `AuthService.java` - Authentication service with user management
- `User.java` - User data model
- `AuthDemo.java` - Demonstration class

## Expected Output

```
🚀 Authentication & Authorization Demo

📝 Initialized sample users: admin, john, jane
💡 Default password for all users: password123

📚 DEMO 1: User Login
==================================================

🔐 Login attempt for user: admin
✅ Login successful for user: admin
🎫 Generated JWT token: eyJhbGciOiJIUzI1NiJ9...
Admin token: Generated successfully

🔐 Login attempt for user: admin
❌ Invalid password for user: admin
Wrong password login: Failed as expected
```

## Security Best Practices

### **JWT Security**
- ✅ Use strong signing algorithms (HS256, RS256)
- ✅ Set appropriate expiration times
- ✅ Use short-lived tokens with refresh tokens
- ✅ Validate all claims
- ✅ Store tokens securely (HttpOnly cookies)
- ❌ Never use the 'none' algorithm
- ❌ Don't store sensitive data in payload

### **Password Security**
- ✅ Never store plain text passwords
- ✅ Use strong hashing algorithms (bcrypt, Argon2)
- ✅ Add salt to prevent rainbow table attacks
- ✅ Enforce password complexity requirements
- ❌ Don't use MD5 or SHA-1 for passwords

### **General Security**
- ✅ Always use HTTPS in production
- ✅ Implement rate limiting
- ✅ Validate all inputs
- ✅ Use CORS properly
- ❌ Don't expose tokens in URLs

## Real-World JWT Usage

### **Authentication Flow**
```
1. User POST /api/login with credentials
2. Server validates credentials
3. Server generates JWT token
4. Server returns token to client
5. Client stores token (cookie/localStorage)
6. Client includes token in Authorization header
7. Server validates token on each request
8. Server grants access if valid
```

### **Token Storage Options**

**HttpOnly Cookies:**
- Pros: Protected from XSS, automatic sending
- Cons: Vulnerable to CSRF

**localStorage:**
- Pros: Easy to implement, not vulnerable to CSRF
- Cons: Vulnerable to XSS, manual sending required

**Recommendation:** Use HttpOnly cookies with CSRF protection

## JWT vs Session-Based Auth

| Feature | JWT | Session-Based |
|---------|-----|---------------|
| Server Storage | No | Yes |
| Scalability | High | Limited |
| Revocation | Difficult | Easy |
| Size | Larger | Smaller |
| Use Case | Microservices, APIs | Traditional web apps |

## Common JWT Libraries

### **Java**
- **jjwt** (io.jsonwebtoken) - Used in this example
- **java-jwt** (Auth0)
- **nimbus-jose-jwt**

### **JavaScript**
- **jsonwebtoken** (Node.js)
- **jsrsasign**

### **Python**
- **PyJWT**
- **python-jose**

## Troubleshooting

### **Token Validation Fails**
- Check if secret key matches
- Verify token hasn't expired
- Ensure token hasn't been tampered with

### **Password Verification Fails**
- Ensure you're using the same hashing algorithm
- Check if salt is being used correctly
- Verify password comparison logic

### **Maven Build Issues**
```bash
mvn clean install
```
**Solution**: Ensure Maven is properly configured and Java 11+ is installed

## Next Steps

1. Implement refresh token mechanism
2. Add token blacklisting for logout
3. Implement OAuth 2.0 integration
4. Add multi-factor authentication
5. Implement rate limiting for login attempts
