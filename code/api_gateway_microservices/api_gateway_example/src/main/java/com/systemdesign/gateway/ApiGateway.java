package com.systemdesign.gateway;

import java.util.HashMap;
import java.util.Map;

/**
 * API Gateway - Routes requests to appropriate microservices
 */
public class ApiGateway {
    private final Map<String, Microservice> serviceRegistry;
    private final Map<String, String> routeRules;
    
    public ApiGateway() {
        this.serviceRegistry = new HashMap<>();
        this.routeRules = new HashMap<>();
    }
    
    /**
     * Register a microservice
     */
    public void registerService(Microservice service) {
        serviceRegistry.put(service.getBasePath(), service);
        System.out.println("📝 Registered service: " + service.getName() + " at " + service.getBasePath());
    }
    
    /**
     * Add routing rule
     */
    public void addRouteRule(String pathPattern, String serviceBasePath) {
        routeRules.put(pathPattern, serviceBasePath);
        System.out.println("🔀 Added route: " + pathPattern + " → " + serviceBasePath);
    }
    
    /**
     * Route request to appropriate service
     */
    public Response route(Request request) {
        System.out.println("\n📨 Incoming Request: " + request.getMethod() + " " + request.getPath());
        
        // Find matching service based on path
        String serviceBasePath = findServiceForPath(request.getPath());
        
        if (serviceBasePath == null) {
            System.out.println("❌ No service found for path: " + request.getPath());
            return Response.notFound("{\"error\": \"Service not found\"}");
        }
        
        Microservice service = serviceRegistry.get(serviceBasePath);
        if (service == null) {
            System.out.println("❌ Service not registered: " + serviceBasePath);
            return Response.error("{\"error\": \"Service not available\"}");
        }
        
        // Add gateway headers
        request.addHeader("X-Gateway-Request-ID", generateRequestId());
        request.addHeader("X-Forwarded-For", "gateway");
        
        // Forward request to service
        return service.handleRequest(request);
    }
    
    /**
     * Find which service handles the given path
     */
    private String findServiceForPath(String path) {
        // Simple path matching - check if path starts with any registered base path
        for (String basePath : serviceRegistry.keySet()) {
            if (path.startsWith(basePath)) {
                return basePath;
            }
        }
        
        // Check route rules
        for (Map.Entry<String, String> rule : routeRules.entrySet()) {
            if (path.matches(rule.getKey())) {
                return rule.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    /**
     * Get registered services
     */
    public Map<String, Microservice> getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * Print gateway status
     */
    public void printStatus() {
        System.out.println("\n📊 API Gateway Status:");
        System.out.println("=".repeat(50));
        System.out.println("Registered Services: " + serviceRegistry.size());
        for (Microservice service : serviceRegistry.values()) {
            System.out.println("  - " + service.getName() + " (" + service.getBasePath() + ")");
        }
        System.out.println("=".repeat(50));
    }
}
