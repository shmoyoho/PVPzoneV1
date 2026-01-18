package com.galyakyxnya.pvpzone.utils;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {
    private final Main plugin;
    private FileConfiguration config;
    private File configFile;
    private File kitsFile;
    private FileConfiguration kitsConfig;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    
    private final Map<String, Object> defaultConfig = new HashMap<>();
    private final Map<String, Object> defaultMessages = new HashMap<>();
    
    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        setupDefaultValues();
        loadConfigs();
    }
    
    private void setupDefaultValues() {
        // Основной конфиг
        defaultConfig.put("zone.check-interval", 20);
        defaultConfig.put("zone.pvp-enabled", true);
        defaultConfig.put("zone.keep-inventory", true);
        defaultConfig.put("zone.disable-mob-spawns", true);
        defaultConfig.put("zone.prevent-block-break", true);
        defaultConfig.put("zone.prevent-block-place", true);
        
        defaultConfig.put("rating.points-per-kill", 1);
        defaultConfig.put("rating.points-per-death", 0);
        defaultConfig.put("rating.kill-streak-bonus", true);
        defaultConfig.put("rating.kill-streak-multiplier", 0.5);
        defaultConfig.put("rating.minimum-players-for-rating", 2);
        
        defaultConfig.put("shop.enabled", true);
        defaultConfig.put("shop.open-command", true);
        
        defaultConfig.put("kit.auto-give", true);
        defaultConfig.put("kit.save-on-death", true);
        defaultConfig.put("kit.default-kit", "default");
        
        defaultConfig.put("database.enabled", false);
        defaultConfig.put("database.type", "sqlite");
        defaultConfig.put("database.host", "localhost");
        defaultConfig.put("database.port", 3306);
        defaultConfig.put("database.database", "pvpzone");
        defaultConfig.put("database.username", "root");
        defaultConfig.put("database.password", "");
        defaultConfig.put("database.table-prefix", "pvpzone_");
        
        defaultConfig.put("logging.enabled", true);
        defaultConfig.put("logging.level", "INFO");
        defaultConfig.put("logging.log-pvp-kills", true);
        defaultConfig.put("logging.log-zone-entries", true);
        defaultConfig.put("logging.log-shop-purchases", true);
        
        defaultConfig.put("updates.check", true);
        defaultConfig.put("updates.notify", true);
        defaultConfig.put("updates.auto-download", false);
        
        // Бонусы
        defaultConfig.put("bonuses.health.enabled", true);
        defaultConfig.put("bonuses.health.max-level", 5);
        defaultConfig.put("bonuses.health.base-cost", 10);
        defaultConfig.put("bonuses.health.value-per-level", 0.5);
        defaultConfig.put("bonuses.health.display-name", "&cДополнительное сердце");
        defaultConfig.put("bonuses.health.description", "Увеличивает максимальное здоровье");
        
        defaultConfig.put("bonuses.speed.enabled", true);
        defaultConfig.put("bonuses.speed.max-level", 5);
        defaultConfig.put("bonuses.speed.base-cost", 8);
        defaultConfig.put("bonuses.speed.value-per-level", 0.05);
        defaultConfig.put("bonuses.speed.display-name", "&bУвеличение скорости");
        defaultConfig.put("bonuses.speed.description", "Увеличивает скорость передвижения");
        
        defaultConfig.put("bonuses.jump.enabled", true);
        defaultConfig.put("bonuses.jump.max-level", 3);
        defaultConfig.put("bonuses.jump.base-cost", 12);
        defaultConfig.put("bonuses.jump.value-per-level", 0.1);
        defaultConfig.put("bonuses.jump.display-name", "&aВысокий прыжок");
        defaultConfig.put("bonuses.jump.description", "Увеличивает высоту прыжка");
        
        defaultConfig.put("bonuses.damage.enabled", true);
        defaultConfig.put("bonuses.damage.max-level", 3);
        defaultConfig.put("bonuses.damage.base-cost", 15);
        defaultConfig.put("bonuses.damage.value-per-level", 0.05);
        defaultConfig.put("bonuses.damage.display-name", "&4Усиление урона");
        defaultConfig.put("bonuses.damage.description", "Увеличивает наносимый урон");
        
        // Эффекты
        defaultConfig.put("effects.on-enter", new String[]{"SPEED:5:1", "REGENERATION:5:1"});
        defaultConfig.put("effects.on-kill", new String[]{"ABSORPTION:10:1", "REGENERATION:3:1"});
        
        // Сообщения
        defaultMessages.put("prefix", "&6[PvPZone] &f");
        defaultMessages.put("enter-zone", "&6Вы вошли в PvP зону! Ваш инвентарь сохранен.");
        defaultMessages.put("exit-zone", "&6Вы вышли из PvP зоны! Ваш инвентарь восстановлен.");
        defaultMessages.put("pvp-kill", "&a+1 очко за победу в PvP!");
        defaultMessages.put("no-kit", "&cPvP набор не установлен! Администратор должен использовать /pvpkit");
        defaultMessages.put("no-zone", "&cPvP зона не установлена! Администратор должен использовать /pvpz1 и /pvpz2");
        defaultMessages.put("zone-set", "&aТочка PvP зоны установлена!");
        defaultMessages.put("kit-saved", "&aPvP набор сохранен!");
        defaultMessages.put("shop-opened", "&aМагазин PvP бонусов открыт!");
        defaultMessages.put("purchase-success", "&aПокупка успешна!");
        defaultMessages.put("purchase-failed", "&cНедостаточно очков!");
        defaultMessages.put("max-level", "&cВы достигли максимального уровня!");
        defaultMessages.put("reload-success", "&aКонфигурация перезагружена!");
        defaultMessages.put("no-permission", "&cУ вас нет прав для использования этой команды!");
        defaultMessages.put("player-only", "&cЭту команду может использовать только игрок!");
        defaultMessages.put("invalid-arguments", "&cНеверные аргументы команды!");
        defaultMessages.put("zone-not-defined", "&cPvP зона не определена!");
        defaultMessages.put("kit-not-defined", "&cPvP набор не определен!");
        defaultMessages.put("player-not-found", "&cИгрок не найден!");
        defaultMessages.put("data-reset", "&aДанные игрока сброшены!");
        defaultMessages.put("top-header", "&6══ Топ игроков PvP ══");
        defaultMessages.put("shop-header", "&6══ Магазин PvP бонусов ══");
        defaultMessages.put("stats-header", "&6══ Ваша статистика ══");
        defaultMessages.put("bonuses-header", "&6══ Ваши бонусы ══");
    }
    
    private void loadConfigs() {
        // Загружаем основной конфиг
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Загружаем конфиг наборов
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        
        // Загружаем конфиг сообщений
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            createMessagesFile();
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Проверяем и добавляем недостающие значения
        updateConfigs();
    }
    
    private void createMessagesFile() {
        try {
            messagesFile.createNewFile();
            messagesConfig = new YamlConfiguration();
            
            // Записываем дефолтные сообщения
            for (Map.Entry<String, Object> entry : defaultMessages.entrySet()) {
                messagesConfig.set(entry.getKey(), entry.getValue());
            }
            
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось создать файл сообщений", e);
        }
    }
    
    private void updateConfigs() {
        boolean configChanged = false;
        boolean messagesChanged = false;
        
        // Обновляем основной конфиг
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                configChanged = true;
            }
        }
        
        // Обновляем конфиг сообщений
        for (Map.Entry<String, Object> entry : defaultMessages.entrySet()) {
            if (!messagesConfig.contains(entry.getKey())) {
                messagesConfig.set(entry.getKey(), entry.getValue());
                messagesChanged = true;
            }
        }
        
        // Сохраняем изменения
        try {
            if (configChanged) {
                config.save(configFile);
            }
            if (messagesChanged) {
                messagesConfig.save(messagesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить конфигурацию", e);
        }
    }
    
    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        updateConfigs();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить config.yml", e);
        }
    }
    
    public void saveKitsConfig() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить kits.yml", e);
        }
    }
    
    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить messages.yml", e);
        }
    }
    
    // Геттеры для конфигов
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getKitsConfig() {
        return kitsConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    // Методы для удобного доступа к значениям
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    public double getDouble(String path) {
        return config.getDouble(path);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    public String getString(String path) {
        return config.getString(path);
    }
    
    public String[] getStringList(String path) {
        return config.getStringList(path).toArray(new String[0]);
    }
    
    // Методы для работы с сообщениями
    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "Сообщение не найдено: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    public String getFormattedMessage(String path) {
        String prefix = getMessage("prefix");
        String message = getMessage(path);
        
        if (message.startsWith(prefix)) {
            return message;
        }
        
        return prefix + message;
    }
    
    // Методы для работы с наборами
    public String[] getAvailableKits() {
        if (kitsConfig.getConfigurationSection("") != null) {
            return kitsConfig.getConfigurationSection("").getKeys(false).toArray(new String[0]);
        }
        return new String[0];
    }
    
    public boolean kitExists(String kitName) {
        return kitsConfig.contains(kitName);
    }
    
    // Методы для работы с бонусами
    public Map<String, Map<String, Object>> getBonuses() {
        Map<String, Map<String, Object>> bonuses = new HashMap<>();
        
        if (config.contains("bonuses")) {
            for (String bonusId : config.getConfigurationSection("bonuses").getKeys(false)) {
                Map<String, Object> bonusData = new HashMap<>();
                
                bonusData.put("enabled", config.getBoolean("bonuses." + bonusId + ".enabled"));
                bonusData.put("max-level", config.getInt("bonuses." + bonusId + ".max-level"));
                bonusData.put("base-cost", config.getInt("bonuses." + bonusId + ".base-cost"));
                bonusData.put("value-per-level", config.getDouble("bonuses." + bonusId + ".value-per-level"));
                bonusData.put("display-name", config.getString("bonuses." + bonusId + ".display-name"));
                bonusData.put("description", config.getString("bonuses." + bonusId + ".description"));
                
                bonuses.put(bonusId, bonusData);
            }
        }
        
        return bonuses;
    }
    
    public boolean isBonusEnabled(String bonusId) {
        return config.getBoolean("bonuses." + bonusId + ".enabled", false);
    }
    
    public int getBonusMaxLevel(String bonusId) {
        return config.getInt("bonuses." + bonusId + ".max-level", 1);
    }
    
    public int getBonusCost(String bonusId) {
        return config.getInt("bonuses." + bonusId + ".base-cost", 10);
    }
    
    public double getBonusValuePerLevel(String bonusId) {
        return config.getDouble("bonuses." + bonusId + ".value-per-level", 0.0);
    }
    
    // Метод для получения энкодера сообщений (для Json сообщений)
    public String encodeMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    // Метод для логгирования
    public boolean shouldLog(String type) {
        return config.getBoolean("logging.log-" + type, true);
    }
    
    // Метод для получения интервала проверки
    public int getCheckInterval() {
        int interval = config.getInt("zone.check-interval", 20);
        return Math.max(1, interval); // Минимум 1 тик
    }
}