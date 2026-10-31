package com.systemdesign.conferencing.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connected Participant State in a Conference Room.
 */
public class Peer {
    private final String peerId;
    private final String displayName;
    private final WebSocketSession session;
    private final long joinedAt;

    // SFU Transports
    private volatile TransportInfo sendTransport;
    private volatile TransportInfo recvTransport;

    // Active Producers published by this peer (producerId -> ProducerInfo)
    private final Map<String, ProducerInfo> producers = new ConcurrentHashMap<>();

    // Active Consumers received by this peer (consumerId -> ConsumerInfo)
    private final Map<String, ConsumerInfo> consumers = new ConcurrentHashMap<>();

    public Peer(String peerId, String displayName, WebSocketSession session) {
        this.peerId = peerId;
        this.displayName = displayName != null ? displayName : peerId;
        this.session = session;
        this.joinedAt = System.currentTimeMillis();
    }

    public String getPeerId() { return peerId; }
    public String getDisplayName() { return displayName; }
    public WebSocketSession getSession() { return session; }
    public long getJoinedAt() { return joinedAt; }

    public TransportInfo getSendTransport() { return sendTransport; }
    public void setSendTransport(TransportInfo sendTransport) { this.sendTransport = sendTransport; }

    public TransportInfo getRecvTransport() { return recvTransport; }
    public void setRecvTransport(TransportInfo recvTransport) { this.recvTransport = recvTransport; }

    public Map<String, ProducerInfo> getProducers() { return producers; }
    public Map<String, ConsumerInfo> getConsumers() { return consumers; }

    public void addProducer(ProducerInfo producer) {
        producers.put(producer.getId(), producer);
    }

    public void removeProducer(String producerId) {
        producers.remove(producerId);
    }

    public void addConsumer(ConsumerInfo consumer) {
        consumers.put(consumer.getId(), consumer);
    }

    public void removeConsumer(String consumerId) {
        consumers.remove(consumerId);
    }
}

