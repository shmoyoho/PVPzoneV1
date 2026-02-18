package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Location;
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
            sender.sendMessage(Lang.get(plugin, "point_only_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage(Lang.get(plugin, "point_no_permission"));
            return true;
        }

        Location location = player.getLocation();

        plugin.getConfig().set("temp.pos1.world", location.getWorld().getName());
        plugin.getConfig().set("temp.pos1.x", location.getX());
        plugin.getConfig().set("temp.pos1.y", location.getY());
        plugin.getConfig().set("temp.pos1.z", location.getZ());
        plugin.saveConfig();

        player.sendMessage(Lang.get(plugin, "zone_enter_title"));
        player.sendMessage(Lang.get(plugin, "point_first_ok"));
        player.sendMessage(Lang.get(plugin, "point_coords", "%x%", String.format("%.1f", location.getX()), "%y%", String.format("%.1f", location.getY()), "%z%", String.format("%.1f", location.getZ())));
        player.sendMessage(Lang.get(plugin, "point_world", "%world%", location.getWorld().getName()));
        player.sendMessage(Lang.get(plugin, "zone_enter_title"));
        player.sendMessage(Lang.get(plugin, "point_next_pvpz2"));
        player.sendMessage(Lang.get(plugin, "point_next_create"));

        return true;
    }
}