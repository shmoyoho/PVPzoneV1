package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class ShopManager {
    private final Main plugin;
    private final Map<String, ShopItem> shopItems;
    
    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        loadShopItems();
    }
    
    private void loadShopItems() {
        // Предопределенные бонусы
        shopItems.put("health", new ShopItem("health", "Дополнительное сердце", 10, 5, 0.5));
        shopItems.put("speed", new ShopItem("speed", "Увеличение скорости", 8, 10, 0.05));
        shopItems.put("jump", new ShopItem("jump", "Высокий прыжок", 12, 3, 0.1));
        shopItems.put("damage", new ShopItem("damage", "Усиление урона", 15, 3, 0.05));
    }
    
    public Map<String, ShopItem> getShopItems() {
        return shopItems;
    }
    
    public ShopItem getShopItem(String id) {
        return shopItems.get(id);
    }
    
    // Вложенный класс для товаров магазина
    public static class ShopItem {
        private final String id;
        private final String name;
        private final int cost;
        private final int maxLevel;
        private final double valuePerLevel;
        
        public ShopItem(String id, String name, int cost, int maxLevel, double valuePerLevel) {
            this.id = id;
            this.name = name;
            this.cost = cost;
            this.maxLevel = maxLevel;
            this.valuePerLevel = valuePerLevel;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public int getCost() {
            return cost;
        }
        
        public int getMaxLevel() {
            return maxLevel;
        }
        
        public double getValuePerLevel() {
            return valuePerLevel;
        }
        
        public double getTotalValue(int level) {
            return level * valuePerLevel;
        }
    }
}