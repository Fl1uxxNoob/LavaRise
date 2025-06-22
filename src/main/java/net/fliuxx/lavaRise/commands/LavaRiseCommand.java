package net.fliuxx.lavaRise.commands;

import net.fliuxx.lavaRise.LavaRise;
import net.fliuxx.lavaRise.gui.AdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for LavaRise plugin
 * 
 * @author Fl1uxxNoob
 */
public class LavaRiseCommand implements CommandExecutor, TabCompleter {
    
    private final LavaRise plugin;
    
    public LavaRiseCommand(LavaRise plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender);
                
            case "stop":
                return handleStop(sender);
                
            case "gui":
                return handleGUI(sender);
                
            case "spectate":
                return handleSpectate(sender);
                
            case "reload":
                return handleReload(sender);
                
            case "setupworld":
                return handleSetupWorld(sender);
                
            case "arenas":
                return handleArenas(sender);
                
            case "help":
                sendHelp(sender);
                return true;
                
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("general.prefix") + 
                    plugin.getConfigManager().getMessage("general.unknown_command")));
                return true;
        }
    }
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("lavarise.start")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("game.already_running")));
            return true;
        }
        
        if (plugin.getGameManager().startGame()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("game.started")));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("game.not_enough_players", 
                    "%min_players%", String.valueOf(plugin.getConfigManager().getMinPlayers()))));
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("lavarise.stop")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        if (!plugin.getGameManager().isGameActive()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("game.not_running")));
            return true;
        }
        
        plugin.getGameManager().stopGame();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("general.prefix") + 
            plugin.getConfigManager().getMessage("game.game_ended")));
        
        return true;
    }
    
    private boolean handleGUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.player_only")));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("lavarise.gui")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        AdminGUI gui = new AdminGUI(plugin);
        gui.openGUI(player);
        
        return true;
    }
    
    private boolean handleSpectate(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.player_only")));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("lavarise.spectate")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        plugin.getGameManager().addSpectator(player);
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("lavarise.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("general.prefix") + 
            plugin.getConfigManager().getMessage("general.config_reloaded")));
        
        return true;
    }
    
    private boolean handleSetupWorld(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.player_only")));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("lavarise.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        if (plugin.getGameManager().isGameActive()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                "&cCannot setup arenas while a game is running!"));
            return true;
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("general.prefix") + 
            "&eStarting arena setup process..."));
        
        // Start arena setup on main thread (chunk operations must be synchronous)
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = plugin.getArenaManager().setupNewArenas(player);
            if (!success) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("general.prefix") + 
                    "&cFailed to setup arenas!"));
            }
        });
        
        return true;
    }
    
    private boolean handleArenas(CommandSender sender) {
        if (!sender.hasPermission("lavarise.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("general.prefix") + 
                plugin.getConfigManager().getMessage("general.no_permission")));
            return true;
        }
        
        int available = plugin.getArenaManager().getAvailableArenasCount();
        int used = plugin.getArenaManager().getUsedArenasCount();
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("general.prefix") + 
            "&eArena Status:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Available: &a" + available));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Used: &c" + used));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Total: &f" + (available + used)));
        
        if (available <= 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&cWarning: Low arena count! Use /lavarise setupworld to create more."));
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("help.header")));
        
        for (String line : plugin.getConfigManager().getMessage("help.commands").split("\n")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getMessage("help.footer")));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("start", "stop", "gui", "spectate", "reload", "setupworld", "arenas", "help");
            String partial = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        }
        
        return completions;
    }
}
