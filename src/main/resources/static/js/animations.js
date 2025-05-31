/**
 * Mafia Game Animation Manager
 * Handles dynamic animations, transitions, and interactive effects
 */

class AnimationManager {
    constructor() {
        this.animationQueue = [];
        this.isAnimating = false;
        this.currentPhase = null;
        this.phaseTransitionDuration = 1200;
        
        this.init();
    }
    
    init() {
        // Initialize animation observers
        this.initIntersectionObserver();
        
        // Bind phase change events
        this.bindPhaseChangeEvents();
        
        // Initialize existing elements
        this.animateExistingElements();
        
        console.log('Animation Manager initialized');
    }
    
    /**
     * Initialize intersection observer for scroll animations
     */
    initIntersectionObserver() {
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };
        
        this.scrollObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    this.animateOnScroll(entry.target);
                }
            });
        }, observerOptions);
        
        // Observe elements that should animate on scroll
        document.querySelectorAll('.player-item, .action-history-section, .role-info').forEach(el => {
            this.scrollObserver.observe(el);
        });
    }
    
    /**
     * Animate element when it comes into view
     */
    animateOnScroll(element) {
        element.classList.add('animate-fade-in');
        this.scrollObserver.unobserve(element);
    }
    
    /**
     * Bind phase change event listeners
     */
    bindPhaseChangeEvents() {
        // Listen for phase changes via custom events
        document.addEventListener('phaseChange', (event) => {
            this.handlePhaseTransition(event.detail.phase, event.detail.day);
        });
        
        // Listen for player state changes
        document.addEventListener('playerStateChange', (event) => {
            this.handlePlayerStateChange(event.detail);
        });
        
        // Listen for voting events
        document.addEventListener('voteEvent', (event) => {
            this.handleVoteAnimation(event.detail);
        });
        
        // Listen for action events
        document.addEventListener('actionEvent', (event) => {
            this.handleActionAnimation(event.detail);
        });
        
        // Listen for notification events
        document.addEventListener('showNotification', (event) => {
            this.handleNotificationAnimation(event.detail);
        });
    }
    
    /**
     * Animate existing elements on page load
     */
    animateExistingElements() {
        setTimeout(() => {
            // Animate player list
            const playerItems = document.querySelectorAll('.player-item');
            playerItems.forEach((item, index) => {
                setTimeout(() => {
                    item.classList.add('animate-slide-in-left');
                }, index * 100);
            });
            
            // Animate role info
            const roleInfo = document.querySelector('.role-info');
            if (roleInfo) {
                setTimeout(() => {
                    roleInfo.classList.add('animate-flip-in');
                }, 500);
            }
            
            // Animate action panels
            const actionPanels = document.querySelectorAll('.game-actions > div');
            actionPanels.forEach((panel, index) => {
                setTimeout(() => {
                    panel.classList.add('animate-slide-in-right');
                }, 300 + index * 150);
            });
        }, 200);
    }
    
    /**
     * Handle phase transition animations
     */
    async handlePhaseTransition(newPhase, day) {
        if (this.currentPhase === newPhase) return;
        
        const gameContent = document.getElementById('gameContent');
        const phaseTitle = document.getElementById('phaseTitle');
        const gameHeader = document.querySelector('.game-header');
        
        // Add transition class
        gameContent.classList.add('phase-transition', 'transitioning');
        
        // Update phase colors and animations
        if (newPhase === 'DAY') {
            gameHeader.classList.remove('night-phase');
            gameHeader.classList.add('day-phase', 'day-phase-enter');
            
            // Show day phase notification
            this.showPhaseNotification('☀️ Day Phase', `Day ${day} has begun. Discuss and vote!`, 'info');
            
        } else if (newPhase === 'NIGHT') {
            gameHeader.classList.remove('day-phase');
            gameHeader.classList.add('night-phase', 'night-phase-enter');
            
            // Show night phase notification
            this.showPhaseNotification('🌙 Night Phase', 'The town sleeps. Roles perform their actions.', 'warning');
        }
        
        // Animate phase title change
        if (phaseTitle) {
            phaseTitle.classList.add('animate-flip-in');
            setTimeout(() => {
                phaseTitle.classList.remove('animate-flip-in');
            }, 800);
        }
        
        // Remove transition classes after animation
        setTimeout(() => {
            gameContent.classList.remove('phase-transition', 'transitioning');
            gameHeader.classList.remove('day-phase-enter', 'night-phase-enter');
        }, this.phaseTransitionDuration);
        
        this.currentPhase = newPhase;
        
        // Play phase transition sound effect
        this.playTransitionSound(newPhase);
    }
    
    /**
     * Handle player state change animations
     */
    handlePlayerStateChange(data) {
        const { playerId, newState, previousState } = data;
        const playerElement = document.querySelector(`[data-player-id="${playerId}"]`);
        
        if (!playerElement) return;
        
        // Remove old state classes
        playerElement.classList.remove('player-alive', 'player-dead', 'player-eliminated', 'player-saved', 'player-targeted');
        
        // Add new state class with animation
        switch (newState) {
            case 'dead':
                playerElement.classList.add('player-dead');
                this.showFloatingText(playerElement, '💀', 'error');
                break;
                
            case 'eliminated':
                playerElement.classList.add('player-eliminated');
                this.showFloatingText(playerElement, '⚰️', 'warning');
                break;
                
            case 'saved':
                playerElement.classList.add('player-saved');
                this.showFloatingText(playerElement, '🛡️', 'success');
                break;
                
            case 'targeted':
                playerElement.classList.add('player-targeted');
                break;
                
            default:
                playerElement.classList.add('player-alive');
        }
    }
    
    /**
     * Handle vote animation
     */
    handleVoteAnimation(data) {
        const { voter, target, isSkip } = data;
        const voteElement = document.querySelector(`[data-voter="${voter}"]`);
        
        if (voteElement) {
            voteElement.classList.add('vote-cast');
            
            // Show floating vote indicator
            const icon = isSkip ? '🚫' : '🗳️';
            this.showFloatingText(voteElement, icon, isSkip ? 'warning' : 'info');
            
            setTimeout(() => {
                voteElement.classList.remove('vote-cast');
            }, 600);
        }
        
        // Update voting progress animation
        this.updateVotingProgress();
    }
    
    /**
     * Handle action animations
     */
    handleActionAnimation(data) {
        const { type, success, element, message } = data;
        
        if (element) {
            if (success) {
                element.classList.add('action-successful');
                setTimeout(() => element.classList.remove('action-successful'), 1300);
            } else {
                element.classList.add('action-failed');
                setTimeout(() => element.classList.remove('action-failed'), 500);
            }
        }
        
        // Show action result notification
        if (message) {
            this.showNotification(message, success ? 'success' : 'error');
        }
    }
    
    /**
     * Handle notification animations
     */
    handleNotificationAnimation(data) {
        const { message, type, duration = 4000 } = data;
        this.showNotification(message, type, duration);
    }
    
    /**
     * Show floating text above an element
     */
    showFloatingText(element, text, type = 'info') {
        const floatingText = document.createElement('div');
        floatingText.className = `floating-text floating-text-${type}`;
        floatingText.textContent = text;
        floatingText.style.cssText = `
            position: absolute;
            z-index: 1000;
            font-size: 1.5rem;
            font-weight: bold;
            pointer-events: none;
            animation: floatUp 2s ease-out forwards;
        `;
        
        // Position relative to element
        const rect = element.getBoundingClientRect();
        floatingText.style.left = rect.left + rect.width / 2 + 'px';
        floatingText.style.top = rect.top + 'px';
        
        document.body.appendChild(floatingText);
        
        // Remove after animation
        setTimeout(() => {
            if (floatingText.parentNode) {
                floatingText.parentNode.removeChild(floatingText);
            }
        }, 2000);
    }
    
    /**
     * Show phase change notification
     */
    showPhaseNotification(title, message, type = 'info') {
        const notification = this.createNotification(title, message, type, 5000);
        notification.classList.add('phase-notification');
        
        // Add special styling for phase notifications
        notification.style.cssText += `
            font-size: 1.1rem;
            border-radius: 10px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.2);
        `;
    }
    
    /**
     * Show general notification
     */
    showNotification(message, type = 'info', duration = 4000) {
        this.createNotification('', message, type, duration);
    }
    
    /**
     * Create and show notification element
     */
    createNotification(title, message, type, duration) {
        const notificationContainer = this.getOrCreateNotificationContainer();
        
        const notification = document.createElement('div');
        notification.className = `notification notification-${type} animate-slide-in-down`;
        
        notification.innerHTML = `
            ${title ? `<h5 class="notification-title">${title}</h5>` : ''}
            <p class="notification-message">${message}</p>
            <button class="notification-close" onclick="this.parentElement.remove()">×</button>
        `;
        
        notification.style.cssText = `
            margin-bottom: 10px;
            padding: 15px 20px;
            border-radius: 8px;
            position: relative;
            box-shadow: 0 4px 16px rgba(0,0,0,0.1);
        `;
        
        notificationContainer.appendChild(notification);
        
        // Auto-remove after duration
        if (duration > 0) {
            setTimeout(() => {
                notification.classList.add('animate-fade-out');
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 300);
            }, duration);
        }
        
        return notification;
    }
    
    /**
     * Get or create notification container
     */
    getOrCreateNotificationContainer() {
        let container = document.getElementById('notification-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notification-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            document.body.appendChild(container);
        }
        return container;
    }
    
    /**
     * Update voting progress animation
     */
    updateVotingProgress() {
        const voteItems = document.querySelectorAll('.vote-status-item');
        const totalVotes = voteItems.length;
        const votedCount = document.querySelectorAll('.vote-status-item.voted').length;
        
        // Animate progress bar if it exists
        const progressBar = document.querySelector('.voting-progress::before');
        if (progressBar) {
            const percentage = (votedCount / totalVotes) * 100;
            progressBar.style.width = percentage + '%';
        }
    }
    
    /**
     * Play transition sound effect
     */
    playTransitionSound(phase) {
        if (window.soundManager && window.soundManager.playGameEvent) {
            window.soundManager.playGameEvent('PHASE_CHANGE', { phase });
        }
    }
    
    /**
     * Animate button interactions
     */
    animateButton(button, type = 'pulse') {
        if (!button) return;
        
        button.classList.add(`animate-${type}`);
        setTimeout(() => {
            button.classList.remove(`animate-${type}`);
        }, 600);
    }
    
    /**
     * Animate role reveal
     */
    animateRoleReveal(roleElement, role) {
        if (!roleElement) return;
        
        roleElement.classList.add('role-reveal');
        
        // Add role-specific class
        const roleClass = `role-${role.toLowerCase()}`;
        setTimeout(() => {
            roleElement.classList.add(roleClass);
        }, 600);
    }
    
    /**
     * Animate timer states
     */
    animateTimer(timerElement, timeRemaining, totalTime) {
        if (!timerElement) return;
        
        const percentage = (timeRemaining / totalTime) * 100;
        
        // Remove existing timer classes
        timerElement.classList.remove('timer-warning', 'timer-critical');
        
        // Add appropriate class based on time remaining
        if (percentage <= 20) {
            timerElement.classList.add('timer-critical');
        } else if (percentage <= 50) {
            timerElement.classList.add('timer-warning');
        }
        
        // Update progress bar if it exists
        const progressBar = timerElement.querySelector('.timer-progress-bar');
        if (progressBar) {
            progressBar.style.width = percentage + '%';
        }
    }
    
    /**
     * Queue animation for sequential execution
     */
    queueAnimation(animationFunction, delay = 0) {
        this.animationQueue.push({ function: animationFunction, delay });
        
        if (!this.isAnimating) {
            this.processAnimationQueue();
        }
    }
    
    /**
     * Process queued animations
     */
    async processAnimationQueue() {
        this.isAnimating = true;
        
        while (this.animationQueue.length > 0) {
            const { function: animFn, delay } = this.animationQueue.shift();
            
            if (delay > 0) {
                await this.wait(delay);
            }
            
            if (typeof animFn === 'function') {
                await animFn();
            }
        }
        
        this.isAnimating = false;
    }
    
    /**
     * Utility function to wait for a specified time
     */
    wait(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    /**
     * Cleanup animations
     */
    cleanup() {
        if (this.scrollObserver) {
            this.scrollObserver.disconnect();
        }
        
        // Remove all animation classes
        document.querySelectorAll('[class*="animate-"]').forEach(el => {
            el.className = el.className.replace(/animate-[\w-]+/g, '').trim();
        });
    }
}

