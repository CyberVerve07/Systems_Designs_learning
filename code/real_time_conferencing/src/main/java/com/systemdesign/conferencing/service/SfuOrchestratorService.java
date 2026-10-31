package com.systemdesign.conferencing.service;

import com.systemdesign.conferencing.model.ConsumerInfo;
import com.systemdesign.conferencing.model.ProducerInfo;
import com.systemdesign.conferencing.model.TransportInfo;

import java.util.Map;

/**
 * Service contract for interacting with the SFU (e.g. Mediasoup cluster).
 *
 * In a full production setup, this talks to Mediasoup C++ worker nodes
 * via Unix Domain Sockets, FlatBuffers, or high-performance gRPC/WebSocket sidecars.
 */
public interface SfuOrchestratorService {

    /**
     * Obtains or creates a Mediasoup Router instance for a conference room.
     * @param roomId Unique room identifier.
     * @return Generated or assigned Router ID.
     */
    String getOrCreateRouter(String roomId);

    /**
     * Returns the RTP Capabilities of the router (codecs supported: VP8, VP9, H264, Opus).
     */
    Map<String, Object> getRouterRtpCapabilities(String roomId);

    /**
     * Creates a new WebRtcTransport on the SFU for a specific peer.
     * @param roomId The room ID.
     * @param peerId The participant ID.
     * @param direction "send" (publishing) or "recv" (consuming).
     * @return Transport information including ICE and DTLS parameters.
     */
    TransportInfo createWebRtcTransport(String roomId, String peerId, String direction);

    /**
     * Connects an existing WebRtcTransport using the client's DTLS parameters.
     */
    boolean connectTransport(String roomId, String transportId, Map<String, Object> dtlsParameters);

    /**
     * Creates a Producer on the SFU (starts receiving media from client's SendTransport).
     */
    ProducerInfo createProducer(String roomId, String transportId, String peerId, String kind, Map<String, Object> rtpParameters);

    /**
     * Creates a Consumer on the SFU (starts forwarding media from a Producer to a client's RecvTransport).
     */
    ConsumerInfo createConsumer(String roomId, String transportId, String peerId, String producerId, Map<String, Object> clientRtpCapabilities);

    /**
     * Closes an active Producer.
     */
    void closeProducer(String roomId, String producerId);

    /**
     * Closes an active Consumer.
     */
    void closeConsumer(String roomId, String consumerId);

    /**
     * Closes and cleans up a Router when a room is destroyed.
     */
    void closeRouter(String roomId);
}

