# Day 5: Authentication & Authorization

Welcome to Day 5 of our System Design journey! Today we're diving deep into **Authentication & Authorization** - critical security components for any system.

## Authentication vs Authorization

### **Authentication (AuthN)**
**Who are you?**
- Verifying the identity of a user or system
- Examples: Password, OTP, Biometrics, API Keys
- **Analogy:** Showing your ID card to enter a building

### **Authorization (AuthZ)**
**What can you do?**
- Determining what permissions an authenticated user has
- Examples: Read-only, Admin, User roles
- **Analogy:** Your ID card shows you have access to certain floors

**Key Difference:**
- **Authentication** = Identity verification (Login)
- **Authorization** = Permission verification (Access control)

---

## Authentication Methods

### **1. Password-Based Authentication**
The most common but least secure method.

**How it works:**
1. User enters username and password
2. Server hashes the password
3. Compares with stored hash
4. If match, authentication successful

**Best Practices:**
- Never store plain text passwords
- Use strong hashing algorithms (bcrypt, Argon2, PBKDF2)
- Add salt to prevent rainbow table attacks
- Implement password complexity requirements
- Use HTTPS to prevent interception

**Example (bcrypt):**
```java
// Hash password
String hashedPassword = BCrypt.hashpw("plainPassword", BCrypt.gensalt());

// Verify password
boolean isMatch = BCrypt.checkpw("plainPassword", hashedPassword);
```

---

### **2. Multi-Factor Authentication (MFA)**
Requires multiple factors for authentication.

**Three Factors:**
1. **Something you know**: Password, PIN
2. **Something you have**: Phone, Security token, Smart card
3. **Something you are**: Fingerprint, Face recognition, Iris scan

**Common MFA Methods:**
- **SMS OTP**: One-time password sent via SMS
- **TOTP**: Time-based OTP (Google Authenticator, Authy)
- **Push Notifications**: Approve login via mobile app
- **Hardware Tokens**: YubiKey, RSA SecurID

**Pros:** Much more secure than password-only
**Cons:** User friction, requires additional setup

---

### **3. API Key Authentication**
Simple authentication for API access.

**How it works:**
1. User generates API key
2. Includes key in request header
3. Server validates key
4. Grants access if valid

**Example:**
```http
GET /api/users
Authorization: Bearer api_key_abc123
```

**Pros:** Simple to implement, stateless
**Cons:** If leaked, full access compromised, no granular control

---

### **4. OAuth 2.0**
Industry standard for authorization delegation.

**What it is:** Framework for third-party applications to obtain limited access to user accounts.

**Key Components:**
- **Resource Owner**: User who owns the data
- **Client**: Application requesting access
- **Authorization Server**: Issues access tokens
- **Resource Server**: API hosting protected data

**OAuth 2.0 Flow:**
```
1. User clicks "Login with Google"
2. Client redirects to Authorization Server
3. User grants permission
4. Authorization Server redirects with authorization code
5. Client exchanges code for access token
6. Client uses access token to access resources
```

**Use Cases:**
- "Login with Google/Facebook/Twitter"
- Third-party app accessing your data
- Machine-to-machine authentication

---

### **5. OpenID Connect (OIDC)**
Authentication layer on top of OAuth 2.0.

**What it adds:** Identity verification (Authentication) to OAuth (Authorization)

**Key Features:**
- ID Tokens (JWT with user info)
- UserInfo endpoint
- Standardized claims

**When to use:** When you need both authentication and authorization

---

## JSON Web Tokens (JWT)

### **What is JWT?**
JSON Web Token is a compact, URL-safe means of representing claims to be transferred between two parties.

### **JWT Structure**
```
Header.Payload.Signature
```

#### **1. Header**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

#### **2. Payload**
```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022,
  "exp": 1516242622
}
```

#### **3. Signature**
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### **JWT Workflow**
```
1. User logs in with credentials
2. Server validates credentials
3. Server creates JWT with user claims
4. Server sends JWT to client
5. Client stores JWT (localStorage, cookie)
6. Client includes JWT in subsequent requests
7. Server validates JWT signature and claims
8. Server grants access if valid
```

### **JWT Pros & Cons**

**Pros:**
- Stateless (no server-side session storage)
- Compact and URL-safe
- Can contain custom claims
- Standard format

**Cons:**
- Cannot be revoked (until expiration)
- Larger than session IDs
- If secret leaked, attacker can forge tokens
- Must use HTTPS

### **JWT Best Practices**
- Use strong signing algorithms (RS256, not none)
- Set appropriate expiration times
- Use short-lived tokens with refresh tokens
- Validate all claims
- Store securely (HttpOnly cookies recommended)

---

## Authorization Models

### **1. Role-Based Access Control (RBAC)**
Users assigned to roles, roles assigned permissions.

**Example:**
```
Roles: Admin, Editor, Viewer
Permissions: create, read, update, delete

Admin → create, read, update, delete
Editor → create, read, update
Viewer → read
```

**Pros:** Simple to understand, easy to implement
**Cons:** Role explosion, not flexible enough

---

### **2. Attribute-Based Access Control (ABAC)**
Access based on user attributes, resource attributes, and environment.

**Example:**
```
Rule: User can edit document IF
  - User.role = Editor AND
  - Document.department = User.department AND
  - Time = 9AM-5PM
```

