package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZoneManager {
    private final Main plugin;
    private Location pos1;
    private Location pos2;
    private final Set<UUID> playersInZone;
    
    public ZoneManager(Main plugin) {
        this.plugin = plugin;
        this.playersInZone = new HashSet<>();
        loadZone();
    }
    
    public void setPos1(Location location) {
        this.pos1 = location;
        saveZone();
    }
    
    public void setPos2(Location location) {
        this.pos2 = location;
        saveZone();
    }
    
    public boolean isZoneDefined() {
        return pos1 != null && pos2 != null;
    }
    
    public boolean isInZone(Location location) {
        if (!isZoneDefined()) return false;
        if (location == null) return false;
        
        World world1 = pos1.getWorld();
        World world2 = pos2.getWorld();
        
        if (world1 == null || world2 == null) return false;
        if (!location.getWorld().equals(world1)) return false;
        
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
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
    
    public Set<UUID> getPlayersInZone() {
        return new HashSet<>(playersInZone);
    }
    
    public Location getPos1() {
        return pos1;
    }
    
    public Location getPos2() {
        return pos2;
    }
    
    private void saveZone() {
        FileConfiguration config = plugin.getConfig();
        
        if (pos1 != null && pos1.getWorld() != null) {
            config.set("zone.pos1.world", pos1.getWorld().getName());
            config.set("zone.pos1.x", pos1.getX());
            config.set("zone.pos1.y", pos1.getY());
            config.set("zone.pos1.z", pos1.getZ());
            config.set("zone.pos1.yaw", pos1.getYaw());
            config.set("zone.pos1.pitch", pos1.getPitch());
        }
        
        if (pos2 != null && pos2.getWorld() != null) {
            config.set("zone.pos2.world", pos2.getWorld().getName());
            config.set("zone.pos2.x", pos2.getX());
            config.set("zone.pos2.y", pos2.getY());
            config.set("zone.pos2.z", pos2.getZ());
            config.set("zone.pos2.yaw", pos2.getYaw());
            config.set("zone.pos2.pitch", pos2.getPitch());
        }
        
        plugin.saveConfig();
    }
    
    public void loadZone() {
        FileConfiguration config = plugin.getConfig();
        
        if (config.contains("zone.pos1")) {
            String worldName = config.getString("zone.pos1.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("zone.pos1.x");
                    double y = config.getDouble("zone.pos1.y");
                    double z = config.getDouble("zone.pos1.z");
                    float yaw = (float) config.getDouble("zone.pos1.yaw");
                    float pitch = (float) config.getDouble("zone.pos1.pitch");
                    pos1 = new Location(world, x, y, z, yaw, pitch);
                }
            }
        }
        
        if (config.contains("zone.pos2")) {
            String worldName = config.getString("zone.pos2.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("zone.pos2.x");
                    double y = config.getDouble("zone.pos2.y");
                    double z = config.getDouble("zone.pos2.z");
                    float yaw = (float) config.getDouble("zone.pos2.yaw");
                    float pitch = (float) config.getDouble("zone.pos2.pitch");
                    pos2 = new Location(world, x, y, z, yaw, pitch);
                }
            }
        }
    }
    
    public void clearZone() {
        this.pos1 = null;
        this.pos2 = null;
        playersInZone.clear();
        
        FileConfiguration config = plugin.getConfig();
        config.set("zone", null);
        plugin.saveConfig();
    }
}