package com.systemdesign.gateway;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;
    
    public Request() {
        this.headers = new HashMap<>();
    }
    
    public Request(String method, String path) {
        this.method = method;
        this.path = path;
        this.headers = new HashMap<>();
    }
    
    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }
    
    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers.size() +
                ", body='" + body + '\'' +
                '}';
    }
}
