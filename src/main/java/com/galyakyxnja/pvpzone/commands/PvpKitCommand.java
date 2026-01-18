package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpKitCommand implements CommandExecutor {
    private final Main plugin;
    
    public PvpKitCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭту команду может использовать только игрок!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }
        
        plugin.getKitManager().saveKitFromPlayer(player);
        player.sendMessage("§aPvP набор сохранен!");
        
        return true;
    }
}