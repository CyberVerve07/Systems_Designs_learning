package com.systemdesign.conferencing.model;

import java.util.Map;

/**
 * Metadata for a Media Consumer (media stream received by a client from a Producer).
 */
public class ConsumerInfo {
    private String id;
    private String producerId;
    private String peerId;
    private String kind; // "audio" or "video"
    private Map<String, Object> rtpParameters;
    private boolean paused;

    public ConsumerInfo() {}

    public ConsumerInfo(String id, String producerId, String peerId, String kind, Map<String, Object> rtpParameters) {
        this.id = id;
        this.producerId = producerId;
        this.peerId = peerId;
        this.kind = kind;
        this.rtpParameters = rtpParameters;
        this.paused = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public Map<String, Object> getRtpParameters() { return rtpParameters; }
    public void setRtpParameters(Map<String, Object> rtpParameters) { this.rtpParameters = rtpParameters; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
}

