package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpZ1Command implements CommandExecutor {
    private final Main plugin;
    
    public PvpZ1Command(Main plugin) {
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
        
        plugin.getZoneManager().setPos1(player.getLocation());
        player.sendMessage("§aПервая точка PvP зоны установлена!");
        
        return true;
    }
}