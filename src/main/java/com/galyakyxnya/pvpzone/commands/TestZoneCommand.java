package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestZoneCommand implements CommandExecutor {
    private final Main plugin;

    public TestZoneCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        var zoneManager = plugin.getZoneManager();
        var zone = zoneManager.findZoneAtLocation(loc);

        if (zone != null) {
            player.sendMessage(ChatColor.GREEN + "Вы находитесь в зоне: " + zone.getName());
            player.sendMessage(ChatColor.GRAY + "Координаты: X=" + loc.getX() + " Y=" + loc.getY() + " Z=" + loc.getZ());
        } else {
            player.sendMessage(ChatColor.RED + "Вы НЕ находитесь в зоне!");
            player.sendMessage(ChatColor.GRAY + "Координаты: X=" + loc.getX() + " Y=" + loc.getY() + " Z=" + loc.getZ());
        }

        return true;
    }
}