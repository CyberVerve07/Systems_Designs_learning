package com.systemdesign.conferencing.service;

import com.systemdesign.conferencing.model.Peer;
import com.systemdesign.conferencing.model.ProducerInfo;
import com.systemdesign.conferencing.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Room & Participant State Service.
 *
 * Coordinates participants entering and leaving rooms, keeps track of
 * active producers, and triggers SFU router cleanup when rooms become empty.
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SfuOrchestratorService sfuOrchestrator;

    public RoomService(SfuOrchestratorService sfuOrchestrator) {
        this.sfuOrchestrator = sfuOrchestrator;
    }

    /**
     * Gets an existing room or creates a new one with an associated SFU router.
     */
    public Room getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> {
            String routerId = sfuOrchestrator.getOrCreateRouter(id);
            log.info("Initialized new Conference Room '{}' bound to SFU Router '{}'", id, routerId);
            return new Room(id, routerId);
        });
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    /**
     * Adds a participant to a room.
     */
    public void joinRoom(String roomId, Peer peer) {
        Room room = getOrCreateRoom(roomId);
        room.addPeer(peer);
        log.info("Peer '{}' joined Room '{}'. Total participants: {}",
                peer.getPeerId(), roomId, room.getPeers().size());
    }

    /**
     * Removes a participant from a room and cleans up resources if empty.
     */
    public Peer leaveRoom(String roomId, String peerId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        Peer removedPeer = room.removePeer(peerId);
        if (removedPeer != null) {
            log.info("Peer '{}' left Room '{}'. Remaining participants: {}",
                    peerId, roomId, room.getPeers().size());

            // Close all producers belonging to this peer on the SFU
            removedPeer.getProducers().values().forEach(producer ->
                    sfuOrchestrator.closeProducer(roomId, producer.getId())
            );

            // Close all consumers belonging to this peer on the SFU
            removedPeer.getConsumers().values().forEach(consumer ->
                    sfuOrchestrator.closeConsumer(roomId, consumer.getId())
            );

            // If room is empty, destroy SFU router and remove room
            if (room.isEmpty()) {
                rooms.remove(roomId);
                sfuOrchestrator.closeRouter(roomId);
                log.info("Room '{}' is now empty. Destroyed SFU router and cleaned up room state.", roomId);
            }
        }
        return removedPeer;
    }

    /**
     * Registers a newly published media stream into the room's catalog.
     */
    public void registerProducer(String roomId, ProducerInfo producer) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.registerProducer(producer);
        }
    }

    /**
     * Unregisters a closed media stream.
     */
    public void unregisterProducer(String roomId, String producerId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.unregisterProducer(producerId);
            sfuOrchestrator.closeProducer(roomId, producerId);
        }
    }
}

