package com.systemdesign.conferencing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.conferencing.model.*;
import com.systemdesign.conferencing.service.RoomService;
import com.systemdesign.conferencing.service.SfuOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-Performance WebRTC & SFU Signaling Handler.
 *
 * Implements the asynchronous JSON-based signaling protocol over WebSockets.
 * Handles peer admission, SDP/ICE exchanges, Mediasoup transport handshakes,
 * and media producer/consumer synchronization.
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomService roomService;
    private final SfuOrchestratorService sfuOrchestrator;

    // Session ID -> SessionMetadata
    private static class SessionMetadata {
        String roomId;
        String peerId;
        SessionMetadata(String roomId, String peerId) {
            this.roomId = roomId;
            this.peerId = peerId;
        }
    }

    private final Map<String, SessionMetadata> sessionMetadataMap = new ConcurrentHashMap<>();

    public SignalingWebSocketHandler(RoomService roomService, SfuOrchestratorService sfuOrchestrator) {
        this.roomService = roomService;
        this.sfuOrchestrator = sfuOrchestrator;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established. Session ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            SignalMessage message = objectMapper.readValue(textMessage.getPayload(), SignalMessage.class);
            if (message.getType() == null) {
                sendError(session, null, null, "Message 'type' cannot be null.");
                return;
            }

            switch (message.getType()) {
                case JOIN_ROOM -> handleJoinRoom(session, message);
                case GET_ROUTER_CAPABILITIES -> handleGetRouterCapabilities(session, message);
                case CREATE_WEBRTC_TRANSPORT -> handleCreateTransport(session, message);
                case CONNECT_TRANSPORT -> handleConnectTransport(session, message);
                case PRODUCE -> handleProduce(session, message);
                case CONSUME -> handleConsume(session, message);
                case LEAVE_ROOM -> handleLeaveRoom(session, message);
                case OFFER, ANSWER, ICE_CANDIDATE -> handleP2PRelay(session, message);
                default -> log.warn("Unhandled message type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Error processing incoming WebSocket message: {}", e.getMessage(), e);
            sendError(session, null, null, "Failed to process message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionMetadata meta = sessionMetadataMap.remove(session.getId());
        if (meta != null && meta.roomId != null && meta.peerId != null) {
            log.info("Client disconnected (Session: {}). Cleaning up peer '{}' from room '{}'",
                    session.getId(), meta.peerId, meta.roomId);
            cleanUpPeer(meta.roomId, meta.peerId);
        }
    }

    // =========================================================================
    // Core Signaling Handlers
    // =========================================================================

    private void handleJoinRoom(WebSocketSession session, SignalMessage msg) throws IOException {
        String roomId = msg.getRoomId();
        String peerId = msg.getPeerId();
        Map<String, Object> payload = msg.getPayload() != null ? msg.getPayload() : Map.of();
        String displayName = (String) payload.getOrDefault("displayName", peerId);

        if (roomId == null || peerId == null) {
            sendError(session, roomId, peerId, "roomId and peerId are required to join.");
            return;
        }

        // Store session mapping
        sessionMetadataMap.put(session.getId(), new SessionMetadata(roomId, peerId));

        // Create Peer and register in RoomService
        Peer peer = new Peer(peerId, displayName, session);
        roomService.joinRoom(roomId, peer);

        // Fetch SFU Router capabilities
        Map<String, Object> rtpCapabilities = sfuOrchestrator.getRouterRtpCapabilities(roomId);

        // Fetch current room members and existing active producers
        Room room = roomService.getRoom(roomId);
        List<Map<String, Object>> existingPeers = new ArrayList<>();
        List<Map<String, Object>> existingProducers = new ArrayList<>();

        for (Peer otherPeer : room.getPeerList()) {
            if (!otherPeer.getPeerId().equals(peerId)) {
                existingPeers.add(Map.of(
                        "peerId", otherPeer.getPeerId(),
                        "displayName", otherPeer.getDisplayName()
                ));
            }
        }

        for (ProducerInfo prod : room.getActiveProducers().values()) {
            if (!prod.getPeerId().equals(peerId)) {
                existingProducers.add(Map.of(
                        "producerId", prod.getId(),
                        "peerId", prod.getPeerId(),
                        "kind", prod.getKind()
                ));
            }
        }

        // Send confirmation back to newly joined peer
        Map<String, Object> joinResponse = new HashMap<>();
        joinResponse.put("routerRtpCapabilities", rtpCapabilities);
        joinResponse.put("peers", existingPeers);
        joinResponse.put("existingProducers", existingProducers);

        send(session, new SignalMessage(SignalMessage.Type.ROOM_JOINED, roomId, peerId, joinResponse));

        // Broadcast to other peers in room that a new peer joined
        broadcastToRoom(roomId, peerId, new SignalMessage(
                SignalMessage.Type.PEER_JOINED,
                roomId,
                peerId,
                Map.of("peerId", peerId, "displayName", displayName)
        ));
    }

    private void handleGetRouterCapabilities(WebSocketSession session, SignalMessage msg) throws IOException {
        Map<String, Object> caps = sfuOrchestrator.getRouterRtpCapabilities(msg.getRoomId());
        send(session, new SignalMessage(SignalMessage.Type.ROUTER_CAPABILITIES, msg.getRoomId(), msg.getPeerId(), caps));
    }

    private void handleCreateTransport(WebSocketSession session, SignalMessage msg) throws IOException {
        String direction = (String) msg.getPayload().get("direction"); // "send" or "recv"
        TransportInfo transport = sfuOrchestrator.createWebRtcTransport(msg.getRoomId(), msg.getPeerId(), direction);

        Room room = roomService.getRoom(msg.getRoomId());
        if (room != null) {
            Peer peer = room.getPeer(msg.getPeerId());
            if (peer != null) {
                if ("send".equalsIgnoreCase(direction)) {
                    peer.setSendTransport(transport);
                } else {
                    peer.setRecvTransport(transport);
                }
            }
        }

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("id", transport.getId());
        responsePayload.put("direction", transport.getDirection());
        responsePayload.put("iceParameters", transport.getIceParameters());
        responsePayload.put("iceCandidates", transport.getIceCandidates());
        responsePayload.put("dtlsParameters", transport.getDtlsParameters());

        send(session, new SignalMessage(SignalMessage.Type.WEBRTC_TRANSPORT_CREATED, msg.getRoomId(), msg.getPeerId(), responsePayload));
    }

    @SuppressWarnings("unchecked")
    private void handleConnectTransport(WebSocketSession session, SignalMessage msg) throws IOException {
        String transportId = (String) msg.getPayload().get("transportId");
        Map<String, Object> dtlsParameters = (Map<String, Object>) msg.getPayload().get("dtlsParameters");

        boolean connected = sfuOrchestrator.connectTransport(msg.getRoomId(), transportId, dtlsParameters);

        send(session, new SignalMessage(
                SignalMessage.Type.TRANSPORT_CONNECTED,
                msg.getRoomId(),
                msg.getPeerId(),
                Map.of("transportId", transportId, "connected", connected)
        ));
    }

    @SuppressWarnings("unchecked")
    private void handleProduce(WebSocketSession session, SignalMessage msg) throws IOException {
        String transportId = (String) msg.getPayload().get("transportId");
        String kind = (String) msg.getPayload().get("kind"); // "audio" or "video"
        Map<String, Object> rtpParameters = (Map<String, Object>) msg.getPayload().get("rtpParameters");

        ProducerInfo producer = sfuOrchestrator.createProducer(
                msg.getRoomId(), transportId, msg.getPeerId(), kind, rtpParameters
        );

        Room room = roomService.getRoom(msg.getRoomId());
        if (room != null) {
            Peer peer = room.getPeer(msg.getPeerId());
            if (peer != null) {
                peer.addProducer(producer);
            }
            room.registerProducer(producer);
        }

        // 1. Confirm to the producer
        send(session, new SignalMessage(
                SignalMessage.Type.PRODUCED,
                msg.getRoomId(),
                msg.getPeerId(),
                Map.of("id", producer.getId(), "kind", kind)
        ));

        // 2. Announce new media stream to ALL OTHER peers in the room
        broadcastToRoom(msg.getRoomId(), msg.getPeerId(), new SignalMessage(
                SignalMessage.Type.NEW_PRODUCER,
                msg.getRoomId(),
                msg.getPeerId(),
                Map.of("producerId", producer.getId(), "producerPeerId", msg.getPeerId(), "kind", kind)
        ));
    }

    @SuppressWarnings("unchecked")
    private void handleConsume(WebSocketSession session, SignalMessage msg) throws IOException {
        String transportId = (String) msg.getPayload().get("transportId");
        String producerId = (String) msg.getPayload().get("producerId");
        Map<String, Object> rtpCapabilities = (Map<String, Object>) msg.getPayload().get("rtpCapabilities");

        ConsumerInfo consumer = sfuOrchestrator.createConsumer(
                msg.getRoomId(), transportId, msg.getPeerId(), producerId, rtpCapabilities
        );

        if (consumer == null) {
            sendError(session, msg.getRoomId(), msg.getPeerId(), "Unable to create consumer for producer " + producerId);
            return;
        }

        Room room = roomService.getRoom(msg.getRoomId());
        if (room != null) {
            Peer peer = room.getPeer(msg.getPeerId());
            if (peer != null) {
                peer.addConsumer(consumer);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", consumer.getId());
        response.put("producerId", consumer.getProducerId());
        response.put("kind", consumer.getKind());
        response.put("rtpParameters", consumer.getRtpParameters());

        send(session, new SignalMessage(SignalMessage.Type.CONSUMED, msg.getRoomId(), msg.getPeerId(), response));
    }

    private void handleLeaveRoom(WebSocketSession session, SignalMessage msg) {
        cleanUpPeer(msg.getRoomId(), msg.getPeerId());
    }

    private void handleP2PRelay(WebSocketSession session, SignalMessage msg) throws IOException {
        // Fallback relay for pure P2P mesh or direct data channels
        String targetPeerId = msg.getTargetPeerId();
        if (targetPeerId == null) {
            return;
        }

        Room room = roomService.getRoom(msg.getRoomId());
        if (room != null) {
            Peer target = room.getPeer(targetPeerId);
            if (target != null && target.getSession().isOpen()) {
                send(target.getSession(), msg);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void cleanUpPeer(String roomId, String peerId) {
        Peer removed = roomService.leaveRoom(roomId, peerId);
        if (removed != null) {
            // Notify other peers that this user and their streams have left
            broadcastToRoom(roomId, peerId, new SignalMessage(
                    SignalMessage.Type.PEER_LEFT,
                    roomId,
                    peerId,
                    Map.of("peerId", peerId)
            ));

            for (String producerId : removed.getProducers().keySet()) {
                broadcastToRoom(roomId, peerId, new SignalMessage(
                        SignalMessage.Type.PRODUCER_CLOSED,
                        roomId,
                        peerId,
                        Map.of("producerId", producerId)
                ));
            }
        }
    }

    private void broadcastToRoom(String roomId, String excludePeerId, SignalMessage message) {
        Room room = roomService.getRoom(roomId);
        if (room == null) return;

        for (Peer peer : room.getPeerList()) {
            if (excludePeerId != null && excludePeerId.equals(peer.getPeerId())) {
                continue;
            }
            if (peer.getSession().isOpen()) {
                try {
                    send(peer.getSession(), message);
                } catch (IOException e) {
                    log.error("Failed to broadcast message to peer '{}': {}", peer.getPeerId(), e.getMessage());
                }
            }
        }
    }

    private synchronized void send(WebSocketSession session, SignalMessage message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String roomId, String peerId, String errorMessage) {
        try {
            send(session, SignalMessage.error(roomId, peerId, errorMessage));
        } catch (IOException e) {
            log.error("Failed to send error message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}