// Initialize animation manager when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.animationManager = new AnimationManager();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AnimationManager;
}

// Add floating text keyframe animation
const floatingTextStyle = document.createElement('style');
floatingTextStyle.textContent = `
    @keyframes floatUp {
        0% {
            opacity: 1;
            transform: translateY(0) scale(1);
        }
        50% {
            opacity: 1;
            transform: translateY(-30px) scale(1.2);
        }
        100% {
            opacity: 0;
            transform: translateY(-60px) scale(0.8);
        }
    }
    
    .floating-text {
        position: fixed;
        z-index: 1000;
        pointer-events: none;
        user-select: none;
    }
    
    .floating-text-success {
        color: #28a745;
        text-shadow: 0 0 10px rgba(40, 167, 69, 0.5);
    }
    
    .floating-text-error {
        color: #dc3545;
        text-shadow: 0 0 10px rgba(220, 53, 69, 0.5);
    }
    
    .floating-text-warning {
        color: #ffc107;
        text-shadow: 0 0 10px rgba(255, 193, 7, 0.5);
    }
    
    .floating-text-info {
        color: #17a2b8;
        text-shadow: 0 0 10px rgba(23, 162, 184, 0.5);
    }
    
    .notification-close {
        position: absolute;
        top: 10px;
        right: 15px;
        background: none;
        border: none;
        font-size: 1.5rem;
        color: #6c757d;
        cursor: pointer;
        transition: color 0.3s ease;
    }
    
    .notification-close:hover {
        color: #495057;
    }
    
    .notification-title {
        margin: 0 0 8px 0;
        font-weight: bold;
    }
    
    .notification-message {
        margin: 0;
        line-height: 1.4;
    }
`;

document.head.appendChild(floatingTextStyle); 