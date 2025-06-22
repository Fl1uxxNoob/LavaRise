package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.utils.Arena;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages arena creation, selection and cleanup for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class ArenaManager {
    
    private final LavaRise plugin;
    private final List<Arena> availableArenas;
    private final List<Arena> usedArenas;
    private Arena currentArena;
    
    private File arenasFile;
    private FileConfiguration arenasConfig;
    
    public ArenaManager(LavaRise plugin) {
        this.plugin = plugin;
        this.availableArenas = new ArrayList<>();
        this.usedArenas = new ArrayList<>();
        this.currentArena = null;
        
        loadArenasData();
        checkAvailableArenas();
    }
    
    private void loadArenasData() {
        // Ensure plugin data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
                // Create empty configuration structure
                YamlConfiguration emptyConfig = new YamlConfiguration();
                emptyConfig.createSection("available");
                emptyConfig.createSection("used");
                emptyConfig.save(arenasFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.yml file!");
                return;
            }
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        
        // Load available arenas
        if (arenasConfig.contains("available")) {
            for (String key : arenasConfig.getConfigurationSection("available").getKeys(false)) {
                Arena arena = Arena.fromConfig(arenasConfig.getConfigurationSection("available." + key));
                if (arena != null) {
                    availableArenas.add(arena);
                }
            }
        }
        
        // Load used arenas
        if (arenasConfig.contains("used")) {
            for (String key : arenasConfig.getConfigurationSection("used").getKeys(false)) {
                Arena arena = Arena.fromConfig(arenasConfig.getConfigurationSection("used." + key));
                if (arena != null) {
                    usedArenas.add(arena);
                }
            }
        }
    }
    
    private void saveArenasData() {
        // Clear current data
        arenasConfig.set("available", null);
        arenasConfig.set("used", null);
        
        // Save available arenas
        for (int i = 0; i < availableArenas.size(); i++) {
            availableArenas.get(i).saveToConfig(arenasConfig, "available.arena" + i);
        }
        
        // Save used arenas
        for (int i = 0; i < usedArenas.size(); i++) {
            usedArenas.get(i).saveToConfig(arenasConfig, "used.arena" + i);
        }
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arenas.yml file!");
        }
    }
    
    public boolean setupNewArenas(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Creating LavaRise world and arenas... This may take a while!");
        
        // Create or get LavaRise world
        World lavaWorld = createLavaRiseWorld();
        if (lavaWorld == null) {
            player.sendMessage(ChatColor.RED + "Failed to create LavaRise world!");
            return false;
        }
        
        player.sendMessage(ChatColor.GREEN + "LavaRise world created successfully!");
        
        // Generate arenas asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            generateArenasAsync(player, lavaWorld);
        });
        
        return true;
    }
    
    private void generateArenasAsync(Player player, World lavaWorld) {
        List<Arena> newArenas = new ArrayList<>();
        Random random = new Random();
        
        // Generate all coordinates first
        List<int[]> coordinates = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 100;
        
        while (coordinates.size() < maxAttempts) {
            int x = random.nextInt(8000) - 4000; // -4000 to +4000
            int z = random.nextInt(8000) - 4000;
            coordinates.add(new int[]{x, z});
        }
        
        // Process coordinates on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            processArenaCoordinates(player, lavaWorld, coordinates, newArenas, 0);
        });
    }
    
    private void processArenaCoordinates(Player player, World lavaWorld, List<int[]> coordinates, List<Arena> newArenas, int index) {
        if (newArenas.size() >= 4 || index >= coordinates.size()) {
            finishArenaSetup(player, newArenas);
            return;
        }
        
        int[] coord = coordinates.get(index);
        int x = coord[0];
        int z = coord[1];
        
        try {
            // Load chunk synchronously on main thread
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            
            if (!lavaWorld.isChunkLoaded(chunkX, chunkZ)) {
                lavaWorld.loadChunk(chunkX, chunkZ, true);
            }
            
            int y = lavaWorld.getHighestBlockYAt(x, z);
            Location center = new Location(lavaWorld, x, y, z);
            
            if (isSuitableArenaLocationSync(center)) {
                Arena arena = new Arena(
                    "arena_" + System.currentTimeMillis() + "_" + newArenas.size(),
                    center,
                    plugin.getConfigManager().getBorderInitialSize()
                );
                
                newArenas.add(arena);
                player.sendMessage(ChatColor.GREEN + "Arena " + (newArenas.size()) + "/4 created at " + 
                    center.getBlockX() + ", " + center.getBlockZ());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check arena location at " + x + ", " + z + ": " + e.getMessage());
        }
        
        // Schedule next coordinate check with a small delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            processArenaCoordinates(player, lavaWorld, coordinates, newArenas, index + 1);
        }, 2L); // 2 ticks delay
    }
    
    private void finishArenaSetup(Player player, List<Arena> newArenas) {
        if (newArenas.size() < 4) {
            player.sendMessage(ChatColor.RED + "Warning: Could only create " + newArenas.size() + " arenas out of 4!");
        }
        
        // Add new arenas to available list
        availableArenas.addAll(newArenas);
        saveArenasData();
        
        player.sendMessage(ChatColor.GREEN + "Setup complete! " + newArenas.size() + " new arenas created.");
        player.sendMessage(ChatColor.YELLOW + "Total available arenas: " + availableArenas.size());
    }
    
    
    
    private boolean isSuitableArenaLocation(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int z = center.getBlockZ();
        
        // Check a 50x50 area around the center
        int waterBlocks = 0;
        int totalBlocks = 0;
        
        for (int dx = -25; dx <= 25; dx += 5) {
            for (int dz = -25; dz <= 25; dz += 5) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                Location checkLoc = new Location(world, x + dx, y, z + dz);
                
                // Check if it's water or if Y is too low (probably ocean)
                if (checkLoc.getBlock().getType() == Material.WATER || y < 60) {
                    waterBlocks++;
                }
                
                totalBlocks++;
            }
        }
        
        // Reject if more than 30% water
        return (waterBlocks / (double) totalBlocks) < 0.3;
    }
    
    private boolean isSuitableArenaLocationSync(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int z = center.getBlockZ();
        
        // Simplified check to avoid long operations on main thread
        // Just check the center and a few key points
        int waterBlocks = 0;
        int totalBlocks = 0;
        
        // Check only 9 points in a 3x3 grid to be faster
        for (int dx = -10; dx <= 10; dx += 10) {
            for (int dz = -10; dz <= 10; dz += 10) {
                try {
                    int y = world.getHighestBlockYAt(x + dx, z + dz);
                    Location checkLoc = new Location(world, x + dx, y, z + dz);
                    
                    // Check if it's water or if Y is too low (probably ocean)
                    if (checkLoc.getBlock().getType() == Material.WATER || y < 60) {
                        waterBlocks++;
                    }
                    
                    totalBlocks++;
                } catch (Exception e) {
                    // If we can't check this location, count it as unsuitable
                    waterBlocks++;
                    totalBlocks++;
                }
            }
        }
        
        // Reject if more than 30% water
        return (waterBlocks / (double) totalBlocks) < 0.3;
    }
    
    private void preloadArenaChunks(Arena arena) {
        World world = arena.getCenter().getWorld();
        int centerX = arena.getCenter().getChunk().getX();
        int centerZ = arena.getCenter().getChunk().getZ();
        
        // Load only essential chunks during game
        int radius = 2; // 5x5 chunk area
        
        // Must be executed on main thread for chunk operations
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> preloadArenaChunks(arena));
            return;
        }
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                // Load chunk synchronously on main thread
                world.loadChunk(x, z, true);
                
                Chunk chunk = world.getChunkAt(x, z);
                // Keep chunk loaded during game
                chunk.setForceLoaded(true);
            }
        }
        
        plugin.getLogger().info("Pre-loaded game chunks for arena: " + arena.getId());
    }
    
    private World createLavaRiseWorld() {
        String worldName = plugin.getConfigManager().getWorldName();
        
        // Delete existing world if it exists
        World existingWorld = Bukkit.getWorld(worldName);
        if (existingWorld != null) {
            // Unload world first
            Bukkit.unloadWorld(existingWorld, false);
            
            // Delete world folder
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            deleteDirectory(worldFolder);
        }
        
        // Create new world
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(true);
        
        World world = creator.createWorld();
        
        if (world != null) {
            // Configure world settings
            world.setDifficulty(Difficulty.NORMAL);
            world.setSpawnFlags(false, false); // No monsters/animals spawning
            world.setPVP(true);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
            world.setTime(6000); // Set to day
            
            plugin.getLogger().info("Successfully created LavaRise world: " + worldName);
        }
        
        return world;
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    private void preloadArenaChunksOnly(Arena arena) {
        World world = arena.getCenter().getWorld();
        int centerX = arena.getCenter().getChunk().getX();
        int centerZ = arena.getCenter().getChunk().getZ();
        
        // Load only the minimum chunks needed for the arena (3x3 chunk area)
        int radius = 1; // Only 3x3 chunks around center
        
        // Must be executed on main thread for chunk operations
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> preloadArenaChunksOnly(arena));
            return;
        }
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                // Load chunk synchronously but don't force-load to avoid server stress
                world.loadChunk(x, z, true);
            }
        }
        
        plugin.getLogger().info("Pre-loaded minimal chunks for arena: " + arena.getId());
    }
    
    public Arena getNextAvailableArena() {
        if (availableArenas.isEmpty()) {
            return null;
        }
        
        // Return first available arena
        Arena arena = availableArenas.get(0);
        currentArena = arena;
        return arena;
    }
    
    public void markArenaAsUsed(Arena arena) {
        if (arena == null) return;
        
        availableArenas.remove(arena);
        usedArenas.add(arena);
        saveArenasData();
        
        plugin.getLogger().info("Arena " + arena.getId() + " marked as used");
        
        // Clean up the arena
        cleanupArena(arena);
    }
    
    private void cleanupArena(Arena arena) {
        World world = arena.getCenter().getWorld();
        if (world == null) return;
        
        Location center = arena.getCenter();
        int radius = arena.getSize() / 2;
        
        // Remove all lava from the arena
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                    for (int y = 0; y <= plugin.getConfigManager().getMaxHeight(); y++) {
                        Location blockLoc = new Location(world, x, y, z);
                        if (blockLoc.getBlock().getType() == Material.LAVA) {
                            blockLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Cleaned up arena: " + arena.getId());
        });
        
        // Unload chunks to free memory
        unloadArenaChunks(arena);
    }
    
    private void unloadArenaChunks(Arena arena) {
        World world = arena.getCenter().getWorld();
        int centerX = arena.getCenter().getChunk().getX();
        int centerZ = arena.getCenter().getChunk().getZ();
        
        int radius = (arena.getSize() / 2) / 16 + 2;
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                chunk.setForceLoaded(false);
            }
        }
    }
    
    public void checkAvailableArenas() {
        int available = availableArenas.size();
        
        if (available == 0) {
            plugin.getLogger().warning("No arenas available! Use /lavarise setupworld to create new arenas.");
        } else if (available <= 2) {
            plugin.getLogger().warning("Only " + available + " arenas remaining! Consider creating new ones with /lavarise setupworld.");
        } else {
            plugin.getLogger().info("Arenas available: " + available);
        }
    }
    
    public Arena getCurrentArena() {
        return currentArena;
    }
    
    public int getAvailableArenasCount() {
        return availableArenas.size();
    }
    
    public int getUsedArenasCount() {
        return usedArenas.size();
    }
    
    public void finishCurrentGame() {
        if (currentArena != null) {
            markArenaAsUsed(currentArena);
            currentArena = null;
        }
    }
}