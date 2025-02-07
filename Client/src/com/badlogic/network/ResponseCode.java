package com.badlogic.network;

/**
 * Response code received from the server
 */
public class ResponseCode {
    public static int
        // Does nothing
        Ping = 0,

        // Message about created game
        GameCreated = 1,

        // Message about joined game
        GameJoined = 2,

        // Message about quit game
        GameQuit = 3,

        // Message about updated position
        PositionUpdated = 4,

        // Message about created bonuses
        BonusesCreated = 5,

        // Message about game list
        GameListFormed = 6,

        // Player joined client's room
        NewPlayerJoined = 7,

        // Timer updated
        NewTimerValue = 8,

        // After game match has ended
        GameEnded = 9,

        // When player reaches 0 HP
        PlayerDied = 10;
}
