package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class KitManager {
    private final Main plugin;
    private ItemStack[] kitItems;
    private ItemStack[] kitArmor;
    
    public KitManager(Main plugin) {
        this.plugin = plugin;
        this.kitItems = new ItemStack[0];
        this.kitArmor = new ItemStack[0];
        loadKit();
    }
    
    public void saveKitFromPlayer(Player player) {
        PlayerInventory inventory = player.getInventory();
        this.kitItems = inventory.getContents();
        this.kitArmor = inventory.getArmorContents();
        saveKit();
        
        player.sendMessage("§aPvP набор сохранен!");
    }
    
    public void applyKitToPlayer(Player player) {
        if (kitItems.length == 0 && kitArmor.length == 0) {
            player.sendMessage("§cPvP набор не установлен! Администратор должен использовать /pvpkit");
            return;
        }
        
        // Очищаем инвентарь
        player.getInventory().clear();
        
        // Устанавливаем PvP набор
        if (kitItems.length > 0) {
            player.getInventory().setContents(kitItems);
        }
        
        if (kitArmor.length > 0) {
            player.getInventory().setArmorContents(kitArmor);
        }
        
        player.updateInventory();
    }
    
    private void saveKit() {
        File file = new File(plugin.getDataFolder(), "kit.yml");
        FileConfiguration config = new YamlConfiguration();
        
        // Сохраняем предметы инвентаря
        for (int i = 0; i < kitItems.length; i++) {
            if (kitItems[i] != null) {
                config.set("inventory." + i, kitItems[i]);
            }
        }
        
        // Сохраняем броню
        for (int i = 0; i < kitArmor.length; i++) {
            if (kitArmor[i] != null) {
                config.set("armor." + i, kitArmor[i]);
            }
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить набор", e);
        }
    }
    
    public void loadKit() {
        File file = new File(plugin.getDataFolder(), "kit.yml");
        
        if (!file.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Загружаем предметы инвентаря
        if (config.contains("inventory")) {
            kitItems = new ItemStack[41]; // 36 слотов + 5 дополнительных
            
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    kitItems[slot] = config.getItemStack("inventory." + slot);
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные ключи
                }
            }
        }
        
        // Загружаем броню
        if (config.contains("armor")) {
            kitArmor = new ItemStack[4];
            
            for (String key : config.getConfigurationSection("armor").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 4) {
                        kitArmor[slot] = config.getItemStack("armor." + slot);
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные ключи
                }
            }
        }
    }
    
    public boolean isKitSet() {
        return kitItems.length > 0 || kitArmor.length > 0;
    }
}