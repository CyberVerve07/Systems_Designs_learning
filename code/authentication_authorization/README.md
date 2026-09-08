# Authentication & Authorization Code Examples

This directory contains practical implementations of authentication and authorization patterns discussed in the theory section.

## Files Structure

- `jwt_example/` - Complete authentication and authorization simulation in Java, demonstrating:
  - Secure password hashing and salt verification with BCrypt
  - Cryptographic HMAC-SHA256 JWT token generation, parsing, and expiration validation
  - Role-Based Access Control (RBAC) checking (`ADMIN`, `USER`)
  - Session lifecycle and token invalidation

## Setup Instructions

### Prerequisites
- Java 11+
- Maven

## Running Examples

Each subdirectory contains a README with specific instructions for that implementation.

## Key Concepts Demonstrated

1. **JWT Implementation**: Token generation, validation, and claims
2. **Password Hashing**: Secure password storage with bcrypt
3. **RBAC**: Role-based access control with permissions
4. **OAuth 2.0**: Authorization code flow implementation

## Security Best Practices

- Never store plain text passwords
- Use strong hashing algorithms (bcrypt, Argon2)
- Always use HTTPS in production
- Implement rate limiting
- Validate all inputs
- Use short-lived tokens with refresh tokens
