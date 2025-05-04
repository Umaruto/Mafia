/**
 * Mafia: Web of Lies
 * Main JavaScript for game functionality
 * Enhanced with improved WebSocket handling and UI animations
 */

// Game state management
const gameState = {
    currentScreen: 'home-screen',
    roomCode: null,
    username: null,
    player: null,
    playerRole: null,
    players: [],
    currentPhase: 'LOBBY',
    stompClient: null,
    isHost: false,
    selectedTarget: null,
    votes: {},
    reconnectAttempts: 0,
    maxReconnectAttempts: 5,
    websocketConnected: false,
    notificationSound: new Audio('/sounds/notification.mp3')
};

// DOM elements (cached for performance)
const screens = {
    home: document.getElementById('home-screen'),
    joinRoom: document.getElementById('join-room-screen'),
    createRoom: document.getElementById('create-room-screen'),
    lobby: document.getElementById('lobby-screen'),
    game: document.getElementById('game-screen'),
    gameOver: document.getElementById('game-over-screen')
};

// UI components
const ui = {
    roomCodeDisplay: document.getElementById('room-code-display'),
    playersList: document.getElementById('players-list'),
    playersGrid: document.getElementById('players-grid'),
    startGameBtn: document.getElementById('start-game-btn'),
    phaseIndicator: document.getElementById('phase-indicator'),
    gameStatus: document.getElementById('game-status'),
    roleDisplay: document.getElementById('role-display'),
    gameLog: document.getElementById('game-log'),
    voteSelector: document.getElementById('vote-selector'),
    roleSpecificAction: document.getElementById('role-specific-action'),
    dayPhaseUI: document.getElementById('day-phase-ui'),
    nightPhaseUI: document.getElementById('night-phase-ui'),
    winnerDisplay: document.getElementById('winner-display'),
    gameOverMessage: document.getElementById('game-over-message'),
    connectionStatus: document.getElementById('connection-status')
};

// Input validation rules
const validationRules = {
    username: {
        minLength: 3,
        maxLength: 15,
        pattern: /^[a-zA-Z0-9_-]+$/,
        message: 'Username must be 3-15 characters and contain only letters, numbers, underscores and hyphens.'
    },
    roomCode: {
        length: 6,
        pattern: /^[A-Z0-9]+$/,
        message: 'Room code must be 6 characters and contain only uppercase letters and numbers.'
    }
};

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    showScreen('home-screen');
    
    // Apply fade-in animation to home screen elements
    const homeElements = document.querySelectorAll('#home-screen h1, #home-screen p, #home-screen button');
    homeElements.forEach((el, index) => {
        setTimeout(() => {
            el.classList.add('animate__animated', 'animate__fadeInUp');
        }, index * 200);
    });
    
    // Create a loading notification system
    createLoadingNotification();
    
    logMessage('Game initialized successfully');
});

// Create loading notification
function createLoadingNotification() {
    const loadingEl = document.createElement('div');
    loadingEl.className = 'loading-notification d-none';
    loadingEl.innerHTML = `
        <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
        </div>
        <span class="loading-text ms-2">Processing...</span>
    `;
    document.body.appendChild(loadingEl);
}

// Show loading notification
function showLoading(message = 'Processing...') {
    const loadingEl = document.querySelector('.loading-notification');
    if (loadingEl) {
        loadingEl.querySelector('.loading-text').textContent = message;
        loadingEl.classList.remove('d-none');
    }
}

// Hide loading notification
function hideLoading() {
    const loadingEl = document.querySelector('.loading-notification');
    if (loadingEl) {
        loadingEl.classList.add('d-none');
    }
}

