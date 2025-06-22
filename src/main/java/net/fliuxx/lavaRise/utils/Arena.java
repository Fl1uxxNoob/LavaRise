package net.fliuxx.lavaRise.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a game arena for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class Arena {
    
    private final String id;
    private final Location center;
    private final int size;
    private boolean used;
    
    public Arena(String id, Location center, int size) {
        this.id = id;
        this.center = center;
        this.size = size;
        this.used = false;
    }
    
    public String getId() {
        return id;
    }
    
    public Location getCenter() {
        return center;
    }
    
    public int getSize() {
        return size;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public void saveToConfig(ConfigurationSection config, String path) {
        config.set(path + ".id", id);
        config.set(path + ".world", center.getWorld().getName());
        config.set(path + ".x", center.getX());
        config.set(path + ".y", center.getY());
        config.set(path + ".z", center.getZ());
        config.set(path + ".size", size);
        config.set(path + ".used", used);
    }
    
    public static Arena fromConfig(ConfigurationSection config) {
        if (config == null) return null;
        
        try {
            String id = config.getString("id");
            String worldName = config.getString("world");
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            int size = config.getInt("size");
            boolean used = config.getBoolean("used", false);
            
            Location center = new Location(Bukkit.getWorld(worldName), x, y, z);
            Arena arena = new Arena(id, center, size);
            arena.setUsed(used);
            
            return arena;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "Arena{id='" + id + "', center=" + center + ", size=" + size + ", used=" + used + "}";
    }
}