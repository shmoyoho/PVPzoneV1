package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
                    return true;
                }
                
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "Плагин перезагружен!");
                break;
                
            case "status":
                showStatus(sender);
                break;
                
            case "resetdata":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
                    return true;
                }
                
                if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    // Здесь можно добавить сброс данных
                    sender.sendMessage(ChatColor.YELLOW + "Функция сброса данных в разработке");
                } else {
                    sender.sendMessage(ChatColor.RED + "Используйте: /pvpzone resetdata confirm");
                    sender.sendMessage(ChatColor.RED + "Это действие необратимо!");
                }
                break;
                
            case "setzone":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
                    return true;
                }
                showZoneInfo(sender);
                break;
                
            case "cleardata":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
                    return true;
                }
                
                if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    // Очистка данных из памяти (только для текущей сессии)
                    plugin.getPlayerDataManager().getAllPlayerData().clear();
                    sender.sendMessage(ChatColor.GREEN + "Данные в памяти очищены!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Используйте: /pvpzone cleardata confirm");
                    sender.sendMessage(ChatColor.RED + "Очищает только данные в оперативной памяти!");
                }
                break;
                
            case "help":
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== PvP Zone Plugin ===");
        sender.sendMessage(ChatColor.YELLOW + "/pvpz1 §7- Установить первую точку зоны");
        sender.sendMessage(ChatColor.YELLOW + "/pvpz2 §7- Установить вторую точку зоны");
        sender.sendMessage(ChatColor.YELLOW + "/pvpkit §7- Установить PvP набор");
        sender.sendMessage(ChatColor.YELLOW + "/pvptop [страница] §7- Топ игроков по рейтингу");
        sender.sendMessage(ChatColor.YELLOW + "/pvpshop §7- Магазин бонусов");
        
        if (sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "=== Админ команды ===");
            sender.sendMessage(ChatColor.RED + "/pvpzone reload §7- Перезагрузить плагин");
            sender.sendMessage(ChatColor.RED + "/pvpzone status §7- Статус плагина");
            sender.sendMessage(ChatColor.RED + "/pvpzone setzone §7- Информация о зоне");
            sender.sendMessage(ChatColor.RED + "/pvpzone resetdata confirm §7- Сброс данных (опасно!)");
            sender.sendMessage(ChatColor.RED + "/pvpzone cleardata confirm §7- Очистка данных в памяти");
            sender.sendMessage(ChatColor.RED + "/pvpzone help §7- Показать это сообщение");
        }
    }
    
    private void showStatus(CommandSender sender) {
        if (!sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Статус PvP Zone ===");
        
        boolean zoneDefined = plugin.getZoneManager().isZoneDefined();
        sender.sendMessage(ChatColor.GRAY + "Зона установлена: " + 
                          (zoneDefined ? ChatColor.GREEN + "Да" : ChatColor.RED + "Нет"));
        
        boolean kitSet = plugin.getKitManager().isKitSet();
        sender.sendMessage(ChatColor.GRAY + "PvP набор: " + 
                          (kitSet ? ChatColor.GREEN + "Установлен" : ChatColor.RED + "Не установлен"));
        
        int playersInZone = plugin.getZoneManager().getPlayersInZoneCount();
        // Исправлено: явное преобразование числа в строку
        sender.sendMessage(ChatColor.GRAY + "Игроков в зоне: " + ChatColor.YELLOW + String.valueOf(playersInZone));
        
        int loadedPlayers = plugin.getPlayerDataManager().getLoadedPlayersCount();
        // Исправлено: явное преобразование числа в строку
        sender.sendMessage(ChatColor.GRAY + "Загружено данных: " + ChatColor.YELLOW + String.valueOf(loadedPlayers));
        
        // Информация о конфигурации
        var config = plugin.getConfigManager().getConfig();
        boolean shopEnabled = config.getBoolean("shop.enabled", true);
        sender.sendMessage(ChatColor.GRAY + "Магазин включен: " + 
                          (shopEnabled ? ChatColor.GREEN + "Да" : ChatColor.RED + "Нет"));
        
        boolean pvpEnabled = config.getBoolean("zone.pvp-enabled", true);
        sender.sendMessage(ChatColor.GRAY + "PvP в зоне: " + 
                          (pvpEnabled ? ChatColor.GREEN + "Включено" : ChatColor.RED + "Выключено"));
        
        int pointsPerKill = config.getInt("rating.points-per-kill", 1);
        // Исправлено: явное преобразование числа в строку
        sender.sendMessage(ChatColor.GRAY + "Очков за убийство: " + ChatColor.YELLOW + String.valueOf(pointsPerKill));
        
        // Версия плагина
        sender.sendMessage(ChatColor.GRAY + "Версия плагина: " + ChatColor.YELLOW + plugin.getDescription().getVersion());
    }
    
    private void showZoneInfo(CommandSender sender) {
        if (!sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }
        
        var zoneManager = plugin.getZoneManager();
        
        if (!zoneManager.isZoneDefined()) {
            sender.sendMessage(ChatColor.RED + "PvP зона не установлена!");
            sender.sendMessage(ChatColor.YELLOW + "Используйте /pvpz1 и /pvpz2 для установки точек");
            return;
        }
        
        var pos1 = zoneManager.getPos1();
        var pos2 = zoneManager.getPos2();
        
        sender.sendMessage(ChatColor.GOLD + "=== Информация о PvP зоне ===");
        
        if (pos1 != null) {
            // Исправлено: явное преобразование чисел в строки
            String pos1Info = ChatColor.GRAY + "Точка 1: " + ChatColor.YELLOW + 
                            "X: " + String.valueOf(Math.round(pos1.getX())) + 
                            ", Y: " + String.valueOf(Math.round(pos1.getY())) + 
                            ", Z: " + String.valueOf(Math.round(pos1.getZ())) + 
                            ChatColor.GRAY + " (Мир: " + ChatColor.YELLOW + 
                            (pos1.getWorld() != null ? pos1.getWorld().getName() : "Неизвестно") + 
                            ChatColor.GRAY + ")";
            sender.sendMessage(pos1Info);
        }
        
        if (pos2 != null) {
            // Исправлено: явное преобразование чисел в строки
            String pos2Info = ChatColor.GRAY + "Точка 2: " + ChatColor.YELLOW + 
                            "X: " + String.valueOf(Math.round(pos2.getX())) + 
                            ", Y: " + String.valueOf(Math.round(pos2.getY())) + 
                            ", Z: " + String.valueOf(Math.round(pos2.getZ())) + 
                            ChatColor.GRAY + " (Мир: " + ChatColor.YELLOW + 
                            (pos2.getWorld() != null ? pos2.getWorld().getName() : "Неизвестно") + 
                            ChatColor.GRAY + ")";
            sender.sendMessage(pos2Info);
        }
        
        // Рассчитываем размер зоны
        if (pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null) {
            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());
            
            double width = maxX - minX;
            double height = maxY - minY;
            double length = maxZ - minZ;
            double volume = width * height * length;
            
            // Исправлено: явное преобразование чисел в строки
            sender.sendMessage(ChatColor.GRAY + "Размеры зоны: " + ChatColor.YELLOW + 
                             String.valueOf(Math.round(width)) + "×" + 
                             String.valueOf(Math.round(height)) + "×" + 
                             String.valueOf(Math.round(length)));
            
            // Исправлено: явное преобразование числа в строку
            sender.sendMessage(ChatColor.GRAY + "Объем: " + ChatColor.YELLOW + 
                             String.valueOf(Math.round(volume)) + " блоков³");
        }
        
        int playersInZone = zoneManager.getPlayersInZoneCount();
        // Исправлено: явное преобразование числа в строку
        sender.sendMessage(ChatColor.GRAY + "Игроков в зоне сейчас: " + ChatColor.YELLOW + 
                         String.valueOf(playersInZone));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("reload");
            suggestions.add("status");
            suggestions.add("setzone");
            suggestions.add("help");
            
            if (sender.hasPermission("pvpzone.admin")) {
                suggestions.add("resetdata");
                suggestions.add("cleardata");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("resetdata") || args[0].equalsIgnoreCase("cleardata")) {
                suggestions.add("confirm");
            }
        }
        
        return suggestions;
    }
}