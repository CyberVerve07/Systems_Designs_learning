package com.systemdesign.conferencing.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conference Room domain model.
 *
 * Encapsulates the room ID, the associated SFU Router ID,
 * and the thread-safe collection of participating peers.
 */
public class Room {
    private final String roomId;
    private final String routerId;
    private final long createdAt;

    // Active participants in this room (peerId -> Peer)
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    // Map of all active producers in this room (producerId -> ProducerInfo)
    private final Map<String, ProducerInfo> activeProducers = new ConcurrentHashMap<>();

    public Room(String roomId, String routerId) {
        this.roomId = roomId;
        this.routerId = routerId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getRoomId() { return roomId; }
    public String getRouterId() { return routerId; }
    public long getCreatedAt() { return createdAt; }

    public Map<String, Peer> getPeers() { return peers; }
    public Collection<Peer> getPeerList() { return peers.values(); }

    public void addPeer(Peer peer) {
        peers.put(peer.getPeerId(), peer);
    }

    public Peer getPeer(String peerId) {
        return peers.get(peerId);
    }

    public Peer removePeer(String peerId) {
        Peer removed = peers.remove(peerId);
        if (removed != null) {
            // Remove all producers published by this peer from the room's active producers
            removed.getProducers().keySet().forEach(activeProducers::remove);
        }
        return removed;
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }

    public Map<String, ProducerInfo> getActiveProducers() {
        return activeProducers;
    }

    public void registerProducer(ProducerInfo producer) {
        activeProducers.put(producer.getId(), producer);
    }

    public void unregisterProducer(String producerId) {
        activeProducers.remove(producerId);
    }
}