// Set up event listeners for all interactive elements
function setupEventListeners() {
    // Navigation buttons
    document.getElementById('create-room-btn').addEventListener('click', () => showScreen('create-room-screen'));
    document.getElementById('join-room-btn').addEventListener('click', () => showScreen('join-room-screen'));
    document.querySelectorAll('.back-btn').forEach(btn => {
        btn.addEventListener('click', () => showScreen('home-screen'));
    });
    
    // Game actions
    document.getElementById('create-game-btn').addEventListener('click', handleCreateGame);
    document.getElementById('join-game-btn').addEventListener('click', handleJoinGame);
    document.getElementById('start-game-btn').addEventListener('click', handleStartGame);
    document.getElementById('leave-lobby-btn').addEventListener('click', handleLeaveLobby);
    document.getElementById('submit-vote-btn').addEventListener('click', handleSubmitVote);
    document.getElementById('back-to-home-btn').addEventListener('click', handleBackToHome);
    
    // Input validation for usernames and room codes
    document.getElementById('username-input').addEventListener('input', validateUsernameInput);
    document.getElementById('creator-name-input').addEventListener('input', validateUsernameInput);
    document.getElementById('room-code-input').addEventListener('input', validateRoomCodeInput);
    
    // Add Enter key support for forms
    document.getElementById('username-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            document.getElementById('room-code-input').focus();
        }
    });
    
    document.getElementById('room-code-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            document.getElementById('join-game-btn').click();
        }
    });
    
    document.getElementById('creator-name-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            document.getElementById('create-game-btn').click();
        }
    });
}

// Input validation functions
function validateUsernameInput(e) {
    const input = e.target;
    const value = input.value.trim();
    const feedbackElement = input.nextElementSibling || createFeedbackElement(input);
    
    if (value.length < validationRules.username.minLength || 
        value.length > validationRules.username.maxLength || 
        !validationRules.username.pattern.test(value)) {
        
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        feedbackElement.textContent = validationRules.username.message;
        feedbackElement.className = 'invalid-feedback';
        return false;
    } else {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        feedbackElement.textContent = 'Looks good!';
        feedbackElement.className = 'valid-feedback';
        return true;
    }
}

function validateRoomCodeInput(e) {
    const input = e.target;
    const value = input.value.trim().toUpperCase();
    input.value = value; // Force uppercase
    
    const feedbackElement = input.nextElementSibling || createFeedbackElement(input);
    
    if (value.length !== validationRules.roomCode.length || 
        !validationRules.roomCode.pattern.test(value)) {
        
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        feedbackElement.textContent = validationRules.roomCode.message;
        feedbackElement.className = 'invalid-feedback';
        return false;
    } else {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        feedbackElement.textContent = 'Valid room code format';
        feedbackElement.className = 'valid-feedback';
        return true;
    }
}

function createFeedbackElement(input) {
    const feedbackEl = document.createElement('div');
    feedbackEl.className = 'invalid-feedback';
    input.parentNode.appendChild(feedbackEl);
    return feedbackEl;
}

// Screen navigation with animation
function showScreen(screenId) {
    // Fade out current screen
    const currentScreen = document.getElementById(gameState.currentScreen);
    if (currentScreen) {
        currentScreen.classList.add('fade-out');
        
        setTimeout(() => {
            // Hide all screens
            for (const key in screens) {
                if (screens[key]) {
                    screens[key].classList.add('d-none');
                    screens[key].classList.remove('fade-in', 'fade-out');
                }
            }
            
            // Show the requested screen with fade-in animation
            const targetScreen = document.getElementById(screenId);
            if (targetScreen) {
                targetScreen.classList.remove('d-none');
                targetScreen.classList.add('fade-in');
                gameState.currentScreen = screenId;
                
                // Add theme based on current phase if it's the game screen
                if (screenId === 'game-screen') {
                    applyPhaseTheme();
                }
            }
        }, 300); // Short delay to allow fade out animation
    } else {
        // First load, no animation needed
        const targetScreen = document.getElementById(screenId);
        if (targetScreen) {
            targetScreen.classList.remove('d-none');
            gameState.currentScreen = screenId;
        }
    }
}

// API Calls to Backend
async function createRoom(username) {
    try {
        const response = await fetch('/api/room/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        gameState.roomCode = data.roomCode;
        gameState.isHost = true;
        
        // Join the room with the creator's name
        return joinRoom(username, data.roomCode);
    } catch (error) {
        logError('Failed to create room: ' + error.message);
        return null;
    }
}

async function joinRoom(username, roomCode) {
    try {
        const response = await fetch('/api/room/join', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                roomCode: roomCode
            })
        });
        
        if (!response.ok) {
            throw new Error('Failed to join room: ' + response.statusText);
        }
        
        const player = await response.json();
        gameState.player = player;
        gameState.username = username;
        gameState.roomCode = roomCode;
        
        return player;
    } catch (error) {
        logError('Failed to join room: ' + error.message);
        return null;
    }
}

