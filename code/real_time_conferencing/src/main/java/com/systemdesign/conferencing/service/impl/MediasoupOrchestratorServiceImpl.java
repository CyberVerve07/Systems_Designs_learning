package com.systemdesign.conferencing.service.impl;

import com.systemdesign.conferencing.model.ConsumerInfo;
import com.systemdesign.conferencing.model.ProducerInfo;
import com.systemdesign.conferencing.model.TransportInfo;
import com.systemdesign.conferencing.service.SfuOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mediasoup SFU Orchestration Service Implementation.
 *
 * Coordinates routers, WebRtcTransports, producers, and consumers.
 * Features built-in mock/simulation mode so the system can be run and tested
 * standalone without needing to install C++ native toolchains or external Node.js daemons,
 * while providing an extensible bridge for production gRPC / REST worker sidecars.
 */
@Service
public class MediasoupOrchestratorServiceImpl implements SfuOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(MediasoupOrchestratorServiceImpl.class);

    @Value("${app.conferencing.sfu.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.conferencing.sfu.worker-count:4}")
    private int workerCount;

    // Room ID -> Router ID
    private final Map<String, String> roomRouters = new ConcurrentHashMap<>();

    // Transport ID -> TransportInfo
    private final Map<String, TransportInfo> activeTransports = new ConcurrentHashMap<>();

    // Producer ID -> ProducerInfo
    private final Map<String, ProducerInfo> activeProducers = new ConcurrentHashMap<>();

    // Consumer ID -> ConsumerInfo
    private final Map<String, ConsumerInfo> activeConsumers = new ConcurrentHashMap<>();

    @Override
    public String getOrCreateRouter(String roomId) {
        return roomRouters.computeIfAbsent(roomId, id -> {
            String routerId = "router-" + UUID.randomUUID().toString().substring(0, 8);
            int assignedWorker = Math.abs(roomId.hashCode()) % workerCount;
            log.info("Allocated new Mediasoup Router '{}' for Room '{}' on Worker #{}", routerId, roomId, assignedWorker);
            return routerId;
        });
    }

    @Override
    public Map<String, Object> getRouterRtpCapabilities(String roomId) {
        // Standard WebRTC audio and video codec capabilities defined by Mediasoup
        List<Map<String, Object>> codecs = new ArrayList<>();

        // 1. Audio: Opus
        Map<String, Object> opus = new HashMap<>();
        opus.put("kind", "audio");
        opus.put("mimeType", "audio/opus");
        opus.put("clockRate", 48000);
        opus.put("channels", 2);
        codecs.add(opus);

        // 2. Video: VP8
        Map<String, Object> vp8 = new HashMap<>();
        vp8.put("kind", "video");
        vp8.put("mimeType", "video/VP8");
        vp8.put("clockRate", 90000);
        codecs.add(vp8);

        // 3. Video: H264
        Map<String, Object> h264 = new HashMap<>();
        h264.put("kind", "video");
        h264.put("mimeType", "video/H264");
        h264.put("clockRate", 90000);
        h264.put("parameters", Map.of(
                "packetization-mode", 1,
                "profile-level-id", "42e01f"
        ));
        codecs.add(h264);

        Map<String, Object> rtpCapabilities = new HashMap<>();
        rtpCapabilities.put("codecs", codecs);
        rtpCapabilities.put("headerExtensions", List.of(
                Map.of("kind", "audio", "uri", "urn:ietf:params:rtp-hdrext:ssrc-audio-level", "preferredId", 1),
                Map.of("kind", "video", "uri", "http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time", "preferredId", 2)
        ));

        return rtpCapabilities;
    }

    @Override
    public TransportInfo createWebRtcTransport(String roomId, String peerId, String direction) {
        String transportId = "transport-" + direction + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Simulated ICE parameters
        Map<String, Object> iceParameters = Map.of(
                "usernameFragment", UUID.randomUUID().toString().substring(0, 8),
                "password", UUID.randomUUID().toString().substring(0, 24),
                "iceLite", true
        );

        // Simulated ICE candidates (SFU endpoint)
        List<Map<String, Object>> iceCandidates = List.of(
                Map.of(
                        "foundation", "udpcandidate",
                        "ip", "127.0.0.1",
                        "port", 20000 + (new Random().nextInt(1000)),
                        "priority", 1076558079,
                        "protocol", "udp",
                        "type", "host"
                )
        );

        // Simulated DTLS parameters
        Map<String, Object> dtlsParameters = Map.of(
                "role", "auto",
                "fingerprints", List.of(
                        Map.of(
                                "algorithm", "sha-256",
                                "value", "B6:69:B8:3E:97:5B:4E:94:4E:10:49:A3:C2:59:E7:E8:3E:A6:49:15:2B:A3:F6:74:81:4A:2C:14:0E:C3:68:5A"
                        )
                )
        );

        TransportInfo transport = new TransportInfo(transportId, peerId, direction, iceParameters, iceCandidates, dtlsParameters);
        activeTransports.put(transportId, transport);

        log.info("Created {} WebRtcTransport '{}' for Peer '{}' in Room '{}'", direction, transportId, peerId, roomId);
        return transport;
    }

    @Override
    public boolean connectTransport(String roomId, String transportId, Map<String, Object> dtlsParameters) {
        TransportInfo transport = activeTransports.get(transportId);
        if (transport != null) {
            transport.setConnected(true);
            log.info("WebRtcTransport '{}' connected successfully via DTLS in Room '{}'", transportId, roomId);
            return true;
        }
        log.warn("WebRtcTransport '{}' not found for connection in Room '{}'", transportId, roomId);
        return false;
    }

    @Override
    public ProducerInfo createProducer(String roomId, String transportId, String peerId, String kind, Map<String, Object> rtpParameters) {
        String producerId = "prod-" + kind + "-" + UUID.randomUUID().toString().substring(0, 8);
        ProducerInfo producer = new ProducerInfo(producerId, peerId, kind, rtpParameters);
        activeProducers.put(producerId, producer);

        log.info("Created Producer '{}' (kind: {}) on Transport '{}' for Peer '{}' in Room '{}'",
                producerId, kind, transportId, peerId, roomId);
        return producer;
    }

    @Override
    public ConsumerInfo createConsumer(String roomId, String transportId, String peerId, String producerId, Map<String, Object> clientRtpCapabilities) {
        ProducerInfo sourceProducer = activeProducers.get(producerId);
        if (sourceProducer == null) {
            log.warn("Cannot create consumer: Producer '{}' not found in Room '{}'", producerId, roomId);
            return null;
        }

        String consumerId = "cons-" + UUID.randomUUID().toString().substring(0, 8);
        ConsumerInfo consumer = new ConsumerInfo(consumerId, producerId, peerId, sourceProducer.getKind(), sourceProducer.getRtpParameters());
        activeConsumers.put(consumerId, consumer);

        log.info("Created Consumer '{}' on Transport '{}' for Peer '{}' feeding from Producer '{}'",
                consumerId, transportId, peerId, producerId);
        return consumer;
    }

    @Override
    public void closeProducer(String roomId, String producerId) {
        ProducerInfo removed = activeProducers.remove(producerId);
        if (removed != null) {
            log.info("Closed Producer '{}' in Room '{}'", producerId, roomId);
        }
    }

    @Override
    public void closeConsumer(String roomId, String consumerId) {
        ConsumerInfo removed = activeConsumers.remove(consumerId);
        if (removed != null) {
            log.info("Closed Consumer '{}' in Room '{}'", consumerId, roomId);
        }
    }

    @Override
    public void closeRouter(String roomId) {
        String routerId = roomRouters.remove(roomId);
        if (routerId != null) {
            log.info("Closed Router '{}' and released Worker resources for Room '{}'", routerId, roomId);
        }
    }
}

