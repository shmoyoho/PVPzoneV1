package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ContainerProtectionListener implements Listener {
    private final Main plugin;

    public ContainerProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    // Запрещаем ОТКРЫВАТЬ контейнеры в зоне
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        // Проверяем, находится ли игрок в зоне
        if (!plugin.getZoneManager().isPlayerInZone(player)) {
            return;
        }

        // Проверяем, является ли блок контейнером
        if (isContainer(clickedBlock)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя открывать контейнеры находясь в PvP зоне!");

            // Дополнительный эффект для наглядности
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // Запрещаем ВЗАИМОДЕЙСТВОВАТЬ с контейнерами в зоне
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Проверяем, находится ли игрок в зоне
        if (!plugin.getZoneManager().isPlayerInZone(player)) {
            return;
        }

        // Запрещаем взаимодействие с контейнерами в зоне
        if (isContainerInventory(event.getInventory().getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя использовать контейнеры в PvP зоне!");
            player.closeInventory();
        }
    }

    // Проверяем, является ли блок контейнером
    private boolean isContainer(Block block) {
        return block.getState() instanceof Container;
    }

    // Проверяем, является ли инвентарь контейнером
    private boolean isContainerInventory(InventoryType type) {
        return type == InventoryType.CHEST ||
                type == InventoryType.BARREL ||
                type == InventoryType.SHULKER_BOX ||
                type == InventoryType.ENDER_CHEST ||
                type == InventoryType.HOPPER ||
                type == InventoryType.DROPPER ||
                type == InventoryType.DISPENSER ||
                type == InventoryType.FURNACE ||
                type == InventoryType.BLAST_FURNACE ||
                type == InventoryType.SMOKER ||
                type == InventoryType.BREWING ||
                type == InventoryType.LECTERN;
    }
}