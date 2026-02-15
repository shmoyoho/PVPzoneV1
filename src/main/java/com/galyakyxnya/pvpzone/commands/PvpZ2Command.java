package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpZ2Command implements CommandExecutor {
    private final Main plugin;

    public PvpZ2Command(Main plugin) {
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

        // Сохраняем во временный конфиг
        plugin.getConfig().set("temp.pos2.world", location.getWorld().getName());
        plugin.getConfig().set("temp.pos2.x", location.getX());
        plugin.getConfig().set("temp.pos2.y", location.getY()); // Сохраняем Y для отображения
        plugin.getConfig().set("temp.pos2.z", location.getZ());
        plugin.saveConfig();

        player.sendMessage(Lang.get(plugin, "zone_enter_title"));
        player.sendMessage(Lang.get(plugin, "point_second_ok"));
        player.sendMessage(Lang.get(plugin, "point_coords", "%x%", String.format("%.1f", location.getX()), "%y%", String.format("%.1f", location.getY()), "%z%", String.format("%.1f", location.getZ())));
        player.sendMessage(Lang.get(plugin, "point_world", "%world%", location.getWorld().getName()));
        player.sendMessage(Lang.get(plugin, "zone_enter_title"));

        if (!plugin.getConfig().contains("temp.pos1")) {
            player.sendMessage(Lang.get(plugin, "point_warning_no_first"));
            player.sendMessage(Lang.get(plugin, "point_use_pvpz1"));
        } else {
            Location pos1 = loadLocationFromConfig("temp.pos1");
            if (pos1 != null) {
                player.sendMessage(Lang.get(plugin, "point_distance_title"));
                player.sendMessage(Lang.get(plugin, "point_distance_x", "%value%", String.format("%.1f", Math.abs(location.getX() - pos1.getX()))));
                player.sendMessage(Lang.get(plugin, "point_distance_z", "%value%", String.format("%.1f", Math.abs(location.getZ() - pos1.getZ()))));
                player.sendMessage(Lang.get(plugin, "point_height_full"));
            }
            player.sendMessage(Lang.get(plugin, "point_next_create_only"));
        }

        return true;
    }

    private Location loadLocationFromConfig(String path) {
        var config = plugin.getConfig();

        if (!config.contains(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        var world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");

        return new Location(world, x, y, z);
    }
}