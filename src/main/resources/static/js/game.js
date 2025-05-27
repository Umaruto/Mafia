class GameManager {
    constructor(gameCode, username) {
        this.gameCode = gameCode;
        this.username = username;
        this.gameWs = new GameWebSocket(gameCode, username);
        this.currentPhase = 'DAY';
        this.currentDay = 1;
        this.playerRole = null;
        this.isAlive = true;
        
        this.initializeWebSocket();
        this.initializeEventListeners();
    }

    initializeWebSocket() {
        this.gameWs.connect();
        
        // Game phase changes
        this.gameWs.on('PHASE_CHANGE', (payload) => {
            this.handlePhaseChange(payload);
        });
        
        // Player actions
        this.gameWs.on('PLAYER_KILLED', (payload) => {
            this.handlePlayerKilled(payload);
        });
        
        this.gameWs.on('PLAYER_SAVED', (payload) => {
            this.handlePlayerSaved(payload);
        });
        
        // Private messages (role-specific)
        this.gameWs.onPrivate('INVESTIGATION_RESULT', (payload) => {
            this.handleInvestigationResult(payload);
        });
        
        // Voting
        this.gameWs.on('VOTE_CAST', (payload) => {
            this.handleVoteCast(payload);
        });
    }

    initializeEventListeners() {
        // Vote button
        document.getElementById('voteButton').addEventListener('click', () => {
            const target = document.getElementById('voteTarget').value;
            if (target) {
                this.gameWs.sendAction('VOTE', target);
            }
        });

        // Skip vote button
        document.getElementById('skipVoteButton').addEventListener('click', () => {
            this.gameWs.sendAction('SKIP_VOTE', null);
        });

        // Chat
        document.getElementById('sendMessage').addEventListener('click', () => {
            const message = document.getElementById('chatInput').value;
            if (message) {
                this.gameWs.sendAction('CHAT', message);
                document.getElementById('chatInput').value = '';
            }
        });
    }

    handlePhaseChange(payload) {
        this.currentPhase = payload.phase;
        this.currentDay = payload.day;
        
        // Update UI
        document.getElementById('phase').textContent = this.currentPhase;
        document.getElementById('day').textContent = this.currentDay;
        
        // Show/hide appropriate action panels
        document.getElementById('dayActions').style.display = 
            this.currentPhase === 'DAY' ? 'block' : 'none';
        document.getElementById('nightActions').style.display = 
            this.currentPhase === 'NIGHT' ? 'block' : 'none';
        
        // Update role actions if it's night phase
        if (this.currentPhase === 'NIGHT') {
            this.updateNightActions();
        }
    }

    handlePlayerKilled(payload) {
        const playerElement = document.querySelector(`[data-username="${payload.target}"]`);
        if (playerElement) {
            playerElement.classList.add('dead');
        }
        
        // Add to chat
        this.addChatMessage('System', `${payload.target} was killed during the night!`);
    }

    handlePlayerSaved(payload) {
        // Add to chat (only visible to the doctor)
        this.addChatMessage('System', `You successfully saved ${payload.target}!`);
    }

    handleInvestigationResult(payload) {
        const result = payload.isDetective ? 'is a Detective' : 'is not a Detective';
        this.addChatMessage('System', `Investigation result: ${payload.target} ${result}`);
    }

    handleVoteCast(payload) {
        if (payload.skip) {
            this.addChatMessage('System', `${payload.voter} chose to skip their vote`);
        } else {
            this.addChatMessage('System', `${payload.voter} voted for ${payload.target}`);
        }
        
        // Update voting status
        this.updateVotingStatus();
    }

    updateNightActions() {
        const roleActions = document.querySelector('.role-actions');
        roleActions.innerHTML = ''; // Clear existing actions
        
        switch (this.playerRole) {
            case 'MAFIA':
                this.addMafiaActions(roleActions);
                break;
            case 'DOCTOR':
                this.addDoctorActions(roleActions);
                break;
            case 'DETECTIVE':
                this.addDetectiveActions(roleActions);
                break;
        }
    }

    addMafiaActions(container) {
        const select = document.createElement('select');
        select.className = 'form-select';
        select.innerHTML = '<option value="">Select target to kill</option>';
        
        const button = document.createElement('button');
        button.className = 'btn btn-danger';
        button.textContent = 'Kill';
        button.onclick = () => {
            if (select.value) {
                this.gameWs.sendAction('KILL', select.value);
            }
        };
        
        container.appendChild(select);
        container.appendChild(button);
    }

    addDoctorActions(container) {
        const select = document.createElement('select');
        select.className = 'form-select';
        select.innerHTML = '<option value="">Select player to save</option>';
        
        const button = document.createElement('button');
        button.className = 'btn btn-success';
        button.textContent = 'Save';
        button.onclick = () => {
            if (select.value) {
                this.gameWs.sendAction('SAVE', select.value);
            }
        };
        
        container.appendChild(select);
        container.appendChild(button);
    }

    addDetectiveActions(container) {
        const select = document.createElement('select');
        select.className = 'form-select';
        select.innerHTML = '<option value="">Select player to investigate</option>';
        
        const investigateButton = document.createElement('button');
        investigateButton.className = 'btn btn-info';
        investigateButton.textContent = 'Investigate';
        investigateButton.onclick = () => {
            if (select.value) {
                this.gameWs.sendAction('INVESTIGATE', select.value);
            }
        };

        const killButton = document.createElement('button');
        killButton.className = 'btn btn-danger';
        killButton.textContent = 'Kill';
        killButton.onclick = () => {
            if (select.value) {
                this.gameWs.sendAction('KILL', select.value);
            }
        };
        
        container.appendChild(select);
        container.appendChild(investigateButton);
        container.appendChild(killButton);
    }

    addChatMessage(sender, message) {
        const chatMessages = document.getElementById('chatMessages');
        const messageElement = document.createElement('div');
        messageElement.className = 'chat-message';
        messageElement.innerHTML = `<strong>${sender}:</strong> ${message}`;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    updateVotingStatus() {
        // This would be implemented to show who has voted
        // and update the voting status display
    }
}

// Initialize game when page loads
document.addEventListener('DOMContentLoaded', function() {
    const gameCode = document.getElementById('gameCode').textContent;
    const username = document.getElementById('username').textContent;
    const gameManager = new GameManager(gameCode, username);
});