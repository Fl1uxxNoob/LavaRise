package net.fliuxx.lavaRise.gui;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.utils.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Admin GUI for managing LavaRise games
 * 
 * @author Fl1uxxNoob
 */
public class AdminGUI {
    
    private final LavaRise plugin;
    private final String title;
    
    public AdminGUI(LavaRise plugin) {
        this.plugin = plugin;
        this.title = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("gui.admin_panel"));
    }
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, title);
        
        // Start Game button
        ItemStack startGame = createItem(Material.EMERALD_BLOCK, 
            plugin.getConfigManager().getMessage("gui.start_game"),
            plugin.getConfigManager().getMessage("gui.start_game_desc"));
        gui.setItem(10, startGame);
        
        // Stop Game button
        ItemStack stopGame = createItem(Material.REDSTONE_BLOCK, 
            plugin.getConfigManager().getMessage("gui.stop_game"),
            plugin.getConfigManager().getMessage("gui.stop_game_desc"));
        gui.setItem(12, stopGame);
        
        // Reload Config button
        ItemStack reloadConfig = createItem(Material.COMMAND_BLOCK, 
            plugin.getConfigManager().getMessage("gui.reload_config"),
            plugin.getConfigManager().getMessage("gui.reload_config_desc"));
        gui.setItem(14, reloadConfig);
        
        // Game Status
        ItemStack status = createGameStatusItem();
        gui.setItem(16, status);
        
        // Player List
        ItemStack playerList = createPlayerListItem();
        gui.setItem(22, playerList);
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui);
        
        player.openInventory(gui);
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            if (lore.length > 0) {
                List<String> loreList = Arrays.asList(lore);
                loreList.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createGameStatusItem() {
        GameState state = plugin.getGameManager().getCurrentState();
        Material material;
        String statusText;
        
        switch (state) {
            case WAITING:
                material = Material.GRAY_WOOL;
                statusText = plugin.getConfigManager().getMessage("status.waiting");
                break;
            case STARTING:
                material = Material.YELLOW_WOOL;
                statusText = plugin.getConfigManager().getMessage("status.countdown", "%time%", "...");
                break;
            case ACTIVE:
            case PVP_ENABLED:
                material = Material.GREEN_WOOL;
                statusText = plugin.getConfigManager().getMessage("status.active");
                break;
            default:
                material = Material.RED_WOOL;
                statusText = "Unknown";
                break;
        }
        
        String pvpStatus = plugin.getGameManager().isPvpEnabled() ? 
            plugin.getConfigManager().getMessage("status.pvp_enabled") : 
            plugin.getConfigManager().getMessage("status.pvp_disabled");
        
        return createItem(material, 
            plugin.getConfigManager().getMessage("gui.game_status"),
            plugin.getConfigManager().getMessage("gui.game_status_desc"),
            "&7Status: " + statusText,
            "&7Players Alive: &f" + plugin.getGameManager().getAlivePlayerCount(),
            "&7Spectators: &f" + plugin.getGameManager().getSpectators().size(),
            "&7PvP: " + pvpStatus,
            "&7Lava Level: &f" + plugin.getLavaManager().getCurrentLevel());
    }
    
    private ItemStack createPlayerListItem() {
        int aliveCount = plugin.getGameManager().getAlivePlayerCount();
        int spectatorCount = plugin.getGameManager().getSpectators().size();
        
        return createItem(Material.PLAYER_HEAD, 
            plugin.getConfigManager().getMessage("gui.player_list"),
            plugin.getConfigManager().getMessage("gui.player_list_desc"),
            "&7Alive Players: &a" + aliveCount,
            "&7Spectators: &e" + spectatorCount,
            "&7Total Online: &f" + Bukkit.getOnlinePlayers().size());
    }
    
    private void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    public boolean isAdminGUI(Inventory inventory) {
        if (inventory.getSize() != 27) return false;
        
        // Check if the inventory holder is null (custom inventory)
        if (inventory.getHolder() != null) return false;
        
        // For newer versions, we can check the inventory type or use other methods
        // Since getTitle() is deprecated in newer versions, we'll use a different approach
        try {
            // Try to access the title through reflection or other means
            // For now, we'll just check size and holder
            return true; // This is our custom GUI if size is 27 and holder is null
        } catch (Exception e) {
            return false;
        }
    }
    
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case 10: // Start Game
                if (player.hasPermission("lavarise.start")) {
                    if (plugin.getGameManager().isGameActive()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getMessage("general.prefix") + 
                            plugin.getConfigManager().getMessage("game.already_running")));
                    } else {
                        if (plugin.getGameManager().startGame()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                plugin.getConfigManager().getMessage("general.prefix") + 
                                plugin.getConfigManager().getMessage("game.started")));
                            player.closeInventory();
                        } else {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                plugin.getConfigManager().getMessage("general.prefix") + 
                                plugin.getConfigManager().getMessage("game.not_enough_players", 
                                    "%min_players%", String.valueOf(plugin.getConfigManager().getMinPlayers()))));
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessage("general.prefix") + 
                        plugin.getConfigManager().getMessage("general.no_permission")));
                }
                break;
                
            case 12: // Stop Game
                if (player.hasPermission("lavarise.stop")) {
                    if (plugin.getGameManager().isGameActive()) {
                        plugin.getGameManager().stopGame();
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getMessage("general.prefix") + 
                            plugin.getConfigManager().getMessage("game.game_ended")));
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getMessage("general.prefix") + 
                            plugin.getConfigManager().getMessage("game.not_running")));
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessage("general.prefix") + 
                        plugin.getConfigManager().getMessage("general.no_permission")));
                }
                break;
                
            case 14: // Reload Config
                if (player.hasPermission("lavarise.reload")) {
                    plugin.getConfigManager().reloadConfigs();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessage("general.prefix") + 
                        plugin.getConfigManager().getMessage("general.config_reloaded")));
                    // Refresh GUI
                    openGUI(player);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessage("general.prefix") + 
                        plugin.getConfigManager().getMessage("general.no_permission")));
                }
                break;
                
            case 16: // Game Status (refresh)
                openGUI(player);
                break;
                
            case 22: // Player List (could open detailed view in future)
                // For now, just refresh
                openGUI(player);
                break;
        }
    }
}
