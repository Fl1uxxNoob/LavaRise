package net.fliuxx.lavaRise.managers;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.tasks.CountdownTask;
import net.fliuxx.lavaRise.tasks.LavaRiseTask;
import net.fliuxx.lavaRise.utils.GameState;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * Manages the overall game state and logic for LavaRise
 * 
 * @author Fl1uxxNoob
 */
public class GameManager {
    
    private final LavaRise plugin;
    private GameState currentState;
    private final Set<UUID> alivePlayers;
    private final Set<UUID> spectators;
    
    private BossBar bossBar;
    private Scoreboard scoreboard;
    private Objective objective;
    
    private BukkitTask countdownTask;
    private BukkitTask lavaTask;
    private BukkitTask scoreboardTask;
    
    private int gameTime;
    private boolean pvpEnabled;
    
    public GameManager(LavaRise plugin) {
        this.plugin = plugin;
        this.currentState = GameState.WAITING;
        this.alivePlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.gameTime = 0;
        this.pvpEnabled = false;
        
        setupScoreboard();
        setupBossBar();
    }
    
    private void setupScoreboard() {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;
        
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("lavarise", "dummy", 
            ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessage("scoreboard.title")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    private void setupBossBar() {
        if (!plugin.getConfigManager().isBossBarEnabled()) return;
        
        bossBar = Bukkit.createBossBar("LavaRise", BarColor.RED, BarStyle.SOLID);
    }
    
    public boolean startGame() {
        if (currentState != GameState.WAITING) {
            return false;
        }
        
        // Check minimum players
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.size() < plugin.getConfigManager().getMinPlayers()) {
            return false;
        }
        
        // Reset all game state
        resetGameState();
        
        // Initialize game
        currentState = GameState.STARTING;
        alivePlayers.clear();
        spectators.clear();
        gameTime = 0;
        pvpEnabled = false;
        
        // Add all online players to the game
        for (Player player : onlinePlayers) {
            alivePlayers.add(player.getUniqueId());
            if (bossBar != null) {
                bossBar.addPlayer(player);
            }
            if (scoreboard != null) {
                player.setScoreboard(scoreboard);
            }
        }
        
        // Get next available arena
        if (!plugin.getWorldManager().selectNextArena()) {
            currentState = GameState.WAITING;
            return false;
        }
        
        // Teleport players to game arena
        plugin.getPlayerManager().teleportPlayersToGame();
        
        // Start initial countdown
        startInitialCountdown();
        
        // Start scoreboard updates
        startScoreboardUpdates();
        
        // Broadcast game start
        broadcastMessage(plugin.getConfigManager().getMessage("game.started"));
        
        return true;
    }
    
    private void resetGameState() {
        // Reset lava manager
        plugin.getLavaManager().resetLavaLevel();
        
        // Cancel any running tasks
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (lavaTask != null) {
            lavaTask.cancel();
            lavaTask = null;
        }
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
        
        // Reset boss bar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setTitle("");
            bossBar.setVisible(false);
        }
    }
    
    public void stopGame() {
        if (currentState == GameState.WAITING) return;
        
        // Cancel all tasks
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (lavaTask != null) {
            lavaTask.cancel();
            lavaTask = null;
        }
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
        
        // Mark current arena as used and clean it up
        plugin.getArenaManager().finishCurrentGame();
        
        // Reset game state
        currentState = GameState.WAITING;
        gameTime = 0;
        pvpEnabled = false;
        
        // Teleport all players back to spawn
        plugin.getPlayerManager().teleportAllToSpawn();
        
        // Clear boss bar and scoreboard
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setTitle("");
            bossBar.setVisible(false);
        }
        
        // Reset scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        
        // Clear player sets
        alivePlayers.clear();
        spectators.clear();
        
        // Clean up world
        plugin.getWorldManager().cleanup();
        
