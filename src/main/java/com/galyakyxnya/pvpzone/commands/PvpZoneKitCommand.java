package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PvpZoneKitCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PvpZoneKitCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав!");
            return true;
        }

        if (args.length < 2) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String zoneName = args[1];

        ZoneManager zoneManager = plugin.getZoneManager();
        ZoneManager.PvpZone zone = zoneManager.getZone(zoneName);

        if (zone == null) {
            player.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена!");
            return true;
        }

        switch (subCommand) {
            case "set":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Использование: /pvpzone kit set <зона> <название_набора>");
                    return true;
                }
                String kitName = args[2];
                boolean success = zoneManager.setZoneKit(zoneName, kitName);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "✓ Набор '" + kitName + "' установлен для зоны '" + zoneName + "'");

                    // Проверяем, существует ли набор
                    if (!plugin.getKitManager().isKitSet(kitName)) {
                        player.sendMessage(ChatColor.YELLOW + "⚠ Набор '" + kitName + "' не существует!");
                        player.sendMessage(ChatColor.GRAY + "Сохраните его: " + ChatColor.YELLOW +
                                "/pvpzone kit save " + zoneName + " " + kitName);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "✗ Не удалось установить набор");
                }
                break;

            case "save":
                String saveKitName = zone.getKitName();
                if (args.length >= 3) {
                    saveKitName = args[2]; // Если указано название набора
                }

                boolean saved = plugin.getKitManager().saveKit(saveKitName, player);
                if (saved) {
                    player.sendMessage(ChatColor.GREEN + "✓ Набор '" + saveKitName + "' сохранен!");

                    // Автоматически устанавливаем его для зоны
                    if (!saveKitName.equals(zone.getKitName())) {
                        zoneManager.setZoneKit(zoneName, saveKitName);
                        player.sendMessage(ChatColor.GREEN + "✓ Набор автоматически привязан к зоне!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "✗ Не удалось сохранить набор");
                }
                break;

            case "info":
                String currentKit = zoneManager.getZoneKitName(zoneName);
                boolean kitExists = plugin.getKitManager().isKitSet(currentKit);

                player.sendMessage(ChatColor.GOLD + "══ Набор для зоны '" + zoneName + "' ══");
                player.sendMessage(ChatColor.GRAY + "Текущий набор: " + ChatColor.YELLOW + currentKit);
                player.sendMessage(ChatColor.GRAY + "Набор существует: " +
                        (kitExists ? ChatColor.GREEN + "ДА" : ChatColor.RED + "НЕТ"));

                if (!kitExists && !currentKit.equals("default")) {
                    player.sendMessage(ChatColor.YELLOW + "⚠ Набор не существует!");
                    player.sendMessage(ChatColor.GRAY + "Сохраните его: " + ChatColor.YELLOW +
                            "/pvpzone kit save " + zoneName + " " + currentKit);
                } else if (kitExists) {
                    player.sendMessage(ChatColor.GRAY + "Изменить: " + ChatColor.YELLOW +
                            "/pvpzone kit set " + zoneName + " <новое_название>");
                }
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "══ Управление наборами зон ══");
        player.sendMessage(ChatColor.YELLOW + "/pvpzone kit set <зона> <название>");
        player.sendMessage(ChatColor.GRAY + "  Установить набор для зоны");
        player.sendMessage(ChatColor.YELLOW + "/pvpzone kit save <зона> [название]");
        player.sendMessage(ChatColor.GRAY + "  Сохранить набор из инвентаря");
        player.sendMessage(ChatColor.GRAY + "  (без названия - использует текущий набор зоны)");
        player.sendMessage(ChatColor.YELLOW + "/pvpzone kit info <зона>");
        player.sendMessage(ChatColor.GRAY + "  Информация о наборе зоны");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("set");
            suggestions.add("save");
            suggestions.add("info");

        } else if (args.length == 2) {
            suggestions.addAll(plugin.getZoneManager().getZoneNames());

        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            suggestions.add("default");
            suggestions.add("arena");
            suggestions.add("duel");
            suggestions.add("tournament");
        }

        return suggestions;
    }
}