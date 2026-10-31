package com.systemdesign.conferencing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Real-Time Video Conferencing Signaling & SFU Orchestration Application.
 *
 * This Spring Boot service acts as the central Signaling and Orchestration
 * Brain for multi-party WebRTC conferences powered by an SFU (e.g. Mediasoup).
 */
@SpringBootApplication
public class ConferencingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConferencingApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("🚀 WebRTC Signaling & SFU Orchestrator started on http://localhost:8080");
        System.out.println("👉 Open your browser at: http://localhost:8080 to test live video!");
        System.out.println("==================================================================");
    }
}

