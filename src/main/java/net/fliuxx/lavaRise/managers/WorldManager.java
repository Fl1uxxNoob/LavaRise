package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.tasks.BorderTask;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages world creation and manipulation for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class WorldManager {
    
    private final LavaRise plugin;
    private World gameWorld;
    private BukkitTask borderTask;
    
    public WorldManager(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    public boolean selectNextArena() {
        // Get next available arena from ArenaManager
        net.fliuxx.lavaRise.utils.Arena arena = plugin.getArenaManager().getNextAvailableArena();
        
        if (arena == null) {
            plugin.getLogger().severe("No available arenas! Use /lavarise setupworld to create new arenas.");
            return false;
        }
        
        // Set the game world to the arena's world
        gameWorld = arena.getCenter().getWorld();
        
        if (gameWorld == null) {
            plugin.getLogger().severe("Arena world not found!");
            return false;
        }
        
        // Setup world border for this arena
        setupWorldBorderForArena(arena);
        
        plugin.getLogger().info("Selected arena: " + arena.getId() + " at " + 
            arena.getCenter().getBlockX() + ", " + arena.getCenter().getBlockZ());
        return true;
    }
    
    private void setupWorldBorderForArena(net.fliuxx.lavaRise.utils.Arena arena) {
        if (gameWorld == null) return;
        
        WorldBorder border = gameWorld.getWorldBorder();
        
        // Set border center to arena center
        border.setCenter(arena.getCenter());
        
        // Set initial border size
        border.setSize(arena.getSize());
        
        // Start border shrinking task
        startBorderShrinking();
    }
    
    private void setupWorldSettings() {
        if (gameWorld == null) return;
        
        // Disable weather
        gameWorld.setStorm(false);
        gameWorld.setThundering(false);
        gameWorld.setWeatherDuration(0);
        
        // Set time to day
        gameWorld.setTime(6000);
        
        // Disable natural mob spawning
        gameWorld.setSpawnFlags(false, false);
        
        // Set game rules
        gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        gameWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        
        // Set spawn point
        int spawnHeight = plugin.getConfigManager().getSpawnHeight();
        Location spawnLocation = new Location(gameWorld, 0, spawnHeight, 0);
        
        // Find a safe spawn location
        Location safeSpawn = findSafeSpawnLocation(spawnLocation);
        gameWorld.setSpawnLocation(safeSpawn);
    }
    
    private Location findSafeSpawnLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        // Find the highest solid block
        int y = world.getHighestBlockYAt(x, z);
        
        // Make sure it's not too high or too low
        y = Math.max(y, 64);
        y = Math.min(y, plugin.getConfigManager().getMaxHeight() - 10);
        
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
    
    private void setupWorldBorder() {
        if (gameWorld == null) return;
        
        WorldBorder border = gameWorld.getWorldBorder();
        
        // Set initial border size
        int initialSize = plugin.getConfigManager().getBorderInitialSize();
        border.setSize(initialSize);
        
        // Center the border at spawn
        border.setCenter(gameWorld.getSpawnLocation());
        
        // Start border shrinking task
        startBorderShrinking();
    }
    
    private void startBorderShrinking() {
        if (gameWorld == null) return;
        
        borderTask = new BorderTask(plugin, gameWorld).runTaskTimer(plugin, 
            plugin.getConfigManager().getBorderShrinkTime() * 20L, // Initial delay
            20L); // Update every second
    }
    
    private void deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Teleport all players out of the world
            for (Player player : world.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            
            // Unload the world
            Bukkit.unloadWorld(world, false);
        }
        
        // Delete world files
        deleteWorldFiles(worldName);
    }
    
    private void deleteWorldFiles(String worldName) {
        try {
            java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists()) {
                deleteDirectory(worldFolder);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not delete world files for " + worldName + ": " + e.getMessage());
        }
    }
    
    private void deleteDirectory(java.io.File directory) {
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    public void cleanup() {
        // Cancel border task
        if (borderTask != null) {
            borderTask.cancel();
            borderTask = null;
        }
        
        // Delete game world
        if (gameWorld != null) {
            String worldName = gameWorld.getName();
            deleteWorld(worldName);
            gameWorld = null;
        }
    }
    
    public World getGameWorld() {
        return gameWorld;
    }
    
    public Location getRandomSpawnLocation() {
        if (gameWorld == null) return null;
        
        // Get current arena center instead of world spawn
        net.fliuxx.lavaRise.utils.Arena arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null) return null;
        
        Location center = arena.getCenter();
        int spread = plugin.getConfigManager().getGameSpreadDistance();
        
        // Generate random coordinates within spread distance
        int x = center.getBlockX() + (int) ((Math.random() - 0.5) * spread * 2);
        int z = center.getBlockZ() + (int) ((Math.random() - 0.5) * spread * 2);
        
        // Find safe Y coordinate
        int y = gameWorld.getHighestBlockYAt(x, z);
        y = Math.max(y, 64);
        y = Math.min(y, plugin.getConfigManager().getMaxHeight() - 10);
        
        return new Location(gameWorld, x + 0.5, y + 1, z + 0.5);
    }
}
