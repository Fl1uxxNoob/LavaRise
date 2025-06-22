package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Manages player teleportation and state for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class PlayerManager {
    
    private final LavaRise plugin;
    
    public PlayerManager(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    public void teleportPlayersToGame() {
        World gameWorld = plugin.getWorldManager().getGameWorld();
        if (gameWorld == null) {
            plugin.getLogger().severe("Cannot teleport players - game world is null!");
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().getAlivePlayers().contains(player.getUniqueId())) {
                teleportPlayerToGame(player);
            }
        }
    }
    
    public void teleportPlayerToGame(Player player) {
        Location spawnLocation = plugin.getWorldManager().getRandomSpawnLocation();
        if (spawnLocation == null) {
            plugin.getLogger().severe("Cannot get spawn location for player " + player.getName());
            return;
        }
        
        // Set player to survival mode
        player.setGameMode(GameMode.SURVIVAL);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Reset player state
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        
        // Clear effects
        player.getActivePotionEffects().clear();
        
        // Teleport player
        player.teleport(spawnLocation);
        
        // Give starting items (if configured)
        giveStartingItems(player);
        
        plugin.getLogger().info("Teleported " + player.getName() + " to game world at " + 
            spawnLocation.getBlockX() + ", " + spawnLocation.getBlockY() + ", " + spawnLocation.getBlockZ());
    }
    
    private void giveStartingItems(Player player) {
        // This could be expanded to give starting items based on configuration
        // For now, we'll keep it empty as LavaRise typically starts with no items
    }
    
    public void teleportAllToSpawn() {
        String spawnWorldName = plugin.getConfigManager().getSpawnWorld();
        World spawnWorld = Bukkit.getWorld(spawnWorldName);
        
        if (spawnWorld == null) {
            plugin.getLogger().severe("Spawn world '" + spawnWorldName + "' not found!");
            spawnWorld = Bukkit.getWorlds().get(0); // Fallback to first world
        }
        
        Location spawnLocation = new Location(spawnWorld, 
            plugin.getConfigManager().getSpawnX(),
            plugin.getConfigManager().getSpawnY(),
            plugin.getConfigManager().getSpawnZ());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            teleportPlayerToSpawn(player, spawnLocation);
        }
    }
    
    public void teleportPlayerToSpawn(Player player) {
        String spawnWorldName = plugin.getConfigManager().getSpawnWorld();
        World spawnWorld = Bukkit.getWorld(spawnWorldName);
        
        if (spawnWorld == null) {
            plugin.getLogger().severe("Spawn world '" + spawnWorldName + "' not found!");
            spawnWorld = Bukkit.getWorlds().get(0); // Fallback to first world
        }
        
        Location spawnLocation = new Location(spawnWorld, 
            plugin.getConfigManager().getSpawnX(),
            plugin.getConfigManager().getSpawnY(),
            plugin.getConfigManager().getSpawnZ());
            
        teleportPlayerToSpawn(player, spawnLocation);
    }
    
    private void teleportPlayerToSpawn(Player player, Location spawnLocation) {
        // Reset player state
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        
        // Clear effects
        player.getActivePotionEffects().clear();
        
        // Reset scoreboard
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        
        // Teleport
        player.teleport(spawnLocation);
        
        plugin.getLogger().info("Teleported " + player.getName() + " to spawn");
    }
    
    public boolean isPlayerInLava(Player player) {
        Location location = player.getLocation();
        Material blockType = location.getBlock().getType();
        Material belowType = location.subtract(0, 1, 0).getBlock().getType();
        
        return blockType == Material.LAVA || belowType == Material.LAVA;
    }
    
    public void handlePlayerDeath(Player player) {
        if (!plugin.getGameManager().isGameActive()) return;
        
        if (plugin.getGameManager().getAlivePlayers().contains(player.getUniqueId())) {
            plugin.getGameManager().eliminatePlayer(player);
        }
    }
}
