package com.myticket.desktop.network;

import jakarta.websocket.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Lightweight STOMP-over-WebSocket client for the JavaFX desktop app.
 * Connects to the backend's /ws endpoint and supports topic subscriptions.
 * Includes automatic reconnection (up to 3 attempts, 5 second intervals).
 */
@ClientEndpoint
public class StompWebSocketClient {

    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 5000;

    private Session session;
    private String url;
    private final Map<String, Consumer<String>> subscriptions = new ConcurrentHashMap<>();
    private int subscriptionCounter = 0;
    private int reconnectAttempts = 0;
    private volatile boolean intentionalClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    /**
     * Connect to the STOMP WebSocket endpoint.
     */
    public void connect(String url) {
        this.url = url;
        this.intentionalClose = false;
        this.reconnectAttempts = 0;
        doConnect();
    }

    private void doConnect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(url));
        } catch (Exception e) {
            System.err.println("WebSocket connection failed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.reconnectAttempts = 0;
        System.out.println("WebSocket connected to " + url);

        // Send STOMP CONNECT frame
        sendFrame("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0");

        // Re-subscribe to any existing subscriptions
        for (Map.Entry<String, Consumer<String>> entry : subscriptions.entrySet()) {
            doSubscribe(entry.getKey());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        // Parse STOMP frames
        if (message.startsWith("MESSAGE")) {
            String destination = extractHeader(message, "destination");
            String body = extractBody(message);
            if (destination != null) {
                Consumer<String> handler = subscriptions.get(destination);
                if (handler != null) {
                    handler.accept(body);
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket closed: " + reason.getReasonPhrase());
        this.session = null;
        if (!intentionalClose) {
            scheduleReconnect();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    /**
     * Subscribe to a STOMP topic. Callback receives the raw message body.
     */
    public void subscribe(String topic, Consumer<String> onMessage) {
        subscriptions.put(topic, onMessage);
        if (session != null && session.isOpen()) {
            doSubscribe(topic);
        }
    }

    private void doSubscribe(String topic) {
        int id = ++subscriptionCounter;
        sendFrame("SUBSCRIBE\nid:sub-" + id + "\ndestination:" + topic + "\n\n\0");
    }

    /**
     * Disconnect from the WebSocket server.
     */
    public void disconnect() {
        intentionalClose = true;
        try {
            if (session != null && session.isOpen()) {
                sendFrame("DISCONNECT\n\n\0");
                session.close();
            }
        } catch (Exception e) {
            System.err.println("Error disconnecting WebSocket: " + e.getMessage());
        }
        scheduler.shutdownNow();
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    // ── internals ──

    private void sendFrame(String frame) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(frame);
            }
        } catch (Exception e) {
            System.err.println("Failed to send STOMP frame: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("Max WebSocket reconnect attempts reached (" + MAX_RECONNECT_ATTEMPTS + ").");
            return;
        }
        reconnectAttempts++;
        System.out.println("Scheduling WebSocket reconnect attempt " + reconnectAttempts + " in " + RECONNECT_DELAY_MS + "ms...");
        scheduler.schedule(this::doConnect, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private String extractHeader(String frame, String headerName) {
        for (String line : frame.split("\n")) {
            if (line.startsWith(headerName + ":")) {
                return line.substring(headerName.length() + 1).trim();
            }
        }
        return null;
    }

    private String extractBody(String frame) {
        int blankLineIndex = frame.indexOf("\n\n");
        if (blankLineIndex >= 0 && blankLineIndex + 2 < frame.length()) {
            String body = frame.substring(blankLineIndex + 2);
            // Remove STOMP null terminator
            if (body.endsWith("\0")) {
                body = body.substring(0, body.length() - 1);
            }
            return body;
        }
        return "";
    }
}
