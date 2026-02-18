package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class KitManager {
    private final Main plugin;
    private final Map<String, KitData> kits = new HashMap<>();

    private static class KitData {
        ItemStack[] items;
        ItemStack[] armor;

        KitData(ItemStack[] items, ItemStack[] armor) {
            this.items = items != null ? items.clone() : new ItemStack[36];
            this.armor = armor != null ? armor.clone() : new ItemStack[4];
        }
    }

    public KitManager(Main plugin) {
        this.plugin = plugin;
        loadAllKits();
    }

    // СОХРАНЕНИЕ НАБОРА ИЗ ИНВЕНТАРЯ ИГРОКА
    public boolean saveKit(String kitName, Player player) {
        if (kitName == null || kitName.isEmpty()) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents().clone();
        ItemStack[] armor = inventory.getArmorContents().clone();

        kits.put(kitName.toLowerCase(), new KitData(items, armor));
        saveKitToFile(kitName, items, armor);

        player.sendMessage(Lang.get(plugin, "kit_saved", "%kit%", kitName));
        plugin.getLogger().info("Набор '" + kitName + "' сохранен из инвентаря " + player.getName());
        return true;
    }

    // ПРИМЕНЕНИЕ НАБОРА (БЕЗ ОЧИСТКИ ИНВЕНТАРЯ)
    public boolean applyKit(String kitName, Player player) {
        KitData kit = kits.get(kitName.toLowerCase());

        if (kit == null || !isKitSet(kitName)) {
            return false;
        }

        // ВАЖНО: НЕ очищаем инвентарь здесь! Очистка должна быть перед вызовом этого метода

        if (kit.items != null) {
            player.getInventory().setContents(kit.items.clone());
        }

        if (kit.armor != null) {
            player.getInventory().setArmorContents(kit.armor.clone());
        }

        player.updateInventory();
        return true;
    }

    // ПРИМЕНЕНИЕ НАБОРА ДЛЯ ЗОНЫ (С СОХРАНЕНИЕМ И ОЧИСТКОЙ ИНВЕНТАРЯ)
    public boolean applyZoneKit(Player player, String zoneName) {
        String kitName = plugin.getZoneManager().getZoneKitName(zoneName);

        if (kitName == null || kitName.isEmpty()) {
            kitName = "default";
        }

        // ВАЖНО: Инвентарь должен быть сохранен перед вызовом этого метода
        // Инвентарь должен быть очищен перед вызовом этого метода

        boolean applied = applyKit(kitName, player);

        if (applied) {
            player.sendMessage(Lang.get(plugin, "kit_applied", "%kit%", kitName));
        } else {
            player.sendMessage(Lang.get(plugin, "kit_not_found", "%kit%", kitName));
        }

        return applied;
    }

    // ПРИМЕНЕНИЕ НАБОРА БЕЗ ОЧИСТКИ (ДЛЯ PlayerMoveListener)
    public boolean applyKitOnly(String kitName, Player player) {
        return applyKit(kitName, player);
    }

    // ПРОВЕРКА СУЩЕСТВОВАНИЯ НАБОРА
    public boolean isKitSet(String kitName) {
        KitData kit = kits.get(kitName.toLowerCase());
        if (kit == null) return false;

        return countItems(kit.items) > 0 || countItems(kit.armor) > 0;
    }

    // ДЛЯ СОВМЕСТИМОСТИ
    public boolean isKitSet() {
        return isKitSet("default");
    }

    // ПРОВЕРКА ОСНОВНОГО НАБОРА
    public boolean isDefaultKitSet() {
        return isKitSet("default");
    }

    public void saveKitFromPlayer(Player player) {
        saveKit("default", player);
    }

    // МЕТОД ДЛЯ ПРИМЕНЕНИЯ ОСНОВНОГО НАБОРА (УСТАРЕЛО)
    public void applyKitToPlayer(Player player) {
        // ВАЖНО: Этот метод не должен использоваться для зон!
        // Он не сохраняет и не очищает инвентарь
        applyKit("default", player);
    }

    // ЗАГРУЗКА ВСЕХ НАБОРОВ
    private void loadAllKits() {
        kits.clear();

        loadKitFromFile("default", new File(plugin.getDataFolder(), "kit.yml"));

        File kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (kitsFolder.exists() && kitsFolder.isDirectory()) {
            File[] kitFiles = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (kitFiles != null) {
                for (File kitFile : kitFiles) {
                    String kitName = kitFile.getName().replace(".yml", "");
                    loadKitFromFile(kitName, kitFile);
                }
            }
        }

        plugin.getLogger().info("Загружено наборов: " + kits.size());
    }

    private void loadKitFromFile(String kitName, File file) {
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemStack[] items = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        if (config.contains("inventory")) {
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 36) {
                        items[slot] = config.getItemStack("inventory." + slot);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (config.contains("armor")) {
            for (String key : config.getConfigurationSection("armor").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 4) {
                        armor[slot] = config.getItemStack("armor." + slot);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        kits.put(kitName.toLowerCase(), new KitData(items, armor));
    }

    private void saveKitToFile(String kitName, ItemStack[] items, ItemStack[] armor) {
        File file;

        if (kitName.equalsIgnoreCase("default")) {
            file = new File(plugin.getDataFolder(), "kit.yml");
        } else {
            File kitsFolder = new File(plugin.getDataFolder(), "kits");
            kitsFolder.mkdirs();
            file = new File(kitsFolder, kitName.toLowerCase() + ".yml");
        }

        FileConfiguration config = new YamlConfiguration();

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                config.set("inventory." + i, items[i]);
            }
        }

        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                config.set("armor." + i, armor[i]);
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить набор: " + kitName, e);
        }
    }

    private int countItems(ItemStack[] items) {
        if (items == null) return 0;

        int count = 0;
        for (ItemStack item : items) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    public void reloadKits() {
        loadAllKits();
    }

    public void loadKit() {
        reloadKits();
    }

    // МЕТОД ДЛЯ ТЕСТОВОГО НАБОРА
    public void setTestKit() {
        // Создаем тестовый набор
        ItemStack[] items = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        // Тестовый меч в слоте 0
        items[0] = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = items[0].getItemMeta();
        if (swordMeta != null) {
            swordMeta.setDisplayName("§6Тестовый PvP Меч");
            swordMeta.addEnchant(Enchantment.SHARPNESS, 2, true);
            items[0].setItemMeta(swordMeta);
        }

        // Тестовые яблоки в слоте 1
        items[1] = new ItemStack(Material.GOLDEN_APPLE, 3);

        // Тестовая броня
        armor[3] = new ItemStack(Material.IRON_HELMET); // Шлем
        armor[2] = new ItemStack(Material.IRON_CHESTPLATE); // Нагрудник
        armor[1] = new ItemStack(Material.IRON_LEGGINGS); // Поножи
        armor[0] = new ItemStack(Material.IRON_BOOTS); // Ботинки

        kits.put("test", new KitData(items, armor));
        saveKitToFile("test", items, armor);

        plugin.getLogger().info("Создан тестовый набор 'test'");
    }
}