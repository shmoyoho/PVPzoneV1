package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class InventoryClickListener implements Listener {
    private final Main plugin;
    
    public InventoryClickListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();
        
        // Проверяем, что это наш магазин
        if (inventoryTitle.equals(ChatColor.GOLD + "Магазин PvP бонусов")) {
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            
            // Обработка кликов
            handleClick(player, clickedItem, slot);
        }
    }
    
    private void handleClick(Player player, ItemStack item, int slot) {
        Material material = item.getType();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        
        // Закрыть магазин
        if (material == Material.BARRIER && displayName.equals(ChatColor.RED + "Закрыть магазин")) {
            player.closeInventory();
            return;
        }
        
        // Информация
        if (material == Material.BOOK && displayName.equals(ChatColor.YELLOW + "Информация")) {
            player.sendMessage(ChatColor.GOLD + "════ Информация ════");
            player.sendMessage(ChatColor.GRAY + "• Нажмите на бонус для покупки");
            player.sendMessage(ChatColor.GRAY + "• Бонусы работают в PvP зоне");
            player.sendMessage(ChatColor.GRAY + "• Применяются автоматически");
            return;
        }
        
        // Пустые слоты (стекла)
        if (material == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        // Определяем бонус по материалу
        String bonusId = null;
        if (material == Material.RED_DYE) bonusId = "health";
        else if (material == Material.SUGAR) bonusId = "speed";
        else if (material == Material.RABBIT_FOOT) bonusId = "jump";
        else if (material == Material.IRON_SWORD) bonusId = "damage";
        
        if (bonusId != null) {
            attemptPurchase(player, bonusId);
        }
    }
    
    private void attemptPurchase(Player player, String bonusId) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        var shopManager = plugin.getShopManager();
        var shopItem = shopManager.getShopItem(bonusId);
        
        if (shopItem == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: бонус не найден!");
            return;
        }
        
        int currentLevel = playerData.getBonusLevel(bonusId);
        
        if (currentLevel >= shopItem.getMaxLevel()) {
            player.sendMessage(ChatColor.RED + "Вы достигли максимального уровня!");
            return;
        }
        
        if (playerData.removePoints(shopItem.getCost())) {
            // Успешная покупка
            playerData.addBonusLevel(bonusId, 1);
            plugin.getPlayerDataManager().savePlayerData(playerData);
            
            player.sendMessage(ChatColor.GREEN + "✓ Куплено: " + shopItem.getName() + 
                             " уровень " + (currentLevel + 1));
            player.sendMessage(ChatColor.YELLOW + "▸ Потрачено: " + shopItem.getCost() + " очков");
            player.sendMessage(ChatColor.YELLOW + "▸ Осталось: " + playerData.getPoints() + " очков");
            
            // Обновляем инвентарь
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory();
                openShopInventory(player);
            }, 5L);
            
        } else {
            player.sendMessage(ChatColor.RED + "✗ Недостаточно очков!");
            player.sendMessage(ChatColor.YELLOW + "▸ Нужно: " + shopItem.getCost() + " очков");
            player.sendMessage(ChatColor.YELLOW + "▸ У вас: " + playerData.getPoints() + " очков");
        }
    }
    
    public void openShopInventory(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        var shopManager = plugin.getShopManager();
        
        // Создаем инвентарь на 27 слотов (3 ряда)
        Inventory shopInv = Bukkit.createInventory(null, 27, 
            ChatColor.GOLD + "Магазин PvP бонусов");
        
        // Создаем предметы бонусов
        int[] bonusSlots = {10, 12, 14, 16}; // Слоты для бонусов
        String[] bonusIds = {"health", "speed", "jump", "damage"};
        
        for (int i = 0; i < bonusIds.length; i++) {
            String bonusId = bonusIds[i];
            int slot = bonusSlots[i];
            var shopItem = shopManager.getShopItem(bonusId);
            
            if (shopItem != null) {
                int currentLevel = playerData.getBonusLevel(bonusId);
                ItemStack item = createShopItem(bonusId, shopItem, currentLevel);
                shopInv.setItem(slot, item);
            }
        }
        
        // Информационная книга в центре
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.YELLOW + "Информация");
            infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Очки: " + ChatColor.YELLOW + playerData.getPoints(),
                ChatColor.GRAY + "Рейтинг: " + ChatColor.YELLOW + playerData.getRating(),
                "",
                ChatColor.GRAY + "Нажмите для информации"
            ));
            info.setItemMeta(infoMeta);
        }
        shopInv.setItem(4, info);
        
        // Кнопка закрытия
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "Закрыть магазин");
            closeMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Нажмите чтобы закрыть"
            ));
            close.setItemMeta(closeMeta);
        }
        shopInv.setItem(22, close);
        
        // Заполняем пустые слоты стеклами
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
            
            for (int i = 0; i < shopInv.getSize(); i++) {
                if (shopInv.getItem(i) == null) {
                    shopInv.setItem(i, glass);
                }
            }
        }
        
        player.openInventory(shopInv);
    }
    
    private ItemStack createShopItem(String bonusId, ShopManager.ShopItem shopItem, int currentLevel) {
        Material material = Material.PAPER;
        String color = "§a";
        
        switch (bonusId) {
            case "health":
                material = Material.RED_DYE;
                color = "§c";
                break;
            case "speed":
                material = Material.SUGAR;
                color = "§b";
                break;
            case "jump":
                material = Material.RABBIT_FOOT;
                color = "§e";
                break;
            case "damage":
                material = Material.IRON_SWORD;
                color = "§4";
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String status = currentLevel >= shopItem.getMaxLevel() ? 
                "§cМАКСИМУМ" : 
                "§eУровень: §a" + currentLevel + "§7/§2" + shopItem.getMaxLevel();
            
            meta.setDisplayName(color + shopItem.getName());
            meta.setLore(Arrays.asList(
                "§7Стоимость: §e" + shopItem.getCost() + " очков",
                status,
                "§7Эффект: §f" + getEffectDescription(bonusId, shopItem.getValuePerLevel()),
                "",
                currentLevel >= shopItem.getMaxLevel() ? 
                    "§cНельзя купить" : 
                    "§aНажмите для покупки"
            ));
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String getEffectDescription(String bonusId, double valuePerLevel) {
        switch (bonusId) {
            case "health": return "+" + (valuePerLevel * 2) + " сердца";
            case "speed": return "+" + (valuePerLevel * 100) + "% скорости";
            case "jump": return "+" + (valuePerLevel * 100) + "% прыжка";
            case "damage": return "+" + (valuePerLevel * 100) + "% урона";
            default: return "Бонус";
        }
    }
}