package net.fliuxx.lavaRise.listeners;

import net.fliuxx.lavaRise.LavaRise;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles player-related events for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class PlayerListener implements Listener {
    
    private final LavaRise plugin;
    
    public PlayerListener(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If a game is active, add player as spectator
        if (plugin.getGameManager().isGameActive()) {
            plugin.getGameManager().addSpectator(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // If player was in game, eliminate them
        if (plugin.getGameManager().getAlivePlayers().contains(player.getUniqueId())) {
            plugin.getGameManager().eliminatePlayer(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Handle death in game
        if (plugin.getGameManager().isGameActive()) {
            if (plugin.getGameManager().getAlivePlayers().contains(player.getUniqueId())) {
                // Clear drops in game
                event.getDrops().clear();
                event.setDroppedExp(0);
                
                // Eliminate player
                plugin.getGameManager().eliminatePlayer(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // If player died in game, respawn them as spectator in game world
        if (plugin.getGameManager().isGameActive() && 
            plugin.getGameManager().getSpectators().contains(player.getUniqueId())) {
            
            if (plugin.getWorldManager().getGameWorld() != null) {
                event.setRespawnLocation(plugin.getWorldManager().getGameWorld().getSpawnLocation());
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Prevent damage to spectators
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        
        // Check PvP rules
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            
            if (plugin.getGameManager().isGameActive() && !plugin.getGameManager().isPvpEnabled()) {
                // Cancel PvP damage if PvP is not enabled yet
                event.setCancelled(true);
                return;
            }
        }
        
        // Handle lava damage - if player is in lava and would die, eliminate them
        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            if (plugin.getGameManager().isGameActive() && 
                plugin.getGameManager().getAlivePlayers().contains(player.getUniqueId())) {
                
                // Check if this damage would kill the player
                if (player.getHealth() - event.getDamage() <= 0) {
                    event.setCancelled(true);
                    plugin.getPlayerManager().handlePlayerDeath(player);
                }
            }
        }
    }
}
