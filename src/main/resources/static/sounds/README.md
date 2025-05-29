# Mafia Game Sound Effects

This directory contains audio files for the Mafia: Web of Lies game. The sound system provides immersive audio feedback for various game events.

## Sound Categories

### Phase Sounds (90% volume by default)
- `day-start.wav` - Played when day phase begins
- `night-start.wav` - Played when night phase begins  
- `game-start.wav` - Played when the game starts
- `game-end.wav` - Simple pleasant chime for generic game end
- `mafia-win.wav` - Ominous but pleasant victory sound for Mafia wins (3.0s)
- `town-win.wav` - Uplifting musical fanfare for Town wins (2.5s)

### Action Sounds (80% volume by default)
- `vote.wav` - Played when a player votes
- `skip-vote.wav` - Played when a player skips their vote
- `mafia-action.wav` - Played when Mafia performs a night action
- `doctor-save.wav` - Played when Doctor saves someone
- `detective-investigate.wav` - Played when Detective investigates

### Elimination Sounds (100% volume by default)
- `elimination.wav` - Dramatic close-range gunshot for player eliminations
- `mafia-kill.wav` - Distant echoing gunshot for Mafia kills

### Notification Sounds (60% volume by default)
- `notification.wav` - General notification sound
- `timer-warning.wav` - Played when phase timer reaches 30 seconds
- `timer-urgent.wav` - Played when phase timer reaches 10 seconds
- `action-complete.wav` - Played when an action is successfully completed
- `error.wav` - Played when an error occurs

### Ambient Sounds (30% volume by default)
- `ambient-day.wav` - Looping background sound for day phase
- `ambient-night.wav` - Looping background sound for night phase

## Sound Controls

Players can:
- Toggle sound on/off using the volume button in the game header
- Access detailed sound settings through the settings modal
- Adjust master volume and category-specific volumes
- Disable win sounds if they find game end sounds annoying
- Test different sounds in the settings panel
- Settings are automatically saved to localStorage

## Technical Details

- Audio format: WAV files (16-bit, 22.05kHz sample rate)
- Generated using synthetic tones and chords
- Fallback beep sounds if audio files fail to load
- Uses Web Audio API for fallback generation
- Sound manager handles volume control and categorization

## Browser Compatibility

The sound system uses the HTML5 Audio API with Web Audio API fallbacks, compatible with all modern browsers that support these standards.

## File Requirements
- Format: WAV
- Duration: 1-5 seconds for most sounds (ambient can be longer with looping)
- Volume: Normalized (sound manager handles volume control)
- Quality: 44.1kHz, 128kbps or higher

## Sound Sources
You can find free game sounds at:
- Freesound.org
- Zapsplat.com
- Adobe Audition (built-in sounds)
- Or create your own with audio editing software

## Installation
1. Download or create the sound files
2. Name them exactly as listed above
3. Place them in this directory
4. The game will automatically load them on startup 