/**
 * Mafia Game Chat System
 * Handles real-time chat functionality for day and night phases
 */

class ChatManager {
    constructor(gameCode, username) {
        this.gameCode = gameCode;
        this.username = username;
        this.stompClient = null;
        this.isConnected = false;
        this.currentPhase = 'DAY';
        this.playerRole = '';
        this.isPlayerAlive = true;
        this.messages = [];
        
        this.initializeElements();
        this.setupEventListeners();
    }
    
    initializeElements() {
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatInput');
        this.sendBtn = document.getElementById('sendChatBtn');
        this.chatTitle = document.getElementById('chatTitle');
        this.chatPhaseIndicator = document.getElementById('chatPhaseIndicator');
        this.chatStatus = document.getElementById('chatStatus');
    }
    
    setupEventListeners() {
        // Send message on button click
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        
        // Send message on Enter key
        this.chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        
        // Auto-resize input and character counter
        this.chatInput.addEventListener('input', () => {
            this.updateCharacterCount();
        });
    }
    
    connect() {
        if (this.isConnected) return;
        
        try {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            
            this.stompClient.connect({}, 
                (frame) => {
                    console.log('Chat connected: ' + frame);
                    this.isConnected = true;
                    this.subscribeToMessages();
                },
                (error) => {
                    console.error('Chat connection error:', error);
                    this.updateStatus('Connection error - retrying...', 'error');
                    setTimeout(() => this.connect(), 5000);
                }
            );
        } catch (error) {
            console.error('Failed to create chat connection:', error);
            this.updateStatus('Failed to connect to chat', 'error');
        }
    }
    
    subscribeToMessages() {
        // Subscribe to public chat messages
        this.stompClient.subscribe(`/topic/game/${this.gameCode}/chat`, (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayMessage(chatMessage);
        });
        
        // Subscribe to private Mafia chat if player is Mafia
        if (this.playerRole === 'MAFIA') {
            this.stompClient.subscribe(`/topic/game/${this.gameCode}/chat/MAFIA`, (message) => {
                const chatMessage = JSON.parse(message.body);
                this.displayMessage(chatMessage);
            });
        }
    }
    
    sendMessage() {
        const messageText = this.chatInput.value.trim();
        if (!messageText || !this.isConnected || !this.canSendMessage()) {
            return;
        }
        
        const chatType = this.determineChatType();
        const targetRole = chatType === 'PRIVATE' ? 'MAFIA' : null;
        
        const messageData = {
            username: this.username,
            message: messageText,
            chatType: chatType,
            targetRole: targetRole
        };
        
        // Send via WebSocket
        this.stompClient.send(`/app/game/${this.gameCode}/chat`, {}, JSON.stringify(messageData));
        
        // Clear input
        this.chatInput.value = '';
        this.updateCharacterCount();
        
        // Focus back to input
        this.chatInput.focus();
    }
    
    displayMessage(message) {
        const messageElement = document.createElement('div');
        messageElement.className = this.getMessageClass(message);
        
        const isOwnMessage = message.senderUsername === this.username;
        const isSystemMessage = message.senderUsername === 'SYSTEM';
        
        if (isSystemMessage) {
            messageElement.innerHTML = `
                <div class="content">${this.escapeHtml(message.message)}</div>
                <div class="timestamp">${message.timestamp}</div>
            `;
        } else {
            messageElement.innerHTML = `
                <div class="sender">${this.escapeHtml(message.senderUsername)}${isOwnMessage ? ' (You)' : ''}</div>
                <div class="content">${this.escapeHtml(message.message)}</div>
                <div class="timestamp">${message.timestamp}</div>
            `;
        }
        
        this.chatMessages.appendChild(messageElement);
        
        // Add animation
        if (window.animationManager) {
            messageElement.classList.add('chat-message', 'animate-slide-in-up');
        } else {
            messageElement.classList.add('chat-message');
        }
        
        // Scroll to bottom
        this.scrollToBottom();
        
        // Store message
        this.messages.push(message);
        
        // Limit stored messages to prevent memory issues
        if (this.messages.length > 100) {
            this.messages.shift();
        }
    }
    
    getMessageClass(message) {
        const isOwnMessage = message.senderUsername === this.username;
        const isSystemMessage = message.senderUsername === 'SYSTEM';
        
        let className = 'chat-message';
        
        if (isSystemMessage) {
            className += ' system-message';
        } else if (message.chatType === 'PRIVATE' && message.targetRole === 'MAFIA') {
            className += ' mafia-message';
        } else if (isOwnMessage) {
            className += ' own-message';
        } else {
            className += ' other-message';
        }
        
        return className;
    }
    
