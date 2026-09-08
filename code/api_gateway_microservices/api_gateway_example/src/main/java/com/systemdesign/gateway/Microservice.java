package com.systemdesign.gateway;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Represents a backend microservice
 */
public class Microservice {
    private final String name;
    private final String basePath;
    private final String host;
    private final int port;
    private final HttpClient httpClient;
    
    public Microservice(String name, String basePath, String host, int port) {
        this.name = name;
        this.basePath = basePath;
        this.host = host;
        this.port = port;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
    
    /**
     * Handle request for this microservice
     */
    public Response handleRequest(Request request) {
        try {
            // Construct full URL
            String url = "http://" + host + ":" + port + request.getPath();
            
            System.out.println("🔄 Routing to " + name + ": " + url);
            
            // Create HTTP request
            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10));
            
            // Set method
            if (request.getMethod() != null) {
                switch (request.getMethod().toUpperCase()) {
                    case "GET":
                        httpRequestBuilder.GET();
                        break;
                    case "POST":
                        httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getBody() != null ? request.getBody() : ""));
                        break;
                    case "PUT":
                        httpRequestBuilder.PUT(HttpRequest.BodyPublishers.ofString(request.getBody() != null ? request.getBody() : ""));
                        break;
                    case "DELETE":
                        httpRequestBuilder.DELETE();
                        break;
                }
            }
            
            // Add headers
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                httpRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            
            HttpRequest httpRequest = httpRequestBuilder.build();
            
            // Send request
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            // Create response
            Response response = new Response();
            response.setStatusCode(httpResponse.statusCode());
            response.setBody(httpResponse.body());
            
            System.out.println("✅ " + name + " responded: " + httpResponse.statusCode());
            
            return response;
            
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error calling " + name + ": " + e.getMessage());
            return Response.error("{\"error\": \"Service unavailable: " + name + "\"}");
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getBasePath() {
        return basePath;
    }
}
