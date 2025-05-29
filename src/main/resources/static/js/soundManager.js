/**
 * Sound Manager for Mafia Game
 * Handles all sound effects with volume control and categories
 */
class SoundManager {
    constructor() {
        this.sounds = new Map();
        this.volume = 0.7; // Default volume
        this.enabled = true;
        this.isInitialized = false;
        this.pendingCalls = []; // Queue for calls made before initialization
        this.categories = {
            action: 0.8,
            notification: 0.6,
            phase: 0.9,
            elimination: 1.0,
            ambient: 0.3
        };
        this.disableWinSounds = false; // Option to disable potentially annoying win sounds
        this.muteEliminationWhenVoting = false; // Temporarily mute elimination sounds when voting
        this.winSoundsPlayed = false; // Track if win sounds have already been played this game
        
        console.log('SoundManager: Initializing...');
        
        // Delay loading sounds slightly to ensure DOM and server are ready
        setTimeout(() => {
            this.loadSounds();
            this.loadSettings();
            this.isInitialized = true;
            console.log('SoundManager: Initialization complete');
            
            // Process any pending sound calls
            this.processPendingCalls();
        }, 1000);
    }
    
    /**
     * Process queued sound calls that came in before initialization
     */
    processPendingCalls() {
        while (this.pendingCalls.length > 0) {
            const call = this.pendingCalls.shift();
            if (call.method === 'play') {
                this.play(call.soundName, call.options);
            } else if (call.method === 'playGameEvent') {
                this.playGameEvent(call.eventType, call.eventData);
            }
        }
    }
    
    /**
     * Load all sound files
     */
    loadSounds() {
        const soundFiles = {
            // Game Phase Sounds
            'day-start': { file: 'day-start.wav', category: 'phase' },
            'night-start': { file: 'night-start.wav', category: 'phase' },
            'game-start': { file: 'game-start.wav', category: 'phase' },
            'game-end': { file: 'game-end.wav', category: 'phase' },
            
            // Action Sounds
            'vote': { file: 'vote.wav', category: 'action' },
            'skip-vote': { file: 'skip-vote.wav', category: 'action' },
            'mafia-action': { file: 'mafia-action.wav', category: 'action' },
            'doctor-save': { file: 'doctor-save.wav', category: 'action' },
            'detective-investigate': { file: 'detective-investigate.wav', category: 'action' },
            
            // Elimination Sounds
            'elimination': { file: 'elimination.wav', category: 'elimination' },
            'mafia-kill': { file: 'mafia-kill.wav', category: 'elimination' },
            
            // Notification Sounds
            'new-message': { file: 'notification.wav', category: 'notification' },
            'timer-warning': { file: 'timer-warning.wav', category: 'notification' },
            'timer-urgent': { file: 'timer-urgent.wav', category: 'notification' },
            'action-complete': { file: 'action-complete.wav', category: 'notification' },
            'error': { file: 'error.wav', category: 'notification' },
            
            // Role-specific Sounds
            'mafia-win': { file: 'mafia-win.wav', category: 'phase' },
            'town-win': { file: 'town-win.wav', category: 'phase' },
            
            // Ambient Sounds
            'ambient-day': { file: 'ambient-day.wav', category: 'ambient', loop: true },
            'ambient-night': { file: 'ambient-night.wav', category: 'ambient', loop: true }
        };
        
        Object.entries(soundFiles).forEach(([name, config]) => {
            const audio = new Audio(`/sounds/${config.file}`);
            audio.volume = this.volume * this.categories[config.category];
            audio.preload = 'auto';
            if (config.loop) {
                audio.loop = true;
            }
            
            // Handle loading errors gracefully
            audio.addEventListener('error', (e) => {
                console.warn(`Failed to load sound: ${config.file}. This is normal if the server is still starting or files are not yet available.`);
                // Mark as not loaded but don't prevent the system from working
                this.sounds.get(name).loaded = false;
                
                // Retry loading after a short delay
                setTimeout(() => {
                    const retryAudio = new Audio(`/sounds/${config.file}`);
                    retryAudio.volume = this.volume * this.categories[config.category];
                    retryAudio.preload = 'auto';
                    if (config.loop) {
                        retryAudio.loop = true;
                    }
                    
                    retryAudio.addEventListener('canplaythrough', () => {
                        if (!this.sounds.get(name).loaded) {
                            this.sounds.get(name).audio = retryAudio;
                            this.sounds.get(name).loaded = true;
                            console.log(`Successfully loaded sound on retry: ${config.file}`);
                        }
                    });
                    
                    retryAudio.addEventListener('error', () => {
                        console.warn(`Sound file ${config.file} failed to load after retry - using fallback beeps`);
                    });
                }, 2000); // Retry after 2 seconds
            });
            
            this.sounds.set(name, {
                audio: audio,
                category: config.category,
                loop: config.loop || false,
                loaded: false
            });
            
            // Check if file loads successfully
            audio.addEventListener('canplaythrough', () => {
                if (!this.sounds.get(name).loaded) {
                    this.sounds.get(name).loaded = true;
                    console.log(`Successfully loaded sound: ${config.file}`);
                }
            });
        });
    }
    
