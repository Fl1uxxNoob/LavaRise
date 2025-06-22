package net.fliuxx.lavaRise.tasks;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task that handles the rising lava mechanics
 * 
 * @author Fl1uxxNoob
 */
public class LavaRiseTask extends BukkitRunnable {
    
    private final LavaRise plugin;
    
    public LavaRiseTask(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        if (!plugin.getGameManager().isGameActive()) {
            cancel();
            return;
        }
        
        // Raise the lava
        plugin.getLavaManager().raiseLava();
        
        // Check if lava has reached max height
        if (plugin.getLavaManager().getCurrentLevel() >= plugin.getConfigManager().getMaxHeight()) {
            // Lava has reached the top - game should end soon
            plugin.getLogger().info("Lava has reached maximum height!");
            cancel();
        }
    }
}
