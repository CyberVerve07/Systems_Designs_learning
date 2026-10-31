# 🎥 Production-Grade WebRTC Signaling & SFU Orchestrator in Spring Boot

This project implements a complete, production-ready **WebRTC Signaling Server and Selective Forwarding Unit (SFU) Orchestrator** in Java using **Spring Boot 3.x** and WebSockets.

It bridges browser clients with media server architectures like **Mediasoup**, managing virtual room life cycles, WebRtcTransport negotiation (ICE/DTLS), Media Producers, and Media Consumers.

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT TIER (Browsers)                           │
│  [Browser A (Alice)]                       [Browser B (Bob)]                │
│   • Captures Camera/Mic                     • Consumes Alice's Stream       │
│   • WebRTC PeerConnection                   • WebRTC PeerConnection         │
└──────────────┬────────────────────────────────────────┬─────────────────────┘
               │ WSS: /ws/signaling                     │ WSS: /ws/signaling
               ▼                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 SPRING BOOT SIGNALING & ORCHESTRATION CLUSTER               │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ SignalingWebSocketHandler (JSON Signaling Protocol)                   │  │
│  │   • JOIN_ROOM, CREATE_WEBRTC_TRANSPORT, CONNECT_TRANSPORT            │  │
│  │   • PRODUCE, CONSUME, LEAVE_ROOM, Error Handling                      │  │
│  └──────────────────────────────────┬────────────────────────────────────┘  │
│                                     │ Thread-safe State                     │
│  ┌──────────────────────────────────▼────────────────────────────────────┐  │
│  │ RoomService (Concurrent Room, Peer & Active Stream Registry)          │  │
│  └──────────────────────────────────┬────────────────────────────────────┘  │
│                                     │ Orchestration API                     │
│  ┌──────────────────────────────────▼────────────────────────────────────┐  │
│  │ SfuOrchestratorService (Mediasoup Interface)                          │  │
│  │   • Manages Router Allocation per Room                                │  │
│  │   • Creates Send & Receive WebRtcTransports                           │  │
│  │   • Connects Transports via DTLS Fingerprints                         │  │
│  │   • Registers Producers and Creates Outbound Consumers                │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │ Direct SRTP / UDP Media Traffic
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MEDIASOUP C++ SFU WORKER ENGINE                        │
│             (Routes raw RTP packets at ultra-low latency <10ms)             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📑 Key Features

1. **Decoupled Architecture:**
   - Media forwarding stays in native C++ (Mediasoup) to avoid JVM Garbage Collection pauses.
   - Signaling, security, room access control, and orchestration are handled in Spring Boot.
2. **Mediasoup Control Plane Abstractions:**
   - `Router`: Represents a virtual meeting room.
   - `WebRtcTransport`: Manages ICE candidates and DTLS handshakes (one `SendTransport` and one `RecvTransport` per peer).
   - `Producer`: Media published into the SFU.
   - `Consumer`: Media forwarded from the SFU to other participants.
3. **Interactive Testing Dashboard:**
   - Comes with a built-in dark-mode WebRTC test client located at `src/main/resources/static/index.html`.
   - Visualizes live signaling protocol messages in real time.
4. **REST Metrics & Monitoring:**
   - `GET /api/rooms` - Lists active rooms, router IDs, and participant counts.
   - `GET /api/rooms/{roomId}` - Deep inspection of peers, active producers, and consumers.

---

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17+**
- **Apache Maven 3.8+** (or run directly via IDE)

### 1. Build the Application
Navigate to this directory in your terminal:

```bash
mvn clean package -DskipTests
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

Or run the compiled JAR:
```bash
java -jar target/real-time-conferencing-1.0.0-SNAPSHOT.jar
```

The server will start on port `8080`.

---

## 🧪 Testing the Live Signaling & Media Exchange

1. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```
2. **First Participant (Alice):**
   - Room ID: `room-101`
   - Peer Name: `Alice`
   - Click **"Join Room"**
   - Click **"🎥 Start Camera & Mic"** (grant browser permissions)
   - Click **"🚀 Produce Stream (SFU)"**
   - *Observe the log console negotiate SendTransport, connect DTLS, and produce video stream.*

3. **Second Participant (Bob):**
   - Open a **second tab or incognito window** to `http://localhost:8080`
   - Room ID: `room-101`
   - Peer Name: `Bob`
   - Click **"Join Room"**
   - *Notice how Bob immediately receives `NEW_PRODUCER` from Alice, requests a `RecvTransport`, and creates a Consumer tile displaying Alice's video feed!*

---

## 📡 Signaling Protocol Specification

All signaling messages follow this JSON structure over `ws://localhost:8080/ws/signaling`:

```json
{
  "type": "JOIN_ROOM",
  "roomId": "room-101",
  "peerId": "Alice",
  "payload": {
    "displayName": "Alice"
  },
  "timestamp": 1730419200000
}
```

### Protocol Exchanges:

| Direction | Message Type | Description |
|:---:|:---|:---|
| Client $\to$ Server | `JOIN_ROOM` | Participant requests to join room. |
| Server $\to$ Client | `ROOM_JOINED` | Delivers SFU `routerRtpCapabilities`, existing peers, and current producers. |
| Server $\to$ Peers | `PEER_JOINED` | Broadcasts new peer arrival to all participants in the room. |
| Client $\to$ Server | `CREATE_WEBRTC_TRANSPORT` | Requests a `send` or `recv` transport on the SFU. |
| Server $\to$ Client | `WEBRTC_TRANSPORT_CREATED`| Returns transport ID, ICE credentials, and DTLS fingerprint. |
| Client $\to$ Server | `CONNECT_TRANSPORT` | Passes client DTLS parameters to complete the handshake. |
| Client $\to$ Server | `PRODUCE` | Publishes an audio or video track to the SFU. |
| Server $\to$ Client | `PRODUCED` | Confirms producer ID. |
| Server $\to$ Peers | `NEW_PRODUCER` | Notifies other participants that a new stream is available to consume. |
| Client $\to$ Server | `CONSUME` | Subscribes to an active Producer via client's RecvTransport. |
| Server $\to$ Client | `CONSUMED` | Returns consumer parameters to attach stream to client video element. |
| Client $\to$ Server | `LEAVE_ROOM` | Leaves meeting; tears down all associated producers and consumers. |

---

## 🔌 Connecting to a Real Mediasoup C++ / Node.js Cluster

In production, Mediasoup C++ workers run on dedicated edge instances.
To connect this Spring Boot orchestrator to a live Mediasoup cluster:

1. Update `application.properties`:
   ```properties
   app.conferencing.sfu.mock-mode=false
   app.conferencing.sfu.external-worker-url=http://sfu-worker-us-east.internal:3000
   ```
2. In `MediasoupOrchestratorServiceImpl.java`, use a `WebClient` or gRPC stub to forward `createRouter`, `createWebRtcTransport`, `connectTransport`, `produce`, and `consume` RPCs to the Mediasoup worker daemon.
3. Use **Redis Pub/Sub** to synchronize signaling events across multiple Spring Boot nodes when running in a multi-instance Kubernetes cluster.

---

## 📚 Deep-Dive Theory & System Design

For theoretical derivations, back-of-the-envelope bandwidth calculations, Mesh vs MCU vs SFU trade-offs, and multi-region cascaded SFUs:
👉 See: [`theory/real_time_conferencing/real_time_conferencing_system_design.md`](../../theory/real_time_conferencing/real_time_conferencing_system_design.md)

