package com.galyakyxnya.pvpzone;

import com.galyakyxnya.pvpzone.commands.*;
import com.galyakyxnya.pvpzone.listeners.*;
import com.galyakyxnya.pvpzone.managers.*;
import com.galyakyxnya.pvpzone.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Main extends JavaPlugin {
    private static Main instance;
    private ZoneManager zoneManager;
    private PlayerDataManager playerDataManager;
    private KitManager kitManager;
    private ShopManager shopManager;
    private ConfigManager configManager;
    private InventoryClickListener inventoryClickListener;
    private PlayerMoveListener playerMoveListener;
    private DuelManager duelManager;

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
        this.duelManager = new DuelManager(this);
        this.inventoryClickListener = new InventoryClickListener(this);
        this.playerMoveListener = new PlayerMoveListener(this);

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
            PvpZoneCommand pvpZoneCommand = new PvpZoneCommand(this);
            getCommand("pvpzone").setExecutor(pvpZoneCommand);
            getCommand("pvpzone").setTabCompleter(pvpZoneCommand);
        } else {
            getLogger().warning("Команда pvpzone не найдена в plugin.yml!");
        }

        // Команды дуэлей
        if (getCommand("pvp") != null) {
            PvpDuelCommand pvpDuelCommand = new PvpDuelCommand(this);
            getCommand("pvp").setExecutor(pvpDuelCommand);
            getCommand("pvp").setTabCompleter(pvpDuelCommand);
        } else {
            getLogger().warning("Команда pvp не найдена в plugin.yml!");
        }

        if (getCommand("pvpaccept") != null) {
            getCommand("pvpaccept").setExecutor(new PvpAcceptCommand(this));
        } else {
            getLogger().warning("Команда pvpaccept не найдена в plugin.yml!");
        }

        if (getCommand("pvpdeny") != null) {
            getCommand("pvpdeny").setExecutor(new PvpDenyCommand(this));
        } else {
            getLogger().warning("Команда pvpdeny не найдена в plugin.yml!");
        }

        // Команда для исправления состояния
        if (getCommand("pvpfix") != null) {
            getCommand("pvpfix").setExecutor(new PvpFixCommand(this));
        } else {
            getLogger().warning("Команда pvpfix не найдена в plugin.yml!");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(playerMoveListener, this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(inventoryClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemDropPickupListener(this), this);
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

    public PlayerMoveListener getPlayerMoveListener() {
        return playerMoveListener;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    // Метод для перезагрузки плагина
    public void reload() {
        configManager.reloadConfigs();
        zoneManager.loadZonesPublic();
        kitManager.loadKit();
        getLogger().info(ChatColor.GREEN + "Плагин перезагружен!");
    }
}