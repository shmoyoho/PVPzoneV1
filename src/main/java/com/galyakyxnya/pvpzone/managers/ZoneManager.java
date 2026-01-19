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
        private String kitName;

        // ОПТИМИЗАЦИЯ: Кэшируем границы для быстрой проверки
        private double minX, maxX, minZ, maxZ;
        private World world;

        public PvpZone(String name, Location pos1, Location pos2, boolean bonusesEnabled) {
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.bonusesEnabled = bonusesEnabled;
            this.kitName = "default";
            calculateBounds();
            calculateCenter();
        }

        // ОПТИМИЗАЦИЯ: Вычисляем границы один раз
        private void calculateBounds() {
            if (pos1 != null && pos2 != null && pos1.getWorld() != null) {
                this.world = pos1.getWorld();
                this.minX = Math.min(pos1.getX(), pos2.getX());
                this.maxX = Math.max(pos1.getX(), pos2.getX());
                this.minZ = Math.min(pos1.getZ(), pos2.getZ());
                this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
            }
        }

        private void calculateCenter() {
            if (pos1 != null && pos2 != null && world != null) {
                double centerX = (minX + maxX) / 2;
                double centerY = (pos1.getY() + pos2.getY()) / 2;
                double centerZ = (minZ + maxZ) / 2;
                this.center = new Location(world, centerX, centerY, centerZ);
            }
        }

        public String getName() { return name; }
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
        public boolean isBonusesEnabled() { return bonusesEnabled; }
        public Location getCenter() { return center; }
        public String getKitName() { return kitName; }

        public void setBonusesEnabled(boolean enabled) {
            this.bonusesEnabled = enabled;
        }

        public void setKitName(String kitName) {
            this.kitName = kitName;
        }

        // ОПТИМИЗАЦИЯ: Ускоренная проверка нахождения в зоне
        public boolean isInZone(Location location) {
            if (world == null || location == null || !location.getWorld().equals(world)) {
                return false;
            }

            double x = location.getX();
            double z = location.getZ();

            // Быстрая проверка по X и Z
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                return false;
            }

            // Проверка Y (вся высота мира)
            return location.getY() >= 0 && location.getY() <= world.getMaxHeight();
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

    public boolean setZoneKit(String zoneName, String kitName) {
        PvpZone zone = getZone(zoneName);
        if (zone == null) {
            return false;
        }

        zone.setKitName(kitName);
        saveZones();
        return true;
    }

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

    // ОПТИМИЗАЦИЯ: Ускоренный поиск зоны по местоположению
    public PvpZone findZoneAtLocation(Location location) {
        if (location == null || zones.isEmpty()) {
            return null;
        }

        // ОПТИМИЗАЦИЯ: Проверяем только зоны в том же мире
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
                config.set(key + ".kitName", zone.getKitName());

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
            String kitName = config.getString(path + ".kitName", "default");

            Location pos1 = loadLocationFromConfig(path + ".pos1");
            Location pos2 = loadLocationFromConfig(path + ".pos2");

            if (pos1 != null && pos2 != null) {
                PvpZone zone = new PvpZone(name, pos1, pos2, bonusesEnabled);
                zone.setKitName(kitName);
                zones.put(name.toLowerCase(), zone);
            }
        }

        plugin.getLogger().info("Загружено зон: " + zones.size());
    }

    private Location loadLocationFromConfig(String path) {
        var config = plugin.getConfig();

        if (!config.contains(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");

        return new Location(world, x, y, z);
    }

    public void loadZonesPublic() {
        loadZones();
    }
}