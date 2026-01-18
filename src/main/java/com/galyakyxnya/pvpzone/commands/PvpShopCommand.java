package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.listeners.InventoryClickListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PvpShopCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final InventoryClickListener inventoryClickListener;
    
    public PvpShopCommand(Main plugin, InventoryClickListener inventoryClickListener) {
        this.plugin = plugin;
        this.inventoryClickListener = inventoryClickListener;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Открываем магазин через слушатель
            inventoryClickListener.openShopInventory(player);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("info")) {
            showPlayerInfo(player);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("bonuses")) {
            showBonusesInfo(player);
            return true;
        }
        
        player.sendMessage(ChatColor.RED + "Использование: /pvpshop [info|bonuses]");
        return true;
    }
    
    private void showPlayerInfo(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        
        player.sendMessage(ChatColor.GOLD + "══ Ваша статистика ══");
        player.sendMessage(ChatColor.GRAY + "Рейтинг: " + ChatColor.YELLOW + 
                         String.valueOf(playerData.getRating()) + " очков");
        player.sendMessage(ChatColor.GRAY + "Очки для покупок: " + ChatColor.YELLOW + 
                         String.valueOf(playerData.getPoints()));
        
        if (!playerData.getPurchasedBonuses().isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "══ Ваши бонусы ══");
            for (var entry : playerData.getPurchasedBonuses().entrySet()) {
                String bonusName = getBonusName(entry.getKey());
                double bonusValue = getBonusValue(entry.getKey(), entry.getValue());
                String bonusDescription = getBonusDescription(entry.getKey());
                
                player.sendMessage(ChatColor.GREEN + "• " + bonusName + 
                                 ChatColor.GRAY + " (Уровень " + ChatColor.YELLOW + 
                                 String.valueOf(entry.getValue()) + ChatColor.GRAY + ")");
                player.sendMessage(ChatColor.DARK_GRAY + "  " + bonusDescription + 
                                 ": " + ChatColor.YELLOW + "+" + String.valueOf(bonusValue));
                player.sendMessage("");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "У вас нет купленных бонусов");
            player.sendMessage(ChatColor.GRAY + "Заработайте очки в PvP зоне и купите их в магазине!");
        }
    }
    
    private void showBonusesInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "══ Доступные бонусы ══");
        
        var shopItems = plugin.getShopManager().getShopItems();
        for (var entry : shopItems.entrySet()) {
            var shopItem = entry.getValue();
            
            player.sendMessage(ChatColor.GREEN + "• " + shopItem.getName());
            player.sendMessage(ChatColor.GRAY + "  Стоимость: " + ChatColor.YELLOW + 
                             String.valueOf(shopItem.getCost()) + " очков за уровень");
            player.sendMessage(ChatColor.GRAY + "  Макс. уровень: " + ChatColor.YELLOW + 
                             String.valueOf(shopItem.getMaxLevel()));
            player.sendMessage(ChatColor.GRAY + "  Эффект: " + ChatColor.WHITE + 
                             getBonusDescription(shopItem.getId()));
            player.sendMessage("");
        }
        
        player.sendMessage(ChatColor.GRAY + "Откройте магазин: " + ChatColor.YELLOW + "/pvpshop");
    }
    
    private String getBonusName(String bonusId) {
        var shopItem = plugin.getShopManager().getShopItem(bonusId);
        return shopItem != null ? shopItem.getName() : bonusId;
    }
    
    private double getBonusValue(String bonusId, int level) {
        var shopItem = plugin.getShopManager().getShopItem(bonusId);
        if (shopItem != null) {
            return shopItem.getTotalValue(level);
        }
        return 0;
    }
    
    private String getBonusDescription(String bonusId) {
        var shopItem = plugin.getShopManager().getShopItem(bonusId);
        if (shopItem == null) return "";
        
        switch (bonusId) {
            case "health": 
                return "Добавляет " + String.valueOf(shopItem.getValuePerLevel() * 2) + " сердца за уровень";
            case "speed": 
                return "Увеличивает скорость на " + String.valueOf(shopItem.getValuePerLevel() * 100) + "% за уровень";
            case "jump": 
                return "Увеличивает высоту прыжка на " + String.valueOf(shopItem.getValuePerLevel() * 100) + "% за уровень";
            case "damage": 
                return "Увеличивает урон на " + String.valueOf(shopItem.getValuePerLevel() * 100) + "% за уровень";
            default: return "Бонус";
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("info");
            suggestions.add("bonuses");
        }
        
        return suggestions;
    }
}