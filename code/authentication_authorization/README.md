# Authentication & Authorization Code Examples

This directory contains practical implementations of authentication and authorization patterns discussed in the theory section.

## Files Structure

- `jwt_example/` - JWT (JSON Web Token) implementation
- `password_hashing/` - Secure password hashing with bcrypt
- `rbac_example/` - Role-Based Access Control implementation
- `oauth_example/` - OAuth 2.0 client implementation

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