**Pros:** Very flexible, fine-grained control
**Cons:** Complex to implement, performance overhead

---

### **3. Access Control Lists (ACL)**
Each resource has a list of who can access it.

**Example:**
```
File1: User1 (read, write), User2 (read)
File2: User1 (read), User3 (read, write)
```

**Pros:** Simple, direct control
**Cons:** Hard to manage at scale

---

## Session Management

### **Session-Based Authentication**
Server stores session data, client receives session ID.

**Workflow:**
```
1. User logs in
2. Server creates session in database/memory
3. Server sends session ID cookie to client
4. Client includes session ID in requests
5. Server looks up session and validates
```

**Pros:** Can revoke sessions, can store arbitrary data
**Cons:** Server-side storage required, not horizontally scalable

---

### **Token-Based Authentication (JWT)**
Client stores token, server validates signature.

**Workflow:**
```
1. User logs in
2. Server creates JWT
3. Server sends JWT to client
4. Client includes JWT in requests
5. Server validates JWT signature
```

**Pros:** Stateless, scalable, works well with microservices
**Cons:** Cannot revoke easily, larger payload

---

## Security Best Practices

### **1. Password Security**
- **Hash passwords**: Never store plain text
- **Use strong algorithms**: bcrypt, Argon2, PBKDF2
- **Add salt**: Prevent rainbow table attacks
- **Enforce complexity**: Minimum length, special characters
- **Rate limiting**: Prevent brute force attacks

### **2. Token Security**
- **Use HTTPS**: Prevent token interception
- **Short expiration**: Reduce window for abuse
- **Refresh tokens**: Balance security and UX
- **Secure storage**: HttpOnly cookies, not localStorage
- **Validate signatures**: Prevent token tampering

### **3. API Security**
- **API Keys**: Rotate regularly, scope permissions
- **Rate Limiting**: Prevent abuse
- **Input Validation**: Prevent injection attacks
- **CORS**: Configure properly
- **Security Headers**: CSP, X-Frame-Options, etc.

### **4. Common Attacks & Prevention**

#### **SQL Injection**
```java
// Bad
String query = "SELECT * FROM users WHERE id = " + userId;

// Good (Prepared Statements)
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setInt(1, userId);
```

#### **XSS (Cross-Site Scripting)**
- Sanitize user input
- Use Content Security Policy (CSP)
- Encode output

#### **CSRF (Cross-Site Request Forgery)**
- Use CSRF tokens
- SameSite cookie attribute
- Verify origin header

#### **Man-in-the-Middle**
- Always use HTTPS
- Implement HSTS
- Certificate pinning

---

## OAuth 2.0 Grant Types

### **1. Authorization Code Grant**
Most secure, for server-side applications.

**Flow:**
1. Client redirects user to auth server
2. User approves
3. Auth server returns authorization code
4. Client exchanges code for access token

**Use:** Web applications with backend

---

### **2. Implicit Grant**
Less secure, for single-page applications.

**Flow:**
1. Client redirects user to auth server
2. User approves
3. Auth server returns access token directly in URL

**Use:** SPAs, mobile apps (deprecated in favor of PKCE)

---

### **3. Resource Owner Password Credentials**
User provides credentials directly to client.

**Flow:**
1. User provides username/password to client
2. Client sends credentials to auth server
3. Auth server returns access token

**Use:** First-party apps, legacy systems

---

### **4. Client Credentials Grant**
Machine-to-machine authentication.

**Flow:**
1. Client sends client ID and secret
2. Auth server validates
3. Auth server returns access token

**Use:** Service accounts, background jobs

---

## Real-World Examples

### **Google OAuth**
- "Sign in with Google"
- Uses Authorization Code Grant
- Returns JWT (ID token) and access token

### **GitHub API**
- Uses Personal Access Tokens
- Token-based authentication
- Scopes for permissions

### **AWS IAM**
- Role-based access control
- Temporary credentials
- Fine-grained permissions

### **Stripe API**
- API key authentication
- Publishable vs Secret keys
- Webhook signatures

---

## Choosing the Right Approach

### **For Web Applications**
- **Session-based** or **JWT** for authentication
- **RBAC** for authorization
- **OAuth 2.0** for third-party login

### **For Mobile Apps**
- **JWT** for stateless authentication
- **OAuth 2.0 with PKCE** for third-party login
- **Device binding** for additional security

### **For APIs**
- **API Keys** for simple use cases
- **OAuth 2.0** for complex scenarios
- **JWT** for microservices

### **For Microservices**
- **JWT** for stateless authentication
- **Service-to-service** with mTLS or OAuth
- **API Gateway** for centralized auth

---

## Summary

Authentication and Authorization are fundamental to system security:

**Key Takeaways:**
1. **Authentication** = Who you are (Identity)
2. **Authorization** = What you can do (Permissions)
3. **JWT** = Stateless token-based authentication
4. **OAuth 2.0** = Authorization delegation framework
5. **RBAC** = Role-based access control
6. **Security** = Always use HTTPS, hash passwords, validate input

**Remember:** Security is not a feature, it's a requirement. Implement it from the start, not as an afterthought!

---

## Next Steps

In our next lesson, we'll implement Authentication & Authorization with practical code examples including JWT implementation, OAuth 2.0 integration, and security best practices.
