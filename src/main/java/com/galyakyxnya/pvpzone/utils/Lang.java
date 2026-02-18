package com.galyakyxnya.pvpzone.utils;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Загрузка сообщений по выбранному в config.yml языку (language: ru | en).
 */
public final class Lang {
    private static final String LANG_FOLDER = "lang";
    private static Map<String, String> messages = new HashMap<>();
    private static String currentLang = "ru";

    private Lang() {}

    public static void reload(Main plugin) {
        currentLang = plugin.getConfig().getString("language", "ru");
        if (!currentLang.equals("ru") && !currentLang.equals("en")) {
            currentLang = "ru";
        }
        File langDir = new File(plugin.getDataFolder(), LANG_FOLDER);
        if (!langDir.exists()) langDir.mkdirs();
        File langFile = new File(langDir, currentLang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource(LANG_FOLDER + "/" + currentLang + ".yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
        messages.clear();
        for (String key : cfg.getKeys(true)) {
            Object val = cfg.get(key);
            if (val instanceof String) {
                messages.put(key, (String) val);
            }
        }
        // Defaults from jar if key missing
        InputStream res = plugin.getResource(LANG_FOLDER + "/" + currentLang + ".yml");
        if (res != null) {
            YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(res, StandardCharsets.UTF_8));
            for (String key : defCfg.getKeys(true)) {
                if (!messages.containsKey(key) && defCfg.get(key) instanceof String) {
                    messages.put(key, defCfg.getString(key));
                }
            }
        }
    }

    public static String get(Main plugin, String key) {
        if (messages.isEmpty()) reload(plugin);
        String msg = messages.get(key);
        if (msg == null) return "[" + key + "]";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String get(Main plugin, String key, String... placeholders) {
        String msg = get(plugin, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }

    public static String getCurrentLanguage() {
        return currentLang;
    }
}
