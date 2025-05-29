/**
 * Sound Settings UI Component
 */
class SoundSettingsUI {
    constructor() {
        this.modal = null;
        
        // Wait for DOM to be ready before creating modal
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.createModal();
                this.bindEvents();
            });
        } else {
            // DOM is already ready
            this.createModal();
            this.bindEvents();
        }
    }
    
    /**
     * Create the sound settings modal
     */
    createModal() {
        const modalHTML = `
            <div class="modal fade" id="soundSettingsModal" tabindex="-1" aria-labelledby="soundSettingsModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="soundSettingsModalLabel">
                                <i class="fas fa-volume-up me-2"></i>Sound Settings
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <!-- Master Settings -->
                            <div class="row">
                                <div class="col-12 mb-3">
                                    <div class="form-check form-switch">
                                        <input class="form-check-input" type="checkbox" id="soundEnabled" checked>
                                        <label class="form-check-label" for="soundEnabled">
                                            <i class="fas fa-volume-up me-2"></i>Enable Sound Effects
                                        </label>
                                    </div>
                                </div>
                                
                                <div class="col-12 mb-3">
                                    <div class="form-check form-switch">
                                        <input class="form-check-input" type="checkbox" id="winSoundsEnabled" checked>
                                        <label class="form-check-label" for="winSoundsEnabled">
                                            <i class="fas fa-trophy me-2"></i>Enable Win Sounds
                                        </label>
                                        <small class="d-block text-muted">Disable if game end sounds are annoying</small>
                                    </div>
                                </div>
                                
                                <div class="col-12 mb-4">
                                    <label for="masterVolume" class="form-label">
                                        <i class="fas fa-volume-up me-2"></i>Master Volume
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="masterVolume" min="0" max="100" value="70">
                                        <span id="masterVolumeValue" class="badge bg-primary">70%</span>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Category Volume Controls -->
                            <h6 class="mb-3"><i class="fas fa-sliders-h me-2"></i>Category Volumes</h6>
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="phaseVolume" class="form-label">
                                        <i class="fas fa-clock me-2"></i>Phase Changes
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="phaseVolume" min="0" max="100" value="90">
                                        <span id="phaseVolumeValue" class="badge bg-info">90%</span>
                                    </div>
                                    <small class="text-muted">Day/Night transitions, game start/end</small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="actionVolume" class="form-label">
                                        <i class="fas fa-mouse-pointer me-2"></i>Actions
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="actionVolume" min="0" max="100" value="80">
                                        <span id="actionVolumeValue" class="badge bg-success">80%</span>
                                    </div>
                                    <small class="text-muted">Voting, night actions</small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="eliminationVolume" class="form-label">
                                        <i class="fas fa-skull me-2"></i>Eliminations
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="eliminationVolume" min="0" max="100" value="100">
                                        <span id="eliminationVolumeValue" class="badge bg-danger">100%</span>
                                    </div>
                                    <small class="text-muted">Player eliminations and deaths</small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="notificationVolume" class="form-label">
                                        <i class="fas fa-bell me-2"></i>Notifications
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="notificationVolume" min="0" max="100" value="60">
                                        <span id="notificationVolumeValue" class="badge bg-warning">60%</span>
                                    </div>
                                    <small class="text-muted">Timers, alerts, errors</small>
                                </div>
                                
                                <div class="col-md-6 mb-3">
                                    <label for="ambientVolume" class="form-label">
                                        <i class="fas fa-music me-2"></i>Ambient
                                    </label>
                                    <div class="d-flex align-items-center">
                                        <input type="range" class="form-range me-3" id="ambientVolume" min="0" max="100" value="30">
                                        <span id="ambientVolumeValue" class="badge bg-secondary">30%</span>
                                    </div>
                                    <small class="text-muted">Background atmospheric sounds</small>
                                </div>
                            </div>
                            
                            <!-- Sound Test Buttons -->
                            <div class="mt-4">
                                <h6 class="mb-3"><i class="fas fa-play me-2"></i>Test Sounds</h6>
                                <div class="d-flex flex-wrap gap-2">
                                    <button type="button" class="btn btn-outline-primary btn-sm" data-test-sound="vote">
                                        <i class="fas fa-vote-yea me-1"></i>Vote
                                    </button>
                                    <button type="button" class="btn btn-outline-secondary btn-sm" data-test-sound="skip-vote">
                                        <i class="fas fa-forward me-1"></i>Skip Vote
                                    </button>
                                    <button type="button" class="btn btn-outline-info btn-sm" data-test-sound="day-start">
                                        <i class="fas fa-sun me-1"></i>Day Start
                                    </button>
                                    <button type="button" class="btn btn-outline-dark btn-sm" data-test-sound="night-start">
                                        <i class="fas fa-moon me-1"></i>Night Start
                                    </button>
                                    <button type="button" class="btn btn-outline-danger btn-sm" data-test-sound="elimination">
                                        <i class="fas fa-skull me-1"></i>Elimination
                                    </button>
                                    <button type="button" class="btn btn-outline-warning btn-sm" data-test-sound="timer-warning">
                                        <i class="fas fa-clock me-1"></i>Timer Warning
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" id="resetSoundSettings">
                                <i class="fas fa-undo me-2"></i>Reset to Defaults
                            </button>
                            <button type="button" class="btn btn-primary" data-bs-dismiss="modal">
                                <i class="fas fa-save me-2"></i>Save Settings
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Add modal to the page
        document.body.insertAdjacentHTML('beforeend', modalHTML);
        this.modal = document.getElementById('soundSettingsModal');
        
        // Load current settings into UI
        this.loadSettingsToUI();
    }
    
    /**
     * Bind event listeners
     */
    bindEvents() {
        // Sound enabled toggle
        const soundEnabled = document.getElementById('soundEnabled');
        soundEnabled.addEventListener('change', (e) => {
            window.soundManager.setEnabled(e.target.checked);
            this.updateDisabledState();
        });
        
        // Win sounds toggle
        const winSoundsEnabled = document.getElementById('winSoundsEnabled');
        winSoundsEnabled.addEventListener('change', (e) => {
            window.soundManager.disableWinSounds = !e.target.checked;
            window.soundManager.saveSettings();
        });
        
        // Master volume
        const masterVolume = document.getElementById('masterVolume');
        masterVolume.addEventListener('input', (e) => {
            const volume = e.target.value / 100;
            window.soundManager.setVolume(volume);
            document.getElementById('masterVolumeValue').textContent = e.target.value + '%';
        });
        
        // Category volumes
        const categories = ['phase', 'action', 'elimination', 'notification', 'ambient'];
        categories.forEach(category => {
            const slider = document.getElementById(category + 'Volume');
            const valueSpan = document.getElementById(category + 'VolumeValue');
            
            slider.addEventListener('input', (e) => {
                const volume = e.target.value / 100;
                window.soundManager.setCategoryVolume(category, volume);
                valueSpan.textContent = e.target.value + '%';
            });
        });
        
        // Test sound buttons
        const testButtons = document.querySelectorAll('[data-test-sound]');
        testButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const soundName = e.target.closest('[data-test-sound]').dataset.testSound;
                window.soundManager.play(soundName);
            });
        });
        
        // Reset settings
        document.getElementById('resetSoundSettings').addEventListener('click', () => {
            this.resetToDefaults();
        });
    }
    
    /**
     * Load current settings into the UI
     */
    loadSettingsToUI() {
        const settings = window.soundManager.getSettings();
        
        document.getElementById('soundEnabled').checked = settings.enabled;
        document.getElementById('winSoundsEnabled').checked = !settings.disableWinSounds;
        document.getElementById('masterVolume').value = Math.round(settings.volume * 100);
        document.getElementById('masterVolumeValue').textContent = Math.round(settings.volume * 100) + '%';
        
        Object.entries(settings.categories).forEach(([category, volume]) => {
            const slider = document.getElementById(category + 'Volume');
            const valueSpan = document.getElementById(category + 'VolumeValue');
            if (slider && valueSpan) {
                slider.value = Math.round(volume * 100);
                valueSpan.textContent = Math.round(volume * 100) + '%';
            }
        });
        
        this.updateDisabledState();
    }
    
    /**
     * Update disabled state of controls
     */
    updateDisabledState() {
        const enabled = document.getElementById('soundEnabled').checked;
        const controls = document.querySelectorAll('#soundSettingsModal input[type="range"], #soundSettingsModal button[data-test-sound]');
        
        controls.forEach(control => {
            control.disabled = !enabled;
        });
    }
    
    /**
     * Reset all settings to defaults
     */
    resetToDefaults() {
        // Reset sound manager to defaults
        window.soundManager.setVolume(0.7);
        window.soundManager.setEnabled(true);
        window.soundManager.disableWinSounds = false;
        window.soundManager.setCategoryVolume('action', 0.8);
        window.soundManager.setCategoryVolume('notification', 0.6);
        window.soundManager.setCategoryVolume('phase', 0.9);
        window.soundManager.setCategoryVolume('elimination', 1.0);
        window.soundManager.setCategoryVolume('ambient', 0.3);
        
        // Update UI
        this.loadSettingsToUI();
    }
    
    /**
     * Show the settings modal
     */
    show() {
        // Ensure modal is created before showing
        if (!this.modal) {
            console.warn('Sound settings modal not ready yet');
            return;
        }
        
        const modal = new bootstrap.Modal(this.modal);
        modal.show();
    }
}

// Create global sound settings UI instance when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.soundSettingsUI = new SoundSettingsUI();
    });
} else {
    // DOM is already ready
    window.soundSettingsUI = new SoundSettingsUI();
} 