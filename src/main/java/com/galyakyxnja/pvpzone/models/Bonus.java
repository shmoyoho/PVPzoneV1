package com.galyakyxnya.pvpzone.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class Bonus {
    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final int cost;
    private final int maxLevel;
    private final double valuePerLevel;
    private final BonusType type;
    
    public enum BonusType {
        HEALTH("Здоровье"),
        SPEED("Скорость"),
        JUMP("Прыжок"),
        DAMAGE("Урон"),
        REGENERATION("Регенерация"),
        RESISTANCE("Защита");
        
        private final String displayName;
        
        BonusType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public Bonus(String id, String name, String description, Material icon, 
                 int cost, int maxLevel, double valuePerLevel, BonusType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.cost = cost;
        this.maxLevel = maxLevel;
        this.valuePerLevel = valuePerLevel;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getIcon() {
        return icon;
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
    
    public BonusType getType() {
        return type;
    }
    
    public ItemStack createShopItem(int currentLevel) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e" + name);
            
            String status;
            if (currentLevel >= maxLevel) {
                status = "§c§lМАКСИМАЛЬНЫЙ УРОВЕНЬ";
            } else {
                status = "§7Уровень: §e" + currentLevel + "§7/§a" + maxLevel;
            }
            
            meta.setLore(Arrays.asList(
                "§7" + description,
                "",
                status,
                "§7Стоимость: §e" + cost + " очков",
                "§7Эффект: §a+" + (valuePerLevel * 100) + "% за уровень",
                "",
                currentLevel >= maxLevel ? 
                    "§cНельзя купить" : 
                    "§aНажмите для покупки"
            ));
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public double calculateTotalEffect(int level) {
        return valuePerLevel * level;
    }
}