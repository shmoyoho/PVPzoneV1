package com.galyakyxnya.pvpzone;

import org.bukkit.entity.Player;
import com.galyakyxnya.pvpzone.commands.*;
import com.galyakyxnya.pvpzone.listeners.*;
import com.galyakyxnya.pvpzone.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private LeaderEffectManager leaderEffectManager;
    public LeaderEffectManager getLeaderEffectManager() {
        return leaderEffectManager;
    }

    // Флаг для отслеживания состояния отключения
    private boolean isShuttingDown = false;

    @Override
    public void onEnable() {
        instance = this;
        isShuttingDown = false;

        // Проверяем наличие папки плагина
        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();
            if (created) {
                getLogger().info("Создана папка плагина: " + getDataFolder().getAbsolutePath());
            }
        }

        // Инициализация менеджеров в правильном порядке
        try {
            // 1. ZoneManager (не зависит от других)
            this.zoneManager = new ZoneManager(this);

            // 2. PlayerDataManager (создает базу данных)
            this.playerDataManager = new PlayerDataManager(this);

            // 3. KitManager (загружает наборы)
            this.kitManager = new KitManager(this);

            // 4. ShopManager (загружает товары)
            this.shopManager = new ShopManager(this);

            // 5. DuelManager (зависит от других менеджеров)
            this.duelManager = new DuelManager(this);

            // 6. Слушатели (зависят от менеджеров)
            this.inventoryClickListener = new InventoryClickListener(this);
            this.playerMoveListener = new PlayerMoveListener(this);
            this.leaderEffectManager = new LeaderEffectManager(this);

            // После инициализации менеджеров
            this.leaderEffectManager = new LeaderEffectManager(this);

        } catch (Exception e) {
            getLogger().severe("Ошибка инициализации менеджеров!");
            getLogger().severe(e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            return;
        }

        // Регистрация команд
        try {
            registerCommands();
        } catch (Exception e) {
            getLogger().warning("Ошибка регистрации команд: " + e.getMessage());
        }

        // Регистрация слушателей
        try {
            registerListeners();
        } catch (Exception e) {
            getLogger().warning("Ошибка регистрации слушателей: " + e.getMessage());
        }

        // Статистика запуска
        getLogger().info(ChatColor.GREEN + "══════════════════════════════════════");
        getLogger().info(ChatColor.GREEN + "PvP Zone Plugin успешно загружен!");
        getLogger().info(ChatColor.GREEN + "Версия: " + getDescription().getVersion());
        getLogger().info(ChatColor.YELLOW + "Загружено PvP зон: " + zoneManager.getAllZones().size());
        getLogger().info(ChatColor.YELLOW + "Основной PvP набор: " +
                (kitManager.isDefaultKitSet() ? ChatColor.GREEN + "установлен" : ChatColor.RED + "не установлен"));
        getLogger().info(ChatColor.GREEN + "══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        isShuttingDown = true;

        getLogger().info(ChatColor.YELLOW + "Отключение PvP Zone Plugin...");

        // 1. Останавливаем эффекты лидера
        if (leaderEffectManager != null) {
            try {
                getLogger().info(ChatColor.YELLOW + "Остановка эффектов лидера...");
                leaderEffectManager.stopEffects();
            } catch (Exception e) {
                getLogger().warning("Ошибка при остановке эффектов лидера: " + e.getMessage());
            }
        }

        // 2. Сохраняем данные игроков
        if (playerDataManager != null) {
            try {
                getLogger().info(ChatColor.YELLOW + "Сохранение данных игроков...");
                playerDataManager.saveAllData();

                // Даем небольшое время на асинхронное сохранение (но не слишком долго)
                Thread.sleep(1500);

            } catch (Exception e) {
                getLogger().warning("Ошибка при сохранении данных: " + e.getMessage());
            }
        }

        // 3. Закрываем соединение с базой данных
        if (playerDataManager != null) {
            try {
                getLogger().info(ChatColor.YELLOW + "Закрытие базы данных...");
                playerDataManager.closeDatabase();
            } catch (Exception e) {
                getLogger().warning("Ошибка при закрытии базы данных: " + e.getMessage());
            }
        }

        // 4. Очищаем активные дуэли
        if (duelManager != null) {
            try {
                getLogger().info(ChatColor.YELLOW + "Завершение активных дуэлей...");
                // Можно добавить принудительное завершение всех дуэлей
            } catch (Exception e) {
                getLogger().warning("Ошибка при завершении дуэлей: " + e.getMessage());
            }
        }

        // 5. Выходим из всех зон (на всякий случай)
        if (zoneManager != null) {
            try {
                getLogger().info(ChatColor.YELLOW + "Выход из всех PvP зон...");
                // Восстанавливаем инвентарь всем онлайн игрокам
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (zoneManager.isPlayerInZone(player)) {
                        playerDataManager.restoreOriginalInventory(player);
                        zoneManager.removePlayerFromZone(player);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Ошибка при выходе из зон: " + e.getMessage());
            }
        }

        // 6. Сбрасываем все кэши и списки
        if (playerMoveListener != null) {
            try {
                // Очищаем трекер зон
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerMoveListener.removePlayer(player.getUniqueId());
                }
            } catch (Exception e) {
                getLogger().warning("Ошибка при очистке трекера зон: " + e.getMessage());
            }
        }

        getLogger().info(ChatColor.RED + "══════════════════════════════════════");
        getLogger().info(ChatColor.RED + "PvP Zone Plugin отключен!");
        getLogger().info(ChatColor.RED + "══════════════════════════════════════");
    }
    private void registerCommands() {
        // Проверяем наличие команд в plugin.yml перед регистрацией
        if (getCommand("pvpz1") != null) {
            getCommand("pvpz1").setExecutor(new PvpZ1Command(this));
        } else {
            getLogger().warning("Команда /pvpz1 не найдена в plugin.yml!");
        }

        if (getCommand("pvpz2") != null) {
            getCommand("pvpz2").setExecutor(new PvpZ2Command(this));
        } else {
            getLogger().warning("Команда /pvpz2 не найдена в plugin.yml!");
        }

        if (getCommand("pvpkit") != null) {
            getCommand("pvpkit").setExecutor(new PvpKitCommand(this));
        } else {
            getLogger().warning("Команда /pvpkit не найдена в plugin.yml!");
        }

        if (getCommand("pvptop") != null) {
            getCommand("pvptop").setExecutor(new PvpTopCommand(this));
        } else {
            getLogger().warning("Команда /pvptop не найдена в plugin.yml!");
        }

        if (getCommand("pvpaccept") != null) {
            getCommand("pvpaccept").setExecutor(new PvpAcceptCommand(this));
        } else {
            getLogger().warning("Команда /pvpaccept не найдена в plugin.yml!");
        }

        if (getCommand("pvpdeny") != null) {
            getCommand("pvpdeny").setExecutor(new PvpDenyCommand(this));
        } else {
            getLogger().warning("Команда /pvpdeny не найдена в plugin.yml!");
        }

        if (getCommand("pvpfix") != null) {
            getCommand("pvpfix").setExecutor(new PvpResetCommand(this));
        } else {
            getLogger().warning("Команда /pvpfix не найдена в plugin.yml!");
        }

        // Команды с TabCompleter
        if (getCommand("pvp") != null) {
            PvpDuelCommand pvpDuelCommand = new PvpDuelCommand(this);
            getCommand("pvp").setExecutor(pvpDuelCommand);
            getCommand("pvp").setTabCompleter(pvpDuelCommand);
        } else {
            getLogger().warning("Команда /pvp не найдена в plugin.yml!");
        }

        if (getCommand("pvpzone") != null) {
            PvpZoneCommand pvpZoneCommand = new PvpZoneCommand(this);
            getCommand("pvpzone").setExecutor(pvpZoneCommand);
            getCommand("pvpzone").setTabCompleter(pvpZoneCommand);
        } else {
            getLogger().warning("Команда /pvpzone не найдена в plugin.yml!");
        }

        if (getCommand("pvpshop") != null) {
            PvpShopCommand pvpShopCommand = new PvpShopCommand(this, inventoryClickListener);
            getCommand("pvpshop").setExecutor(pvpShopCommand);
            getCommand("pvpshop").setTabCompleter(pvpShopCommand);
        } else {
            getLogger().warning("Команда /pvpshop не найдена в plugin.yml!");
        }

        getLogger().info("Зарегистрировано команд: 11");
    }

    private void registerListeners() {
        int listenerCount = 0;

        try {
            // Основные слушатели
            Bukkit.getPluginManager().registerEvents(playerMoveListener, this);
            listenerCount++;

            Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            listenerCount++;

            Bukkit.getPluginManager().registerEvents(inventoryClickListener, this);
            listenerCount++;

            Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
            listenerCount++;

            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            listenerCount++;

            Bukkit.getPluginManager().registerEvents(new ItemDropPickupListener(this), this);
            listenerCount++;

            // Дополнительные слушатели (опционально, если нужны)
           Bukkit.getPluginManager().registerEvents(new ContainerProtectionListener(this), this);
            listenerCount++;

        } catch (Exception e) {
            getLogger().warning("Ошибка при регистрации слушателей: " + e.getMessage());
        }

        getLogger().info("Зарегистрировано слушателей событий: " + listenerCount);
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

    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    // Метод для перезагрузки плагина
    public void reload() {
        getLogger().info(ChatColor.YELLOW + "Перезагрузка PvP Zone Plugin...");

        try {
            // Перезагружаем зоны
            if (zoneManager != null) {
                zoneManager.loadZonesPublic();
            }

            // Перезагружаем наборы
            if (kitManager != null) {
                kitManager.reloadKits();
            }

            getLogger().info(ChatColor.GREEN + "Плагин успешно перезагружен!");
            getLogger().info(ChatColor.YELLOW + "Загружено зон: " + zoneManager.getAllZones().size());

        } catch (Exception e) {
            getLogger().warning("Ошибка при перезагрузке плагина: " + e.getMessage());
        }
    }
}