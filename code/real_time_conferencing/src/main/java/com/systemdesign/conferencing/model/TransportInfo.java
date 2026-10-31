package com.systemdesign.conferencing.model;

import java.util.List;
import java.util.Map;

/**
 * Representation of a WebRTC Transport created on the SFU for a specific peer.
 */
public class TransportInfo {
    private String id;
    private String peerId;
    private String direction; // "send" or "recv"
    private Map<String, Object> iceParameters;
    private List<Map<String, Object>> iceCandidates;
    private Map<String, Object> dtlsParameters;
    private boolean connected;

    public TransportInfo() {}

    public TransportInfo(String id, String peerId, String direction,
                         Map<String, Object> iceParameters,
                         List<Map<String, Object>> iceCandidates,
                         Map<String, Object> dtlsParameters) {
        this.id = id;
        this.peerId = peerId;
        this.direction = direction;
        this.iceParameters = iceParameters;
        this.iceCandidates = iceCandidates;
        this.dtlsParameters = dtlsParameters;
        this.connected = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Map<String, Object> getIceParameters() { return iceParameters; }
    public void setIceParameters(Map<String, Object> iceParameters) { this.iceParameters = iceParameters; }

    public List<Map<String, Object>> getIceCandidates() { return iceCandidates; }
    public void setIceCandidates(List<Map<String, Object>> iceCandidates) { this.iceCandidates = iceCandidates; }

    public Map<String, Object> getDtlsParameters() { return dtlsParameters; }
    public void setDtlsParameters(Map<String, Object> dtlsParameters) { this.dtlsParameters = dtlsParameters; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
}

