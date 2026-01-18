package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemDropPickupListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Location> lastDropLocation = new HashMap<>(); // Последнее место выброса каждого игрока
    private final Set<UUID> markedItems = new HashSet<>(); // Помеченные предметы (выброшенные из зоны)

    public ItemDropPickupListener(Main plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        Location dropLocation = item.getLocation();

        // Сохраняем место выброса
        lastDropLocation.put(player.getUniqueId(), dropLocation);

        // Проверяем, находится ли игрок в зоне в момент выброса
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя выкидывать предметы находясь в PvP зоне!");
            return;
        }

        // Проверяем, выброшен ли предмет ИЗ зоны НАРУЖУ
        checkIfDroppedFromZone(player, item, dropLocation);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Item item = event.getItem();

        // Проверяем, помечен ли предмет как "выброшенный из зоны"
        if (markedItems.contains(item.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Этот предмет был выброшен из PvP зоны!");

            // Удаляем предмет через 2 тика
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (item.isValid()) {
                    item.remove();
                    markedItems.remove(item.getUniqueId());
                }
            }, 2L);
            return;
        }

        // Проверяем, находится ли игрок в зоне сейчас
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя подбирать предметы находясь в PvP зоне!");
        }
    }

    private void checkIfDroppedFromZone(Player player, Item item, Location dropLocation) {
        // Получаем все зоны
        var zones = plugin.getZoneManager().getAllZones();

        for (var zone : zones) {
            // Получаем границы зоны
            Location zonePos1 = zone.getPos1();
            Location zonePos2 = zone.getPos2();

            if (zonePos1 == null || zonePos2 == null) continue;

            double minX = Math.min(zonePos1.getX(), zonePos2.getX());
            double maxX = Math.max(zonePos1.getX(), zonePos2.getX());
            double minZ = Math.min(zonePos1.getZ(), zonePos2.getZ());
            double maxZ = Math.max(zonePos1.getZ(), zonePos2.getZ());

            // Проверяем траекторию: если игрок стоял в зоне недавно
            Location playerLocation = player.getLocation();

            // Простая проверка: если игрок стоит рядом с зоной (в пределах 3 блоков)
            boolean isNearZone = isLocationNearZone(playerLocation, minX, maxX, minZ, maxZ, 3.0);

            // Более сложная проверка: смотрим траекторию броска
            if (isNearZone) {
                // Предмет выброшен из зоны или рядом с ней - помечаем его
                markedItems.add(item.getUniqueId());

                // Логируем для админов
                plugin.getLogger().warning("Предмет помечен как выброшенный из зоны: " +
                        item.getItemStack().getType() + " игроком " + player.getName());

                // Удаляем через 30 секунд, если его не подобрали
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (markedItems.contains(item.getUniqueId()) && item.isValid()) {
                        item.remove();
                        markedItems.remove(item.getUniqueId());
                    }
                }, 20L * 30); // 30 секунд

                break;
            }
        }
    }

    private boolean isLocationNearZone(Location location, double minX, double maxX, double minZ, double maxZ, double margin) {
        double x = location.getX();
        double z = location.getZ();

        // Проверяем, находится ли точка рядом с прямоугольником зоны
        boolean nearX = (x >= minX - margin && x <= maxX + margin);
        boolean nearZ = (z >= minZ - margin && z <= maxZ + margin);

        return nearX && nearZ;
    }

    // Дополнительный метод для защиты от выкидывания через стену
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItemMonitor(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        // Проверяем траекторию через 1 тик
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid()) return;

            Location itemLocation = item.getLocation();

            // Если предмет вылетел из зоны
            if (plugin.getZoneManager().findZoneAtLocation(itemLocation) == null) {
                // Проверяем, не был ли он брошен из зоны
                Location playerLocationAtDrop = lastDropLocation.get(player.getUniqueId());
                if (playerLocationAtDrop != null) {
                    if (plugin.getZoneManager().findZoneAtLocation(playerLocationAtDrop) != null) {
                        // Предмет был выброшен ИЗ зоны НАРУЖУ - помечаем
                        markedItems.add(item.getUniqueId());
                        plugin.getLogger().warning("Обнаружен выброс предмета из зоны наружу: " +
                                player.getName() + " -> " + item.getItemStack().getType());
                    }
                }
            }
        }, 1L);
    }

    private void startCleanupTask() {
        // Очистка старых данных каждые 5 минут
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Очищаем устаревшие записи о местах выброса
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Location>> iterator = lastDropLocation.entrySet().iterator();
            while (iterator.hasNext()) {
                // Удаляем записи старше 5 минут
                iterator.next();
                // Простая очистка - можно добавить timestamp если нужно
            }

            // Очищаем помеченные предметы, которые уже удалены
            Iterator<UUID> itemIterator = markedItems.iterator();
            while (itemIterator.hasNext()) {
                UUID itemId = itemIterator.next();
                Entity entity = plugin.getServer().getEntity(itemId);
                if (entity == null || !entity.isValid()) {
                    itemIterator.remove();
                }
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // Каждые 5 минут
    }
}