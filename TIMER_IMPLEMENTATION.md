# Timer Implementation - Server-Side Consistent Timer

## Overview
This implementation replaces the client-side timer with a server-side timer system that ensures consistency across all players and persistence through page reloads.

## Changes Made

### 1. Game Model Updates (`Game.java`)
- Added `phaseStartTime` field to track when the current phase started
- Added `phaseDurationSeconds` field to store the duration of the current phase
- Added `timerEnabled` field to control timer state
- Added helper methods:
  - `getRemainingTimeSeconds()` - calculates remaining time based on server time
  - `isTimerExpired()` - checks if the timer has expired

### 2. Phase Timer Service (`PhaseTimerService.java`)
- New service class to handle all timer-related operations
- Features:
  - `startPhaseTimer()` - starts timer for a specific phase
  - `stopTimer()` - stops the timer for a game
  - `getTimerStatus()` - returns current timer status
  - `@Scheduled` method that checks for expired timers every 5 seconds
  - Automatic phase advancement when timers expire
- Constants for phase durations:
  - Day phase: 90 seconds (1.5 minutes)
  - Night phase: 60 seconds (1 minute)

### 3. Game Logic Service Updates (`GameLogicService.java`)
- Integrated `PhaseTimerService` into the constructor
- Updated phase transition methods to start timers:
  - `startDayPhase()` - starts day timer
  - `startNightPhase()` - starts night timer
  - `advancePhase()` - starts appropriate timer based on phase
- Added timer stopping when games end
- Updated voting completion to start night timer

### 4. Controller Updates (`GameLogicController.java`)
- Added new endpoint: `GET /{gameCode}/timer-status`
- Returns timer status including:
  - `active` - whether timer is running
  - `remainingSeconds` - time left in current phase
  - `totalSeconds` - total duration of current phase

### 5. Frontend Updates (`game.html`)
- Replaced client-side timer with server-side synchronization
- `startPhaseTimer()` now fetches timer status from server
- Timer updates every second by polling server
- Removed hardcoded phase durations from frontend
- Timer automatically clears when server indicates it's inactive

### 6. Application Configuration
- Added `@EnableScheduling` to main application class to enable scheduled timer checks

## Benefits

### 1. Consistency Across Players
- All players see the same timer countdown
- Timer is synchronized to server time
- No discrepancies between different clients

### 2. Persistence Through Page Reloads
- Timer state is stored in database
- Players can reload page without losing timer sync
- Timer continues running regardless of client actions

### 3. Automatic Phase Management
- Server automatically advances phases when timers expire
- No dependency on client-side actions
- Prevents games from getting stuck

### 4. Reliability
- Timer runs on server, not affected by client performance
- Consistent timing regardless of network latency
- Robust error handling

## How It Works

1. **Phase Start**: When a phase begins, the server records the start time and duration
2. **Client Sync**: Clients poll the server every second for timer status
3. **Time Calculation**: Server calculates remaining time based on current time vs start time
4. **Auto-Advance**: Scheduled task checks every 5 seconds for expired timers
5. **Phase Transition**: When timer expires, server automatically advances to next phase

## API Endpoints

### Get Timer Status
```
GET /api/game-logic/{gameCode}/timer-status
```

Response:
```json
{
  "active": true,
  "remainingSeconds": 45,
  "totalSeconds": 90
}
```

## Database Schema Changes

New columns added to `Game` table:
- `phase_start_time` (TIMESTAMP) - When current phase started
- `phase_duration_seconds` (INTEGER) - Duration of current phase in seconds
- `timer_enabled` (BOOLEAN) - Whether timer is active (default: true)

## Configuration

Phase durations are configured in `PhaseTimerService`:
- `DAY_PHASE_DURATION = 90` seconds
- `NIGHT_PHASE_DURATION = 60` seconds

Timer check interval: 5 seconds (configurable via `@Scheduled(fixedRate = 5000)`)

## Testing

The implementation has been compiled successfully and is ready for testing. Key test scenarios:
1. Timer consistency across multiple players
2. Timer persistence through page reloads
3. Automatic phase advancement
4. Timer stopping when games end
5. Error handling for network issues 