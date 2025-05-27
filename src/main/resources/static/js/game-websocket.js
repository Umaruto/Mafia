class GameWebSocket {
    constructor(gameCode, username) {
        this.gameCode = gameCode;
        this.username = username;
        this.socket = null;
        this.stompClient = null;
        this.callbacks = new Map();
    }

    connect() {
        this.socket = new SockJS('/ws');
        this.stompClient = Stomp.over(this.socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected to WebSocket');
            
            // Subscribe to game updates
            this.stompClient.subscribe(`/topic/game/${this.gameCode}`, (message) => {
                const event = JSON.parse(message.body);
                this.handleGameEvent(event);
            });
            
            // Subscribe to private messages
            this.stompClient.subscribe(`/user/queue/private`, (message) => {
                const event = JSON.parse(message.body);
                this.handlePrivateEvent(event);
            });
        });
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
    }

    sendAction(action, target) {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send(
                `/app/game/${this.gameCode}/action`,
                {},
                JSON.stringify({
                    username: this.username,
                    action: action,
                    target: target
                })
            );
        }
    }

    handleGameEvent(event) {
        const callback = this.callbacks.get(event.type);
        if (callback) {
            callback(event.payload);
        }
    }

    handlePrivateEvent(event) {
        const callback = this.callbacks.get(`private_${event.type}`);
        if (callback) {
            callback(event.payload);
        }
    }

    on(eventType, callback) {
        this.callbacks.set(eventType, callback);
    }

    onPrivate(eventType, callback) {
        this.callbacks.set(`private_${eventType}`, callback);
    }
}