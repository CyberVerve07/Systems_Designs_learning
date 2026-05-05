package com.systemdesign.gateway;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    
    public Response() {
        this.headers = new HashMap<>();
        this.statusCode = 200;
    }
    
    public Response(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = new HashMap<>();
    }
    
    // Getters and Setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }
    
    public static Response ok(String body) {
        return new Response(200, body);
    }
    
    public static Response notFound(String body) {
        return new Response(404, body);
    }
    
    public static Response error(String body) {
        return new Response(500, body);
    }
    
    @Override
    public String toString() {
        return "Response{" +
                "statusCode=" + statusCode +
                ", headers=" + headers.size() +
                ", body='" + body + '\'' +
                '}';
    }
}