    /**
     * Play a sound by name
     */
    play(soundName, options = {}) {
        if (!this.enabled) return;
        
        // If not initialized yet, queue the call
        if (!this.isInitialized) {
            this.pendingCalls.push({ method: 'play', soundName, options });
            return;
        }
        
        const sound = this.sounds.get(soundName);
        if (!sound) {
            console.warn(`Sound not found: ${soundName}`);
            return;
        }
        
        // Check if sound file loaded successfully
        if (!sound.loaded) {
            // Create a simple beep as fallback
            this.createFallbackBeep(soundName);
            return;
        }
        
        const audio = sound.audio;
        audio.currentTime = 0;
        
        // Apply volume adjustments
        const volumeMultiplier = options.volume || 1;
        audio.volume = this.volume * this.categories[sound.category] * volumeMultiplier;
        
        // Play the sound
        const playPromise = audio.play();
        if (playPromise !== undefined) {
            playPromise.catch(error => {
                console.warn('Failed to play sound:', soundName, error);
                // Try fallback beep
                this.createFallbackBeep(soundName);
            });
        }
        
        return audio;
    }
    
    /**
     * Create a simple beep fallback using Web Audio API
     */
    createFallbackBeep(soundName) {
        if (!this.audioContext) {
            try {
                this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            } catch (e) {
                console.warn('Web Audio API not supported');
                return;
            }
        }
        
        // Resume audio context if suspended (browser autoplay policy)
        if (this.audioContext.state === 'suspended') {
            this.audioContext.resume();
        }
        
        // Different frequencies for different sound types
        const frequencies = {
            'vote': 800,
            'skip-vote': 400,
            'day-start': 600,
            'night-start': 300,
            'game-start': 1000,
            'elimination': 200,
            'error': 150,
            'notification': 900,
            'timer-warning': 750,
            'timer-urgent': 500
        };
        
        const frequency = frequencies[soundName] || 500;
        const duration = 0.2; // Short beep
        
        const oscillator = this.audioContext.createOscillator();
        const gainNode = this.audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(this.audioContext.destination);
        
        oscillator.frequency.setValueAtTime(frequency, this.audioContext.currentTime);
        oscillator.type = 'sine';
        
        // Set volume based on category
        const sound = this.sounds.get(soundName);
        const categoryVolume = sound ? this.categories[sound.category] : 0.5;
        gainNode.gain.setValueAtTime(this.volume * categoryVolume * 0.1, this.audioContext.currentTime);
        
        // Fade out to avoid clicks
        gainNode.gain.exponentialRampToValueAtTime(0.001, this.audioContext.currentTime + duration);
        
        oscillator.start(this.audioContext.currentTime);
        oscillator.stop(this.audioContext.currentTime + duration);
    }
    
