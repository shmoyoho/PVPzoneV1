package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
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
            sender.sendMessage("§cТолько игрок может использовать эту команду!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage("§cНет прав!");
            return true;
        }

        Location location = player.getLocation();

        // Сохраняем во временный конфиг
        plugin.getConfig().set("temp.pos2.world", location.getWorld().getName());
        plugin.getConfig().set("temp.pos2.x", location.getX());
        plugin.getConfig().set("temp.pos2.y", location.getY()); // Сохраняем Y для отображения
        plugin.getConfig().set("temp.pos2.z", location.getZ());
        plugin.saveConfig();

        player.sendMessage("§a══════════════════════════════");
        player.sendMessage("§aВторая точка установлена!");
        player.sendMessage("§7Координаты: §eX: " + String.format("%.1f", location.getX()) +
                " §7| §eY: " + String.format("%.1f", location.getY()) +
                " §7| §eZ: " + String.format("%.1f", location.getZ()));
        player.sendMessage("§7Мир: §e" + location.getWorld().getName());
        player.sendMessage("§a══════════════════════════════");

        // Проверяем, есть ли первая точка
        if (!plugin.getConfig().contains("temp.pos1")) {
            player.sendMessage("§cПредупреждение: первая точка не установлена!");
            player.sendMessage("§7Используйте §e/pvpz1 §7для установки первой точки");
        } else {
            // Показываем информацию о будущей зоне
            Location pos1 = loadLocationFromConfig("temp.pos1");
            if (pos1 != null) {
                player.sendMessage("§7Расстояние между точками:");
                player.sendMessage("§7По X: §e" + String.format("%.1f", Math.abs(location.getX() - pos1.getX())) + " блоков");
                player.sendMessage("§7По Z: §e" + String.format("%.1f", Math.abs(location.getZ() - pos1.getZ())) + " блоков");
                player.sendMessage("§7Зона будет охватывать всю высоту мира!");
            }
            player.sendMessage("§7Теперь создайте зону: §e/pvpzone create <название>");
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