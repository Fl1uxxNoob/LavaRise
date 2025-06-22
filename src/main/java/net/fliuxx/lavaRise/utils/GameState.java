package net.fliuxx.lavaRise.utils;

/**
 * Enumeration representing the different states of the LavaRise game
 * 
 * @author Fl1uxxNoob
 */
public enum GameState {
    WAITING,      // Waiting for players to join
    STARTING,     // Initial countdown before game begins
    ACTIVE,       // Game is active, lava rising
    PVP_ENABLED,  // PvP has been enabled
    ENDING,       // Game is ending
    ENDED         // Game has ended
}
