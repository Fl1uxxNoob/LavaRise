package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Manages lava rising mechanics for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class LavaManager {
    
    private final LavaRise plugin;
    private int currentLavaLevel;
    
    public LavaManager(LavaRise plugin) {
        this.plugin = plugin;
        this.currentLavaLevel = plugin.getConfigManager().getStartingLavaLevel();
    }
    
    public void raiseLava() {
        World gameWorld = plugin.getWorldManager().getGameWorld();
        if (gameWorld == null) {
            plugin.getLogger().warning("Cannot raise lava - game world is null!");
            return;
        }
        
        int riseAmount = plugin.getConfigManager().getLavaRiseAmount();
        int maxHeight = plugin.getConfigManager().getMaxHeight();
        
        // Calculate new lava level
        int newLevel = Math.min(currentLavaLevel + riseAmount, maxHeight);
        
        if (newLevel <= currentLavaLevel) {
            return; // No change needed
        }
        
        // Get world border to determine the area to fill
        double borderSize = gameWorld.getWorldBorder().getSize();
        int radius = (int) (borderSize / 2);
        
        org.bukkit.Location center = gameWorld.getWorldBorder().getCenter();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        
        // Fill lava from current level to new level
        for (int y = currentLavaLevel; y < newLevel; y++) {
            fillLavaLayer(gameWorld, centerX, centerZ, radius, y);
        }
        
        currentLavaLevel = newLevel;
        
        plugin.getLogger().info("Raised lava to level Y=" + currentLavaLevel);
        
        // Broadcast lava rise message
        String message = plugin.getConfigManager().getMessage("game.lava_rising", 
            "%level%", String.valueOf(currentLavaLevel));
        
        for (org.bukkit.entity.Player player : gameWorld.getPlayers()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + message));
        }
    }
    
    private void fillLavaLayer(World world, int centerX, int centerZ, int radius, int y) {
        // Use a more efficient approach - fill in chunks
        for (int x = centerX - radius; x <= centerX + radius; x += 16) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 16) {
                // Process chunk by chunk to avoid lag
                fillLavaChunk(world, x, z, centerX, centerZ, radius, y);
            }
        }
    }
    
    private void fillLavaChunk(World world, int startX, int startZ, int centerX, int centerZ, int radius, int y) {
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                // Check if within border radius
                double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
                if (distance <= radius) {
                    Block block = world.getBlockAt(x, y, z);
                    
                    // Replace ALL blocks at this level with lava (complete layer)
                    // Except for bedrock and other indestructible blocks
                    Material blockType = block.getType();
                    if (blockType != Material.BEDROCK && 
                        blockType != Material.BARRIER && 
                        blockType != Material.COMMAND_BLOCK &&
                        blockType != Material.STRUCTURE_BLOCK) {
                        block.setType(Material.LAVA);
                    }
                }
            }
        }
    }
    
    public void resetLavaLevel() {
        currentLavaLevel = plugin.getConfigManager().getStartingLavaLevel();
    }
    
    public int getCurrentLevel() {
        return currentLavaLevel;
    }
    
    public boolean isAboveLava(org.bukkit.Location location) {
        return location.getBlockY() > currentLavaLevel;
    }
    
    public int getDistanceFromLava(org.bukkit.Location location) {
        return location.getBlockY() - currentLavaLevel;
    }
}