    determineChatType() {
        if (this.currentPhase === 'NIGHT' && this.playerRole === 'MAFIA') {
            return 'PRIVATE';
        }
        return 'PUBLIC';
    }
    
    canSendMessage() {
        // Dead players cannot send messages
        if (!this.isPlayerAlive) {
            return false;
        }
        
        // During day phase, everyone can chat publicly
        if (this.currentPhase === 'DAY') {
            return true;
        }
        
        // During night phase, only Mafia can chat privately
        if (this.currentPhase === 'NIGHT' && this.playerRole === 'MAFIA') {
            return true;
        }
        
        return false;
    }
    
    updatePhase(phase, playerRole, isAlive) {
        this.currentPhase = phase;
        this.playerRole = playerRole;
        this.isPlayerAlive = isAlive;
        
        this.updateUI();
        this.loadMessages();
    }
    
    updateUI() {
        // Update phase indicator
        this.chatPhaseIndicator.textContent = this.currentPhase;
        this.chatPhaseIndicator.className = 'badge ms-2';
        
        if (this.currentPhase === 'DAY') {
            this.chatPhaseIndicator.classList.add('bg-success');
            this.chatTitle.textContent = 'Public Chat';
        } else {
            if (this.playerRole === 'MAFIA') {
                this.chatPhaseIndicator.classList.add('bg-danger');
                this.chatTitle.textContent = 'Mafia Chat';
            } else {
                this.chatPhaseIndicator.classList.add('bg-secondary');
                this.chatTitle.textContent = 'Chat Disabled';
            }
        }
        
        // Update input state
        const canChat = this.canSendMessage();
        this.chatInput.disabled = !canChat;
        this.sendBtn.disabled = !canChat;
        
        // Update status message
        this.updateChatStatus();
    }
    
    updateChatStatus() {
        let statusText = '';
        let statusClass = 'text-muted';
        
        if (!this.isPlayerAlive) {
            statusText = 'Dead players cannot chat';
            statusClass = 'text-danger';
        } else if (this.currentPhase === 'DAY') {
            statusText = 'Public chat - all players can see your messages';
            statusClass = 'text-success';
        } else if (this.currentPhase === 'NIGHT') {
            if (this.playerRole === 'MAFIA') {
                statusText = 'Private Mafia chat - only Mafia members can see';
                statusClass = 'text-danger';
            } else {
                statusText = 'Chat disabled during night phase';
                statusClass = 'text-secondary';
            }
        }
        
        this.updateStatus(statusText, statusClass);
    }
    
    updateStatus(text, className = 'text-muted') {
        this.chatStatus.textContent = text;
        this.chatStatus.className = `small mt-1 ${className}`;
    }
    
    updateCharacterCount() {
        const length = this.chatInput.value.length;
        const maxLength = 1000;
        
        if (length > maxLength * 0.8) {
            const remaining = maxLength - length;
            this.updateStatus(`${remaining} characters remaining`, 
                remaining < 50 ? 'text-warning' : 'text-muted');
        } else {
            this.updateChatStatus();
        }
    }
    
    loadMessages() {
        // Clear current messages
        this.chatMessages.innerHTML = '';
        this.messages = [];
        
        // Load appropriate messages based on current phase and role
        const endpoint = `/api/chat/${this.gameCode}/messages?username=${this.username}`;
        
        fetch(endpoint)
            .then(response => response.json())
            .then(messages => {
                messages.forEach(message => this.displayMessage(message));
            })
            .catch(error => {
                console.error('Error loading chat messages:', error);
                this.updateStatus('Error loading messages', 'text-danger');
            });
    }
    
    scrollToBottom() {
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }
    
    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
    
    disconnect() {
        if (this.stompClient && this.isConnected) {
            this.stompClient.disconnect();
            this.isConnected = false;
        }
    }
    
    // System message for game events
    addSystemMessage(message) {
        const systemMessage = {
            senderUsername: 'SYSTEM',
            message: message,
            timestamp: new Date().toLocaleTimeString(),
            chatType: 'SYSTEM'
        };
        this.displayMessage(systemMessage);
    }
}

// Global chat manager instance
let chatManager = null;

// Initialize chat when page loads
function initializeChat(gameCode, username) {
    if (chatManager) {
        chatManager.disconnect();
    }
    
    chatManager = new ChatManager(gameCode, username);
    chatManager.connect();
    
    return chatManager;
}

// Update chat when phase changes
function updateChatPhase(phase, playerRole, isAlive) {
    if (chatManager) {
        chatManager.updatePhase(phase, playerRole, isAlive);
    }
}

// Add system message to chat
function addChatSystemMessage(message) {
    if (chatManager) {
        chatManager.addSystemMessage(message);
    }
} 