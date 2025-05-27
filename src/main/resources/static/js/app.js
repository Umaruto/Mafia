/**
 * Mafia Game - Welcome Page
 * Basic JavaScript for welcome page functionality
 */

document.addEventListener('DOMContentLoaded', function() {
    // Get button elements
    const startNewGameBtn = document.querySelector('.btn-primary');
    const joinGameBtn = document.querySelector('.btn-outline-primary');

    // Add click event listeners
    startNewGameBtn.addEventListener('click', async function() {
        const username = prompt('Enter your username:');
        if (!username) return;

        try {
            const response = await fetch('/api/games', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `createdBy=${encodeURIComponent(username)}`
            });

            if (!response.ok) {
                throw new Error('Failed to create game');
            }

            const game = await response.json();
            // Redirect to the game page
            window.location.href = `/api/games/game/${game.gameCode}?username=${encodeURIComponent(username)}`;
        } catch (error) {
            console.error('Error creating game:', error);
            alert('Failed to create game. Please try again.');
        }
    });

    joinGameBtn.addEventListener('click', async function() {
        const username = prompt('Enter your username:');
        if (!username) return;

        const gameCode = prompt('Enter the game code:');
        if (!gameCode) return;

        try {
            const response = await fetch(`/api/games/${gameCode}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `username=${encodeURIComponent(username)}`
            });

            if (!response.ok) {
                throw new Error('Failed to join game');
            }

            // Redirect to the game page
            window.location.href = `/api/games/game/${gameCode}?username=${encodeURIComponent(username)}`;
        } catch (error) {
            console.error('Error joining game:', error);
            alert('Failed to join game. Please check the game code and try again.');
        }
    });
}); 