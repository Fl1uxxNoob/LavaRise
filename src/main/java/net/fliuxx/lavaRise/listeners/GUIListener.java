package net.fliuxx.lavaRise.listeners;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.gui.AdminGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles GUI-related events for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class GUIListener implements Listener {
    
    private final LavaRise plugin;
    
    public GUIListener(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.CHEST) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if it's the admin GUI
        AdminGUI adminGUI = new AdminGUI(plugin);
        if (adminGUI.isAdminGUI(event.getClickedInventory())) {
            event.setCancelled(true);
            
            // Handle the click
            adminGUI.handleClick(player, event.getSlot());
        }
    }
}
