package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages configuration files for the LavaRise plugin
 * 
 * @author Fl1uxxNoob
 */
public class ConfigManager {
    
    private final LavaRise plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Map<String, String> messageCache = new HashMap<>();
    
    public ConfigManager(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Save default config if not exists
        plugin.saveDefaultConfig();
        
        // Load messages.yml
        createMessagesConfig();
        loadMessages();
    }
    
    private void createMessagesConfig() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    private void loadMessages() {
        messageCache.clear();
        loadMessagesFromSection("", messagesConfig);
    }
    
    private void loadMessagesFromSection(String prefix, org.bukkit.configuration.ConfigurationSection config) {
        for (String key : config.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (config.isConfigurationSection(key)) {
                loadMessagesFromSection(fullKey, config.getConfigurationSection(key));
            } else {
                String value = config.getString(key, "");
                messageCache.put(fullKey, value);
            }
        }
    }
    
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "&c[Missing message: " + key + "]");
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        loadMessages();
    }
    
    // Configuration getters
    public int getMinPlayers() {
        return plugin.getConfig().getInt("game.min_players", 2);
    }
    
    public int getMaxPlayers() {
        return plugin.getConfig().getInt("game.max_players", 20);
    }
    
    public int getInitialCountdown() {
        return plugin.getConfig().getInt("game.initial_countdown", 60);
    }
    
    public int getPvpCountdown() {
        return plugin.getConfig().getInt("game.pvp_countdown", 180);
    }
    
    public int getLavaRiseInterval() {
        return plugin.getConfig().getInt("game.lava_rise_interval", 5);
    }
    
    public int getLavaRiseAmount() {
        return plugin.getConfig().getInt("game.lava_rise_amount", 1);
    }
    
    public int getStartingLavaLevel() {
        return plugin.getConfig().getInt("game.starting_lava_level", 0);
    }
    
    public String getWorldName() {
        return plugin.getConfig().getString("world.name", "lavarise_world");
    }
    
    public int getBorderInitialSize() {
        return plugin.getConfig().getInt("world.border_initial_size", 200);
    }
    
    public int getBorderFinalSize() {
        return plugin.getConfig().getInt("world.border_final_size", 20);
    }
    
    public int getBorderShrinkTime() {
        return plugin.getConfig().getInt("world.border_shrink_time", 600);
    }
    
    public int getSpawnHeight() {
        return plugin.getConfig().getInt("world.spawn_height", 100);
    }
    
    public int getMaxHeight() {
        return plugin.getConfig().getInt("world.max_height", 256);
    }
    
    public String getSpawnWorld() {
        return plugin.getConfig().getString("teleport.spawn_world", "world");
    }
    
    public double getSpawnX() {
        return plugin.getConfig().getDouble("teleport.spawn_x", 0);
    }
    
    public double getSpawnY() {
        return plugin.getConfig().getDouble("teleport.spawn_y", 100);
    }
    
    public double getSpawnZ() {
        return plugin.getConfig().getDouble("teleport.spawn_z", 0);
    }
    
    public int getGameSpreadDistance() {
        return plugin.getConfig().getInt("teleport.game_spread_distance", 50);
    }
    
    public boolean isScoreboardEnabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }
    
    public boolean isBossBarEnabled() {
        return plugin.getConfig().getBoolean("bossbar.enabled", true);
    }
    
    public int getScoreboardUpdateInterval() {
        return plugin.getConfig().getInt("scoreboard.update_interval", 20);
    }
}
