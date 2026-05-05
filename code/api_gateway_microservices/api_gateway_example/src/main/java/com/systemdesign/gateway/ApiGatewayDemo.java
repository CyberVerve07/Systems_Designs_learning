package com.systemdesign.gateway;

/**
 * API Gateway Demo - Demonstrates routing and service discovery
 */
public class ApiGatewayDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 API Gateway Demo\n");
        
        // Create API Gateway
        ApiGateway gateway = new ApiGateway();
        
        // Register microservices
        // Note: In real scenario, these would be actual running services
        // For demo, we'll simulate the routing logic
        
        Microservice userService = new Microservice("User Service", "/api/users", "localhost", 8081);
        Microservice orderService = new Microservice("Order Service", "/api/orders", "localhost", 8082);
        Microservice productService = new Microservice("Product Service", "/api/products", "localhost", 8083);
        
        gateway.registerService(userService);
        gateway.registerService(orderService);
        gateway.registerService(productService);
        
        // Print gateway status
        gateway.printStatus();
        
        // Demo 1: Simple routing
        demonstrateSimpleRouting(gateway);
        
        // Demo 2: Different HTTP methods
        demonstrateHttpMethods(gateway);
        
        // Demo 3: Routing to different services
        demonstrateMultiServiceRouting(gateway);
        
        System.out.println("\n✅ API Gateway Demo completed!");
    }
    
    /**
     * Demo 1: Simple routing
     */
    private static void demonstrateSimpleRouting(ApiGateway gateway) {
        System.out.println("\n📚 DEMO 1: Simple Routing");
        System.out.println("=".repeat(50));
        
        Request request1 = new Request("GET", "/api/users/123");
        Response response1 = gateway.route(request1);
        System.out.println("Response: " + response1.getStatusCode());
        
        Request request2 = new Request("GET", "/api/products/456");
        Response response2 = gateway.route(request2);
        System.out.println("Response: " + response2.getStatusCode());
    }
    
    /**
     * Demo 2: Different HTTP methods
     */
    private static void demonstrateHttpMethods(ApiGateway gateway) {
        System.out.println("\n📚 DEMO 2: Different HTTP Methods");
        System.out.println("=".repeat(50));
        
        Request getRequest = new Request("GET", "/api/users/123");
        gateway.route(getRequest);
        
        Request postRequest = new Request("POST", "/api/orders");
        postRequest.setBody("{\"userId\": 123, \"productId\": 456, \"quantity\": 2}");
        gateway.route(postRequest);
        
        Request putRequest = new Request("PUT", "/api/users/123");
        putRequest.setBody("{\"name\": \"John Updated\"}");
        gateway.route(putRequest);
        
        Request deleteRequest = new Request("DELETE", "/api/orders/789");
        gateway.route(deleteRequest);
    }
    
    /**
     * Demo 3: Multi-service routing
     */
    private static void demonstrateMultiServiceRouting(ApiGateway gateway) {
        System.out.println("\n📚 DEMO 3: Multi-Service Routing");
        System.out.println("=".repeat(50));
        
        System.out.println("Simulating requests to different services:");
        
        String[] paths = {
            "/api/users/1",
            "/api/users/2",
            "/api/orders/100",
            "/api/orders/101",
            "/api/products/50",
            "/api/products/51"
        };
        
        for (String path : paths) {
            Request request = new Request("GET", path);
            gateway.route(request);
        }
    }
}
