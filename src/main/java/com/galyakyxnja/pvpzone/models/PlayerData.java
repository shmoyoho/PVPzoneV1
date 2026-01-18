package com.galyakyxnya.pvpzone.models;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private final UUID playerId;
    private int rating; // Рейтинг (не тратится)
    private int points; // Очки для покупок (тратятся)
    private Map<String, Integer> purchasedBonuses; // Купленные бонусы
    private ItemStack[] originalInventory; // Оригинальный инвентарь
    private ItemStack[] originalArmor; // Оригинальная броня
    
    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.rating = 0;
        this.points = 0;
        this.purchasedBonuses = new HashMap<>();
        this.originalInventory = new ItemStack[0];
        this.originalArmor = new ItemStack[0];
    }
    
    // Геттеры и сеттеры
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public void addRating(int amount) {
        this.rating += amount;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public void addPoints(int amount) {
        this.points += amount;
    }
    
    public boolean removePoints(int amount) {
        if (this.points >= amount) {
            this.points -= amount;
            return true;
        }
        return false;
    }
    
    public Map<String, Integer> getPurchasedBonuses() {
        return purchasedBonuses;
    }
    
    public int getBonusLevel(String bonusId) {
        return purchasedBonuses.getOrDefault(bonusId, 0);
    }
    
    public void addBonusLevel(String bonusId, int level) {
        purchasedBonuses.put(bonusId, purchasedBonuses.getOrDefault(bonusId, 0) + level);
    }
    
    public ItemStack[] getOriginalInventory() {
        return originalInventory;
    }
    
    public void setOriginalInventory(ItemStack[] inventory) {
        this.originalInventory = inventory;
    }
    
    public ItemStack[] getOriginalArmor() {
        return originalArmor;
    }
    
    public void setOriginalArmor(ItemStack[] armor) {
        this.originalArmor = armor;
    }
    
    // Рассчитываем общий бонус здоровья
    public double getHealthBonus() {
        int healthLevel = purchasedBonuses.getOrDefault("health", 0);
        return healthLevel * 0.5; // 0.5 сердца за уровень
    }
    
    // Рассчитываем бонус скорости
    public double getSpeedBonus() {
        int speedLevel = purchasedBonuses.getOrDefault("speed", 0);
        return speedLevel * 0.05; // 5% скорости за уровень
    }
}