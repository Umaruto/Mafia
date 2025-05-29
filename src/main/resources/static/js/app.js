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
            const response = await fetch('/api/games/legacy', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `createdBy=${encodeURIComponent(username)}`
            });

            let data;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                throw new Error('Server returned non-JSON response');
            }

            if (!response.ok) {
                throw new Error(data.message || 'Failed to create game');
            }

            // Redirect to the waiting room
            window.location.href = `/api/games/${data.gameCode}/waiting-room?username=${encodeURIComponent(username)}`;
        } catch (error) {
            console.error('Error creating game:', error);
            if (error instanceof SyntaxError) {
                alert('Received invalid response from server. Please try again.');
            } else {
                alert(error.message || 'Failed to create game. Please try again.');
            }
        }
    });

    joinGameBtn.addEventListener('click', async function() {
        const username = prompt('Enter your username:');
        if (!username) return;

        const gameCode = prompt('Enter the game code:');
        if (!gameCode) return;

        try {
            const response = await fetch(`/api/games/${gameCode}/join/legacy`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `username=${encodeURIComponent(username)}`
            });

            let data;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                throw new Error('Server returned non-JSON response');
            }
            
            if (!response.ok) {
                throw new Error(data.message || 'Failed to join game');
            }

            // Redirect to the waiting room
            window.location.href = `/api/games/${gameCode}/waiting-room?username=${encodeURIComponent(username)}`;
        } catch (error) {
            console.error('Error joining game:', error);
            if (error instanceof SyntaxError) {
                alert('Received invalid response from server. Please try again.');
            } else {
                alert(error.message || 'Failed to join game. Please check the game code and try again.');
            }
        }
    });
}); 