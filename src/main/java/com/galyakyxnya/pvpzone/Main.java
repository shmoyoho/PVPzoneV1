package com.galyakyxnya.pvpzone;

import com.galyakyxnya.pvpzone.commands.*;
import com.galyakyxnya.pvpzone.listeners.*;
import com.galyakyxnya.pvpzone.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private ZoneManager zoneManager;
    private PlayerDataManager playerDataManager;
    private KitManager kitManager;
    private ShopManager shopManager;
    private InventoryClickListener inventoryClickListener;
    private PlayerMoveListener playerMoveListener;
    private DuelManager duelManager;

    @Override
    public void onEnable() {
        instance = this;

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

        // Запускаем очистку в DuelManager
        duelManager.cleanup();

        getLogger().info(ChatColor.GREEN + "PvP Zone Plugin включен!");
        getLogger().info(ChatColor.GREEN + "Версия: " + getDescription().getVersion());
        getLogger().info(ChatColor.GREEN + "Загружено зон: " + zoneManager.getAllZones().size());
        getLogger().info(ChatColor.GREEN + "Загружено наборов: " + (kitManager.isDefaultKitSet() ? "Да" : "Нет"));
    }

    @Override
    public void onDisable() {
        // Сохраняем данные
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
            playerDataManager.closeDatabase();

        }

        getLogger().info(ChatColor.RED + "PvP Zone Plugin выключен!");
    }

    private void registerCommands() {
        // Регистрация простых команд
        getCommand("pvpz1").setExecutor(new PvpZ1Command(this));
        getCommand("pvpz2").setExecutor(new PvpZ2Command(this));
        getCommand("pvpkit").setExecutor(new PvpKitCommand(this));
        getCommand("pvptop").setExecutor(new PvpTopCommand(this));
        getCommand("pvpaccept").setExecutor(new PvpAcceptCommand(this));
        getCommand("pvpdeny").setExecutor(new PvpDenyCommand(this));
        getCommand("pvpfix").setExecutor(new PvpResetCommand(this)); // Используем PvpResetCommand для pvpfix

        // Команды с TabCompleter
        PvpDuelCommand pvpDuelCommand = new PvpDuelCommand(this);
        getCommand("pvp").setExecutor(pvpDuelCommand);
        getCommand("pvp").setTabCompleter(pvpDuelCommand);

        PvpZoneCommand pvpZoneCommand = new PvpZoneCommand(this);
        getCommand("pvpzone").setExecutor(pvpZoneCommand);
        getCommand("pvpzone").setTabCompleter(pvpZoneCommand);

        PvpShopCommand pvpShopCommand = new PvpShopCommand(this, inventoryClickListener);
        getCommand("pvpshop").setExecutor(pvpShopCommand);
        getCommand("pvpshop").setTabCompleter(pvpShopCommand);
    }

    private void registerListeners() {
        // Регистрация слушателей
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
        zoneManager.loadZonesPublic();
        kitManager.reloadKits();
        getLogger().info(ChatColor.GREEN + "Плагин перезагружен!");
    }
}