async function startGame(roomCode) {
    try {
        const response = await fetch(`/api/room/${roomCode}/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        return data;
    } catch (error) {
        logError('Failed to start game: ' + error.message);
        return null;
    }
}

async function getPlayers(roomCode) {
    try {
        const response = await fetch(`/api/room/${roomCode}/players`);
        const players = await response.json();
        gameState.players = players;
        return players;
    } catch (error) {
        logError('Failed to get players: ' + error.message);
        return [];
    }
}

async function toggleGamePhase(roomCode) {
    try {
        const response = await fetch(`/api/room/${roomCode}/toggle-phase`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        return data.phase;
    } catch (error) {
        logError('Failed to toggle game phase: ' + error.message);
        return null;
    }
}

// WebSocket Connection and Handlers
function connectWebSocket() {
    if (gameState.websocketConnected) {
        logMessage('Already connected to WebSocket');
        return;
    }
    
    showLoading('Connecting to game server...');
    updateConnectionStatus('connecting');
    
    const socket = new SockJS('/mafia-websocket');
    gameState.stompClient = Stomp.over(socket);
    
    // Disable console logs from STOMP
    gameState.stompClient.debug = null;
    
    gameState.stompClient.connect({}, 
        // Success callback
        frame => {
            logMessage('Connected to game server');
            gameState.websocketConnected = true;
            gameState.reconnectAttempts = 0;
            updateConnectionStatus('connected');
            hideLoading();
            
            // Subscribe to game updates
            subscribeToPLayerUpdates();
            subscribeToPhaseChanges();
            subscribeToVotes();
            subscribeToGameOver();
            
            // Role-specific subscriptions
            if (gameState.playerRole === 'DETECTIVE') {
                subscribeToInvestigationResults();
            }
        }, 
        // Error callback
        error => {
            logError('WebSocket connection error: ' + error);
            gameState.websocketConnected = false;
            updateConnectionStatus('disconnected');
            hideLoading();
            
            // Attempt reconnection with exponential backoff
            if (gameState.reconnectAttempts < gameState.maxReconnectAttempts) {
                const delay = Math.min(5000 * Math.pow(1.5, gameState.reconnectAttempts), 30000);
                gameState.reconnectAttempts++;
                
                logMessage(`Attempting to reconnect in ${Math.round(delay/1000)} seconds... (Attempt ${gameState.reconnectAttempts}/${gameState.maxReconnectAttempts})`);
                
                setTimeout(connectWebSocket, delay);
            } else {
                showErrorModal(
                    'Connection Lost', 
                    'Unable to connect to the game server after multiple attempts. Please check your internet connection and try again.',
                    () => {
                        window.location.reload();
                    }
                );
            }
        }
    );
}

function updateConnectionStatus(status) {
    if (!ui.connectionStatus) return;
    
    ui.connectionStatus.className = 'connection-status';
    
    switch(status) {
        case 'connected':
            ui.connectionStatus.innerHTML = '<i class="bi bi-wifi text-success"></i> Connected';
            ui.connectionStatus.classList.add('text-success');
            break;
        case 'disconnected':
            ui.connectionStatus.innerHTML = '<i class="bi bi-wifi-off text-danger"></i> Disconnected';
            ui.connectionStatus.classList.add('text-danger');
            break;
        case 'connecting':
            ui.connectionStatus.innerHTML = '<i class="bi bi-wifi text-warning"></i> Connecting...';
            ui.connectionStatus.classList.add('text-warning');
            break;
    }
}

function disconnectWebSocket() {
    if (gameState.stompClient && gameState.websocketConnected) {
        gameState.stompClient.disconnect();
        gameState.websocketConnected = false;
        updateConnectionStatus('disconnected');
        logMessage('Disconnected from game server');
    }
}

function subscribeToPLayerUpdates() {
    if (!gameState.stompClient || !gameState.websocketConnected) return;
    
    gameState.stompClient.subscribe(`/game/${gameState.roomCode}/players`, message => {
        try {
            const players = JSON.parse(message.body);
            playNotificationSound();
            updatePlayersList(players);
            updatePlayersGrid(players);
            
            // Update current player's status
            const currentPlayer = players.find(p => p.username === gameState.username);
            if (currentPlayer) {
                gameState.player = currentPlayer;
                
                // Update UI if player has been eliminated
                if (!currentPlayer.alive && gameState.currentScreen === 'game-screen') {
                    showPlayerEliminationMessage();
                }
            }
        } catch (error) {
            logError('Error processing player updates: ' + error.message);
        }
    });
}

function subscribeToPhaseChanges() {
    if (!gameState.stompClient || !gameState.websocketConnected) return;
    
    gameState.stompClient.subscribe(`/game/${gameState.roomCode}/phase`, message => {
        try {
            const phase = message.body;
            playNotificationSound();
            animatePhaseTransition(gameState.currentPhase, phase);
            updateGamePhase(phase);
        } catch (error) {
            logError('Error processing phase change: ' + error.message);
        }
    });
}

function subscribeToVotes() {
    if (!gameState.stompClient || !gameState.websocketConnected) return;
    
    gameState.stompClient.subscribe(`/game/${gameState.roomCode}/votes`, message => {
        try {
            const voteData = JSON.parse(message.body);
            updateVoteDisplay(voteData);
            logMessage(`${voteData.voter} voted against ${voteData.target}`);
        } catch (error) {
            logError('Error processing vote data: ' + error.message);
        }
    });
}

function subscribeToGameOver() {
    if (!gameState.stompClient || !gameState.websocketConnected) return;
    
    gameState.stompClient.subscribe(`/game/${gameState.roomCode}/game-over`, message => {
        try {
            const gameOverData = JSON.parse(message.body);
            handleGameOver(gameOverData.winner);
        } catch (error) {
            logError('Error processing game over: ' + error.message);
        }
    });
}

function subscribeToInvestigationResults() {
    if (!gameState.stompClient || !gameState.websocketConnected) return;
    
    gameState.stompClient.subscribe(`/user/game/${gameState.roomCode}/check-result`, message => {
        try {
            const result = message.body;
            displayInvestigationResult(result);
        } catch (error) {
            logError('Error processing investigation result: ' + error.message);
        }
    });
}

// Animate phase transition (day/night)
function animatePhaseTransition(oldPhase, newPhase) {
    if (oldPhase === newPhase) return;
    
    const gameContainer = document.getElementById('app-container');
    if (!gameContainer) return;
    
    // Add transition class
    gameContainer.classList.add('phase-transition');
    
    // Apply appropriate theme class
    if (newPhase === 'NIGHT') {
        gameContainer.classList.remove('day-theme');
        
        setTimeout(() => {
            gameContainer.classList.add('night-theme');
            
            // Special night transition effects
            document.querySelectorAll('.player-card:not(.dead)').forEach(card => {
                card.style.transform = 'scale(0.97)';
                setTimeout(() => {
                    card.style.transform = '';
                }, 1000);
            });
        }, 500);
    } else if (newPhase === 'DAY') {
        gameContainer.classList.remove('night-theme');
        
        setTimeout(() => {
            gameContainer.classList.add('day-theme');
            
            // Special day transition effects
            document.querySelectorAll('.player-card:not(.dead)').forEach(card => {
                card.style.transform = 'scale(1.03)';
                setTimeout(() => {
                    card.style.transform = '';
                }, 1000);
            });
        }, 500);
    }
    
    // Remove transition class after animation completes
    setTimeout(() => {
        gameContainer.classList.remove('phase-transition');
    }, 2000);
}

// Apply appropriate theme based on current phase
function applyPhaseTheme() {
    const gameContainer = document.getElementById('app-container');
    if (!gameContainer) return;
    
    gameContainer.classList.remove('day-theme', 'night-theme');
    
    if (gameState.currentPhase === 'NIGHT') {
        gameContainer.classList.add('night-theme');
    } else {
        gameContainer.classList.add('day-theme');
    }
}

// Update vote display
function updateVoteDisplay(voteData) {
    const { voter, target } = voteData;
    
    // Store vote in game state
    gameState.votes[voter] = target;
    
    // Update vote counters for each player
    updateVoteCounters();
    
    // Highlight the player card that received a vote with a pulse animation
    const targetCard = document.querySelector(`.player-card[data-username="${target}"]`);
    if (targetCard) {
        targetCard.classList.add('pulse');
        setTimeout(() => {
            targetCard.classList.remove('pulse');
        }, 1000);
    }
}

// Update vote counters on player cards
function updateVoteCounters() {
    // Clear all existing vote counters
    document.querySelectorAll('.vote-counter').forEach(counter => counter.remove());
    
    // Count votes for each player
    const voteCounts = {};
    Object.values(gameState.votes).forEach(target => {
        voteCounts[target] = (voteCounts[target] || 0) + 1;
    });
    
    // Add vote counters to player cards
    for (const [target, count] of Object.entries(voteCounts)) {
        if (count > 0) {
            const targetCard = document.querySelector(`.player-card[data-username="${target}"]`);
            if (targetCard) {
                // Check if counter already exists
                let counter = targetCard.querySelector('.vote-counter');
                if (!counter) {
                    counter = document.createElement('div');
                    counter.className = 'vote-counter';
                    targetCard.appendChild(counter);
                }
                counter.textContent = count;
            }
        }
    }
}

// Show player elimination message
function showPlayerEliminationMessage() {
    // Only show once
    if (document.getElementById('elimination-message')) return;
    
    const messageEl = document.createElement('div');
    messageEl.id = 'elimination-message';
    messageEl.className = 'alert alert-danger fade-in';
    messageEl.innerHTML = `
        <h4><i class="bi bi-person-x-fill"></i> You have been eliminated!</h4>
        <p>You can continue to watch the game, but you can no longer participate in the actions.</p>
    `;
    
    // Insert at the top of the game screen
    const gameScreen = document.getElementById('game-screen');
    gameScreen.insertBefore(messageEl, gameScreen.firstChild);
}

// Play notification sound
function playNotificationSound() {
    if (gameState.notificationSound) {
        gameState.notificationSound.play().catch(e => {
            // Sound may fail to play if user hasn't interacted with the page
            console.log('Unable to play notification sound:', e);
        });
    }
}

// Show error modal
function showErrorModal(title, message, onClose) {
    // Create modal if it doesn't exist
    let modal = document.getElementById('error-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'error-modal';
        modal.className = 'modal fade';
        modal.setAttribute('tabindex', '-1');
        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title"></h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body"></div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        
        // Add event listener for modal close
        modal.addEventListener('hidden.bs.modal', () => {
            if (typeof onClose === 'function') {
                onClose();
            }
        });
    }
    
    // Update modal content
    modal.querySelector('.modal-title').textContent = title;
    modal.querySelector('.modal-body').textContent = message;
    
    // Show modal
    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

// Send WebSocket Messages (Game Actions)
function sendVote(target) {
    if (gameState.stompClient && gameState.currentPhase === 'DAY') {
        gameState.stompClient.send(`/app/room/${gameState.roomCode}/vote`, {}, JSON.stringify({
            from: gameState.username,
            target: target
        }));
        logMessage(`You voted against ${target}`);
    }
}

function sendMafiaKill(target) {
    if (gameState.stompClient && gameState.currentPhase === 'NIGHT' && gameState.playerRole === 'MAFIA') {
        gameState.stompClient.send(`/app/room/${gameState.roomCode}/night/kill`, {}, JSON.stringify({
            from: gameState.username,
            target: target
        }));
        logMessage(`You selected to eliminate ${target}`);
    }
}

function sendDoctorSave(target) {
    if (gameState.stompClient && gameState.currentPhase === 'NIGHT' && gameState.playerRole === 'DOCTOR') {
        gameState.stompClient.send(`/app/room/${gameState.roomCode}/night/save`, {}, JSON.stringify({
            from: gameState.username,
            target: target
        }));
        logMessage(`You chose to protect ${target}`);
    }
}

function sendDetectiveCheck(target) {
    if (gameState.stompClient && gameState.currentPhase === 'NIGHT' && gameState.playerRole === 'DETECTIVE') {
        gameState.stompClient.send(`/app/room/${gameState.roomCode}/night/check`, {}, JSON.stringify({
            from: gameState.username,
            target: target
        }));
        logMessage(`You are investigating ${target}`);
    }
}

// UI Update Functions
function updatePlayersList(players) {
    gameState.players = players;
    
    if (ui.playersList) {
        ui.playersList.innerHTML = '';
        
        players.forEach(player => {
            const listItem = document.createElement('li');
            listItem.className = 'list-group-item d-flex justify-content-between align-items-center';
            listItem.textContent = player.username;
            
            ui.playersList.appendChild(listItem);
        });
        
        // Enable start button if enough players
        if (ui.startGameBtn && gameState.isHost) {
            ui.startGameBtn.disabled = players.length < 4;
        }
    }
}

function updatePlayersGrid(players) {
    if (ui.playersGrid) {
        ui.playersGrid.innerHTML = '';
        
        players.forEach(player => {
            const playerCard = document.createElement('div');
            playerCard.className = `col-md-4 mb-3 ${player.alive ? '' : 'player-dead'}`;
            playerCard.innerHTML = `
                <div class="card player-card ${!player.alive ? 'dead' : ''}" data-username="${player.username}">
                    <div class="card-body text-center">
                        <h5 class="card-title">${player.username}</h5>
                        <p class="card-text">${player.alive ? 'Alive' : 'Dead'}</p>
                    </div>
                </div>
            `;
            
            // Add click handler for night actions or voting
            playerCard.querySelector('.player-card').addEventListener('click', () => {
                selectTarget(player.username);
            });
            
            ui.playersGrid.appendChild(playerCard);
        });
    }
}

function updateGamePhase(phase) {
    gameState.currentPhase = phase;
    
    if (ui.phaseIndicator) {
        ui.phaseIndicator.textContent = phase;
        ui.phaseIndicator.className = phase === 'NIGHT' ? 'badge bg-dark phase-night' : 'badge bg-warning phase-day';
    }
    
    // Update game status message
    if (ui.gameStatus) {
        ui.gameStatus.textContent = phase === 'NIGHT' ? 
            'Night has fallen. Special roles perform their actions.' : 
            'It\'s daytime. Discuss and vote to eliminate a suspected Mafia member.';
    }
    
    // Show/hide appropriate phase UI
    if (ui.dayPhaseUI && ui.nightPhaseUI) {
        ui.dayPhaseUI.classList.toggle('d-none', phase !== 'DAY');
        ui.nightPhaseUI.classList.toggle('d-none', phase !== 'NIGHT');
    }
    
    // Update role-specific action UI
    updateRoleActionUI();
    
    logMessage(`Game phase changed to ${phase}`);
}

function updateRoleDisplay(role) {
    gameState.playerRole = role;
    
    if (ui.roleDisplay) {
        let roleIcon = '';
        let roleColor = '';
        let roleDescription = '';
        
        switch (role) {
            case 'MAFIA':
                roleIcon = '👤';
                roleColor = 'role-mafia';
                roleDescription = 'Eliminate one player each night. Pretend to be a civilian during the day.';
                break;
            case 'DOCTOR':
                roleIcon = '👨‍⚕️';
                roleColor = 'role-doctor';
                roleDescription = 'Save one player from elimination each night.';
                break;
            case 'DETECTIVE':
                roleIcon = '🕵️';
                roleDescription = 'Investigate one player each night to determine if they are Mafia.';
                break;
            case 'CIVILIAN':
                roleIcon = '🧑';
                roleColor = 'role-civilian';
                roleDescription = 'Work with other players to identify and eliminate the Mafia.';
                break;
            default:
                roleIcon = '❓';
                roleColor = '';
                roleDescription = 'Unknown role';
        }
        
        ui.roleDisplay.innerHTML = `
            <div class="role-icon ${roleColor}">${roleIcon}</div>
            <h4>${role}</h4>
            <p>${roleDescription}</p>
        `;
    }
    
    logMessage(`Your assigned role is: ${role}`);
}

function updateRoleActionUI() {
    if (!ui.roleSpecificAction) return;
    
    // Only show actions during night phase and if player is alive
    const isPlayerAlive = gameState.player && gameState.player.alive;
    
    if (gameState.currentPhase === 'NIGHT' && isPlayerAlive) {
        let actionHtml = '';
        
        switch (gameState.playerRole) {
            case 'MAFIA':
                actionHtml = `
                    <p>Select a player to eliminate:</p>
                    <select class="form-select mb-3" id="mafia-target-selector">
                        <option value="">Select target...</option>
                        ${createPlayerOptions(true)}
                    </select>
                    <button id="mafia-kill-btn" class="btn btn-danger">Confirm Target</button>
                `;
                break;
            case 'DOCTOR':
                actionHtml = `
                    <p>Select a player to protect tonight:</p>
                    <select class="form-select mb-3" id="doctor-target-selector">
                        <option value="">Select player to save...</option>
                        ${createPlayerOptions(false)}
                    </select>
                    <button id="doctor-save-btn" class="btn btn-success">Confirm Protection</button>
                `;
                break;
            case 'DETECTIVE':
                actionHtml = `
                    <p>Select a player to investigate:</p>
                    <select class="form-select mb-3" id="detective-target-selector">
                        <option value="">Select player to investigate...</option>
                        ${createPlayerOptions(true)}
                    </select>
                    <button id="detective-check-btn" class="btn btn-primary">Confirm Investigation</button>
                    <div id="investigation-result" class="mt-3"></div>
                `;
                break;
            case 'CIVILIAN':
                actionHtml = `
                    <p>You are a civilian. Wait for the night to pass.</p>
                    <div class="alert alert-secondary">
                        The special roles are performing their actions. The day phase will begin soon.
                    </div>
                `;
                break;
            default:
                actionHtml = `<p>Unknown role: ${gameState.playerRole}</p>`;
        }
        
        ui.roleSpecificAction.innerHTML = actionHtml;
        
        // Add event listeners for the new buttons
        if (gameState.playerRole === 'MAFIA') {
            document.getElementById('mafia-kill-btn').addEventListener('click', () => {
                const target = document.getElementById('mafia-target-selector').value;
                if (target) sendMafiaKill(target);
            });
        } else if (gameState.playerRole === 'DOCTOR') {
            document.getElementById('doctor-save-btn').addEventListener('click', () => {
                const target = document.getElementById('doctor-target-selector').value;
                if (target) sendDoctorSave(target);
            });
        } else if (gameState.playerRole === 'DETECTIVE') {
            document.getElementById('detective-check-btn').addEventListener('click', () => {
                const target = document.getElementById('detective-target-selector').value;
                if (target) sendDetectiveCheck(target);
            });
        }
    } else if (gameState.currentPhase === 'DAY') {
        ui.roleSpecificAction.innerHTML = '';
    } else if (!isPlayerAlive) {
        ui.roleSpecificAction.innerHTML = `
            <div class="alert alert-danger">
                You have been eliminated from the game. You can still watch how it unfolds.
            </div>
        `;
    }
}

function createPlayerOptions(excludeSelf) {
    return gameState.players
        .filter(p => p.alive && (!excludeSelf || p.username !== gameState.username))
        .map(p => `<option value="${p.username}">${p.username}</option>`)
        .join('');
}

function selectTarget(username) {
    gameState.selectedTarget = username;
    
    // Update UI to show selected player
    document.querySelectorAll('.player-card').forEach(card => {
        card.classList.remove('selected');
    });
    
    const selectedCard = document.querySelector(`.player-card[data-username="${username}"]`);
    if (selectedCard) {
        selectedCard.classList.add('selected');
    }
    
    // Update vote selector
    if (ui.voteSelector) {
        ui.voteSelector.value = username;
    }
}

function displayInvestigationResult(result) {
    const resultDiv = document.getElementById('investigation-result');
    if (resultDiv) {
        resultDiv.innerHTML = `
            <div class="alert ${result === 'MAFIA' ? 'alert-danger' : 'alert-success'}">
                <strong>Investigation Result:</strong> The player is ${result === 'MAFIA' ? 'a member of the Mafia!' : 'not a member of the Mafia.'}
            </div>
        `;
    }
    
    logMessage(`Investigation complete: ${result}`);
}

// Event Handlers
function handleCreateGame() {
    const username = document.getElementById('creator-name-input').value.trim();
    
    if (!username) {
        alert('Please enter your name');
        return;
    }
    
    createRoom(username).then(player => {
        if (player) {
            // Update UI with room code
            ui.roomCodeDisplay.textContent = gameState.roomCode;
            
            // Connect WebSocket
            connectWebSocket();
            
            // Show lobby
            showScreen('lobby-screen');
            
            // Fetch initial player list
            getPlayers(gameState.roomCode).then(players => {
                updatePlayersList(players);
            });
        }
    });
}

function handleJoinGame() {
    const username = document.getElementById('username-input').value.trim();
    const roomCode = document.getElementById('room-code-input').value.trim();
    
    if (!username || !roomCode) {
        alert('Please enter your name and room code');
        return;
    }
    
    joinRoom(username, roomCode).then(player => {
        if (player) {
            // Update UI with room code
            ui.roomCodeDisplay.textContent = gameState.roomCode;
            
            // Connect WebSocket
            connectWebSocket();
            
            // Show lobby
            showScreen('lobby-screen');
            
            // Fetch initial player list
            getPlayers(gameState.roomCode).then(players => {
                updatePlayersList(players);
            });
        }
    });
}

function handleStartGame() {
    if (gameState.players.length < 4) {
        alert('At least 4 players are required to start the game');
        return;
    }
    
    startGame(gameState.roomCode).then(result => {
        if (result && result.status === 'success') {
            showScreen('game-screen');
            updateGamePhase(result.phase);
        } else if (result && result.status === 'error') {
            alert(result.message);
        }
    });
}

function handleLeaveLobby() {
    // Disconnect WebSocket
    if (gameState.stompClient) {
        gameState.stompClient.disconnect();
    }
    
    // Reset game state
    resetGameState();
    
    // Go back to home screen
    showScreen('home-screen');
}

function handleSubmitVote() {
    if (gameState.currentPhase !== 'DAY') {
        alert('Voting is only allowed during the day phase');
        return;
    }
    
    const target = ui.voteSelector.value;
    if (!target) {
        alert('Please select a player to vote against');
        return;
    }
    
    sendVote(target);
}

function handleGameOver(winner) {
    // Update UI
    ui.winnerDisplay.textContent = `Winner: ${winner}`;
    
    let message = '';
    if (winner === 'TOWN') {
        message = 'The Town has successfully identified and eliminated all Mafia members!';
    } else if (winner === 'MAFIA') {
        message = 'The Mafia has taken control of the town! There aren\'t enough townspeople left to resist.';
    }
    
    ui.gameOverMessage.textContent = message;
    
    // Show game over screen
    showScreen('game-over-screen');
    
    // Disconnect WebSocket
    if (gameState.stompClient) {
        gameState.stompClient.disconnect();
    }
    
    logMessage(`Game Over! Winner: ${winner}`);
}

function handleBackToHome() {
    // Reset game state
    resetGameState();
    
    // Go back to home screen
    showScreen('home-screen');
}

// Utility Functions
function resetGameState() {
    gameState.roomCode = null;
    gameState.username = null;
    gameState.player = null;
    gameState.playerRole = null;
    gameState.players = [];
    gameState.currentPhase = 'LOBBY';
    gameState.isHost = false;
    gameState.selectedTarget = null;
    
    // Clear UI elements
    ui.playersList.innerHTML = '';
    ui.playersGrid.innerHTML = '';
    ui.gameLog.innerHTML = '';
}

function logMessage(message) {
    if (ui.gameLog) {
        const now = new Date();
        const time = now.toLocaleTimeString();
        
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        logEntry.innerHTML = `
            <span class="log-time">[${time}]</span> ${message}
        `;
        
        ui.gameLog.appendChild(logEntry);
        ui.gameLog.scrollTop = ui.gameLog.scrollHeight;
    }
    
    console.log(message);
}

function logError(message) {
    logMessage(`ERROR: ${message}`);
    console.error(message);
}
