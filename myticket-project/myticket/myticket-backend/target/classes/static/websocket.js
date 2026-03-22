/**
 * WebSocket STOMP client wrapper for the browser.
 * Requires SockJS and STOMP.js loaded via CDN before importing.
 * 
 * <script src="https://cdn.jsdelivr.net/npm/sockjs-client/dist/sockjs.min.js"></script>
 * <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs/bundles/stomp.umd.min.js"></script>
 */

class WebSocketClient {
    constructor() {
        this.stompClient = null;
        this.subscriptions = [];
    }

    /**
     * Connects to the event topics: seats and attendance.
     * @param {number} eventId The ID of the event to subscribe to.
     * @param {function} onSeatUpdate Callback taking a SeatUpdateMessage obj
     * @param {function} onAttendanceUpdate Callback taking an AttendanceUpdateMessage obj
     */
    connectToEvent(eventId, onSeatUpdate, onAttendanceUpdate) {
        // Build WS URL dynamically from current origin
        const wsUrl = window.location.origin.replace('http', 'ws') + '/ws';
        
        // Use STOMP over WebSocket
        this.stompClient = new window.StompJs.Client({
            brokerURL: wsUrl,
            // Fallback for browsers that don't support native WebSocket
            webSocketFactory: () => new window.SockJS('/ws'), 
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000
        });

        this.stompClient.onConnect = (frame) => {
            console.log('Connected to WebSocket:', frame);

            // Subscribe to Seat Updates
            if (onSeatUpdate) {
                const sub1 = this.stompClient.subscribe(`/topic/seats/${eventId}`, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        onSeatUpdate(data);
                    } catch (e) {
                         console.error("Failed parsing seat update:", e);
                    }
                });
                this.subscriptions.push(sub1);
            }

            // Subscribe to Attendance Updates
            if (onAttendanceUpdate) {
                const sub2 = this.stompClient.subscribe(`/topic/attendance/${eventId}`, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        onAttendanceUpdate(data);
                    } catch (e) {
                         console.error("Failed parsing attendance update:", e);
                    }
                });
                this.subscriptions.push(sub2);
            }
        };

        this.stompClient.onStompError = (frame) => {
            console.error('WebSocket Error:', frame.headers['message'], frame.body);
        };

        // Connect!
        this.stompClient.activate();
    }

    disconnect() {
        if (this.stompClient) {
            this.subscriptions.forEach(sub => sub.unsubscribe());
            this.subscriptions = [];
            this.stompClient.deactivate();
            console.log("WebSocket Disconnected");
        }
    }
}

// Expose globally
const wsClient = new WebSocketClient();
