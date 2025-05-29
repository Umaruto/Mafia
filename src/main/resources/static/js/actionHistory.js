/**
 * Action History Management
 * Handles display and interaction with game action history
 */

class ActionHistoryManager {
    constructor() {
        this.gameCode = null;
        this.historyContainer = null;
        this.refreshInterval = null;
    }
    
    /**
     * Initialize action history display
     */
    init(gameCode, containerId) {
        this.gameCode = gameCode;
        this.historyContainer = document.getElementById(containerId);
        
        if (!this.historyContainer) {
            console.error('Action history container not found:', containerId);
            return;
        }
        
        this.setupHistoryInterface();
        this.loadActionHistory();
        
        // Auto-refresh every 10 seconds during active game
        this.refreshInterval = setInterval(() => {
            this.loadActionHistory();
        }, 10000);
    }
    
    /**
     * Setup the history interface HTML
     */
    setupHistoryInterface() {
        this.historyContainer.innerHTML = `
            <div class="action-history-panel">
                <div class="history-header">
                    <h4>Game History</h4>
                    <div class="history-controls">
                        <button class="btn btn-sm btn-primary" onclick="actionHistory.refreshHistory()">
                            <i class="fas fa-sync-alt"></i> Refresh
                        </button>
                        <button class="btn btn-sm btn-secondary" onclick="actionHistory.toggleHistoryView()">
                            <i class="fas fa-list"></i> Toggle View
                        </button>
                        <button class="btn btn-sm btn-info" onclick="actionHistory.showStatistics()">
                            <i class="fas fa-chart-bar"></i> Stats
                        </button>
                    </div>
                </div>
                
                <div class="history-filters">
                    <select id="actionTypeFilter" class="form-select form-select-sm">
                        <option value="">All Actions</option>
                        <option value="VOTE">Votes</option>
                        <option value="SKIP_VOTE">Skip Votes</option>
                        <option value="KILL">Kills</option>
                        <option value="SAVE">Saves</option>
                        <option value="INVESTIGATE">Investigations</option>
                        <option value="ELIMINATE">Eliminations</option>
                        <option value="PHASE_TRANSITION">Phase Changes</option>
                    </select>
                    
                    <select id="phaseFilter" class="form-select form-select-sm">
                        <option value="">All Phases</option>
                        <option value="0">Day Phase</option>
                        <option value="1">Night Phase</option>
                    </select>
                </div>
                
                <div class="history-content">
                    <div id="historyList" class="history-list">
                        <div class="loading-spinner">
                            <i class="fas fa-spinner fa-spin"></i> Loading history...
                        </div>
                    </div>
                </div>
                
                <div id="statisticsModal" class="modal fade" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Game Statistics</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body" id="statisticsContent">
                                <!-- Statistics will be loaded here -->
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Add event listeners for filters
        document.getElementById('actionTypeFilter').addEventListener('change', () => {
            this.filterHistory();
        });
        
        document.getElementById('phaseFilter').addEventListener('change', () => {
            this.filterHistory();
        });
    }
    
    /**
     * Load action history from server
     */
    async loadActionHistory() {
        try {
            const response = await fetch(`/api/history/game/${this.gameCode}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const history = await response.json();
            this.displayHistory(history);
        } catch (error) {
            console.error('Error loading action history:', error);
            this.displayError('Failed to load action history');
        }
    }
    
    /**
     * Display action history
     */
    displayHistory(history) {
        const historyList = document.getElementById('historyList');
        
        if (!history || history.length === 0) {
            historyList.innerHTML = '<div class="no-history">No actions recorded yet.</div>';
            return;
        }
        
        const historyHTML = history.map(action => this.formatActionItem(action)).join('');
        historyList.innerHTML = historyHTML;
    }
    
    /**
     * Format individual action item
     */
    formatActionItem(action) {
        const timestamp = new Date(action.timestamp).toLocaleTimeString();
        const actionClass = this.getActionClass(action.actionType);
        const phaseText = action.phase === 'Day' ? 'Day' : 'Night';
        
        let actionDescription = this.getActionDescription(action);
        
        return `
            <div class="history-item ${actionClass}" data-action-type="${action.actionType}" data-phase="${action.phase}">
                <div class="history-item-header">
                    <span class="action-type">${action.actionType}</span>
                    <span class="phase-badge badge bg-${action.phase === 'Day' ? 'warning' : 'dark'}">${phaseText} ${action.day}</span>
                    <span class="timestamp">${timestamp}</span>
                </div>
                <div class="action-description">
                    ${actionDescription}
                </div>
                ${action.result ? `<div class="action-result"><small class="text-muted">${action.result}</small></div>` : ''}
            </div>
        `;
    }
    
    /**
     * Get CSS class for action type
     */
    getActionClass(actionType) {
        const classMap = {
            'VOTE': 'action-vote',
            'SKIP_VOTE': 'action-skip',
            'KILL': 'action-kill',
            'SAVE': 'action-save',
            'INVESTIGATE': 'action-investigate',
            'ELIMINATE': 'action-eliminate',
            'PHASE_TRANSITION': 'action-phase',
            'GAME_START': 'action-start',
            'GAME_END': 'action-end'
        };
        return classMap[actionType] || 'action-default';
    }
    
    /**
     * Get human-readable action description
     */
    getActionDescription(action) {
        const actor = action.actor || 'System';
        const target = action.target || '';
        
        switch (action.actionType) {
            case 'VOTE':
                return `<strong>${actor}</strong> voted for <strong>${target}</strong>`;
            case 'SKIP_VOTE':
                return `<strong>${actor}</strong> skipped voting`;
            case 'KILL':
                return `<strong>${actor}</strong> targeted <strong>${target}</strong> for elimination`;
            case 'SAVE':
                return `<strong>${actor}</strong> protected <strong>${target}</strong>`;
            case 'INVESTIGATE':
                return `<strong>${actor}</strong> investigated <strong>${target}</strong>`;
            case 'ELIMINATE':
                return `<strong>${target}</strong> was eliminated ${action.voteCount ? `with ${action.voteCount} votes` : ''}`;
            case 'PHASE_TRANSITION':
                return action.details || 'Phase transition occurred';
            case 'GAME_START':
                return action.details || 'Game started';
            case 'GAME_END':
                return action.details || 'Game ended';
            default:
                return action.details || `${actor} performed ${action.actionType}`;
        }
    }
    
    /**
     * Filter history based on selected criteria
     */
    filterHistory() {
        const actionTypeFilter = document.getElementById('actionTypeFilter').value;
        const phaseFilter = document.getElementById('phaseFilter').value;
        
        const historyItems = document.querySelectorAll('.history-item');
        
        historyItems.forEach(item => {
            let show = true;
            
            if (actionTypeFilter && item.dataset.actionType !== actionTypeFilter) {
                show = false;
            }
            
            if (phaseFilter && item.dataset.phase !== (phaseFilter === '0' ? 'Day' : 'Night')) {
                show = false;
            }
            
            item.style.display = show ? 'block' : 'none';
        });
    }
    
    /**
     * Refresh history manually
     */
    refreshHistory() {
        this.loadActionHistory();
    }
    
    /**
     * Toggle between detailed and compact view
     */
    toggleHistoryView() {
        const historyList = document.getElementById('historyList');
        historyList.classList.toggle('compact-view');
    }
    
    /**
     * Show game statistics
     */
    async showStatistics() {
        try {
            const response = await fetch(`/api/history/game/${this.gameCode}/statistics`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const stats = await response.json();
            this.displayStatistics(stats);
            
            const modal = new bootstrap.Modal(document.getElementById('statisticsModal'));
            modal.show();
        } catch (error) {
            console.error('Error loading statistics:', error);
            alert('Failed to load game statistics');
        }
    }
    
    /**
     * Display game statistics
     */
    displayStatistics(stats) {
        const content = document.getElementById('statisticsContent');
        
        const actionBreakdown = stats.actionBreakdown || {};
        const breakdownHTML = Object.entries(actionBreakdown)
            .map(([action, count]) => `<li>${action}: ${count}</li>`)
            .join('');
        
        content.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6>General Statistics</h6>
                    <ul class="list-unstyled">
                        <li><strong>Total Actions:</strong> ${stats.totalActions || 0}</li>
                        <li><strong>Success Rate:</strong> ${stats.successRate || 0}%</li>
                        <li><strong>Game Duration:</strong> ${stats.gameDuration || 'N/A'}</li>
                    </ul>
                </div>
                <div class="col-md-6">
                    <h6>Action Breakdown</h6>
                    <ul class="list-unstyled">
                        ${breakdownHTML}
                    </ul>
                </div>
            </div>
            
            ${stats.firstActionTime ? `
                <div class="mt-3">
                    <h6>Timeline</h6>
                    <p><strong>First Action:</strong> ${new Date(stats.firstActionTime).toLocaleString()}</p>
                    <p><strong>Last Action:</strong> ${new Date(stats.lastActionTime).toLocaleString()}</p>
                </div>
            ` : ''}
        `;
    }
    
    /**
     * Display error message
     */
    displayError(message) {
        const historyList = document.getElementById('historyList');
        historyList.innerHTML = `
            <div class="error-message alert alert-danger">
                <i class="fas fa-exclamation-triangle"></i> ${message}
            </div>
        `;
    }
    
    /**
     * Clean up resources
     */
    destroy() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}

// Global instance
const actionHistory = new ActionHistoryManager();

// Export for module use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ActionHistoryManager;
} 