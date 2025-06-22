package net.fliuxx.lavaRise.tasks;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task that handles world border shrinking
 * 
 * @author Fl1uxxNoob
 */
public class BorderTask extends BukkitRunnable {
    
    private final LavaRise plugin;
    private final World world;
    private final int shrinkTime;
    private final int finalSize;
    private boolean started = false;
    
    public BorderTask(LavaRise plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        this.shrinkTime = plugin.getConfigManager().getBorderShrinkTime();
        this.finalSize = plugin.getConfigManager().getBorderFinalSize();
    }
    
    @Override
    public void run() {
        if (!plugin.getGameManager().isGameActive()) {
            cancel();
            return;
        }
        
        if (!started) {
            // Start the border shrinking
            WorldBorder border = world.getWorldBorder();
            border.setSize(finalSize, shrinkTime);
            started = true;
            
            plugin.getLogger().info("Started world border shrinking to " + finalSize + " over " + shrinkTime + " seconds");
        }
        
        // Check if border has finished shrinking
        WorldBorder border = world.getWorldBorder();
        if (border.getSize() <= finalSize + 1) { // Small tolerance for floating point precision
            plugin.getLogger().info("World border has finished shrinking");
            cancel();
        }
    }
}
