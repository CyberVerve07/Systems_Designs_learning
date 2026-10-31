package com.systemdesign.conferencing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard WebRTC & Mediasoup Signaling Envelope.
 *
 * Exchanged between Client browsers and Spring Boot signaling backend over WebSocket.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalMessage {

    /**
     * Action types supported by the signaling protocol.
     */
    public enum Type {
        // Room Lifecycle
        JOIN_ROOM,
        ROOM_JOINED,
        LEAVE_ROOM,
        PEER_JOINED,
        PEER_LEFT,

        // Mediasoup Transport Negotiation
        GET_ROUTER_CAPABILITIES,
        ROUTER_CAPABILITIES,
        CREATE_WEBRTC_TRANSPORT,
        WEBRTC_TRANSPORT_CREATED,
        CONNECT_TRANSPORT,
        TRANSPORT_CONNECTED,

        // Producer (Publishing Media)
        PRODUCE,
        PRODUCED,
        NEW_PRODUCER,
        CLOSE_PRODUCER,
        PRODUCER_CLOSED,

        // Consumer (Receiving Media)
        CONSUME,
        CONSUMED,
        RESUME_CONSUMER,
        CONSUMER_CLOSED,

        // P2P Fallback Signaling (for Mesh mode)
        OFFER,
        ANSWER,
        ICE_CANDIDATE,

        // System
        ERROR
    }

    private Type type;
    private String roomId;
    private String peerId;
    private String targetPeerId;
    private Map<String, Object> payload;
    private long timestamp = System.currentTimeMillis();

    public SignalMessage() {}

    public SignalMessage(Type type, String roomId, String peerId, Map<String, Object> payload) {
        this.type = type;
        this.roomId = roomId;
        this.peerId = peerId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public static SignalMessage error(String roomId, String peerId, String errorMessage) {
        SignalMessage msg = new SignalMessage();
        msg.setType(Type.ERROR);
        msg.setRoomId(roomId);
        msg.setPeerId(peerId);
        msg.setPayload(Map.of("error", errorMessage));
        return msg;
    }

    // Getters and Setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getTargetPeerId() { return targetPeerId; }
    public void setTargetPeerId(String targetPeerId) { this.targetPeerId = targetPeerId; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "SignalMessage{" +
                "type=" + type +
                ", roomId='" + roomId + '\'' +
                ", peerId='" + peerId + '\'' +
                ", payload=" + payload +
                '}';
    }
}

