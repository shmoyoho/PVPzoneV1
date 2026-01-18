package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PvpZoneCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PvpZoneCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
                    return true;
                }
                createZone(sender, args);
                break;

            case "list":
                listZones(sender);
                break;

            case "delete":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
                    return true;
                }
                deleteZone(sender, args);
                break;

            case "info":
                zoneInfo(sender, args);
                break;

            case "bonuses":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
                    return true;
                }
                toggleBonuses(sender, args);
                break;

            case "kit":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
                    return true;
                }
                handleZoneKit(sender, args);
                break;

            case "reload":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
                    return true;
                }
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "Плагин перезагружен!");
                break;

            case "status":
                showStatus(sender);
                break;

            case "help":
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void createZone(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone create <название>");
            return;
        }

        Player player = (Player) sender;
        String zoneName = args[1];

        // Проверяем временные точки
        var config = plugin.getConfig();

        if (!config.contains("temp.pos1") || !config.contains("temp.pos2")) {
            player.sendMessage(ChatColor.RED + "Сначала установите обе точки зоны!");
            player.sendMessage(ChatColor.YELLOW + "Используйте: /pvpz1 и /pvpz2");
            return;
        }

        // Загружаем точки из временного конфига
        Location pos1 = loadLocationFromConfig("temp.pos1");
        Location pos2 = loadLocationFromConfig("temp.pos2");

        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "Ошибка загрузки точек зоны!");
            return;
        }

        // Проверяем, что точки в одном мире
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "Точки должны быть в одном мире!");
            return;
        }

        // Создаем зону с включенными бонусами по умолчанию
        boolean success = plugin.getZoneManager().createZone(zoneName, pos1, pos2, true);

        if (success) {
            player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
            player.sendMessage(ChatColor.GREEN + "PvP зона '" + zoneName + "' успешно создана!");

            // Показываем информацию о зоне
            ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);
            if (zone != null) {
                Location zonePos1 = zone.getPos1();
                Location zonePos2 = zone.getPos2();

                double minX = Math.min(zonePos1.getX(), zonePos2.getX());
                double maxX = Math.max(zonePos1.getX(), zonePos2.getX());
                double minZ = Math.min(zonePos1.getZ(), zonePos2.getZ());
                double maxZ = Math.max(zonePos1.getZ(), zonePos2.getZ());

                double width = maxX - minX;
                double length = maxZ - minZ;

                player.sendMessage(ChatColor.GRAY + "Размеры зоны:");
                player.sendMessage(ChatColor.YELLOW + "  Ширина (X): " + String.format("%.1f", width) + " блоков");
                player.sendMessage(ChatColor.YELLOW + "  Длина (Z): " + String.format("%.1f", length) + " блоков");
                player.sendMessage(ChatColor.YELLOW + "  Высота: от 0 до " + zonePos1.getWorld().getMaxHeight());
                player.sendMessage(ChatColor.YELLOW + "  Мир: " + zonePos1.getWorld().getName());
                player.sendMessage(ChatColor.YELLOW + "  Бонусы: " +
                        (zone.isBonusesEnabled() ? ChatColor.GREEN + "ВКЛ" : ChatColor.RED + "ВЫКЛ"));
                player.sendMessage(ChatColor.YELLOW + "  Набор по умолчанию: " + ChatColor.WHITE + zone.getKitName());
            }

            player.sendMessage(ChatColor.GOLD + "══════════════════════════════");

            // Очищаем временные точки
            config.set("temp.pos1", null);
            config.set("temp.pos2", null);
            plugin.saveConfig();

        } else {
            player.sendMessage(ChatColor.RED + "Зона с таким названием уже существует!");
        }
    }

    private void listZones(CommandSender sender) {
        var zoneManager = plugin.getZoneManager();
        var zones = zoneManager.getAllZones();

        if (zones.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Нет созданных PvP зон");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "══ Список PvP зон ══");

        for (ZoneManager.PvpZone zone : zones) {
            Location pos1 = zone.getPos1();
            Location pos2 = zone.getPos2();

            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            double width = maxX - minX;
            double length = maxZ - minZ;

            String status = zone.isBonusesEnabled() ?
                    ChatColor.GREEN + "Бонусы: ВКЛ" :
                    ChatColor.RED + "Бонусы: ВЫКЛ";

            sender.sendMessage(ChatColor.YELLOW + "• " + zone.getName());
            sender.sendMessage(ChatColor.GRAY + "  Размер: " +
                    ChatColor.WHITE + String.format("%.0f", width) + "×" +
                    String.format("%.0f", length) + ChatColor.GRAY + " блоков");
            sender.sendMessage(ChatColor.GRAY + "  Набор: " + ChatColor.WHITE + zone.getKitName());
            sender.sendMessage(ChatColor.GRAY + "  " + status);
            sender.sendMessage("");
        }

        sender.sendMessage(ChatColor.GRAY + "Всего зон: " + ChatColor.YELLOW + zones.size());
    }

    private void deleteZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone delete <название>");
            return;
        }

        String zoneName = args[1];
        boolean success = plugin.getZoneManager().removeZone(zoneName);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Зона '" + zoneName + "' удалена!");
        } else {
            sender.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена!");
        }
    }

    private void zoneInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone info <название>");
            return;
        }

        String zoneName = args[1];
        ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);

        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена!");
            return;
        }

        Location pos1 = zone.getPos1();
        Location pos2 = zone.getPos2();

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double width = maxX - minX;
        double length = maxZ - minZ;
        double area = width * length;

        sender.sendMessage(ChatColor.GOLD + "══ Информация о зоне '" + zoneName + "' ══");
        sender.sendMessage(ChatColor.GRAY + "Координаты:");
        sender.sendMessage(ChatColor.YELLOW + "  X: " + String.format("%.1f", minX) + " → " + String.format("%.1f", maxX));
        sender.sendMessage(ChatColor.YELLOW + "  Z: " + String.format("%.1f", minZ) + " → " + String.format("%.1f", maxZ));
        sender.sendMessage(ChatColor.YELLOW + "  Y: 0 → " + pos1.getWorld().getMaxHeight());

        sender.sendMessage(ChatColor.GRAY + "Размеры:");
        sender.sendMessage(ChatColor.YELLOW + "  Ширина: " + String.format("%.1f", width) + " блоков");
        sender.sendMessage(ChatColor.YELLOW + "  Длина: " + String.format("%.1f", length) + " блоков");
        sender.sendMessage(ChatColor.YELLOW + "  Площадь: " + String.format("%.0f", area) + " блоков²");

        sender.sendMessage(ChatColor.GRAY + "Настройки:");
        sender.sendMessage(ChatColor.YELLOW + "  Набор: " + ChatColor.WHITE + zone.getKitName());
        sender.sendMessage(ChatColor.YELLOW + "  Бонусы: " +
                (zone.isBonusesEnabled() ? ChatColor.GREEN + "ВКЛ" : ChatColor.RED + "ВЫКЛ"));
        sender.sendMessage(ChatColor.YELLOW + "  Мир: " + pos1.getWorld().getName());

        if (zone.getCenter() != null) {
            Location center = zone.getCenter();
            sender.sendMessage(ChatColor.GRAY + "Центр зоны:");
            sender.sendMessage(ChatColor.YELLOW + "  X: " + String.format("%.1f", center.getX()));
            sender.sendMessage(ChatColor.YELLOW + "  Y: " + String.format("%.1f", center.getY()));
            sender.sendMessage(ChatColor.YELLOW + "  Z: " + String.format("%.1f", center.getZ()));
        }
    }

    private void toggleBonuses(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone bonuses <название> <on|off>");
            return;
        }

        String zoneName = args[1];
        String action = args[2].toLowerCase();

        ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена!");
            return;
        }

        if (action.equals("on")) {
            zone.setBonusesEnabled(true);
            sender.sendMessage(ChatColor.GREEN + "Бонусы в зоне '" + zoneName + "' включены!");
        } else if (action.equals("off")) {
            zone.setBonusesEnabled(false);
            sender.sendMessage(ChatColor.YELLOW + "Бонусы в зоне '" + zoneName + "' отключены!");
        } else {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone bonuses <название> <on|off>");
            return;
        }

        plugin.getZoneManager().saveZones();
    }

    private void handleZoneKit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Использование: /pvpzone kit <set|save|info> <зона> [название_набора]");
            return;
        }

        // Используем PvpZoneKitCommand
        String[] newArgs = new String[args.length - 1];
        newArgs[0] = args[1]; // subCommand (set/save/info)
        newArgs[1] = args[2]; // zoneName

        if (args.length > 3) {
            newArgs[2] = args[3]; // kitName (для set)
        }

        PvpZoneKitCommand kitCommand = new PvpZoneKitCommand(plugin);
        kitCommand.onCommand(sender, null, "pvpzone", newArgs);
    }

    private void showStatus(CommandSender sender) {
        var zoneManager = plugin.getZoneManager();

        sender.sendMessage(ChatColor.GOLD + "══ Статус PvP Zone ══");
        sender.sendMessage(ChatColor.GRAY + "Зон создано: " + ChatColor.YELLOW +
                zoneManager.getAllZones().size());
        sender.sendMessage(ChatColor.GRAY + "Игроков в зонах: " + ChatColor.YELLOW +
                zoneManager.getPlayersInZoneCount());
        sender.sendMessage(ChatColor.GRAY + "Основной PvP набор: " +
                (plugin.getKitManager().isKitSet() ?
                        ChatColor.GREEN + "Установлен" :
                        ChatColor.RED + "Не установлен"));
        sender.sendMessage(ChatColor.GRAY + "Магазин: " +
                ChatColor.YELLOW + "Работает");
        sender.sendMessage(ChatColor.GRAY + "Игроков в базе: " + ChatColor.YELLOW +
                plugin.getPlayerDataManager().getLoadedPlayersCount());
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "══ PvP Zone - Помощь ══");

        if (sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "Админ команды:");
            sender.sendMessage(ChatColor.GRAY + "  /pvpz1 " + ChatColor.WHITE + "- Установить первую точку");
            sender.sendMessage(ChatColor.GRAY + "  /pvpz2 " + ChatColor.WHITE + "- Установить вторую точку");
            sender.sendMessage(ChatColor.GRAY + "  /pvpkit " + ChatColor.WHITE + "- Установить основной PvP набор");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone create <название> " +
                    ChatColor.WHITE + "- Создать зону");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone delete <название> " +
                    ChatColor.WHITE + "- Удалить зону");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone bonuses <название> <on|off> " +
                    ChatColor.WHITE + "- Вкл/выкл бонусы");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone kit set <зона> <название> " +
                    ChatColor.WHITE + "- Установить набор для зоны");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone kit save <зона> " +
                    ChatColor.WHITE + "- Сохранить набор для зоны");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone kit info <зона> " +
                    ChatColor.WHITE + "- Информация о наборе зоны");
            sender.sendMessage(ChatColor.GRAY + "  /pvpzone reload " +
                    ChatColor.WHITE + "- Перезагрузить плагин");
            sender.sendMessage("");
        }

        sender.sendMessage(ChatColor.YELLOW + "Основные команды:");
        sender.sendMessage(ChatColor.GRAY + "  /pvptop [страница] " +
                ChatColor.WHITE + "- Топ игроков");
        sender.sendMessage(ChatColor.GRAY + "  /pvpshop " +
                ChatColor.WHITE + "- Магазин бонусов");
        sender.sendMessage(ChatColor.GRAY + "  /pvpzone list " +
                ChatColor.WHITE + "- Список зон");
        sender.sendMessage(ChatColor.GRAY + "  /pvpzone info <название> " +
                ChatColor.WHITE + "- Информация о зоне");
        sender.sendMessage(ChatColor.GRAY + "  /pvpzone status " +
                ChatColor.WHITE + "- Статус плагина");
        sender.sendMessage(ChatColor.GRAY + "  /pvpzone help " +
                ChatColor.WHITE + "- Эта справка");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("create");
            suggestions.add("list");
            suggestions.add("delete");
            suggestions.add("info");
            suggestions.add("bonuses");
            suggestions.add("kit");
            suggestions.add("reload");
            suggestions.add("status");
            suggestions.add("help");

            if (!sender.hasPermission("pvpzone.admin")) {
                suggestions.remove("create");
                suggestions.remove("delete");
                suggestions.remove("bonuses");
                suggestions.remove("kit");
                suggestions.remove("reload");
            }

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("delete") || subCommand.equals("info") ||
                    subCommand.equals("bonuses") || subCommand.equals("kit")) {

                suggestions.addAll(plugin.getZoneManager().getZoneNames());
            }

        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("bonuses")) {
                suggestions.add("on");
                suggestions.add("off");
            } else if (subCommand.equals("kit")) {
                suggestions.add("set");
                suggestions.add("save");
                suggestions.add("info");
            }

        } else if (args.length == 4 && args[0].equalsIgnoreCase("kit") &&
                args[2].equalsIgnoreCase("set")) {
            // Предлагаем названия наборов
            suggestions.add("default");
            suggestions.add("arena");
            suggestions.add("duel");
            suggestions.add("tournament");
        }

        return suggestions;
    }
}