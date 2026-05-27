package com.systemdesign.ratelimiter;

public interface RateLimiter {
    /**
     * Determines if a request from a specific client is allowed or rate-limited.
     * 
     * @param clientId A unique identifier for the client (e.g., API key, user ID, IP address).
     * @return true if the request is within limits and allowed; false otherwise (throttled).
     */
    boolean allowRequest(String clientId);
    
    /**
     * Gets the name of the rate limiting strategy.
     */
    String getStrategyName();
}
