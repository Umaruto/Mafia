package com.victadore.webmafia.mafia_web_of_lies.model;

public enum Role {
    MAFIA,      // Mafia member who can kill at night
    CITIZEN,    // Regular citizen who can vote during the day
    DOCTOR,     // Can save one person each night
    DETECTIVE,  // Can investigate one person each night
    VIGILANTE   // Can kill one person during the game
}