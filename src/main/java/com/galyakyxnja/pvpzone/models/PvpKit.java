package com.galyakyxnya.pvpzone.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PvpKit {
    private final String name;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final Map<Integer, ItemStack> specialItems;
    
    public PvpKit(String name) {
        this.name = name;
        this.inventory = new ItemStack[36];
        this.armor = new ItemStack[4];
        this.specialItems = new HashMap<>();
        createDefaultKit();
    }
    
    public PvpKit(String name, ItemStack[] inventory, ItemStack[] armor) {
        this.name = name;
        this.inventory = inventory;
        this.armor = armor;
        this.specialItems = new HashMap<>();
    }
    
    private void createDefaultKit() {
        // Меч
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setDisplayName("§6PvP Меч");
            swordMeta.setUnbreakable(true);
            sword.setItemMeta(swordMeta);
        }
        sword.addEnchantment(Enchantment.SHARPNESS, 2);
        sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
        inventory[0] = sword;
        
        // Лук
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            bowMeta.setDisplayName("§6PvP Лук");
            bowMeta.setUnbreakable(true);
            bow.setItemMeta(bowMeta);
        }
        bow.addEnchantment(Enchantment.POWER, 3);
        bow.addEnchantment(Enchantment.INFINITY, 1);
        inventory[1] = bow;
        
        // Стрелы
        ItemStack arrows = new ItemStack(Material.ARROW, 64);
        inventory[2] = arrows;
        
        // Зелья лечения - исправлено для 1.21.11
        ItemStack healingPotion = new ItemStack(Material.SPLASH_POTION, 3);
        PotionMeta potionMeta = (PotionMeta) healingPotion.getItemMeta();
        if (potionMeta != null) {
            potionMeta.setDisplayName("§cЗелье лечения");
            // Используем HEALING вместо INSTANT_HEAL
            potionMeta.setBasePotionData(new PotionData(PotionType.HEALING, false, true));
            healingPotion.setItemMeta(potionMeta);
        }
        inventory[3] = healingPotion;
        
        // Зелья скорости - исправлено для 1.21.11
        ItemStack speedPotion = new ItemStack(Material.SPLASH_POTION, 2);
        PotionMeta speedMeta = (PotionMeta) speedPotion.getItemMeta();
        if (speedMeta != null) {
            speedMeta.setDisplayName("§bЗелье скорости");
            // Используем SWIFTNESS вместо SPEED
            potionMeta.setBasePotionData(new PotionData(PotionType.SWIFTNESS, true, true));
            speedPotion.setItemMeta(speedMeta);
        }
        inventory[4] = speedPotion;
        
        // Еда
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 16);
        ItemMeta foodMeta = food.getItemMeta();
        if (foodMeta != null) {
            foodMeta.setDisplayName("§6Еда");
            food.setItemMeta(foodMeta);
        }
        inventory[5] = food;
        
        // Броня
        // Шлем
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 2);
        helmet.addEnchantment(Enchantment.UNBREAKING, 3);
        armor[3] = helmet;
        
        // Нагрудник
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 2);
        chestplate.addEnchantment(Enchantment.UNBREAKING, 3);
        armor[2] = chestplate;
        
        // Поножи
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 2);
        leggings.addEnchantment(Enchantment.UNBREAKING, 3);
        armor[1] = leggings;
        
        // Ботинки
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 2);
        boots.addEnchantment(Enchantment.FEATHER_FALLING, 2);
        boots.addEnchantment(Enchantment.UNBREAKING, 3);
        armor[0] = boots;
    }
    
    public String getName() {
        return name;
    }
    
    public ItemStack[] getInventory() {
        return inventory.clone();
    }
    
    public ItemStack[] getArmor() {
        return armor.clone();
    }
    
    public Map<Integer, ItemStack> getSpecialItems() {
        return new HashMap<>(specialItems);
    }
    
    public void setInventorySlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.length) {
            inventory[slot] = item;
        }
    }
    
    public void setArmorSlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < armor.length) {
            armor[slot] = item;
        }
    }
    
    public void addSpecialItem(int slot, ItemStack item) {
        specialItems.put(slot, item);
    }
    
    public void removeSpecialItem(int slot) {
        specialItems.remove(slot);
    }
    
    public boolean isEmpty() {
        for (ItemStack item : inventory) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        
        for (ItemStack item : armor) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        
        return true;
    }
}