    /**
     * Stop a sound
     */
    stop(soundName) {
        const sound = this.sounds.get(soundName);
        if (sound) {
            sound.audio.pause();
            sound.audio.currentTime = 0;
        }
    }
    
    /**
     * Stop all sounds in a category
     */
    stopCategory(category) {
        this.sounds.forEach((sound, name) => {
            if (sound.category === category) {
                this.stop(name);
            }
        });
    }
    
    /**
     * Stop all sounds
     */
    stopAll() {
        this.sounds.forEach((sound, name) => {
            this.stop(name);
        });
    }
    
    /**
     * Set master volume
     */
    setVolume(volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        this.updateAllVolumes();
        this.saveSettings();
    }
    
    /**
     * Set category volume
     */
    setCategoryVolume(category, volume) {
        if (this.categories[category] !== undefined) {
            this.categories[category] = Math.max(0, Math.min(1, volume));
            this.updateAllVolumes();
            this.saveSettings();
        }
    }
    
    /**
     * Update volumes for all loaded sounds
     */
    updateAllVolumes() {
        this.sounds.forEach((sound) => {
            sound.audio.volume = this.volume * this.categories[sound.category];
        });
    }
    
    /**
     * Enable/disable sound
     */
    setEnabled(enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.stopAll();
        }
        this.saveSettings();
    }
    
    /**
     * Toggle sound on/off
     */
    toggle() {
        this.setEnabled(!this.enabled);
        return this.enabled;
    }
    
    /**
     * Toggle win sounds on/off
     */
    toggleWinSounds() {
        this.disableWinSounds = !this.disableWinSounds;
        this.saveSettings();
        return !this.disableWinSounds; // Return true if win sounds are enabled
    }
    
    /**
     * Temporarily mute elimination sounds (e.g., after voting)
     */
    muteEliminationTemporarily(duration = 5000) {
        this.muteEliminationWhenVoting = true;
        console.log('Temporarily muting elimination sounds for', duration, 'ms');
        
        // Clear the mute after the specified duration
        setTimeout(() => {
            this.muteEliminationWhenVoting = false;
            console.log('Elimination sound mute cleared');
        }, duration);
    }
    
    /**
     * Save settings to localStorage
     */
    saveSettings() {
        const settings = {
            volume: this.volume,
            enabled: this.enabled,
            categories: this.categories,
            disableWinSounds: this.disableWinSounds
        };
        localStorage.setItem('mafia-sound-settings', JSON.stringify(settings));
    }
    
    /**
     * Load settings from localStorage
     */
    loadSettings() {
        const saved = localStorage.getItem('mafia-sound-settings');
        if (saved) {
            try {
                const settings = JSON.parse(saved);
                this.volume = settings.volume || 0.7;
                this.enabled = settings.enabled !== false;
                if (settings.categories) {
                    Object.assign(this.categories, settings.categories);
                }
                this.disableWinSounds = settings.disableWinSounds || false;
                this.updateAllVolumes();
            } catch (error) {
                console.warn('Failed to load sound settings:', error);
            }
        }
    }
    
    /**
     * Play sound for specific game events
     */
    playGameEvent(eventType, eventData = {}) {
        // If not initialized yet, queue the call
        if (!this.isInitialized) {
            this.pendingCalls.push({ method: 'playGameEvent', eventType, eventData });
            return;
        }
        
        switch (eventType) {
            case 'GAME_START':
                this.winSoundsPlayed = false; // Reset win sound tracking for new game
                this.play('game-start');
                break;
            case 'GAME_END':
                this.play('game-end');
                break;
            case 'PHASE_CHANGE':
                if (eventData.phase === 'DAY') {
                    this.stopCategory('ambient');
                    this.play('day-start');
                    setTimeout(() => this.play('ambient-day'), 2000);
                } else {
                    this.stopCategory('ambient');
                    this.play('night-start');
                    setTimeout(() => this.play('ambient-night'), 2000);
                }
                break;
            case 'VOTE':
                this.play('vote');
                break;
            case 'SKIP_VOTE':
                this.play('skip-vote');
                break;
            case 'ELIMINATION':
                // Check if elimination sounds are temporarily muted (e.g., when player just voted)
                if (this.muteEliminationWhenVoting) {
                    console.log('Elimination sound muted - player recently voted');
                    return;
                }
                
                // Play the gunshot elimination sound
                console.log('Playing elimination gunshot sound for:', eventData);
                console.log('Elimination sound enabled:', this.enabled);
                console.log('Elimination sound loaded:', this.sounds.get('elimination')?.loaded);
                this.play('elimination');
                break;
            case 'NIGHT_ACTION':
                if (eventData.role === 'MAFIA') {
                    this.play('mafia-action');
                } else if (eventData.role === 'DOCTOR') {
                    this.play('doctor-save');
                } else if (eventData.role === 'DETECTIVE') {
                    this.play('detective-investigate');
                }
                break;
            case 'TIMER_WARNING':
                this.play('timer-warning');
                break;
            case 'TIMER_URGENT':
                this.play('timer-urgent');
                break;
            case 'ACTION_COMPLETE':
                this.play('action-complete');
                break;
            case 'ERROR':
                this.play('error');
                break;
            case 'WIN_MAFIA':
                this.stopAll(); // Always stop all sounds first
                if (!this.disableWinSounds && !this.winSoundsPlayed) {
                    setTimeout(() => this.play('mafia-win'), 500); // Slight delay for clean transition
                    this.winSoundsPlayed = true;
                }
                break;
            case 'WIN_TOWN':
                this.stopAll(); // Always stop all sounds first
                if (!this.disableWinSounds && !this.winSoundsPlayed) {
                    setTimeout(() => this.play('town-win'), 500); // Slight delay for clean transition
                    this.winSoundsPlayed = true;
                }
                break;
        }
    }
    
    /**
     * Reset win sound tracking (for new games)
     */
    resetWinSounds() {
        this.winSoundsPlayed = false;
        console.log('Win sound tracking reset');
    }
    
    /**
     * Get current settings
     */
    getSettings() {
        return {
            volume: this.volume,
            enabled: this.enabled,
            categories: { ...this.categories },
            disableWinSounds: this.disableWinSounds
        };
    }
    
    /**
     * Get sound loading status for debugging
     */
    getSoundStatus() {
        const status = {};
        this.sounds.forEach((sound, name) => {
            status[name] = {
                loaded: sound.loaded,
                category: sound.category,
                loop: sound.loop
            };
        });
        return status;
    }
    
    /**
     * Log sound loading status to console
     */
    logSoundStatus() {
        console.log('SoundManager Status:');
        console.log('Enabled:', this.enabled);
        console.log('Volume:', this.volume);
        console.log('Sound Loading Status:');
        
        const status = this.getSoundStatus();
        const loadedCount = Object.values(status).filter(s => s.loaded).length;
        const totalCount = Object.keys(status).length;
        
        console.log(`Loaded: ${loadedCount}/${totalCount} sounds`);
        
        Object.entries(status).forEach(([name, info]) => {
            const icon = info.loaded ? '✅' : '❌';
            console.log(`  ${icon} ${name} (${info.category})`);
        });
        
        if (loadedCount < totalCount) {
            console.log('Note: Failed sounds will use fallback beeps');
        }
    }
    
    /**
     * Test function to manually play elimination sound (for debugging)
     */
    testEliminationSound() {
        console.log('Testing elimination sound...');
        this.playGameEvent('ELIMINATION', { testCall: true });
    }
}

// Create global sound manager instance when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.soundManager = new SoundManager();
        
        // Log status after sounds have had time to load
        setTimeout(() => {
            window.soundManager.logSoundStatus();
        }, 5000);
    });
} else {
    // DOM is already ready
    window.soundManager = new SoundManager();
    
    // Log status after sounds have had time to load
    setTimeout(() => {
        window.soundManager.logSoundStatus();
    }, 5000);
} 