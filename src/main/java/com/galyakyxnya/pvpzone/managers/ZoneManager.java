package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ZoneManager {
    private final Main plugin;
    private final Map<String, PvpZone> zones;
    private final Set<UUID> playersInZone;

    public class PvpZone {
        private String name;
        private Location pos1;
        private Location pos2;
        private boolean bonusesEnabled;
        private Location center;
        private String kitName; // Новое поле для названия набора

        public PvpZone(String name, Location pos1, Location pos2, boolean bonusesEnabled) {
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.bonusesEnabled = bonusesEnabled;
            this.kitName = "default"; // Набор по умолчанию
            calculateCenter();
        }

        private void calculateCenter() {
            if (pos1 != null && pos2 != null && pos1.getWorld() != null) {
                double centerX = (pos1.getX() + pos2.getX()) / 2;
                double centerY = (pos1.getY() + pos2.getY()) / 2;
                double centerZ = (pos1.getZ() + pos2.getZ()) / 2;
                this.center = new Location(pos1.getWorld(), centerX, centerY, centerZ);
            }
        }

        public String getName() { return name; }
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
        public boolean isBonusesEnabled() { return bonusesEnabled; }
        public Location getCenter() { return center; }
        public String getKitName() { return kitName; } // Геттер для набора

        public void setBonusesEnabled(boolean enabled) {
            this.bonusesEnabled = enabled;
        }

        public void setKitName(String kitName) { // Сеттер для набора
            this.kitName = kitName;
        }

        public boolean isInZone(Location location) {
            if (pos1 == null || pos2 == null || location == null) return false;

            World world1 = pos1.getWorld();
            World world2 = pos2.getWorld();

            if (world1 == null || world2 == null) return false;
            if (!location.getWorld().equals(world1)) return false;

            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = 0;
            double maxY = world1.getMaxHeight();
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        }

        public Location getSpawnLocation(int playerNumber) {
            if (center == null) return null;

            double offset = 5.0;
            if (playerNumber == 1) {
                return center.clone().add(offset, 0, 0);
            } else {
                return center.clone().subtract(offset, 0, 0);
            }
        }
    }

    public ZoneManager(Main plugin) {
        this.plugin = plugin;
        this.zones = new HashMap<>();
        this.playersInZone = new HashSet<>();
        loadZones();
    }

    public boolean createZone(String name, Location pos1, Location pos2, boolean bonusesEnabled) {
        if (zones.containsKey(name.toLowerCase())) {
            return false;
        }

        // Проверяем, что точки в одном мире
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return false;
        }

        World world = pos1.getWorld();

        // Берем минимальные и максимальные координаты X и Z из точек
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // Для Y используем всю высоту мира
        double minY = 0;
        double maxY = world.getMaxHeight();

        // Создаем новые точки
        Location correctedPos1 = new Location(world, minX, minY, minZ);
        Location correctedPos2 = new Location(world, maxX, maxY, maxZ);

        PvpZone zone = new PvpZone(name, correctedPos1, correctedPos2, bonusesEnabled);
        zones.put(name.toLowerCase(), zone);
        saveZones();
        return true;
    }

    // Метод для установки набора для зоны
    public boolean setZoneKit(String zoneName, String kitName) {
        PvpZone zone = getZone(zoneName);
        if (zone == null) {
            return false;
        }

        zone.setKitName(kitName);
        saveZones();
        return true;
    }

    // Метод для получения названия набора зоны
    public String getZoneKitName(String zoneName) {
        PvpZone zone = getZone(zoneName);
        return zone != null ? zone.getKitName() : "default";
    }

    public boolean removeZone(String name) {
        if (!zones.containsKey(name.toLowerCase())) {
            return false;
        }

        zones.remove(name.toLowerCase());
        saveZones();
        return true;
    }

    public PvpZone getZone(String name) {
        return zones.get(name.toLowerCase());
    }

    public Set<String> getZoneNames() {
        return new HashSet<>(zones.keySet());
    }

    public List<PvpZone> getAllZones() {
        return new ArrayList<>(zones.values());
    }

    public PvpZone findZoneAtLocation(Location location) {
        for (PvpZone zone : zones.values()) {
            if (zone.isInZone(location)) {
                return zone;
            }
        }
        return null;
    }

    public void addPlayerToZone(Player player) {
        playersInZone.add(player.getUniqueId());
    }

    public void removePlayerFromZone(Player player) {
        playersInZone.remove(player.getUniqueId());
    }

    public boolean isPlayerInZone(Player player) {
        return playersInZone.contains(player.getUniqueId());
    }

    public int getPlayersInZoneCount() {
        return playersInZone.size();
    }

    public void saveZones() {
        FileConfiguration config = plugin.getConfig();
        config.set("zones", null);

        for (Map.Entry<String, PvpZone> entry : zones.entrySet()) {
            String key = "zones." + entry.getKey();
            PvpZone zone = entry.getValue();

            if (zone.getPos1() != null && zone.getPos1().getWorld() != null) {
                config.set(key + ".name", zone.getName());
                config.set(key + ".bonusesEnabled", zone.isBonusesEnabled());
                config.set(key + ".kitName", zone.getKitName()); // Сохраняем набор

                config.set(key + ".pos1.world", zone.getPos1().getWorld().getName());
                config.set(key + ".pos1.x", zone.getPos1().getX());
                config.set(key + ".pos1.y", zone.getPos1().getY());
                config.set(key + ".pos1.z", zone.getPos1().getZ());

                config.set(key + ".pos2.world", zone.getPos2().getWorld().getName());
                config.set(key + ".pos2.x", zone.getPos2().getX());
                config.set(key + ".pos2.y", zone.getPos2().getY());
                config.set(key + ".pos2.z", zone.getPos2().getZ());
            }
        }

        plugin.saveConfig();
    }

    private void loadZones() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("zones")) {
            return;
        }

        for (String zoneKey : config.getConfigurationSection("zones").getKeys(false)) {
            String path = "zones." + zoneKey;

            String name = config.getString(path + ".name", zoneKey);
            boolean bonusesEnabled = config.getBoolean(path + ".bonusesEnabled", true);
            String kitName = config.getString(path + ".kitName", "default"); // Загружаем набор

            Location pos1 = null;
            if (config.contains(path + ".pos1")) {
                String worldName = config.getString(path + ".pos1.world");
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = config.getDouble(path + ".pos1.x");
                        double y = config.getDouble(path + ".pos1.y");
                        double z = config.getDouble(path + ".pos1.z");
                        pos1 = new Location(world, x, y, z);
                    }
                }
            }

            Location pos2 = null;
            if (config.contains(path + ".pos2")) {
                String worldName = config.getString(path + ".pos2.world");
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = config.getDouble(path + ".pos2.x");
                        double y = config.getDouble(path + ".pos2.y");
                        double z = config.getDouble(path + ".pos2.z");
                        pos2 = new Location(world, x, y, z);
                    }
                }
            }

            if (pos1 != null && pos2 != null) {
                PvpZone zone = new PvpZone(name, pos1, pos2, bonusesEnabled);
                zone.setKitName(kitName); // Устанавливаем загруженный набор
                zones.put(name.toLowerCase(), zone);
            }
        }
    }

    public void loadZonesPublic() {
        loadZones();
    }
}