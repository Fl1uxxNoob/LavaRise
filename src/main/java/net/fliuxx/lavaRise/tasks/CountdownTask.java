package net.fliuxx.lavaRise.tasks;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * Generic countdown task for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class CountdownTask extends BukkitRunnable {
    
    private final LavaRise plugin;
    private int timeLeft;
    private final Runnable onFinish;
    private final Consumer<Integer> onTick;
    
    public CountdownTask(LavaRise plugin, int startTime, Runnable onFinish, Consumer<Integer> onTick) {
        this.plugin = plugin;
        this.timeLeft = startTime;
        this.onFinish = onFinish;
        this.onTick = onTick;
    }
    
    @Override
    public void run() {
        if (timeLeft <= 0) {
            // Countdown finished
            if (onFinish != null) {
                onFinish.run();
            }
            cancel();
            return;
        }
        
        // Execute tick callback
        if (onTick != null) {
            onTick.accept(timeLeft);
        }
        
        timeLeft--;
    }
    
    public int getTimeLeft() {
        return timeLeft;
    }
}
