package com.victadore.webmafia.mafia_web_of_lies.model;

public enum GameState {
    WAITING_FOR_PLAYERS,  // Game is created but waiting for players to join
    IN_PROGRESS,         // Game is currently being played
    FINISHED,           // Game has ended
    CANCELLED           // Game was cancelled before starting
}