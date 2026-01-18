package com.galyakyxnya.pvpzone;

import com.galyakyxnya.pvpzone.commands.*;
import com.galyakyxnya.pvpzone.listeners.*;
import com.galyakyxnya.pvpzone.managers.*;
import com.galyakyxnya.pvpzone.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Main extends JavaPlugin {
    private static Main instance;
    private ZoneManager zoneManager;
    private PlayerDataManager playerDataManager;
    private KitManager kitManager;
    private ShopManager shopManager;
    private ConfigManager configManager;
    private InventoryClickListener inventoryClickListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация ConfigManager
        this.configManager = new ConfigManager(this);
        
        // Создаем папки если их нет
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Инициализация менеджеров
        this.zoneManager = new ZoneManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.kitManager = new KitManager(this);
        this.shopManager = new ShopManager(this);
        this.inventoryClickListener = new InventoryClickListener(this);
        
        // Регистрация команд
        registerCommands();
        
        // Регистрация слушателей
        registerListeners();
        
        getLogger().info(ChatColor.GREEN + "PvP Zone Plugin включен!");
        getLogger().info(ChatColor.GREEN + "Версия: " + getDescription().getVersion());
    }
    
    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info(ChatColor.RED + "PvP Zone Plugin выключен!");
    }
    
    private void registerCommands() {
        // Проверяем наличие команд в plugin.yml
        if (getCommand("pvpz1") != null) {
            getCommand("pvpz1").setExecutor(new PvpZ1Command(this));
        } else {
            getLogger().warning("Команда pvpz1 не найдена в plugin.yml!");
        }
        
        if (getCommand("pvpz2") != null) {
            getCommand("pvpz2").setExecutor(new PvpZ2Command(this));
        } else {
            getLogger().warning("Команда pvpz2 не найдена в plugin.yml!");
        }
        
        if (getCommand("pvpkit") != null) {
            getCommand("pvpkit").setExecutor(new PvpKitCommand(this));
        } else {
            getLogger().warning("Команда pvpkit не найдена в plugin.yml!");
        }
        
        if (getCommand("pvptop") != null) {
            getCommand("pvptop").setExecutor(new PvpTopCommand(this));
        } else {
            getLogger().warning("Команда pvptop не найдена в plugin.yml!");
        }
        
        if (getCommand("pvpshop") != null) {
            getCommand("pvpshop").setExecutor(new PvpShopCommand(this, inventoryClickListener));
        } else {
            getLogger().warning("Команда pvpshop не найдена в plugin.yml!");
        }
        
        if (getCommand("pvpzone") != null) {
            getCommand("pvpzone").setExecutor(new PvpZoneCommand(this));
        } else {
            getLogger().warning("Команда pvpzone не найдена в plugin.yml!");
        }
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(inventoryClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    }
    
    public static Main getInstance() {
        return instance;
    }
    
    public ZoneManager getZoneManager() {
        return zoneManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public InventoryClickListener getInventoryClickListener() {
        return inventoryClickListener;
    }
    
    // Метод для перезагрузки плагина
    public void reload() {
        configManager.reloadConfigs();
        zoneManager.loadZone();
        kitManager.loadKit();
        getLogger().info(ChatColor.GREEN + "Плагин перезагружен!");
    }
}