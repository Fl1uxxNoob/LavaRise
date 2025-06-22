package net.fliuxx.lavaRise;

import net.fliuxx.lavaRise.commands.LavaRiseCommand;
import net.fliuxx.lavaRise.listeners.GUIListener;
import net.fliuxx.lavaRise.listeners.PlayerListener;
import net.fliuxx.lavaRise.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for LavaRise minigame
 * 
 * @author Fl1uxxNoob
 */
public final class LavaRise extends JavaPlugin {

    private static LavaRise instance;
    
    // Managers
    private ConfigManager configManager;
    private GameManager gameManager;
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private LavaManager lavaManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.arenaManager = new ArenaManager(this);
        this.worldManager = new WorldManager(this);
        this.playerManager = new PlayerManager(this);
        this.lavaManager = new LavaManager(this);
        this.gameManager = new GameManager(this);
        
        // Load configuration
        configManager.loadConfigs();
        
        // Register commands
        getCommand("lavarise").setExecutor(new LavaRiseCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        
        getLogger().info("LavaRise plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop any running games
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.stopGame();
        }
        
        // Clean up worlds
        if (worldManager != null) {
            worldManager.cleanup();
        }
        
        getLogger().info("LavaRise plugin has been disabled!");
    }
    
    // Getters for managers
    public static LavaRise getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public WorldManager getWorldManager() {
        return worldManager;
    }
    
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public LavaManager getLavaManager() {
        return lavaManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}
