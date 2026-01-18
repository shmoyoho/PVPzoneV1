package com.galyakyxnya.pvpzone.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class InventorySerializer {
    
    // Сериализация инвентаря в Base64 строку
    public static String serializeInventory(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать инвентарь.", e);
        }
    }
    
    // Десериализация инвентаря из Base64 строки
    public static ItemStack[] deserializeInventory(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось десериализовать инвентарь.", e);
        }
    }
    
    // Альтернативный метод: сохранение в ConfigurationSection
    public static void saveInventoryToConfig(ItemStack[] items, ConfigurationSection section, String path) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                section.set(path + "." + i, items[i]);
            }
        }
    }
    
    // Альтернативный метод: загрузка из ConfigurationSection
    public static ItemStack[] loadInventoryFromConfig(ConfigurationSection section, String path, int size) {
        ItemStack[] items = new ItemStack[size];
        
        if (section.contains(path)) {
            for (String key : section.getConfigurationSection(path).getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    items[slot] = section.getItemStack(path + "." + slot);
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные ключи
                }
            }
        }
        
        return items;
    }
}