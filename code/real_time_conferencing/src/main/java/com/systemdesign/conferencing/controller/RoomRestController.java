package com.systemdesign.conferencing.controller;

import com.systemdesign.conferencing.model.Room;
import com.systemdesign.conferencing.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for inspecting conference rooms, active participants,
 * and SFU router status.
 */
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomRestController {

    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Lists all active conference rooms and their metrics.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listActiveRooms() {
        List<Map<String, Object>> response = new ArrayList<>();

        for (Room room : roomService.getAllRooms()) {
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomId", room.getRoomId());
            roomData.put("routerId", room.getRouterId());
            roomData.put("participantCount", room.getPeers().size());
            roomData.put("activeProducersCount", room.getActiveProducers().size());
            roomData.put("createdAt", room.getCreatedAt());
            response.add(roomData);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed information about a specific room.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> details = new HashMap<>();
        details.put("roomId", room.getRoomId());
        details.put("routerId", room.getRouterId());

        List<Map<String, Object>> peers = new ArrayList<>();
        room.getPeerList().forEach(p -> peers.add(Map.of(
                "peerId", p.getPeerId(),
                "displayName", p.getDisplayName(),
                "joinedAt", p.getJoinedAt(),
                "producersCount", p.getProducers().size(),
                "consumersCount", p.getConsumers().size()
        )));
        details.put("peers", peers);

        List<Map<String, Object>> producers = new ArrayList<>();
        room.getActiveProducers().values().forEach(prod -> producers.add(Map.of(
                "id", prod.getId(),
                "peerId", prod.getPeerId(),
                "kind", prod.getKind()
        )));
        details.put("activeProducers", producers);

        return ResponseEntity.ok(details);
    }
}