        // Broadcast game end
        broadcastMessage(plugin.getConfigManager().getMessage("game.game_ended"));
    }
    
    private void startInitialCountdown() {
        int countdown = plugin.getConfigManager().getInitialCountdown();
        
        countdownTask = new CountdownTask(plugin, countdown, 
            () -> {
                // On countdown finish - start lava rising
                currentState = GameState.ACTIVE;
                
                // Clear boss bar countdown message
                if (bossBar != null) {
                    bossBar.setTitle("");
                    bossBar.setVisible(false);
                }
                
                startLavaRising();
                startPvpCountdown();
            },
            (timeLeft) -> {
                // Update boss bar and broadcast
                if (bossBar != null) {
                    bossBar.setVisible(true);
                    updateBossBar(plugin.getConfigManager().getMessage("countdown.game_start", "%time%", formatTime(timeLeft)));
                }
                
                if (timeLeft <= 10 || timeLeft % 10 == 0) {
                    broadcastMessage(plugin.getConfigManager().getMessage("game.starting", "%seconds%", String.valueOf(timeLeft)));
                }
            }
        ).runTaskTimer(plugin, 0L, 20L);
    }
    
    private void startLavaRising() {
        lavaTask = new LavaRiseTask(plugin).runTaskTimer(plugin, 0L, 
            plugin.getConfigManager().getLavaRiseInterval() * 20L);
        
        broadcastMessage(plugin.getConfigManager().getMessage("game.lava_rising", 
            "%level%", String.valueOf(plugin.getLavaManager().getCurrentLevel())));
    }
    
    private void startPvpCountdown() {
        int pvpCountdown = plugin.getConfigManager().getPvpCountdown();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pvpEnabled = true;
            currentState = GameState.PVP_ENABLED;
            broadcastMessage(plugin.getConfigManager().getMessage("game.pvp_enabled"));
        }, pvpCountdown * 20L);
    }
    
    private void startScoreboardUpdates() {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;
        
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, 
            this::updateScoreboard, 
            0L, 
            plugin.getConfigManager().getScoreboardUpdateInterval());
    }
    
    private void updateScoreboard() {
        if (objective == null) return;
        
        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        int score = 10;
        
        // Players alive
        String playersText = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("scoreboard.players_alive", "%count%", String.valueOf(alivePlayers.size())));
        objective.getScore(playersText).setScore(score--);
        
        // Lava level
        String lavaText = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("scoreboard.lava_level", "%level%", String.valueOf(plugin.getLavaManager().getCurrentLevel())));
        objective.getScore(lavaText).setScore(score--);
        
        // Game time
        String timeText = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("scoreboard.game_time", "%time%", formatTime(gameTime)));
        objective.getScore(timeText).setScore(score--);
        
        // PvP status
        String pvpStatus = pvpEnabled ? 
            plugin.getConfigManager().getMessage("status.pvp_enabled") : 
            plugin.getConfigManager().getMessage("status.pvp_disabled");
        String pvpText = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("scoreboard.pvp_status", "%status%", pvpStatus));
        objective.getScore(pvpText).setScore(score--);
        
        // Border size
        World gameWorld = plugin.getWorldManager().getGameWorld();
        if (gameWorld != null) {
            String borderText = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("scoreboard.border_size", "%size%", String.valueOf((int)gameWorld.getWorldBorder().getSize())));
            objective.getScore(borderText).setScore(score--);
        }
        
        gameTime++;
    }
    
    private void updateBossBar(String message) {
        if (bossBar == null) return;
        
        bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    public void eliminatePlayer(Player player) {
        if (!alivePlayers.contains(player.getUniqueId())) return;
        
        alivePlayers.remove(player.getUniqueId());
        spectators.add(player.getUniqueId());
        
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Send elimination message
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("game.eliminated")));
        
        // Check for winner
        checkForWinner();
    }
    
    private void checkForWinner() {
        if (alivePlayers.size() == 1) {
            // We have a winner!
            Player winner = Bukkit.getPlayer(alivePlayers.iterator().next());
            if (winner != null) {
                broadcastMessage(plugin.getConfigManager().getMessage("game.winner", "%player%", winner.getName()));
            }
            
            // End game after a delay
            currentState = GameState.ENDING;
            Bukkit.getScheduler().runTaskLater(plugin, this::stopGame, 100L); // 5 seconds
        } else if (alivePlayers.isEmpty()) {
            // No winners (shouldn't happen but just in case)
            stopGame();
        }
    }
    
    public void addSpectator(Player player) {
        if (currentState == GameState.WAITING) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("game.not_running")));
            return;
        }
        
        spectators.add(player.getUniqueId());
        
        // Teleport to game world
        World gameWorld = plugin.getWorldManager().getGameWorld();
        if (gameWorld != null) {
            player.teleport(gameWorld.getSpawnLocation());
        }
        
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Add to boss bar and scoreboard
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("game.joined_spectator")));
    }
    
    private void broadcastMessage(String message) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("general.prefix") + message);
            
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(formattedMessage);
            }
        }
        
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(formattedMessage);
            }
        }
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
    
    // Getters
    public GameState getCurrentState() {
        return currentState;
    }
    
    public boolean isGameActive() {
        return currentState != GameState.WAITING && currentState != GameState.ENDED;
    }
    
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
    
    public Set<UUID> getAlivePlayers() {
        return new HashSet<>(alivePlayers);
    }
    
    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }
    
    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }
}
